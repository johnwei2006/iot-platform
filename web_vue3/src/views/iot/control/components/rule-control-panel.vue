<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { listDevices } from '@/api/iot/gateway';
import { checkRule, createRule, deleteRule, listRules, toggleRule, updateRule } from '@/api/iot/control';
import type { ConflictResult, ControlRule, DeviceEntry } from '@/types/kb';
import type { Device } from '@/types';

const loading = ref(false);
const rules = ref<ControlRule[]>([]);
const devices = ref<Device[]>([]);
const subTab = ref<'TIMED' | 'CONDITION'>('CONDITION');

const formVisible = ref(false);
const saving = ref(false);
const conflict = ref<ConflictResult | null>(null);
const editingId = ref<string | null>(null);
const intent = ref<'DRAFT' | 'ENABLE'>('DRAFT');

const METRIC_LABELS: Record<string, string> = { temp: '空气温度', humid: '空气湿度', press: '空气气压' };

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
    .map((d) => ({ deviceId: d.deviceId, nodeId: d.nodeId || undefined, channel: 1, deviceName: d.name || d.deviceId, deviceType: '控制设备' }));
}

function deviceName(deviceId?: string) {
  if (!deviceId) return '--';
  return devices.value.find((d) => d.deviceId === deviceId)?.name || deviceId;
}

function conditionText(r: ControlRule, close: boolean) {
  const metric = r.metric ? (METRIC_LABELS[r.metric] || r.metric) : '';
  const operator = r.operator || '>';
  const threshold = r.threshold ?? '--';
  const prefix = close && r.useReturn ? '恢复 ' : '';
  const name = close ? deviceName(r.metricDeviceId) : deviceName(r.metricDeviceId);
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
    conflict.value = res.data || { error: '校验失败', hits: [] };
  } catch (error) {
    ElMessage.error((error as Error).message || '校验失败');
  }
}

async function doSave(withEnable: boolean) {
  saving.value = true;
  try {
    intent.value = withEnable ? 'ENABLE' : 'DRAFT';
    const body = payload();
    if (editingId.value) await updateRule(editingId.value, body, intent.value);
    else await createRule(body, intent.value);
    ElMessage.success(withEnable ? '已校验并启用' : '已保存停用草稿');
    formVisible.value = false;
    await refresh();
  } catch (error) {
    ElMessage.error((error as Error).message || '保存失败');
  } finally {
    saving.value = false;
  }
}

async function doToggle(r: ControlRule) {
  try {
    await toggleRule(r.id!);
    ElMessage.success(r.enabled ? '已停用' : '已校验并启用');
    await refresh();
  } catch (error) {
    ElMessage.error((error as Error).message || '操作失败');
  }
}

