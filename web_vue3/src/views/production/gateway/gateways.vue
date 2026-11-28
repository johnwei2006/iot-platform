<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Search } from '@element-plus/icons-vue';
import { useRouter } from 'vue-router';
import {
  assignGatewayOrg,
  checkProductionCode,
  createProductionGateway,
  deleteProductionGateway,
  getNextProductionCode,
  listProductionGateways,
  updateProductionGateway,
  type ProductionKind,
} from '@/api/iot/production';
import { fetchOrgList } from '@/api/system/org';
import type { Device, OrgRecord } from '@/types';

const { t } = useI18n();
const router = useRouter();
const loading = ref(false);
const gateways = ref<Device[]>([]);
const orgs = ref<OrgRecord[]>([]);
const keyword = ref('');
const typeFilter = ref<number | ''>('');
const statusFilter = ref('');

const dialog = reactive({ visible: false, mode: 'create' as 'create' | 'edit', id: '' });
const form = reactive<{ deviceId: string; name: string; gatewayType: number; firmwareVersion: string }>({
  deviceId: '', name: '', gatewayType: 1, firmwareVersion: '',
});
const KIND: ProductionKind = 'GATEWAY';
const codeError = ref('');
const codeDirty = ref(false);

async function refreshNextCode() {
  if (dialog.mode === 'edit' || codeDirty.value) return;
  try {
    const res = await getNextProductionCode(KIND, { type: form.gatewayType });
    form.deviceId = res.data || '';
  } catch {
    // 预览失败时保留当前值，提交前仍会校验唯一性
  }
}

