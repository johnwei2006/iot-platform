<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { ElMessage } from 'element-plus';
import { listDevices } from '@/api/iot/gateway';
import { checkRule, createRule, deleteRule, listRules, toggleRule, updateRule } from '@/api/iot/control';
import type { ConflictResult, ControlRule, DeviceEntry } from '@/types/kb';
import type { Device } from '@/types';

const { t } = useI18n();
const loading = ref(false);
const rules = ref<ControlRule[]>([]);
const devices = ref<Device[]>([]);
const subTab = ref<'TIMED' | 'CONDITION'>('CONDITION');

const formVisible = ref(false);
const saving = ref(false);
const conflict = ref<ConflictResult | null>(null);
const editingId = ref<string | null>(null);
const intent = ref<'DRAFT' | 'ENABLE'>('DRAFT');

const METRIC_KEYS: Record<string, string> = { temp: 'control.metricTemp', humid: 'control.metricHumid', press: 'control.metricPress' };

const form = reactive({
  name: '',
  kind: 'CONDITION' as 'TIMED' | 'CONDITION',
  action: 'ON' as 'ON' | 'OFF',
  selected: [] as string[],
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
    .filter((d) => form.selected.includes(d.deviceId))
    .map((d) => ({ deviceId: d.deviceId, channel: 1, deviceName: d.name || d.deviceId, deviceType: t('device.title') }));
}

function deviceName(deviceId?: string) {
  if (!deviceId) return '--';
  return devices.value.find((d) => d.deviceId === deviceId)?.name || deviceId;
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
  const name = deviceName(r.metricDeviceId);
  return `${prefix}${name} ${metric} ${operator} ${threshold}`;
}

function timeRange(r: ControlRule) {
  if (r.kind === 'TIMED') {
    const close = r.closeTime ? `~${r.closeTime}` : '';
    return `${r.time || '--'}${close}`;
  }
  return `${r.startTime || '00:00'} ~ ${r.endTime || '23:59'}`;
}

function resetForm(kind: 'TIMED' | 'CONDITION') {
  editingId.value = null;
  conflict.value = null;
  Object.assign(form, {
    name: '', kind, action: 'ON', selected: [], time: '07:00', closeTime: '',
    metric: 'temp', metricDeviceId: devices.value[0]?.deviceId || '', operator: '>',
    threshold: 32, startTime: '00:00', endTime: '23:59', useReturn: false, returnThreshold: 30,
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
  intent.value = 'DRAFT';
  formVisible.value = true;
}

function payload() {
  const isCondition = form.kind === 'CONDITION';
  return {
    name: form.name, kind: form.kind, trigger: form.kind,
    devices: JSON.stringify(deviceEntries()), action: form.action,
    time: !isCondition ? form.time : undefined,
    closeTime: !isCondition ? form.closeTime || undefined : undefined,
    metric: isCondition ? form.metric : undefined,
    metricDeviceId: isCondition ? form.metricDeviceId : undefined,
    operator: isCondition ? form.operator : undefined,
    threshold: isCondition ? form.threshold : undefined,
    startTime: isCondition ? form.startTime : undefined,
    endTime: isCondition ? form.endTime : undefined,
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
    const [ruleRes, devRes] = await Promise.all([listRules(), listDevices()]);
    rules.value = ruleRes.data || [];
    devices.value = devRes.data || [];
    if (!form.metricDeviceId && devices.value.length) form.metricDeviceId = devices.value[0].deviceId;
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
          <template #default>--</template>
        </el-table-column>
        <el-table-column :label="$t('control.expiryDate')" min-width="120">
          <template #default>--</template>
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

    <el-dialog v-model="formVisible" :title="`${editingId ? $t('common.edit') : $t('control.create')}${form.kind === 'CONDITION' ? $t('control.conditionWord') : $t('control.timedWord')}`" width="560px">
      <el-form label-width="110px">
        <el-form-item :label="$t('common.name')"><el-input v-model="form.name" maxlength="40" :placeholder="$t('control.namePlaceholder')" /></el-form-item>
        <el-form-item :label="$t('control.targetDevices')">
          <el-select v-model="form.selected" multiple filterable style="width: 100%" :placeholder="$t('control.selectDevices')">
            <el-option v-for="d in devices" :key="d.deviceId" :value="d.deviceId" :label="d.name || d.deviceId" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('control.action')">
          <el-radio-group v-model="form.action">
            <el-radio value="ON">{{ $t('control.statusOn') }}</el-radio>
            <el-radio value="OFF">{{ $t('control.statusOff') }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <template v-if="form.kind === 'TIMED'">
          <el-form-item :label="$t('control.execTime')"><el-time-picker v-model="form.time" format="HH:mm" :placeholder="$t('control.selectTime')" /></el-form-item>
          <el-form-item :label="$t('control.reverseTime')"><el-time-picker v-model="form.closeTime" format="HH:mm" :placeholder="$t('control.reverseTimePlaceholder')" /></el-form-item>
        </template>
        <template v-else>
          <el-form-item :label="$t('control.metricDevice')">
            <el-select v-model="form.metricDeviceId" filterable style="width: 100%">
              <el-option v-for="d in devices" :key="d.deviceId" :value="d.deviceId" :label="d.name || d.deviceId" />
            </el-select>
          </el-form-item>
          <el-form-item :label="$t('control.metric')">
            <el-select v-model="form.metric" style="width: 100%">
              <el-option value="temp" :label="$t('control.metricTemp')" /><el-option value="humid" :label="$t('control.metricHumid')" /><el-option value="press" :label="$t('control.metricPress')" />
            </el-select>
          </el-form-item>
          <el-form-item :label="$t('control.triggerCondition')">
            <el-select v-model="form.operator" style="width: 90px">
              <el-option value=">" :label="$t('control.opAbove')" /><el-option value=">=" :label="$t('control.opAboveEq')" />
              <el-option value="<" :label="$t('control.opBelow')" /><el-option value="<=" :label="$t('control.opBelowEq')" />
            </el-select>
            <el-input-number v-model="form.threshold" style="width: 140px; margin-left: 8px" />
          </el-form-item>
          <el-form-item :label="$t('control.activePeriod')">
            <el-time-picker v-model="form.startTime" format="HH:mm" style="width: 130px" />
            <span style="margin: 0 6px">{{ $t('control.to') }}</span>
            <el-time-picker v-model="form.endTime" format="HH:mm" style="width: 130px" />
          </el-form-item>
          <el-form-item :label="$t('control.useReturn')">
            <el-switch v-model="form.useReturn" />
            <el-input-number v-if="form.useReturn" v-model="form.returnThreshold" style="width: 140px; margin-left: 12px" />
          </el-form-item>
        </template>
      </el-form>

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
.empty-hint { padding: 16px 0; text-align: center; color: var(--kb-text-muted); font-size: 12px; }
.hit-row { padding: 6px 10px; border-bottom: 1px solid var(--kb-line); font-size: var(--kb-fs-caption); color: var(--kb-text-muted); }
</style>