async function doDelete(r: ControlRule) {
  try {
    await deleteRule(r.id!);
    ElMessage.success('已删除');
    await refresh();
  } catch (error) {
    ElMessage.error((error as Error).message || '删除失败');
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
    ElMessage.error((error as Error).message || '规则加载失败');
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
            <button type="button" class="sub-tab" :class="{ active: subTab === 'TIMED' }" @click="subTab = 'TIMED'">定时控制</button>
            <button type="button" class="sub-tab" :class="{ active: subTab === 'CONDITION' }" @click="subTab = 'CONDITION'">条件控制</button>
          </div>
          <el-button type="primary" v-permission="'iot:rule:create'" @click="openCreate">增加控制</el-button>
        </div>
      </template>

      <el-table :data="filtered" stripe border>
        <el-table-column label="控制名称" prop="name" min-width="180" show-overflow-tooltip />
        <el-table-column label="生效日期" min-width="120">
          <template #default>--</template>
        </el-table-column>
        <el-table-column label="失效日期" min-width="120">
          <template #default>--</template>
        </el-table-column>
        <el-table-column label="每日生效时间范围" min-width="170">
          <template #default="{ row }">{{ timeRange(row) }}</template>
        </el-table-column>
        <el-table-column v-if="subTab === 'CONDITION'" label="开启条件" min-width="220">
          <template #default="{ row }">{{ conditionText(row, false) }}</template>
        </el-table-column>
        <el-table-column v-if="subTab === 'CONDITION'" label="关闭条件" min-width="220">
          <template #default="{ row }">{{ conditionText(row, true) }}</template>
        </el-table-column>
        <el-table-column v-else label="执行动作" min-width="140">
          <template #default="{ row }">{{ row.action === 'ON' ? '开启' : '关闭' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" size="small" effect="light">{{ row.enabled ? '启用' : '关闭' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" v-permission="'iot:rule:toggle'" @click="doToggle(row)">{{ row.enabled ? '停用' : '启用' }}</el-button>
            <el-button link type="primary" v-permission="'iot:rule:edit'" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" v-permission="'iot:rule:edit'" @click="doDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="empty-hint" v-if="!filtered.length">当前分类暂无控制规则，点击「增加控制」创建。</div>
    </el-card>

    <el-dialog v-model="formVisible" :title="`${editingId ? '编辑' : '新建'}${form.kind === 'CONDITION' ? '条件' : '定时'}控制`" width="560px">
      <el-form label-width="110px">
        <el-form-item label="名称"><el-input v-model="form.name" maxlength="40" placeholder="输入控制名称" /></el-form-item>
        <el-form-item label="执行设备">
          <el-select v-model="form.selected" multiple filterable style="width: 100%" placeholder="选择设备">
            <el-option v-for="d in devices" :key="d.deviceId" :value="d.deviceId" :label="d.name || d.deviceId" />
          </el-select>
        </el-form-item>
        <el-form-item label="执行动作">
          <el-radio-group v-model="form.action">
            <el-radio value="ON">开启</el-radio>
            <el-radio value="OFF">关闭</el-radio>
          </el-radio-group>
        </el-form-item>
        <template v-if="form.kind === 'TIMED'">
          <el-form-item label="执行时间"><el-time-picker v-model="form.time" format="HH:mm" placeholder="选择时间" /></el-form-item>
          <el-form-item label="反向时间（选填）"><el-time-picker v-model="form.closeTime" format="HH:mm" placeholder="不设置则留空" /></el-form-item>
        </template>
        <template v-else>
          <el-form-item label="取数设备">
            <el-select v-model="form.metricDeviceId" filterable style="width: 100%">
              <el-option v-for="d in devices" :key="d.deviceId" :value="d.deviceId" :label="d.name || d.deviceId" />
            </el-select>
          </el-form-item>
          <el-form-item label="指标">
            <el-select v-model="form.metric" style="width: 100%">
              <el-option value="temp" label="空气温度" /><el-option value="humid" label="空气湿度" /><el-option value="press" label="空气气压" />
            </el-select>
          </el-form-item>
          <el-form-item label="触发条件">
            <el-select v-model="form.operator" style="width: 90px">
              <el-option value=">" label="高于" /><el-option value=">=" label="不低于" />
              <el-option value="<" label="低于" /><el-option value="<=" label="不高于" />
            </el-select>
            <el-input-number v-model="form.threshold" style="width: 140px; margin-left: 8px" />
          </el-form-item>
          <el-form-item label="生效时段">
            <el-time-picker v-model="form.startTime" format="HH:mm" style="width: 130px" />
            <span style="margin: 0 6px">至</span>
            <el-time-picker v-model="form.endTime" format="HH:mm" style="width: 130px" />
          </el-form-item>
          <el-form-item label="恢复反向动作">
            <el-switch v-model="form.useReturn" />
            <el-input-number v-if="form.useReturn" v-model="form.returnThreshold" style="width: 140px; margin-left: 12px" />
          </el-form-item>
        </template>
      </el-form>

      <el-alert v-if="conflict" :type="conflict.error ? 'warning' : conflict.hits.some((h) => h.hard) ? 'error' : conflict.hits.length ? 'warning' : 'success'"
        :closable="false" show-icon
        :title="conflict.error || (conflict.hits.some((h) => h.hard) ? '存在控制冲突' : conflict.hits.length ? '存在同向重复' : '校验通过')"
        style="margin-bottom: 12px" />
      <div v-for="h in conflict?.hits || []" :key="h.ruleName" class="hit-row">{{ h.ruleName }} · {{ h.detail }}</div>

      <template #footer>
        <el-button v-permission="'iot:rule:check'" :disabled="saving" @click="doCheck">检查冲突</el-button>
        <el-button v-permission="'iot:rule:edit'" :disabled="saving || (conflict?.hits.some((h) => h.hard) || false)" @click="doSave(false)">保存草稿</el-button>
        <el-button type="primary" v-permission="'iot:rule:edit'" :disabled="saving || (conflict?.hits.some((h) => h.hard) || false)" @click="doSave(true)">保存并启用</el-button>
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
