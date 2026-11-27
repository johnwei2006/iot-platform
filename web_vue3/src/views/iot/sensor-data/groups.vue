<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  createSensorGroup,
  deleteSensorGroup,
  listSensorGroups,
  listSensors,
  updateSensorGroup,
  type SensorEntity,
  type SensorGroupEntity,
} from '@/api/iot/sensor';
import { useUserStore } from '@/stores/modules/user';

const { t } = useI18n();
const router = useRouter();
const store = useUserStore();
const canEdit = computed(
  () => !!store.superAdmin || (store.roles || []).some((r) => String(r).toUpperCase() === 'ADMIN'),
);

const loading = ref(false);
const saving = ref(false);
const groups = ref<SensorGroupEntity[]>([]);
const allSensors = ref<SensorEntity[]>([]);
const dialogVisible = ref(false);
const editingId = ref('');
const form = reactive({ groupName: '', description: '', sensorCodes: [] as string[] });

const sensorOptions = computed(() =>
  allSensors.value.map((s) => ({
    label: `${s.sensorName || s.sensorCode}（${s.sensorCode}）`,
    value: s.sensorCode,
  })),
);

function parseCodes(raw?: string): string[] {
  if (!raw) return [];
  try {
    const parsed: unknown = JSON.parse(raw);
    if (Array.isArray(parsed)) return parsed.map(String);
  } catch {
    // 兼容旧数据：逗号分隔
  }
  return raw.split(',').map((s) => s.trim()).filter(Boolean);
}

function codeName(code: string): string {
  return allSensors.value.find((s) => s.sensorCode === code)?.sensorName || code;
}

async function load() {
  loading.value = true;
  try {
    const [groupRes, sensorRes] = await Promise.all([listSensorGroups(), listSensors()]);
    groups.value = groupRes.data || [];
    allSensors.value = sensorRes.data || [];
  } catch (error) {
    ElMessage.error((error as Error).message || t('common.loadFailed'));
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  editingId.value = '';
  form.groupName = '';
  form.description = '';
  form.sensorCodes = [];
  dialogVisible.value = true;
}

function openEdit(g: SensorGroupEntity) {
  editingId.value = g.id;
  form.groupName = g.groupName;
  form.description = g.description || '';
  form.sensorCodes = parseCodes(g.sensorCodes);
  dialogVisible.value = true;
}

async function submit() {
  if (!form.groupName.trim()) {
    ElMessage.warning(t('sensorGroups.nameRequired'));
    return;
  }
  if (!form.sensorCodes.length) {
    ElMessage.warning(t('sensorGroups.sensorsRequired'));
    return;
  }
  saving.value = true;
  try {
    const payload = {
      groupName: form.groupName.trim(),
      description: form.description,
      sensorCodes: JSON.stringify(form.sensorCodes),
    };
    if (editingId.value) {
      await updateSensorGroup(editingId.value, payload);
      ElMessage.success(t('sensorGroups.updated'));
    } else {
      await createSensorGroup(payload);
      ElMessage.success(t('sensorGroups.created'));
    }
    dialogVisible.value = false;
    await load();
  } catch (error) {
    ElMessage.error((error as Error).message || t('gateway.saveFailed'));
  } finally {
    saving.value = false;
  }
}

async function remove(g: SensorGroupEntity) {
  try {
    await ElMessageBox.confirm(t('sensorGroups.deleteConfirm', { name: g.groupName }), t('common.deleteTitle'), { type: 'warning' });
    await deleteSensorGroup(g.id);
    ElMessage.success(t('sensorGroups.deleted'));
    await load();
  } catch (error) {
    if (error !== 'cancel') ElMessage.error((error as Error).message || t('control.deleteFailed'));
  }
}

function viewData(g: SensorGroupEntity) {
  router.push({ path: '/iot/sensor-data', query: { groupId: g.id } });
}

onMounted(load);
</script>

<template>
  <div v-loading="loading" class="group-page">
    <el-card shadow="never">
      <template #header>
        <div class="page-header">
          <span class="page-title">{{ $t('sensorGroups.title') }}</span>
          <div class="page-actions">
            <el-button @click="router.push({ path: '/iot/sensor-data' })">{{ $t('sensorGroups.backToData') }}</el-button>
            <el-button v-if="canEdit" type="primary" @click="openCreate">{{ $t('sensorGroups.create') }}</el-button>
          </div>
        </div>
      </template>

      <el-empty v-if="!groups.length" :description="$t('sensorGroups.empty')" :image-size="90" />
      <div v-else class="card-grid">
        <el-card v-for="g in groups" :key="g.id" shadow="hover" class="group-card">
          <template #header>
            <div class="card-head">
              <span class="card-title">{{ g.groupName }}</span>
              <el-tag size="small" type="info">{{ $t('sensorGroups.pointCount', { count: parseCodes(g.sensorCodes).length }) }}</el-tag>
            </div>
          </template>
          <div class="card-body">
            <p class="desc">{{ g.description || $t('sensorGroups.noDescription') }}</p>
            <div class="tags">
              <el-tag v-for="code in parseCodes(g.sensorCodes)" :key="code" size="small" class="code-tag">
                {{ codeName(code) }}
              </el-tag>
            </div>
          </div>
          <template #footer>
            <div class="card-actions">
              <el-button type="primary" link @click="viewData(g)">{{ $t('sensorGroups.viewData') }}</el-button>
              <template v-if="canEdit">
                <el-button link @click="openEdit(g)">{{ $t('common.edit') }}</el-button>
                <el-button type="danger" link @click="remove(g)">{{ $t('common.delete') }}</el-button>
              </template>
            </div>
          </template>
        </el-card>
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? $t('sensorGroups.editTitle') : $t('sensorGroups.createTitle')" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item :label="$t('sensorGroups.groupName')" required>
          <el-input v-model="form.groupName" maxlength="128" :placeholder="$t('sensorGroups.groupNamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('sensor.title')" required>
          <el-select
            v-model="form.sensorCodes"
            multiple
            filterable
            collapse-tags
            :placeholder="$t('sensorGroups.selectSensors')"
            style="width: 100%"
          >
            <el-option v-for="opt in sensorOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('common.remark')">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="255" :placeholder="$t('sensorGroups.optional')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="submit">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.group-page :deep(.el-card__header) { padding: 12px 20px; }
.page-header { display: flex; align-items: center; justify-content: space-between; }
.page-title { font-size: 16px; font-weight: 700; letter-spacing: 0.3px; }
.page-actions { display: flex; gap: 8px; }
.card-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 14px; }
.card-head { display: flex; align-items: center; justify-content: space-between; }
.card-title { font-weight: 700; }
.desc { margin: 0 0 10px; min-height: 20px; color: #606266; font-size: 13px; }
.tags { display: flex; flex-wrap: wrap; gap: 6px; }
.card-actions { display: flex; justify-content: flex-end; gap: 4px; }
</style>
