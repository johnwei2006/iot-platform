<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { ElMessage } from 'element-plus';
import { listControlDevices, type DeviceCtrlEntity } from '@/api/iot/device';
import { listAreas, type AreaEntity } from '@/api/iot/area';
import { listSensors, type SensorEntity } from '@/api/iot/sensor';
import { checkRule, createRule, deleteRule, listRules, toggleRule, updateRule } from '@/api/iot/control';
import type { ConflictResult, ControlRule, DeviceEntry } from '@/types/kb';

const { t } = useI18n();
const loading = ref(false);
const rules = ref<ControlRule[]>([]);
const devices = ref<DeviceCtrlEntity[]>([]);
const areas = ref<AreaEntity[]>([]);
const sensors = ref<SensorEntity[]>([]);
const subTab = ref<'TIMED' | 'CONDITION'>('CONDITION');

const formVisible = ref(false);
const deviceKeyword = ref('');
const saving = ref(false);
const conflict = ref<ConflictResult | null>(null);
const editingId = ref<string | null>(null);
const intent = ref<'DRAFT' | 'ENABLE'>('DRAFT');

const METRIC_KEYS: Record<string, string> = { temp: 'control.metricTemp', humid: 'control.metricHumid', press: 'control.metricPress' };

interface TimeSlot { open: string; close: string }

const form = reactive({
  name: '',
  kind: 'CONDITION' as 'TIMED' | 'CONDITION',
  action: 'ON' as 'ON' | 'OFF',
  selected: [] as string[],
  effectiveFrom: '',
  effectiveTo: '',
  timeSlots: [{ open: '07:00', close: '' }] as TimeSlot[],
  time: '07:00',
  closeTime: '',
  metric: 'temp',
  metricDeviceId: '',
  operator: '>',
  threshold: 32,
  startTime: '00:00',
  endTime: '23:59',
  useReturn: false,
  returnThreshold: 30,
});

const filtered = computed(() => rules.value.filter((r) => r.kind === subTab.value));

function deviceEntries(): DeviceEntry[] {
  return devices.value
    .filter((d) => form.selected.includes(d.deviceCode))
    .map((d) => ({
      deviceId: d.deviceCode,
      nodeId: d.nodeCode || undefined,
      channel: Number(d.channel) || 1,
      deviceName: d.deviceName || d.deviceCode,
      deviceType: d.deviceClass || t('device.title'),
    }));
}

/** 定时控制的可选设备：按区域分组的节点下控制设备（非网关）。 */
const deviceTree = computed(() => {
  const byArea = new Map<string, DeviceCtrlEntity[]>();
  for (const d of devices.value) {
    const key = d.areaId || '';
    if (!byArea.has(key)) byArea.set(key, []);
    byArea.get(key)!.push(d);
  }
  const nodes = areas.value.map((a) => ({
    id: a.id,
    label: a.areaName,
    children: (byArea.get(a.id) || []).map((d) => ({
      id: d.deviceCode,
      label: d.deviceName || d.deviceCode,
    })),
  }));
  const unassigned = byArea.get('') || [];
  if (unassigned.length) {
    nodes.push({
      id: '',
      label: t('control.unassignedArea'),
      children: unassigned.map((d) => ({ id: d.deviceCode, label: d.deviceName || d.deviceCode })),
    });
  }
  return nodes.filter((n) => n.children.length > 0);
});

const filteredDeviceTree = computed(() => {
  const kw = deviceKeyword.value.trim().toLowerCase();
  if (!kw) return deviceTree.value;
  return deviceTree.value
    .map((area) => ({
      ...area,
      children: area.children.filter((d) => d.label.toLowerCase().includes(kw)),
    }))
    .filter((area) => area.children.length > 0);
});

function onDeviceCheck(data: { id: string }, checked: boolean) {
  const code = data.id;
  if (checked && !form.selected.includes(code)) form.selected.push(code);
  if (!checked) form.selected = form.selected.filter((x) => x !== code);
}

