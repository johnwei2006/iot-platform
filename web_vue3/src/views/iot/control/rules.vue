<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { listDevices } from '@/api/devices';
import { checkRule, createRule, deleteRule, listRules, toggleRule, updateRule } from '@/api/control';
import type { ConflictResult, ControlRule, DeviceEntry } from '@/types/kb';
import type { Device } from '@/types';

const loading = ref(false);
const rules = ref<ControlRule[]>([]);
const devices = ref<Device[]>([]);
const kindFilter = ref('全部');

const formVisible = ref(false);
const saving = ref(false);
const conflict = ref<ConflictResult | null>(null);
const editingId = ref<string | null>(null);
const intent = ref<'DRAFT' | 'ENABLE'>('DRAFT');

const form = reactive({
  name: '',
  kind: 'TIMED' as 'GROUP' | 'TIMED' | 'CONDITION',
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

const KIND_LABELS: Record<string, string> = { GROUP: '组合', TIMED: '定时', CONDITION: '条件' };

const filtered = computed(() =>
  rules.value.filter((r) => kindFilter.value === '全部' || r.kind === kindFilter.value)
);

function deviceEntries(): DeviceEntry[] {
  return devices.value
    .filter((d) => form.selected.includes(d.deviceId))
    .map((d) => ({ deviceId: d.deviceId, nodeId: d.nodeId || undefined, channel: 1, deviceName: d.name || d.deviceId, deviceType: '风机' }));
}

function summary(r: ControlRule) {
  if (r.trigger === 'MANUAL') return `手动整组${r.action === 'ON' ? '开启' : '关闭'} · ${r.devices.length} 台`;
  if (r.trigger === 'TIMED') {
    const close = r.closeTime ? `，${r.closeTime} ${r.action === 'ON' ? '关闭' : '开启'}` : '';
    return `每天 ${r.time} ${r.action === 'ON' ? '开启' : '关闭'}${close}`;
  }
  const ret = r.useReturn ? `，恢复至 ${r.returnThreshold} 时反向` : '';
  return `${r.startTime}-${r.endTime}，${r.metric} ${r.operator} ${r.threshold} 时${r.action === 'ON' ? '开启' : '关闭'}${ret}`;
}

function resetForm(kind: 'GROUP' | 'TIMED' | 'CONDITION') {
  editingId.value = null;
  conflict.value = null;
  Object.assign(form, {
    name: '', kind, action: 'ON', selected: [], time: '07:00', closeTime: '',
    metric: 'temp', metricDeviceId: devices.value[0]?.deviceId || '', operator: '>',
    threshold: 32, startTime: '00:00', endTime: '23:59', useReturn: false, returnThreshold: 30,
  });
}

function openCreate(kind: 'GROUP' | 'TIMED' | 'CONDITION') {
  resetForm(kind);
  intent.value = 'DRAFT';
  formVisible.value = true;
}

function openEdit(r: ControlRule) {
  editingId.value = r.id || null;
  conflict.value = null;
  const entries = JSON.parse(r.devices || '[]') as DeviceEntry[];
  Object.assign(form, {
    name: r.name, kind: r.kind, action: (r.action as 'ON' | 'OFF') || 'ON',
    selected: entries.map((e) => e.deviceId), time: r.time || '07:00', closeTime: r.closeTime || '',
    metric: r.metric || 'temp', metricDeviceId: r.metricDeviceId || '', operator: r.operator || '>',
    threshold: Number(r.threshold) || 32, startTime: r.startTime || '00:00', endTime: r.endTime || '23:59',
    useReturn: !!r.useReturn, returnThreshold: Number(r.returnThreshold) || 30,
  });
  intent.value = 'DRAFT';
  formVisible.value = true;
}

function payload() {
  return {
    name: form.name, kind: form.kind, trigger: form.kind,
    devices: JSON.stringify(deviceEntries()), action: form.action,
    time: form.kind === 'TIMED' ? form.time : undefined,
    closeTime: form.kind === 'TIMED' ? form.closeTime || undefined : undefined,
    metric: form.kind === 'CONDITION' ? form.metric : undefined,
    metricDeviceId: form.kind === 'CONDITION' ? form.metricDeviceId : undefined,
    operator: form.kind === 'CONDITION' ? form.operator : undefined,
    threshold: form.kind === 'CONDITION' ? form.threshold : undefined,
    startTime: form.kind === 'CONDITION' ? form.startTime : undefined,
    endTime: form.kind === 'CONDITION' ? form.endTime : undefined,
    useReturn: form.kind === 'CONDITION' ? form.useReturn : undefined,
    returnThreshold: form.kind === 'CONDITION' && form.useReturn ? form.returnThreshold : undefined,
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
    if (editingId.value) {
      await updateRule(editingId.value, body, intent.value);
    } else {
      await createRule(body, intent.value);
    }
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
  <div v-loading="loading">
    <el-card shadow="never">
      <template #header>
        <div class="head-bar">
          <el-segmented v-model="kindFilter" :options="['全部', 'GROUP', 'TIMED', 'CONDITION']" />
          <div class="head-actions">
            <el-button type="primary" v-permission="'iot:rule:create'" @click="openCreate('TIMED')">新建定时</el-button>
            <el-button type="primary" v-permission="'iot:rule:create'" @click="openCreate('CONDITION')">新建条件</el-button>
            <el-button type="primary" v-permission="'iot:rule:create'" @click="openCreate('GROUP')">新建组合</el-button>
          </div>
        </div>
      </template>
      <el-table :data="filtered" stripe>
        <el-table-column prop="name" label="规则名称" min-width="140" />
        <el-table-column label="类型" width="80">
          <template #default="{ row }">{{ KIND_LABELS[row.kind] }}</template>
        </el-table-column>
        <el-table-column label="规则摘要" min-width="260">
          <template #default="{ row }">{{ summary(row) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" size="small">{{ row.enabled ? '已启用' : '已停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button link type="primary" v-permission="'iot:rule:edit'" @click="openEdit(row)">编辑</el-button>
            <el-button link type="primary" v-permission="'iot:rule:toggle'" @click="doToggle(row)">{{ row.enabled ? '停用' : '校验并启用' }}</el-button>
            <el-button link type="danger" v-permission="'iot:rule:edit'" @click="doDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    <el-dialog v-model="formVisible" :title="`${editingId ? '编辑' : '新建'}${KIND_LABELS[form.kind]}控制`" width="560px">
      <el-form label-width="110px">
        <el-form-item label="名称"><el-input v-model="form.name" maxlength="40" /></el-form-item>
        <el-form-item label="执行设备">
          <el-select v-model="form.selected" multiple style="width: 100%">
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
          <el-form-item label="每天执行时间"><el-time-picker v-model="form.time" format="HH:mm" /></el-form-item>
          <el-form-item label="反向动作时间（选填）"><el-time-picker v-model="form.closeTime" format="HH:mm" placeholder="不设置则留空" /></el-form-item>
        </template>
        <template v-if="form.kind === 'CONDITION'">
          <el-form-item label="取数设备">
            <el-select v-model="form.metricDeviceId" style="width: 100%">
              <el-option v-for="d in devices" :key="d.deviceId" :value="d.deviceId" :label="d.name || d.deviceId" />
            </el-select>
          </el-form-item>
          <el-form-item label="指标">
            <el-select v-model="form.metric" style="width: 100%">
              <el-option value="temp" label="空气温度" /><el-option value="humid" label="空气湿度" />
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

      <el-alert v-if="conflict" :type="conflict.error ? 'warning' : conflict.hits.some((h) => h.hard) ? 'error' : conflict.hits.length ? 'warning' : 'success'" :closable="false" show-icon
        :title="conflict.error || (conflict.hits.some((h) => h.hard) ? '存在控制冲突' : conflict.hits.length ? '存在同向重复' : '校验通过')" style="margin-bottom: 12px" />
      <div v-for="h in conflict?.hits || []" :key="h.ruleName" class="hit-row">
        <span>{{ h.ruleName }} · {{ h.detail }}</span>
      </div>

      <template #footer>
        <el-button v-permission="'iot:rule:check'" :disabled="saving" @click="doCheck">检查冲突</el-button>
        <el-button v-permission="'iot:rule:edit'" :disabled="saving || (conflict?.hits.some((h) => h.hard) || false)" @click="doSave(false)">保存停用草稿</el-button>
        <el-button type="primary" v-permission="'iot:rule:edit'" :disabled="saving || (conflict?.hits.some((h) => h.hard) || false)" @click="doSave(true)">保存并启用</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.head-bar { display: flex; justify-content: space-between; align-items: center; }
.head-actions { display: flex; gap: 8px; }
.hit-row { padding: 6px 10px; border-bottom: 1px solid var(--kb-line); font-size: var(--kb-fs-caption); color: var(--kb-text-muted); }
</style>