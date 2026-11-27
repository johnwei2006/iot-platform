<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Search } from '@element-plus/icons-vue';
import type { NodeEntity } from '@/api/iot/node';
import {
  checkProductionCode,
  createProductionNode,
  deleteProductionNode,
  getNextProductionCode,
  listProductionGateways,
  listProductionNodes,
  updateProductionNode,
  type ProductionKind,
} from '@/api/iot/production';
import { fetchOrgList } from '@/api/system/org';
import type { Device, OrgRecord } from '@/types';

const { t } = useI18n();
const loading = ref(false);
const nodes = ref<NodeEntity[]>([]);
const gateways = ref<Device[]>([]);
const orgs = ref<OrgRecord[]>([]);
const keyword = ref('');
const typeFilter = ref<number | ''>('');
const gatewayFilter = ref('');
const statusFilter = ref('');

const NODE_TYPES: Record<number, string> = { 1: 'node.type_1', 2: 'node.type_2', 3: 'node.type_3', 4: 'node.type_4' };

const gatewayNameMap = computed(() => new Map(gateways.value.map((g) => [g.deviceId, g.name || g.deviceId])));
const orgNameMap = computed(() => new Map(orgs.value.map((o) => [o.code, o.name])));

function typeName(type?: number) {
  if (type === undefined || type === null) return '--';
  const label = NODE_TYPES[type];
  if (!label) return String(type);
  return label.startsWith('node.') ? t(label) : label;
}

function gatewayName(code?: string) {
  if (!code) return '--';
  return gatewayNameMap.value.get(code) || code;
}

function orgName(code?: string) {
  if (!code) return '--';
  return orgNameMap.value.get(code) || code;
}

const isAssigned = (row: NodeEntity) => !!row.orgCode;

