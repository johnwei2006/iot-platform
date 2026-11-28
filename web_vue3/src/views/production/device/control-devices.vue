<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Search } from '@element-plus/icons-vue';
import type { DeviceCtrlEntity, DeviceClassDict, DeviceClassTypeDict } from '@/api/iot/device';
import { listDeviceClass, listDeviceClassType } from '@/api/iot/device';
import type { NodeEntity } from '@/api/iot/node';
import {
  checkProductionCode,
  createProductionControlDevice,
  deleteProductionControlDevice,
  getNextProductionCode,
  listProductionNodes,
  listProductionControlDevices,
  updateProductionControlDevice,
  type ProductionKind,
} from '@/api/iot/production';
import { fetchOrgList } from '@/api/system/org';
import type { OrgRecord } from '@/types';

const { t } = useI18n();
const loading = ref(false);
const devices = ref<DeviceCtrlEntity[]>([]);
const nodes = ref<NodeEntity[]>([]);
const classes = ref<DeviceClassDict[]>([]);
const classTypes = ref<DeviceClassTypeDict[]>([]);
const orgs = ref<OrgRecord[]>([]);
const keyword = ref('');
const nodeFilter = ref('');
const classFilter = ref('');
const statusFilter = ref('');

const SWITCH_KEYS: Record<number, string> = { 1: 'device.switchMode_1', 2: 'device.switchMode_4', 3: 'device.switchMode_5' };
const nodeNameMap = computed(() => new Map(nodes.value.map((n) => [n.nodeCode, n.nodeName || n.nodeCode])));
const orgNameMap = computed(() => new Map(orgs.value.map((o) => [o.code, o.name])));

function nodeName(code?: string) {
  if (!code) return '--';
  return nodeNameMap.value.get(code) || code;
}

function orgName(code?: string) {
  if (!code) return '--';
  return orgNameMap.value.get(code) || code;
}

const isAssigned = (row: DeviceCtrlEntity) => !!row.orgCode;

function switchMode(row: DeviceCtrlEntity) {
  const mode = classes.value.find((c) => c.deviceClass === row.deviceClass)?.switchMode;
  if (!mode) return '--';
  const key = SWITCH_KEYS[mode];
  return key ? t(key) : String(mode);
}

function typeName(row: DeviceCtrlEntity) {
  const type = classTypes.value.find((c) => c.type === row.deviceClassType)?.typeName;
  if (type) return type;
  const cls = classes.value.find((c) => c.deviceClass === row.deviceClass)?.className;
  return cls || row.deviceClassType || row.deviceClass || '--';
}

const filtered = computed(() => devices.value.filter((d) => {
  const kw = keyword.value.trim().toLowerCase();
  const nf = nodeFilter.value.trim().toLowerCase();
  return (!kw || (d.deviceCode || '').toLowerCase().includes(kw) || (d.deviceName || '').toLowerCase().includes(kw))
    && (!nf || (d.nodeCode || '').toLowerCase().includes(nf) || nodeName(d.nodeCode).toLowerCase().includes(nf))
    && (!classFilter.value || d.deviceClass === classFilter.value)
    && (!statusFilter.value || isAssigned(d) === (statusFilter.value === 'ASSIGNED'));
}));

function resetFilter() {
  keyword.value = '';
  nodeFilter.value = '';
  classFilter.value = '';
  statusFilter.value = '';
}

async function load() {
  loading.value = true;
  try {
    const [devRes, nodeRes, classRes, typeRes, orgRes] = await Promise.all([
      listProductionControlDevices(), listProductionNodes(), listDeviceClass(), listDeviceClassType(), fetchOrgList(),
    ]);
    devices.value = devRes.data ?? [];
    nodes.value = nodeRes.data ?? [];
    classes.value = classRes.data ?? [];
    classTypes.value = typeRes.data ?? [];
    orgs.value = orgRes;
  } catch (error) {
    ElMessage.error((error as Error).message || t('production.loadFailed'));
  } finally {
    loading.value = false;
  }
}

const dialog = reactive({ visible: false, mode: 'create' as 'create' | 'edit', id: '' });
const form = reactive<{
  deviceCode: string; deviceName: string; deviceClass: string; deviceClassType: string; channel: string;
  nodeCode: string; connectType: number;
}>({
  deviceCode: '', deviceName: '', deviceClass: '', deviceClassType: '', channel: '', nodeCode: '', connectType: 0,
});
const KIND: ProductionKind = 'CTRL';
const codeError = ref('');
const codeDirty = ref(false);

const firstTypeOf = (clazz: string) =>
  classTypes.value.find((item) => !!clazz && String(item.type).startsWith(clazz))?.type || '';

async function refreshNextCode() {
  if (dialog.mode === 'edit' || codeDirty.value) return;
  try {
    const res = await getNextProductionCode(KIND, { nodeCode: form.nodeCode, channel: form.channel });
    form.deviceCode = res.data || '';
  } catch {
    // 预览失败时保留当前值，提交前仍会校验唯一性
  }
}

