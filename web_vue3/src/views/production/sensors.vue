<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Search } from '@element-plus/icons-vue';
import type { SensorEntity, SensorClassDict, SensorClassTypeDict } from '@/api/iot/sensor';
import { listSensorClass, listSensorClassType } from '@/api/iot/sensor';
import type { NodeEntity } from '@/api/iot/node';
import {
  checkProductionCode,
  createProductionSensor,
  deleteProductionSensor,
  getNextProductionCode,
  listProductionNodes,
  listProductionSensors,
  updateProductionSensor,
  type ProductionKind,
} from '@/api/iot/production';
import { fetchOrgList } from '@/api/system/org';
import type { OrgRecord } from '@/types';

const { t } = useI18n();
const loading = ref(false);
const sensors = ref<SensorEntity[]>([]);
const nodes = ref<NodeEntity[]>([]);
const classes = ref<SensorClassDict[]>([]);
const classTypes = ref<SensorClassTypeDict[]>([]);
const orgs = ref<OrgRecord[]>([]);
const keyword = ref('');
const classFilter = ref('');
const statusFilter = ref('');

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

const isAssigned = (row: SensorEntity) => !!row.orgCode;

function typeName(row: SensorEntity) {
  const type = classTypes.value.find((c) => c.classType === row.sensorClassType)?.classTypeName;
  if (type) return type;
  const cls = classes.value.find((c) => c.sensorClass === row.sensorClass)?.className;
  return cls || row.sensorClassType || row.sensorClass || '--';
}

const filtered = computed(() => sensors.value.filter((s) => {
  const kw = keyword.value.trim().toLowerCase();
  return (!kw || (s.sensorCode || '').toLowerCase().includes(kw) || (s.sensorName || '').toLowerCase().includes(kw))
    && (!classFilter.value || s.sensorClass === classFilter.value)
    && (!statusFilter.value || isAssigned(s) === (statusFilter.value === 'ASSIGNED'));
}));

function resetFilter() {
  keyword.value = '';
  classFilter.value = '';
  statusFilter.value = '';
}