/** 传感器大类 -> 条件规则指标键（与后端 ConditionRuleEvaluateHandler 保持一致）。 */
const SENSOR_CLASS_METRIC: Record<string, string> = { '20': 'temp', '21': 'humid', '22': 'press' };

interface SensorTreeNode { id: string; label: string; disabled?: boolean; children?: SensorTreeNode[] }

/** 条件取数传感器：按区域分组（仅可选传感器叶子节点，区域节点禁用）。 */
const sensorTree = computed<SensorTreeNode[]>(() => {
  const byArea = new Map<string, SensorEntity[]>();
  for (const s of sensors.value) {
    const key = s.areaId || '';
    if (!byArea.has(key)) byArea.set(key, []);
    byArea.get(key)!.push(s);
  }
  const nodes: SensorTreeNode[] = areas.value.map((a) => ({
    id: `area-${a.id}`,
    label: a.areaName,
    disabled: true,
    children: (byArea.get(a.id) || []).map((s) => ({
      id: s.sensorCode,
      label: s.sensorName ? `${s.sensorCode} - ${s.sensorName}` : s.sensorCode,
    })),
  }));
  const unassigned = byArea.get('') || [];
  if (unassigned.length) {
    nodes.push({
      id: 'area-unassigned',
      label: t('control.unassignedArea'),
      disabled: true,
      children: unassigned.map((s) => ({
        id: s.sensorCode,
        label: s.sensorName ? `${s.sensorCode} - ${s.sensorName}` : s.sensorCode,
      })),
    });
  }
  return nodes.filter((n) => (n.children?.length || 0) > 0);
});

function onSensorChange(code: string) {
  const sensor = sensors.value.find((s) => s.sensorCode === code);
  if (sensor) form.metric = SENSOR_CLASS_METRIC[sensor.sensorClass || ''] || 'temp';
}

function sensorName(code?: string) {
  if (!code) return '--';
  return sensors.value.find((s) => s.sensorCode === code)?.sensorName || code;
}

function addTimeSlot() {
  form.timeSlots.push({ open: '', close: '' });
}

function removeTimeSlot(index: number) {
  form.timeSlots.splice(index, 1);
}

function metricLabel(metric?: string) {
  if (!metric) return '';
  const key = METRIC_KEYS[metric];
  return key ? t(key) : metric;
}

function conditionText(r: ControlRule, close: boolean) {
  const metric = metricLabel(r.metric);
  const operator = r.operator || '>';
  const threshold = r.threshold ?? '--';
  const prefix = close && r.useReturn ? `${t('control.returnPrefix')} ` : '';
  const name = sensorName(r.metricDeviceId);
  return `${prefix}${name} ${metric} ${operator} ${threshold}`;
}

function effectiveDateOf(r: ControlRule) {
  return r.kind === 'TIMED' ? (r.startTime || '--') : (r.effectiveFrom || '--');
}

function expiryDateOf(r: ControlRule) {
  return r.kind === 'TIMED' ? (r.endTime || '--') : (r.effectiveTo || '--');
}

function timeRange(r: ControlRule) {
  if (r.kind === 'TIMED') {
    const close = r.closeTime ? `~${r.closeTime}` : '';
    return `${r.time || '--'}${close}`;
  }
  return `${r.startTime || '00:00'} ~ ${r.endTime || '23:59'}`;
}

function todayStr() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

function resetForm(kind: 'TIMED' | 'CONDITION') {
  editingId.value = null;
  conflict.value = null;
  deviceKeyword.value = '';
  Object.assign(form, {
    name: '', kind, action: 'ON', selected: [],
    timeSlots: [{ open: '07:00', close: '' }], time: '07:00', closeTime: '',
    metric: 'temp', metricDeviceId: sensors.value[0]?.sensorCode || '', operator: '>',
    threshold: 32, startTime: '00:00', endTime: '23:59',
    effectiveFrom: kind === 'CONDITION' ? todayStr() : '', effectiveTo: '',
    useReturn: false, returnThreshold: 30,
  });
}

function openCreate() {
  resetForm(subTab.value);
  intent.value = 'DRAFT';
  formVisible.value = true;
}