function onDeviceClassChange() {
  form.deviceClassType = firstTypeOf(form.deviceClass);
}

async function validateCode(): Promise<boolean> {
  const code = form.deviceCode.trim();
  if (!code) {
    codeError.value = t('production.codeRequired');
    return false;
  }
  try {
    const res = await checkProductionCode(KIND, code, dialog.mode === 'edit' ? dialog.id : undefined);
    const ok = !!res.data;
    codeError.value = ok ? '' : t('production.codeTaken');
    return ok;
  } catch {
    codeError.value = t('production.loadFailed');
    return false;
  }
}

async function openCreate() {
  dialog.mode = 'create';
  dialog.id = '';
  Object.assign(form, { deviceCode: '', deviceName: '', deviceClass: '', deviceClassType: '', channel: '', nodeCode: '', connectType: 0 });
  codeError.value = '';
  codeDirty.value = false;
  dialog.visible = true;
  await refreshNextCode();
}

function openEdit(row: DeviceCtrlEntity) {
  if (isAssigned(row)) return;
  dialog.mode = 'edit';
  dialog.id = row.id || '';
  Object.assign(form, {
    deviceCode: row.deviceCode || '', deviceName: row.deviceName || '', deviceClass: row.deviceClass || '',
    deviceClassType: row.deviceClassType || '', channel: row.channel || '', nodeCode: row.nodeCode || '',
    connectType: row.connectType ?? 0,
  });
  codeError.value = '';
  codeDirty.value = false;
  dialog.visible = true;
}

async function submit() {
  if (!form.deviceName.trim()) {
    ElMessage.warning(t('production.nameRequired'));
    return;
  }
  if (!form.deviceClass) {
    ElMessage.warning(t('production.classRequired'));
    return;
  }
  if (dialog.mode === 'create' && !form.nodeCode) {
    ElMessage.warning(t('production.nodeRequired'));
    return;
  }
  if (dialog.mode === 'create' && !String(form.channel).trim()) {
    ElMessage.warning(t('production.channelRequired'));
    return;
  }
  if (!form.deviceClassType) {
    form.deviceClassType = firstTypeOf(form.deviceClass);
  }
  if (!(await validateCode())) return;
  try {
    if (dialog.mode === 'create') {
      const res = await createProductionControlDevice({ ...form });
      ElMessage.success(t('production.created', { code: res.data?.deviceCode || '' }));
    } else {
      await updateProductionControlDevice(dialog.id, { ...form });
      ElMessage.success(t('gateway.saved'));
    }
    dialog.visible = false;
    await load();
  } catch (error) {
    ElMessage.error((error as Error).message || t('gateway.saveFailed'));
  }
}

async function remove(row: DeviceCtrlEntity) {
  if (isAssigned(row)) return;
  try {
    await ElMessageBox.confirm(t('production.deleteConfirm', { code: row.deviceCode }), t('common.deleteTitle'), { type: 'warning' });
  } catch {
    return;
  }
  try {
    await deleteProductionControlDevice(row.id!);
    ElMessage.success(t('control.deleted'));
    await load();
  } catch (error) {
    ElMessage.error((error as Error).message || t('control.deleteFailed'));
  }
}

onMounted(load);
</script>

