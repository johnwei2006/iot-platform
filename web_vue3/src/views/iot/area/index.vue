<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Search } from '@element-plus/icons-vue';
import {
  createArea,
  deleteArea,
  listAreas,
  parseBoundary,
  stringifyBoundary,
  updateArea,
  type AreaEntity,
  type AreaPoint,
} from '@/api/iot/area';
import { useUserStore } from '@/stores/modules/user';
import AreaMap from './components/area-map.vue';
import AreaFormDialog from './components/area-form-dialog.vue';
import AreaEntityPanel from './components/area-entity-panel.vue';

const store = useUserStore();
const canEdit = computed(
  () => !!store.superAdmin || (store.roles || []).some((r) => String(r).toUpperCase() === 'ADMIN'),
);
const keyReady = computed(() => !!(import.meta.env.VITE_AMAP_KEY && import.meta.env.VITE_AMAP_SECURITY_CODE));

const loading = ref(false);
const saving = ref(false);
const areas = ref<AreaEntity[]>([]);
const keyword = ref('');
const selectedId = ref('');
const dialogVisible = ref(false);
const dialogMode = ref<'create' | 'edit'>('create');
const form = ref<{ areaName: string; labelAreaMu?: number }>({ areaName: '' });
const drawAreaMu = ref<number | undefined>(undefined);
const boundaryPoints = ref<AreaPoint[]>([]);
const editingId = ref('');
const mapRef = ref<InstanceType<typeof AreaMap> | null>(null);

const filteredAreas = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  if (!kw) return areas.value;
  return areas.value.filter((a) => a.areaName.toLowerCase().includes(kw));
});

const selectedArea = computed(() => areas.value.find((a) => a.id === selectedId.value) || null);

function displayArea(area: AreaEntity): string {
  return (area.labelAreaMu ?? area.drawAreaMu ?? 0).toFixed(2);
}

function onSearch() {
  keyword.value = keyword.value.trim();
}

async function load() {
  loading.value = true;
  try {
    const res = await listAreas();
    areas.value = res.data || [];
  } catch (error) {
    ElMessage.error((error as Error).message || '区域列表加载失败');
  } finally {
    loading.value = false;
  }
}

function selectArea(area: AreaEntity) {
  selectedId.value = area.id;
  mapRef.value?.focusArea(area.id);
}

async function openCreate() {
  dialogMode.value = 'create';
  editingId.value = '';
  form.value = { areaName: '' };
  drawAreaMu.value = undefined;
  boundaryPoints.value = [];
  dialogVisible.value = true;
  await nextTick();
  mapRef.value?.startDraw();
}

function openEdit(area: AreaEntity) {
  dialogMode.value = 'edit';
  editingId.value = area.id;
  form.value = { areaName: area.areaName, labelAreaMu: area.labelAreaMu ?? area.drawAreaMu };
  drawAreaMu.value = area.drawAreaMu;
  boundaryPoints.value = parseBoundary(area.boundary);
  selectedId.value = area.id;
  dialogVisible.value = true;
}

function onDrawn(payload: { points: AreaPoint[]; areaMu: number }) {
  if (dialogMode.value !== 'create') return;
  boundaryPoints.value = payload.points;
  drawAreaMu.value = Number(payload.areaMu.toFixed(2));
  if (form.value.labelAreaMu == null) {
    form.value.labelAreaMu = drawAreaMu.value;
  }
}

function clearBoundary() {
  boundaryPoints.value = [];
  drawAreaMu.value = undefined;
  form.value.labelAreaMu = undefined;
  mapRef.value?.clearDraw();
  if (dialogVisible.value && dialogMode.value === 'create') {
    nextTick(() => mapRef.value?.startDraw());
  }
}

async function save() {
  if (!form.value.areaName.trim()) {
    ElMessage.warning('请输入区域名称');
    return;
  }
  if (dialogMode.value === 'create') {
    if (boundaryPoints.value.length < 3) {
      ElMessage.warning('请在地图上绘制区域边界（至少三个点）');
      return;
    }
    if (!drawAreaMu.value || drawAreaMu.value <= 0) {
      ElMessage.warning('绘制面积无效，请重新绘制边界');
      return;
    }
  }
  saving.value = true;
  try {
    if (dialogMode.value === 'create') {
      await createArea({
        areaName: form.value.areaName.trim(),
        boundary: stringifyBoundary(boundaryPoints.value),
        drawAreaMu: drawAreaMu.value,
        labelAreaMu: form.value.labelAreaMu ?? drawAreaMu.value,
      });
      ElMessage.success('区域创建成功');
    } else {
      await updateArea(editingId.value, {
        areaName: form.value.areaName.trim(),
        labelAreaMu: form.value.labelAreaMu,
      });
      ElMessage.success('区域更新成功');
    }
    dialogVisible.value = false;
    await load();
  } catch (error) {
    ElMessage.error((error as Error).message || '保存失败');
  } finally {
    saving.value = false;
  }
}