function openEdit(r: ControlRule) {
  editingId.value = r.id || null;
  conflict.value = null;
  const entries = JSON.parse(r.devices || '[]') as DeviceEntry[];
  Object.assign(form, {
    name: r.name, kind: r.kind === 'CONDITION' ? 'CONDITION' : 'TIMED',
    action: (r.action as 'ON' | 'OFF') || 'ON', selected: entries.map((e) => e.deviceId),
    time: r.time || '07:00', closeTime: r.closeTime || '',
    metric: r.metric || 'temp', metricDeviceId: r.metricDeviceId || '', operator: r.operator || '>',
    threshold: Number(r.threshold) || 32, startTime: r.startTime || '00:00', endTime: r.endTime || '23:59',
    useReturn: !!r.useReturn, returnThreshold: Number(r.returnThreshold) || 30,
  });
  form.effectiveFrom = r.effectiveFrom || (form.kind === 'TIMED' && /^\d{4}-\d{2}-\d{2}$/.test(r.startTime || '') ? r.startTime! : '');
  form.effectiveTo = r.effectiveTo || (form.kind === 'TIMED' && /^\d{4}-\d{2}-\d{2}$/.test(r.endTime || '') ? r.endTime! : '');
  if (form.kind === 'CONDITION') onSensorChange(form.metricDeviceId);
  if (form.kind === 'TIMED') {
    const opens = (r.time || '07:00').split(',');
    const closes = (r.closeTime || '').split(',');
    form.timeSlots = opens.map((open, i) => ({ open, close: closes[i] || '' }));
  }
  intent.value = 'DRAFT';
  formVisible.value = true;
}

function payload() {
  const isCondition = form.kind === 'CONDITION';
  return {
    name: form.name, kind: form.kind, trigger: form.kind,
    devices: JSON.stringify(deviceEntries()), action: form.action,
    time: !isCondition ? form.timeSlots.map((s) => s.open).filter(Boolean).join(',') : undefined,
    closeTime: !isCondition ? form.timeSlots.map((s) => s.close).filter(Boolean).join(',') || undefined : undefined,
    metric: isCondition ? form.metric : undefined,
    metricDeviceId: isCondition ? form.metricDeviceId : undefined,
    operator: isCondition ? form.operator : undefined,
    threshold: isCondition ? form.threshold : undefined,
    startTime: isCondition ? form.startTime : form.effectiveFrom || undefined,
    endTime: isCondition ? form.endTime : form.effectiveTo || undefined,
    effectiveFrom: isCondition ? form.effectiveFrom || undefined : undefined,
    effectiveTo: isCondition ? form.effectiveTo || undefined : undefined,
    useReturn: isCondition ? form.useReturn : undefined,
    returnThreshold: isCondition && form.useReturn ? form.returnThreshold : undefined,
  };
}

async function doCheck() {
  conflict.value = null;
  try {
    const res = await checkRule(payload());
    conflict.value = res.data || { error: t('control.checkFailed'), hits: [] };
  } catch (error) {
    ElMessage.error((error as Error).message || t('control.checkFailed'));
  }
}

async function doSave(withEnable: boolean) {
  saving.value = true;
  try {
    intent.value = withEnable ? 'ENABLE' : 'DRAFT';
    const body = payload();
    if (editingId.value) await updateRule(editingId.value, body, intent.value);
    else await createRule(body, intent.value);
    ElMessage.success(withEnable ? t('control.savedEnabled') : t('control.savedDraft'));
    formVisible.value = false;
    await refresh();
  } catch (error) {
    ElMessage.error((error as Error).message || t('gateway.saveFailed'));
  } finally {
    saving.value = false;
  }
}

async function doToggle(r: ControlRule) {
  try {
    await toggleRule(r.id!);
    ElMessage.success(r.enabled ? t('control.disabled') : t('control.savedEnabled'));
    await refresh();
  } catch (error) {
    ElMessage.error((error as Error).message || t('common.failed'));
  }
}