async function load() {
  loading.value = true;
  try {
    const [sensorRes, nodeRes, classRes, typeRes, orgRes] = await Promise.all([
      listProductionSensors(), listProductionNodes(), listSensorClass(), listSensorClassType(), fetchOrgList(),
    ]);
    sensors.value = sensorRes.data ?? [];
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
  sensorCode: string; sensorName: string; sensorClass: string; sensorClassType: string; unit: string;
  channel: string; nodeCode: string;
}>({
  sensorCode: '', sensorName: '', sensorClass: '', sensorClassType: '', unit: '', channel: '', nodeCode: '',
});
const KIND: ProductionKind = 'SENSOR';
const codeError = ref('');
const codeDirty = ref(false);

const firstTypeOf = (clazz: string) =>
  classTypes.value.find((item) => !!clazz && item.classType.startsWith(clazz))?.classType || '';

const classUnit = (clazz: string) => classes.value.find((item) => item.sensorClass === clazz)?.unit || '';

async function refreshNextCode() {
  if (dialog.mode === 'edit' || codeDirty.value) return;
  try {
    const res = await getNextProductionCode(KIND, { clazz: form.sensorClass });
    form.sensorCode = res.data || '';
  } catch {
    // 预览失败时保留当前值，提交前仍会校验唯一性
  }
}

function onSensorClassChange() {
  form.sensorClassType = firstTypeOf(form.sensorClass);
  form.unit = classUnit(form.sensorClass);
  void refreshNextCode();
}

async function validateCode(): Promise<boolean> {
  const code = form.sensorCode.trim();
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
  Object.assign(form, {
    sensorCode: '', sensorName: '', sensorClass: '', sensorClassType: '', unit: '', channel: '', nodeCode: '',
  });
  codeError.value = '';
  codeDirty.value = false;
  dialog.visible = true;
  await refreshNextCode();
}

function openEdit(row: SensorEntity) {
  if (isAssigned(row)) return;
  dialog.mode = 'edit';
  dialog.id = row.id || '';
  Object.assign(form, {
    sensorCode: row.sensorCode || '', sensorName: row.sensorName || '', sensorClass: row.sensorClass || '',
    sensorClassType: row.sensorClassType || '', unit: row.unit || classUnit(row.sensorClass || ''),
    channel: row.channel || '', nodeCode: row.nodeCode || '',
  });
  codeError.value = '';
  codeDirty.value = false;
  dialog.visible = true;
}

async function submit() {
  if (!form.sensorName.trim()) {
    ElMessage.warning(t('production.nameRequired'));
    return;
  }
  if (!form.sensorClass) {
    ElMessage.warning(t('production.classRequired'));
    return;
  }
  if (dialog.mode === 'create' && !form.nodeCode) {
    ElMessage.warning(t('production.nodeRequired'));
    return;
  }
  if (!form.sensorClassType) {
    form.sensorClassType = firstTypeOf(form.sensorClass);
  }
  form.unit = classUnit(form.sensorClass);
  if (!(await validateCode())) return;
  try {
    if (dialog.mode === 'create') {
      const res = await createProductionSensor({ ...form });
      ElMessage.success(t('production.created', { code: res.data?.sensorCode || '' }));
    } else {
      await updateProductionSensor(dialog.id, { ...form });
      ElMessage.success(t('gateway.saved'));
    }
    dialog.visible = false;
    await load();
  } catch (error) {
    ElMessage.error((error as Error).message || t('gateway.saveFailed'));
  }
}

async function remove(row: SensorEntity) {
  if (isAssigned(row)) return;
  try {
    await ElMessageBox.confirm(t('production.deleteConfirm', { code: row.sensorCode }), t('common.deleteTitle'), { type: 'warning' });
  } catch {
    return;
  }
  try {
    await deleteProductionSensor(row.id!);
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
    <h2 class="page-title">{{ $t('production.sensorTitle') }}</h2>

    <div class="filter-bar">
      <el-input v-model="keyword" class="keyword-input" clearable :placeholder="$t('production.searchCode')">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="classFilter" class="type-select" clearable :placeholder="$t('sensor.class')" :aria-label="$t('sensor.class')">
        <el-option v-for="c in classes" :key="c.sensorClass" :label="c.className" :value="c.sensorClass" />
      </el-select>
      <el-select v-model="statusFilter" class="status-select" clearable :placeholder="$t('production.assignStatus')" :aria-label="$t('production.assignStatus')">
        <el-option :label="$t('production.unassigned')" value="UNASSIGNED" />
        <el-option :label="$t('production.assigned')" value="ASSIGNED" />
      </el-select>
      <el-button type="primary" @click="load">{{ $t('common.search') }}</el-button>
      <el-button @click="resetFilter">{{ $t('common.reset') }}</el-button>
      <el-button type="primary" v-permission="'production:sensor:create'" @click="openCreate">{{ $t('production.create') }}</el-button>
    </div>

    <div class="table-card" v-loading="loading">
      <el-table :data="filtered" border style="width: 100%" :empty-text="$t('production.empty')">
        <el-table-column :label="$t('common.page')" type="index" width="64" align="center" />
        <el-table-column prop="sensorCode" :label="$t('sensor.sensorCode')" min-width="200">
          <template #default="{ row }"><span class="mono">{{ row.sensorCode }}</span></template>
        </el-table-column>
        <el-table-column prop="sensorName" :label="$t('sensor.name')" min-width="140" show-overflow-tooltip />
        <el-table-column :label="$t('sensor.type')" min-width="120">
          <template #default="{ row }">{{ typeName(row) }}</template>
        </el-table-column>
        <el-table-column :label="$t('production.slotPort')" width="90" align="center">
          <template #default="{ row }">{{ row.channel || '--' }}</template>
        </el-table-column>
        <el-table-column :label="$t('sensor.unit')" width="70" align="center">
          <template #default="{ row }">{{ row.unit || '--' }}</template>
        </el-table-column>
        <el-table-column :label="$t('production.boundNode')" min-width="190">
          <template #default="{ row }">
            <span class="mono">{{ row.nodeCode || '--' }}</span>
            <span v-if="row.nodeCode" class="bind-name">（{{ nodeName(row.nodeCode) }}）</span>
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
        <el-table-column :label="$t('common.actions')" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <template v-if="!isAssigned(row)">
              <el-button link type="primary" v-permission="'production:sensor:edit'" @click="openEdit(row)">{{ $t('common.edit') }}</el-button>
              <el-button link type="danger" v-permission="'production:sensor:delete'" @click="remove(row)">{{ $t('common.delete') }}</el-button>
            </template>
            <el-tooltip v-else :content="$t('production.assignedLockTip')" placement="top">
              <span class="locked">{{ $t('production.locked') }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialog.visible" :title="dialog.mode === 'create' ? $t('production.createSensor') : $t('production.editSensor')" width="540px">
      <el-alert v-if="dialog.mode === 'create'" :title="$t('production.codeAutoHint')" type="info" :closable="false" show-icon class="code-hint" />
      <el-form label-width="110px">
        <el-form-item :label="$t('production.code')" :error="codeError">
          <el-input v-model="form.sensorCode" :disabled="dialog.mode === 'edit'" @input="codeDirty = true; codeError = ''" @blur="validateCode()" />
        </el-form-item>
        <el-form-item :label="$t('sensor.name')"><el-input v-model="form.sensorName" :placeholder="$t('production.sensorNamePlaceholder')" /></el-form-item>
        <el-form-item :label="$t('sensor.class')">
          <el-select v-model="form.sensorClass" style="width: 100%" @change="onSensorClassChange">
            <el-option v-for="c in classes" :key="c.sensorClass" :label="c.className" :value="c.sensorClass" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('sensor.type')">
          <el-select v-model="form.sensorClassType" clearable style="width: 100%">
            <el-option v-for="ct in classTypes.filter((item) => !form.sensorClass || item.classType.startsWith(form.sensorClass))" :key="ct.classType" :label="ct.classTypeName" :value="ct.classType" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('sensor.unit')">
          <el-input v-model="form.unit" style="width: 100%" disabled />
          <div class="form-tip">{{ $t('production.unitAutoTip') }}</div>
        </el-form-item>
        <el-form-item :label="$t('production.slotPort')">
          <el-input v-model="form.channel" style="width: 140px" :placeholder="$t('production.channelPlaceholder')" />
          <span class="form-tip">{{ $t('production.channelTip') }}</span>
        </el-form-item>
        <el-form-item :label="$t('production.boundNode')">
          <el-select v-model="form.nodeCode" clearable filterable style="width: 100%" :placeholder="$t('production.selectNode')">
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