async function remove(area: AreaEntity) {
  try {
    await ElMessageBox.confirm(`确认删除区域「${area.areaName}」？`, '删除确认', { type: 'warning' });
    await deleteArea(area.id);
    ElMessage.success('删除成功');
    if (selectedId.value === area.id) selectedId.value = '';
    await load();
  } catch (error) {
    if (error !== 'cancel') ElMessage.error((error as Error).message || '删除失败');
  }
}

function handleCommand(command: string, area: AreaEntity) {
  if (command === 'edit') openEdit(area);
  else if (command === 'delete') void remove(area);
}

onMounted(load);
</script>

<template>
  <div v-loading="loading" class="area-page">
    <AreaMap
      ref="mapRef"
      :areas="areas"
      :selected-id="selectedId"
      :key-ready="keyReady"
      @drawn="onDrawn"
      @select="(id) => (selectedId = id)"
    />

    <AreaEntityPanel
      v-if="selectedArea"
      :area="selectedArea"
      @close="selectedId = ''"
    />

    <aside class="side-panel">
      <div class="panel-head">
        <div class="panel-title">区域管理</div>
        <el-input v-model="keyword" placeholder="请输入名称" clearable @keyup.enter="onSearch">
          <template #append>
            <el-button @click="onSearch"><el-icon><Search /></el-icon></el-button>
          </template>
        </el-input>
      </div>
      <div class="panel-list">
        <el-empty
          v-if="!filteredAreas.length"
          :description="areas.length ? '未找到匹配区域' : '暂无区域'"
          :image-size="70"
        />
        <div
          v-for="area in filteredAreas"
          :key="area.id"
          class="area-item"
          :class="{ active: selectedId === area.id }"
          @click="selectArea(area)"
        >
          <div class="item-name">{{ area.areaName }}</div>
          <div class="item-row">
            <el-tag size="small" type="primary" effect="light">区域</el-tag>
            <span class="item-area">{{ displayArea(area) }}亩</span>
          </div>
          <el-dropdown v-if="canEdit" trigger="click" @command="(cmd: unknown) => handleCommand(String(cmd), area)">
            <span class="item-more" @click.stop>···</span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="edit">编辑</el-dropdown-item>
                <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
      <div v-if="canEdit" class="panel-foot">
        <el-button type="primary" style="width: 100%" @click="openCreate">新建区域</el-button>
      </div>
    </aside>

    <AreaFormDialog
      v-model:visible="dialogVisible"
      :mode="dialogMode"
      :form="form"
      :draw-area-mu="drawAreaMu"
      :boundary-points="boundaryPoints"
      :saving="saving"
      @save="save"
      @clear-boundary="clearBoundary"
    />
  </div>
</template>

<style scoped>
.area-page { position: relative; height: calc(100vh - 118px); min-height: 560px; background: #e8eaed; overflow: hidden; }
.side-panel {
  position: absolute; top: 16px; left: 16px; bottom: 16px; width: 320px; z-index: 10;
  display: flex; flex-direction: column; overflow: hidden;
  background: rgba(255, 255, 255, 0.96); backdrop-filter: blur(12px);
  border-radius: 12px; box-shadow: 0 8px 32px rgba(0, 0, 0, 0.18);
}
.panel-head { padding: 14px; border-bottom: 1px solid #ebeef5; }
.panel-title { margin-bottom: 10px; font-size: 16px; font-weight: 700; letter-spacing: 0.3px; }
.panel-list { flex: 1; padding: 6px 0; overflow-y: auto; }
.area-item { position: relative; padding: 10px 14px; cursor: pointer; border-left: 3px solid transparent; transition: background 0.15s; }
.area-item:hover { background: #f0f6ff; }
.area-item.active { background: #e6f0ff; border-left-color: #1677ff; }
.item-name { margin-bottom: 6px; font-size: 14px; font-weight: 600; color: #303133; }
.item-row { display: flex; align-items: center; gap: 8px; }
.item-area { color: #606266; font-size: 13px; }
.item-more { position: absolute; top: 10px; right: 12px; color: #909399; font-size: 18px; line-height: 1; letter-spacing: 1px; }
.panel-foot { padding: 12px 14px; border-top: 1px solid #ebeef5; }
</style>