function formatTime(value?: string) {
  if (!value) return '--';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

const filtered = computed(() => nodes.value.filter((n) => {
  const kw = keyword.value.trim().toLowerCase();
  return (!kw || (n.nodeCode || '').toLowerCase().includes(kw) || (n.nodeName || '').toLowerCase().includes(kw))
    && (typeFilter.value === '' || n.nodeType === typeFilter.value)
    && (!gatewayFilter.value || n.gatewayCode === gatewayFilter.value)
    && (!statusFilter.value || isAssigned(n) === (statusFilter.value === 'ASSIGNED'));
}));

function resetFilter() {
  keyword.value = '';
  typeFilter.value = '';
  gatewayFilter.value = '';
  statusFilter.value = '';
}

async function load() {
  loading.value = true;
  try {
    const [nodeRes, gwRes, orgRes] = await Promise.all([
      listProductionNodes(), listProductionGateways(), fetchOrgList(),
    ]);
    nodes.value = nodeRes.data ?? [];
    gateways.value = gwRes.data ?? [];
    orgs.value = orgRes;
  } catch (error) {
    ElMessage.error((error as Error).message || t('production.loadFailed'));
  } finally {
    loading.value = false;
  }
}

const dialog = reactive({ visible: false, mode: 'create' as 'create' | 'edit', id: '' });
const form = reactive<{ nodeCode: string; nodeName: string; nodeType: number; gatewayCode: string; firmwareVersion: string; reportInterval: number | undefined }>({
  nodeCode: '', nodeName: '', nodeType: 1, gatewayCode: '', firmwareVersion: '', reportInterval: undefined,
});
const KIND: ProductionKind = 'NODE';
const codeError = ref('');
const codeDirty = ref(false);

async function refreshNextCode() {
  if (dialog.mode === 'edit' || codeDirty.value) return;
  try {
    const res = await getNextProductionCode(KIND, { type: form.nodeType });
    form.nodeCode = res.data || '';
  } catch {
    // 预览失败时保留当前值，提交前仍会校验唯一性
  }
}

async function validateCode(): Promise<boolean> {
  const code = form.nodeCode.trim();
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
  form.nodeCode = '';
  form.nodeName = '';
  form.nodeType = 1;
  form.gatewayCode = '';
  form.firmwareVersion = '';
  form.reportInterval = undefined;
  codeError.value = '';
  codeDirty.value = false;
  dialog.visible = true;
  await refreshNextCode();
}

function openEdit(row: NodeEntity) {
  if (isAssigned(row)) return;
  dialog.mode = 'edit';
  dialog.id = row.id || '';
  form.nodeCode = row.nodeCode || '';
  form.nodeName = row.nodeName || '';
  form.nodeType = row.nodeType ?? 1;
  form.gatewayCode = row.gatewayCode || '';
  form.firmwareVersion = row.firmwareVersion || '';
  form.reportInterval = row.reportInterval;
  codeError.value = '';
  codeDirty.value = false;
  dialog.visible = true;
}

async function submit() {
  if (!form.nodeName.trim()) {
    ElMessage.warning(t('production.nameRequired'));
    return;
  }
  if (dialog.mode === 'create' && !form.gatewayCode) {
    ElMessage.warning(t('production.gatewayRequired'));
    return;
  }
  if (!(await validateCode())) return;
  try {
    if (dialog.mode === 'create') {
      const res = await createProductionNode({ ...form });
      ElMessage.success(t('production.created', { code: res.data?.nodeCode || '' }));
    } else {
      await updateProductionNode(dialog.id, {
        nodeName: form.nodeName, nodeType: form.nodeType, gatewayCode: form.gatewayCode || '',
        firmwareVersion: form.firmwareVersion, reportInterval: form.reportInterval,
      });
      ElMessage.success(t('gateway.saved'));
    }
    dialog.visible = false;
    await load();
  } catch (error) {
    ElMessage.error((error as Error).message || t('gateway.saveFailed'));
  }
}

async function remove(row: NodeEntity) {
  if (isAssigned(row)) return;
  try {
    await ElMessageBox.confirm(t('production.deleteConfirm', { code: row.nodeCode }), t('common.deleteTitle'), { type: 'warning' });
  } catch {
    return;
  }
  try {
    await deleteProductionNode(row.id!);
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
    <h2 class="page-title">{{ $t('production.nodeTitle') }}</h2>

    <div class="filter-bar">
      <el-input v-model="keyword" class="keyword-input" clearable :placeholder="$t('production.searchCode')">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="typeFilter" class="type-select" clearable :placeholder="$t('node.type')" :aria-label="$t('node.type')">
        <el-option v-for="(label, value) in NODE_TYPES" :key="value" :label="label.startsWith('node.') ? $t(label) : label" :value="Number(value)" />
      </el-select>
      <el-select v-model="gatewayFilter" class="gw-select" clearable filterable :placeholder="$t('gateway.code')" :aria-label="$t('gateway.code')">
        <el-option v-for="g in gateways" :key="g.deviceId" :label="g.deviceId" :value="g.deviceId" />
      </el-select>
      <el-select v-model="statusFilter" class="status-select" clearable :placeholder="$t('production.assignStatus')" :aria-label="$t('production.assignStatus')">
        <el-option :label="$t('production.unassigned')" value="UNASSIGNED" />
        <el-option :label="$t('production.assigned')" value="ASSIGNED" />
      </el-select>
      <el-button type="primary" @click="load">{{ $t('common.search') }}</el-button>
      <el-button @click="resetFilter">{{ $t('common.reset') }}</el-button>
      <el-button type="primary" v-permission="'production:node:create'" @click="openCreate">{{ $t('production.create') }}</el-button>
    </div>

    <div class="table-card" v-loading="loading">
      <el-table :data="filtered" border style="width: 100%" :empty-text="$t('production.empty')">
        <el-table-column :label="$t('common.page')" type="index" width="64" align="center" />
        <el-table-column prop="nodeCode" :label="$t('node.nodeCode')" min-width="200">
          <template #default="{ row }"><span class="mono">{{ row.nodeCode }}</span></template>
        </el-table-column>
        <el-table-column prop="nodeName" :label="$t('node.name')" min-width="140" show-overflow-tooltip />
        <el-table-column :label="$t('node.type')" width="100" align="center">
          <template #default="{ row }">{{ typeName(row.nodeType) }}</template>
        </el-table-column>
        <el-table-column :label="$t('production.boundGateway')" min-width="190">
          <template #default="{ row }">
            <span class="mono">{{ row.gatewayCode || '--' }}</span>
            <span v-if="row.gatewayCode" class="bind-name">（{{ gatewayName(row.gatewayCode) }}）</span>
          </template>
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
        <el-table-column :label="$t('common.createTime')" width="160" align="center">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column :label="$t('common.actions')" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <template v-if="!isAssigned(row)">
              <el-button link type="primary" v-permission="'production:node:edit'" @click="openEdit(row)">{{ $t('common.edit') }}</el-button>
              <el-button link type="danger" v-permission="'production:node:delete'" @click="remove(row)">{{ $t('common.delete') }}</el-button>
            </template>
            <el-tooltip v-else :content="$t('production.assignedLockTip')" placement="top">
              <span class="locked">{{ $t('production.locked') }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialog.visible" :title="dialog.mode === 'create' ? $t('production.createNode') : $t('production.editNode')" width="500px">
      <el-alert v-if="dialog.mode === 'create'" :title="$t('production.codeAutoHint')" type="info" :closable="false" show-icon class="code-hint" />
      <el-form label-width="110px">
        <el-form-item :label="$t('production.code')" :error="codeError">
          <el-input v-model="form.nodeCode" :disabled="dialog.mode === 'edit'" @input="codeDirty = true; codeError = ''" @blur="validateCode()" />
        </el-form-item>
        <el-form-item :label="$t('node.name')"><el-input v-model="form.nodeName" :placeholder="$t('production.nodeNamePlaceholder')" /></el-form-item>
        <el-form-item :label="$t('node.type')">
          <el-select v-model="form.nodeType" style="width: 100%" @change="refreshNextCode">
            <el-option v-for="(label, value) in NODE_TYPES" :key="value" :label="label.startsWith('node.') ? $t(label) : label" :value="Number(value)" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('production.boundGateway')">
          <el-select v-model="form.gatewayCode" clearable filterable style="width: 100%" :placeholder="dialog.mode === 'create' ? $t('production.selectGateway') : $t('production.selectGatewayOptional')">
            <el-option v-for="g in gateways.filter((x) => !x.orgCode)" :key="g.deviceId" :label="`${g.deviceId}（${g.name || ''}）`" :value="g.deviceId!" />
          </el-select>
          <div class="form-tip">{{ $t('production.gatewayBindTip') }}</div>
        </el-form-item>
        <el-form-item :label="$t('gateway.firmware')"><el-input v-model="form.firmwareVersion" :placeholder="$t('production.optional')" /></el-form-item>
        <el-form-item :label="$t('node.reportInterval')"><el-input-number v-model="form.reportInterval" :min="0" style="width: 100%" /></el-form-item>
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
.type-select { width: 140px; }
.gw-select { width: 210px; }
.status-select { width: 130px; }
.table-card { background: #fff; border: 1px solid #ebeef5; border-radius: 4px; padding: 4px; }
.mono { font-family: monospace; }
.bind-name { color: #909399; font-size: 12px; }
.code-hint { margin-bottom: 12px; }
.form-tip { font-size: 12px; color: #909399; line-height: 1.4; margin-top: 4px; }
.locked { color: #909399; font-size: 12px; }
:deep(.el-table th.el-table__cell) { background: #f5f7fa; color: #606266; font-weight: 600; }
:deep(.el-table .cell) { font-size: 13px; }
</style>