async function doDelete(r: ControlRule) {
  try {
    await deleteRule(r.id!);
    ElMessage.success(t('control.deleted'));
    await refresh();
  } catch (error) {
    ElMessage.error((error as Error).message || t('control.deleteFailed'));
  }
}

async function refresh() {
  loading.value = true;
  try {
    const [ruleRes, devRes, areaRes, sensorRes] = await Promise.all([
      listRules(), listControlDevices(), listAreas(), listSensors(),
    ]);
    rules.value = ruleRes.data || [];
    devices.value = devRes.data || [];
    areas.value = areaRes.data || [];
    sensors.value = sensorRes.data || [];
    if (!form.metricDeviceId && sensors.value.length) form.metricDeviceId = sensors.value[0].sensorCode;
  } catch (error) {
    ElMessage.error((error as Error).message || t('control.loadFailed'));
  } finally {
    loading.value = false;
  }
}

onMounted(refresh);
</script>

<template>
  <div v-loading="loading" class="panel">
    <el-card shadow="never">
      <template #header>
        <div class="head-row">
          <div class="sub-tabs" role="tablist">
            <button type="button" class="sub-tab" :class="{ active: subTab === 'TIMED' }" @click="subTab = 'TIMED'">{{ $t('control.tabTimed') }}</button>
            <button type="button" class="sub-tab" :class="{ active: subTab === 'CONDITION' }" @click="subTab = 'CONDITION'">{{ $t('control.tabCondition') }}</button>
          </div>
          <el-button type="primary" v-permission="'iot:rule:create'" @click="openCreate">{{ $t('control.addRule') }}</el-button>
        </div>
      </template>

      <el-table :data="filtered" stripe border>
        <el-table-column :label="$t('control.ruleName')" prop="name" min-width="180" show-overflow-tooltip />
        <el-table-column :label="$t('control.effectiveDate')" min-width="120">
          <template #default="{ row }">{{ effectiveDateOf(row) }}</template>
        </el-table-column>
        <el-table-column :label="$t('control.expiryDate')" min-width="120">
          <template #default="{ row }">{{ expiryDateOf(row) }}</template>
        </el-table-column>
        <el-table-column :label="$t('control.dailyRange')" min-width="170">
          <template #default="{ row }">{{ timeRange(row) }}</template>
        </el-table-column>
        <el-table-column v-if="subTab === 'CONDITION'" :label="$t('control.openCondition')" min-width="220">
          <template #default="{ row }">{{ conditionText(row, false) }}</template>
        </el-table-column>
        <el-table-column v-if="subTab === 'CONDITION'" :label="$t('control.closeCondition')" min-width="220">
          <template #default="{ row }">{{ conditionText(row, true) }}</template>
        </el-table-column>
        <el-table-column v-else :label="$t('control.action')" min-width="140">
          <template #default="{ row }">{{ row.action === 'ON' ? $t('control.statusOn') : $t('control.statusOff') }}</template>
        </el-table-column>
        <el-table-column :label="$t('common.status')" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" size="small" effect="light">{{ row.enabled ? $t('common.enable') : $t('control.statusOff') }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('common.actions')" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" v-permission="'iot:rule:toggle'" @click="doToggle(row)">{{ row.enabled ? $t('common.disable') : $t('common.enable') }}</el-button>
            <el-button link type="primary" v-permission="'iot:rule:edit'" @click="openEdit(row)">{{ $t('common.edit') }}</el-button>
            <el-button link type="danger" v-permission="'iot:rule:edit'" @click="doDelete(row)">{{ $t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="empty-hint" v-if="!filtered.length">{{ $t('control.emptyHint') }}</div>
    </el-card>

    <el-dialog
      v-model="formVisible"
      :title="`${editingId ? $t('common.edit') : $t('control.create')}${form.kind === 'CONDITION' ? $t('control.conditionWord') : $t('control.timedWord')}`"
      width="980px"
    >
      <template v-if="form.kind === 'TIMED'">
        <div class="create-layout">
          <div class="device-column">
            <div class="column-title">{{ $t('control.deviceList') }}</div>
            <el-input v-model="deviceKeyword" :placeholder="$t('control.searchDevice')" clearable style="margin-bottom: 8px" />
            <el-tree
              :data="filteredDeviceTree"
              show-checkbox
              node-key="id"
              :default-checked-keys="form.selected"
              :props="{ label: 'label', children: 'children' }"
              @check="onDeviceCheck"
            />
          </div>
          <div class="form-column">
            <el-form label-width="120px">
              <el-form-item :label="$t('control.ruleName')">
                <el-input v-model="form.name" maxlength="40" :placeholder="$t('control.namePlaceholder')" />
              </el-form-item>
              <el-form-item :label="$t('control.effectiveDate')">
                <el-date-picker
                  v-model="form.effectiveFrom"
                  type="date"
                  value-format="YYYY-MM-DD"
                  :placeholder="$t('control.startTime')"
                  style="width: 155px"
                />
                <span style="margin: 0 8px">{{ $t('control.to') }}</span>
                <el-date-picker
                  v-model="form.effectiveTo"
                  type="date"
                  value-format="YYYY-MM-DD"
                  :placeholder="$t('control.endTime')"
                  style="width: 155px"
                />
              </el-form-item>
              <el-form-item v-for="(slot, index) in form.timeSlots" :key="index" :label="index === 0 ? $t('control.dailyOpen') : ''">
                <el-time-picker v-model="slot.open" value-format="HH:mm" format="HH:mm" :placeholder="$t('control.selectTime')" style="width: 155px" />
                <span style="margin: 0 8px">{{ $t('control.dailyClose') }}</span>
                <el-time-picker v-model="slot.close" value-format="HH:mm" format="HH:mm" :placeholder="$t('control.selectTime')" style="width: 155px" />
                <el-button v-if="form.timeSlots.length > 1" link type="danger" @click="removeTimeSlot(index)">{{ $t('common.delete') }}</el-button>
              </el-form-item>
              <el-form-item>
                <el-button link type="primary" @click="addTimeSlot">+ {{ $t('control.addTime') }}</el-button>
              </el-form-item>
              <el-form-item :label="$t('control.openSetting')">
                <el-switch :model-value="form.action === 'ON'" @change="(v: boolean) => form.action = v ? 'ON' : 'OFF'" />
              </el-form-item>
            </el-form>
          </div>
        </div>
      </template>
      <template v-else>
        <div class="create-layout">
          <div class="device-column">
            <div class="column-title">{{ $t('control.deviceList') }}</div>
            <el-input v-model="deviceKeyword" :placeholder="$t('control.searchDevice')" clearable style="margin-bottom: 8px" />
            <el-tree
              :data="filteredDeviceTree"
              show-checkbox
              node-key="id"
              :default-checked-keys="form.selected"
              :props="{ label: 'label', children: 'children' }"
              @check="onDeviceCheck"
            />
          </div>
          <div class="form-column">
            <el-form label-width="120px">
              <el-form-item :label="$t('control.ruleName')">
                <el-input v-model="form.name" maxlength="40" :placeholder="$t('control.namePlaceholder')" />
              </el-form-item>
              <el-form-item :label="$t('control.action')">
                <el-radio-group v-model="form.action">
                  <el-radio value="ON">{{ $t('control.statusOn') }}</el-radio>
                  <el-radio value="OFF">{{ $t('control.statusOff') }}</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item :label="$t('control.metricDevice')">
                <el-tree-select
                  v-model="form.metricDeviceId"
                  :data="sensorTree"
                  node-key="id"
                  :props="{ label: 'label', children: 'children', disabled: 'disabled' }"
                  check-strictly
                  filterable
                  default-expand-all
                  style="width: 100%"
                  :placeholder="$t('control.selectSensor')"
                  @change="onSensorChange"
                />
              </el-form-item>
              <el-form-item :label="$t('control.effectiveDate')">
                <el-date-picker
                  v-model="form.effectiveFrom"
                  type="date"
                  value-format="YYYY-MM-DD"
                  :placeholder="$t('control.startTime')"
                  style="width: 155px"
                />
                <span style="margin: 0 8px">{{ $t('control.to') }}</span>
                <el-date-picker
                  v-model="form.effectiveTo"
                  type="date"
                  value-format="YYYY-MM-DD"
                  :placeholder="$t('control.endTime')"
                  style="width: 155px"
                />
              </el-form-item>
              <el-form-item :label="$t('control.triggerCondition')">
                <el-select v-model="form.operator" style="width: 100px">
                  <el-option value=">" :label="$t('control.opAbove')" />
                  <el-option value=">=" :label="$t('control.opAboveEq')" />
                  <el-option value="<" :label="$t('control.opBelow')" />
                  <el-option value="<=" :label="$t('control.opBelowEq')" />
                </el-select>
                <el-input-number v-model="form.threshold" style="width: 150px; margin-left: 8px" />
              </el-form-item>
              <el-form-item :label="$t('control.activePeriod')">
                <el-time-picker v-model="form.startTime" value-format="HH:mm" format="HH:mm" style="width: 130px" />
                <span style="margin: 0 6px">{{ $t('control.to') }}</span>
                <el-time-picker v-model="form.endTime" value-format="HH:mm" format="HH:mm" style="width: 130px" />
              </el-form-item>
              <el-form-item :label="$t('control.useReturn')">
                <el-switch v-model="form.useReturn" />
                <el-input-number v-if="form.useReturn" v-model="form.returnThreshold" style="width: 150px; margin-left: 12px" />
              </el-form-item>
            </el-form>
          </div>
        </div>
      </template>

      <el-alert v-if="conflict" :type="conflict.error ? 'warning' : conflict.hits.some((h) => h.hard) ? 'error' : conflict.hits.length ? 'warning' : 'success'"
        :closable="false" show-icon
        :title="conflict.error || (conflict.hits.some((h) => h.hard) ? $t('control.hasConflict') : conflict.hits.length ? $t('control.hasDuplicate') : $t('control.checkPassed'))"
        style="margin-bottom: 12px" />
      <div v-for="h in conflict?.hits || []" :key="h.ruleName" class="hit-row">{{ h.ruleName }} · {{ h.detail }}</div>

      <template #footer>
        <el-button v-permission="'iot:rule:check'" :disabled="saving" @click="doCheck">{{ $t('control.checkConflict') }}</el-button>
        <el-button v-permission="'iot:rule:edit'" :disabled="saving || (conflict?.hits.some((h) => h.hard) || false)" @click="doSave(false)">{{ $t('control.saveDraft') }}</el-button>
        <el-button type="primary" v-permission="'iot:rule:edit'" :disabled="saving || (conflict?.hits.some((h) => h.hard) || false)" @click="doSave(true)">{{ $t('control.saveEnable') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.panel { min-height: 400px; }
.head-row { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.sub-tabs { display: flex; gap: 4px; padding: 4px; border-radius: 9px; background: var(--kb-fill-soft); }
.sub-tab { padding: 6px 18px; border: 0; border-radius: 7px; background: transparent; color: var(--kb-text-muted); font-size: 13px; cursor: pointer; transition: all 0.2s ease; }
.sub-tab:hover { color: var(--kb-primary); }
.sub-tab.active { background: #fff; color: var(--kb-primary); font-weight: 700; box-shadow: var(--kb-shadow-1); }
.create-layout { display: grid; grid-template-columns: 300px 1fr; gap: 20px; min-height: 420px; }
.device-column { border-right: 1px solid var(--kb-line); padding-right: 16px; max-height: 460px; overflow: auto; }
.form-column { min-width: 0; }
.column-title { font-size: var(--kb-fs-subheading); font-weight: 600; margin-bottom: 10px; }
.empty-hint { padding: 16px 0; text-align: center; color: var(--kb-text-muted); font-size: 12px; }
.hit-row { padding: 6px 10px; border-bottom: 1px solid var(--kb-line); font-size: var(--kb-fs-caption); color: var(--kb-text-muted); }
</style>
