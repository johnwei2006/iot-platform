<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
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
    ElMessage.error((error as Error).message || '加载失败');
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
    ElMessage.warning('请输入组合名称');
    return;
  }
  if (!form.sensorCodes.length) {
    ElMessage.warning('请至少选择一个传感器');
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
      ElMessage.success('更新成功');
    } else {
      await createSensorGroup(payload);
      ElMessage.success('创建成功');
    }
    dialogVisible.value = false;
    await load();
  } catch (error) {
    ElMessage.error((error as Error).message || '保存失败');
  } finally {
    saving.value = false;
  }
}

async function remove(g: SensorGroupEntity) {
  try {
    await ElMessageBox.confirm(`确认删除组合「${g.groupName}」？`, '删除确认', { type: 'warning' });
    await deleteSensorGroup(g.id);
    ElMessage.success('删除成功');
    await load();
  } catch (error) {
    if (error !== 'cancel') ElMessage.error((error as Error).message || '删除失败');
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
          <span class="page-title">传感器组合管理</span>
          <div class="page-actions">
            <el-button @click="router.push({ path: '/iot/sensor-data' })">返回数据页</el-button>
            <el-button v-if="canEdit" type="primary" @click="openCreate">新建组合</el-button>
          </div>
        </div>
      </template>

      <el-empty v-if="!groups.length" description="暂无组合，点击右上角新建" :image-size="90" />
      <div v-else class="card-grid">
        <el-card v-for="g in groups" :key="g.id" shadow="hover" class="group-card">
          <template #header>
            <div class="card-head">
              <span class="card-title">{{ g.groupName }}</span>
              <el-tag size="small" type="info">{{ parseCodes(g.sensorCodes).length }} 个测点</el-tag>
            </div>
          </template>
          <div class="card-body">
            <p class="desc">{{ g.description || '暂无描述' }}</p>
            <div class="tags">
              <el-tag v-for="code in parseCodes(g.sensorCodes)" :key="code" size="small" class="code-tag">
                {{ codeName(code) }}
              </el-tag>
            </div>
          </div>
          <template #footer>
            <div class="card-actions">
              <el-button type="primary" link @click="viewData(g)">查看数据</el-button>
              <template v-if="canEdit">
                <el-button link @click="openEdit(g)">编辑</el-button>
                <el-button type="danger" link @click="remove(g)">删除</el-button>
              </template>
            </div>
          </template>
        </el-card>
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑组合' : '新建组合'" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="组合名称" required>
          <el-input v-model="form.groupName" maxlength="128" placeholder="请输入组合名称" />
        </el-form-item>
        <el-form-item label="传感器" required>
          <el-select
            v-model="form.sensorCodes"
            multiple
            filterable
            collapse-tags
            placeholder="选择要加入组合的传感器"
            style="width: 100%"
          >
            <el-option v-for="opt in sensorOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="255" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
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