<template>
  <div class="prod-page">
    <h2 class="page-title">{{ $t('production.ctrlTitle') }}</h2>

    <div class="filter-bar">
      <el-input v-model="keyword" class="keyword-input" clearable :placeholder="$t('production.searchCode')">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-input v-model="nodeFilter" class="node-input" clearable :placeholder="$t('production.searchNode')">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="classFilter" class="type-select" clearable :placeholder="$t('device.class')" :aria-label="$t('device.class')">
        <el-option v-for="c in classes" :key="c.deviceClass" :label="c.className" :value="c.deviceClass" />
      </el-select>
      <el-select v-model="statusFilter" class="status-select" clearable :placeholder="$t('production.assignStatus')" :aria-label="$t('production.assignStatus')">
        <el-option :label="$t('production.unassigned')" value="UNASSIGNED" />
        <el-option :label="$t('production.assigned')" value="ASSIGNED" />
      </el-select>
      <el-button type="primary" @click="load">{{ $t('common.search') }}</el-button>
      <el-button @click="resetFilter">{{ $t('common.reset') }}</el-button>
      <el-button type="primary" v-permission="'production:ctrldev:create'" @click="openCreate">{{ $t('production.create') }}</el-button>
    </div>

    <div class="table-card" v-loading="loading">
      <el-table :data="filtered" border style="width: 100%" :empty-text="$t('production.empty')">
        <el-table-column :label="$t('common.page')" type="index" width="64" align="center" />
        <el-table-column prop="deviceCode" :label="$t('device.code')" min-width="200">
          <template #default="{ row }"><span class="mono">{{ row.deviceCode }}</span></template>
        </el-table-column>
        <el-table-column prop="deviceName" :label="$t('device.name')" min-width="140" show-overflow-tooltip />
        <el-table-column :label="$t('device.type')" min-width="120">
          <template #default="{ row }">{{ typeName(row) }}</template>
        </el-table-column>
        <el-table-column :label="$t('device.switchMode')" width="100" align="center">
          <template #default="{ row }">{{ switchMode(row) }}</template>
        </el-table-column>
        <el-table-column :label="$t('production.slotPort')" width="90" align="center">
          <template #default="{ row }">{{ row.channel || '--' }}</template>
        </el-table-column>
        <el-table-column :label="$t('device.connectType')" width="110" align="center">
          <template #default="{ row }">{{ row.connectType === 1 ? $t('device.connect485') : $t('device.connectDirect') }}</template>
        </el-table-column>
        <el-table-column :label="$t('production.boundNode')" min-width="190">
          <template #default="{ row }">
            <span class="mono">{{ row.nodeCode || '--' }}</span>
            <span v-if="row.nodeCode" class="bind-name">（{{ nodeName(row.nodeCode) }}）</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" :label="$t('production.createDate')" min-width="170" sortable>
          <template #default="{ row }">{{ row.createTime ? row.createTime.replace('T', ' ').slice(0, 19) : '--' }}</template>
        </el-table-column>
        <el-table-column :label="$t('production.assignStatus')" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="isAssigned(row) ? 'success' : 'warning'" size="small">
              {{ isAssigned(row) ? $t('production.assigned') : $t('production.unassigned') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('production.orgName')" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ orgName(row.orgCode) }}</template>
        </el-table-column>
        <el-table-column :label="$t('common.actions')" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <template v-if="!isAssigned(row)">
              <el-button link type="primary" v-permission="'production:ctrldev:edit'" @click="openEdit(row)">{{ $t('common.edit') }}</el-button>
              <el-button link type="danger" v-permission="'production:ctrldev:delete'" @click="remove(row)">{{ $t('common.delete') }}</el-button>
            </template>
            <el-tooltip v-else :content="$t('production.assignedLockTip')" placement="top">
              <span class="locked">{{ $t('production.locked') }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialog.visible" :title="dialog.mode === 'create' ? $t('production.createCtrl') : $t('production.editCtrl')" width="520px">
      <el-alert v-if="dialog.mode === 'create'" :title="$t('production.codeAutoHint')" type="info" :closable="false" show-icon class="code-hint" />
      <el-form label-width="110px">
        <el-form-item :label="$t('production.code')" :error="codeError">
          <el-input v-model="form.deviceCode" :disabled="dialog.mode === 'edit'" @input="codeDirty = true; codeError = ''" @blur="validateCode()" />
        </el-form-item>
        <el-form-item :label="$t('device.name')"><el-input v-model="form.deviceName" :placeholder="$t('production.deviceNamePlaceholder')" /></el-form-item>
        <el-form-item :label="$t('device.class')">
          <el-select v-model="form.deviceClass" style="width: 100%" @change="onDeviceClassChange">
            <el-option v-for="c in classes" :key="c.deviceClass" :label="c.className" :value="c.deviceClass" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('device.type')">
          <el-select v-model="form.deviceClassType" clearable style="width: 100%">
            <el-option v-for="ct in classTypes.filter((item) => !form.deviceClass || String(item.type).startsWith(form.deviceClass))" :key="ct.type" :label="ct.typeName" :value="ct.type" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('production.slotPort')">
          <el-input v-model="form.channel" style="width: 140px" :placeholder="$t('production.channelPlaceholder')"
            @change="refreshNextCode" />
          <span class="form-tip">{{ $t('production.channelTip') }}</span>
        </el-form-item>
        <el-form-item :label="$t('production.boundNode')">
          <el-select v-model="form.nodeCode" clearable filterable style="width: 100%" :placeholder="$t('production.selectNode')"
            @change="refreshNextCode">
            <el-option v-for="n in nodes.filter((x) => !x.orgCode)" :key="n.nodeCode" :label="`${n.nodeCode}（${n.nodeName || ''}）`" :value="n.nodeCode" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="submit">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.prod-page { padding: 20px 24px 32px; }
.page-title { margin: 0 0 20px; font-size: 20px; font-weight: 600; color: #303133; }
.filter-bar { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin-bottom: 16px; }
.keyword-input { width: 200px; }
.node-input { width: 200px; }
.type-select { width: 160px; }
.status-select { width: 130px; }
.table-card { background: #fff; border: 1px solid #ebeef5; border-radius: 4px; padding: 4px; }
.mono { font-family: monospace; }
.bind-name { color: #909399; font-size: 12px; }
.code-hint { margin-bottom: 12px; }
.form-tip { margin-left: 10px; font-size: 12px; color: #909399; }
.locked { color: #909399; font-size: 12px; }
:deep(.el-table th.el-table__cell) { background: #f5f7fa; color: #606266; font-weight: 600; }
:deep(.el-table .cell) { font-size: 13px; }
</style>