async function validateCode(): Promise<boolean> {
  const code = form.deviceId.trim();
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
const assign = reactive({ visible: false, id: '', code: '', orgCode: '' });

const typeName = (type?: number) => {
  if (type === undefined || type === null) return '--';
  const key = `gateway.type_${type}`;
  const translated = t(key);
  return translated === key ? String(type) : translated;
};

const orgNameMap = computed(() => new Map(orgs.value.map((o) => [o.code, o.name])));

function orgName(code?: string) {
  if (!code) return '--';
  return orgNameMap.value.get(code) || code;
}

const isAssigned = (row: Device) => !!row.orgCode;

function openGatewayPage(path: string, row: Device) {
  router.push({ path, query: { gatewayCode: row.deviceId } });
}

function formatTime(value?: string) {
  if (!value) return '--';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

const filtered = computed(() => gateways.value.filter((g) => {
  const kw = keyword.value.trim().toLowerCase();
  return (!kw || (g.deviceId || '').toLowerCase().includes(kw) || (g.name || '').toLowerCase().includes(kw))
    && (typeFilter.value === '' || g.gatewayType === typeFilter.value)
    && (!statusFilter.value || isAssigned(g) === (statusFilter.value === 'ASSIGNED'));
}));

function resetFilter() {
  keyword.value = '';
  typeFilter.value = '';
  statusFilter.value = '';
}

async function load() {
  loading.value = true;
  try {
    const [gwRes, orgRes] = await Promise.all([listProductionGateways(), fetchOrgList()]);
    gateways.value = gwRes.data ?? [];
    orgs.value = orgRes;
  } catch (error) {
    ElMessage.error((error as Error).message || t('production.loadFailed'));
  } finally {
    loading.value = false;
  }
}

async function openCreate() {
  dialog.mode = 'create';
  dialog.id = '';
  form.deviceId = '';
  form.name = '';
  form.gatewayType = 1;
  form.firmwareVersion = '';
  codeError.value = '';
  codeDirty.value = false;
  dialog.visible = true;
  await refreshNextCode();
}

function openEdit(row: Device) {
  if (isAssigned(row)) return;
  dialog.mode = 'edit';
  dialog.id = row.id || '';
  form.deviceId = row.deviceId || '';
  form.name = row.name || '';
  form.gatewayType = row.gatewayType ?? 1;
  form.firmwareVersion = row.firmwareVersion || '';
  codeError.value = '';
  codeDirty.value = false;
  dialog.visible = true;
}

async function submit() {
  if (!form.name.trim()) {
    ElMessage.warning(t('production.nameRequired'));
    return;
  }
  if (!(await validateCode())) return;
  try {
    if (dialog.mode === 'create') {
      const res = await createProductionGateway({ ...form });
      ElMessage.success(t('production.created', { code: res.data?.deviceId || '' }));
    } else {
      await updateProductionGateway(dialog.id, { ...form });
      ElMessage.success(t('gateway.saved'));
    }
    dialog.visible = false;
    await load();
  } catch (error) {
    ElMessage.error((error as Error).message || t('gateway.saveFailed'));
  }
}

async function remove(row: Device) {
  if (isAssigned(row)) return;
  try {
    await ElMessageBox.confirm(t('production.deleteConfirm', { code: row.deviceId || row.name }), t('common.deleteTitle'), { type: 'warning' });
  } catch {
    return;
  }
  try {
    await deleteProductionGateway(row.id!);
    ElMessage.success(t('control.deleted'));
    await load();
  } catch (error) {
    ElMessage.error((error as Error).message || t('control.deleteFailed'));
  }
}

function openAssign(row: Device) {
  assign.id = row.id || '';
  assign.code = row.deviceId || '';
  assign.orgCode = '';
  assign.visible = true;
}

async function submitAssign() {
  if (!assign.orgCode) {
    ElMessage.warning(t('production.orgRequired'));
    return;
  }
  try {
    const res = await assignGatewayOrg(assign.id, assign.orgCode);
    const stat = res.data;
    ElMessage.success(t('production.assignDone', {
      nodes: stat?.nodes ?? 0, sensors: stat?.sensors ?? 0, devices: stat?.devices ?? 0,
    }));
    assign.visible = false;
    await load();
  } catch (error) {
    ElMessage.error((error as Error).message || t('gateway.saveFailed'));
  }
}

onMounted(load);
</script>

<template>
  <div class="prod-page">
    <h2 class="page-title">{{ $t('production.gatewayTitle') }}</h2>

    <div class="filter-bar">
      <el-input v-model="keyword" class="keyword-input" clearable :placeholder="$t('production.searchCode')">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="typeFilter" class="type-select" clearable :placeholder="$t('gateway.type')" :aria-label="$t('gateway.type')">
        <el-option :label="$t('gateway.type_1')" :value="1" />
        <el-option :label="$t('gateway.type_2')" :value="2" />
        <el-option :label="$t('gateway.type_3')" :value="3" />
        <el-option :label="$t('gateway.type_4')" :value="4" />
      </el-select>
      <el-select v-model="statusFilter" class="status-select" clearable :placeholder="$t('production.assignStatus')" :aria-label="$t('production.assignStatus')">
        <el-option :label="$t('production.unassigned')" value="UNASSIGNED" />
        <el-option :label="$t('production.assigned')" value="ASSIGNED" />
      </el-select>
      <el-button type="primary" @click="load">{{ $t('common.search') }}</el-button>
      <el-button @click="resetFilter">{{ $t('common.reset') }}</el-button>
      <el-button type="primary" v-permission="'production:gateway:create'" @click="openCreate">{{ $t('production.create') }}</el-button>
    </div>

    <div class="table-card" v-loading="loading">
      <el-table :data="filtered" border style="width: 100%" :empty-text="$t('production.empty')">
        <el-table-column :label="$t('common.page')" type="index" width="64" align="center" />
        <el-table-column prop="deviceId" :label="$t('gateway.code')" min-width="200">
          <template #default="{ row }"><span class="mono">{{ row.deviceId }}</span></template>
        </el-table-column>
        <el-table-column prop="name" :label="$t('gateway.name')" min-width="150" show-overflow-tooltip />
        <el-table-column :label="$t('gateway.type')" width="110" align="center">
          <template #default="{ row }">{{ typeName(row.gatewayType) }}</template>
        </el-table-column>
        <el-table-column prop="firmwareVersion" :label="$t('gateway.firmware')" min-width="100">
          <template #default="{ row }">{{ row.firmwareVersion || '--' }}</template>
        </el-table-column>
        <el-table-column :label="$t('production.assignStatus')" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="isAssigned(row) ? 'success' : 'warning'" size="small">
              {{ isAssigned(row) ? $t('production.assigned') : $t('production.unassigned') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('production.orgName')" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ orgName(row.orgCode) }}</template>
        </el-table-column>
        <el-table-column :label="$t('common.createTime')" width="160" align="center">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column :label="$t('common.actions')" width="300" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openGatewayPage('/production/gateway/info-config', row)">{{ $t('common.detail') }}</el-button>
            <template v-if="!isAssigned(row)">
              <el-button link type="primary" v-permission="'production:gateway:edit'" @click="openEdit(row)">{{ $t('common.edit') }}</el-button>
              <el-button link type="success" v-permission="'production:gateway:assign'" @click="openAssign(row)">{{ $t('production.assignOrg') }}</el-button>
              <el-button link type="danger" v-permission="'production:gateway:delete'" @click="remove(row)">{{ $t('common.delete') }}</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialog.visible" :title="dialog.mode === 'create' ? $t('production.createGateway') : $t('production.editGateway')" width="480px">
      <el-alert v-if="dialog.mode === 'create'" :title="$t('production.codeAutoHint')" type="info" :closable="false" show-icon class="code-hint" />
      <el-form label-width="100px">
        <el-form-item :label="$t('production.code')" :error="codeError">
          <el-input v-model="form.deviceId" :disabled="dialog.mode === 'edit'" @input="codeDirty = true; codeError = ''" @blur="validateCode()" />
        </el-form-item>
        <el-form-item :label="$t('gateway.name')"><el-input v-model="form.name" :placeholder="$t('production.gatewayNamePlaceholder')" /></el-form-item>
        <el-form-item :label="$t('gateway.type')">
          <el-radio-group v-model="form.gatewayType" :disabled="dialog.mode === 'edit'" @change="refreshNextCode">
            <el-radio :value="1">{{ $t('gateway.type_1') }}</el-radio>
            <el-radio :value="2">{{ $t('gateway.type_2') }}</el-radio>
            <el-radio :value="3">{{ $t('gateway.type_3') }}</el-radio>
            <el-radio :value="4">{{ $t('gateway.type_4') }}</el-radio>
          </el-radio-group>
          <div v-if="dialog.mode === 'edit'" class="form-tip">{{ $t('production.typeLockedTip') }}</div>
        </el-form-item>
        <el-form-item :label="$t('gateway.firmware')">
          <el-input v-model="form.firmwareVersion" disabled :placeholder="$t('production.firmwarePending')" />
          <div class="form-tip">{{ $t('production.firmwareReadOnlyTip') }}</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="submit">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="assign.visible" :title="$t('production.assignTitle', { code: assign.code })" width="460px">
      <el-form label-width="100px">
        <el-form-item :label="$t('user.org')">
          <el-select v-model="assign.orgCode" filterable style="width: 100%" :placeholder="$t('production.selectOrg')">
            <el-option v-for="org in orgs" :key="org.id" :label="`${org.name}（${org.code}）`" :value="org.code" />
          </el-select>
        </el-form-item>
      </el-form>
      <div class="assign-tip">{{ $t('production.assignCascadeTip') }}</div>
      <template #footer>
        <el-button @click="assign.visible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="submitAssign">{{ $t('production.assignConfirm') }}</el-button>
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
.status-select { width: 130px; }
.table-card { background: #fff; border: 1px solid #ebeef5; border-radius: 4px; padding: 4px; }
.mono { font-family: monospace; }
.code-hint { margin-bottom: 12px; }
.form-tip { font-size: 12px; color: #909399; line-height: 1.4; margin-top: 4px; }
.assign-tip { padding: 8px 12px; background: #f4f8ff; border-radius: 4px; color: #606266; font-size: 12px; line-height: 1.6; }
.locked { color: #909399; font-size: 12px; }
:deep(.el-table th.el-table__cell) { background: #f5f7fa; color: #606266; font-weight: 600; }
:deep(.el-table .cell) { font-size: 13px; }
</style>
