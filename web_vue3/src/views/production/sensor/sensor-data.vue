<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { ElMessage } from 'element-plus';
import { listNodes, type NodeEntity } from '@/api/iot/node';
import { getSensorHistory, listSensors, type SensorEntity } from '@/api/iot/sensor';

interface TableColumn {
  key: string;
  label: string;
  unit?: string;
}

interface TableRow {
  ts: number;
  [key: string]: number | string;
}

const { t } = useI18n();

const loading = ref(false);
const nodes = ref<NodeEntity[]>([]);
const selectedNodeCode = ref('');
const sensors = ref<SensorEntity[]>([]);
const tableColumns = ref<TableColumn[]>([]);
const tableRows = ref<TableRow[]>([]);
const currentPage = ref(1);
const pageSize = ref(20);

const nodeOptions = computed(() => nodes.value.map((n) => ({
  label: `${n.nodeCode} - ${n.nodeName || ''}`,
  value: n.nodeCode,
})));
const selectedNode = computed(() => nodes.value.find((n) => n.nodeCode === selectedNodeCode.value));
const selectedNodeLabel = computed(() => selectedNode.value
  ? `${selectedNode.value.nodeCode} - ${selectedNode.value.nodeName || ''}`
  : '--');
const pagedRows = computed(() => tableRows.value.slice(
  (currentPage.value - 1) * pageSize.value,
  currentPage.value * pageSize.value,
));

async function loadNodes() {
  loading.value = true;
  try {
    const res = await listNodes();
    nodes.value = res.data || [];
    selectedNodeCode.value = nodes.value[0]?.nodeCode || '';
    await loadHistory();
  } catch (error) {
    ElMessage.error((error as Error).message || t('production.sensorData.loadFailed'));
  } finally {
    loading.value = false;
  }
}

async function loadHistory() {
  if (!selectedNodeCode.value) {
    sensors.value = [];
    tableColumns.value = [];
    tableRows.value = [];
    currentPage.value = 1;
    return;
  }
  loading.value = true;
  try {
    const list = (await listSensors(selectedNodeCode.value)).data || [];
    sensors.value = list;
    if (!list.length) {
      tableColumns.value = [];
      tableRows.value = [];
      currentPage.value = 1;
      return;
    }
    const to = Date.now();
    const results = await Promise.all(list.map(async (sensor) => ({
      sensor,
      points: (await getSensorHistory(sensor.id, 0, to)).data || [],
    })));
    tableColumns.value = results.map(({ sensor }) => ({
      key: sensor.sensorCode,
      label: `${sensor.sensorCode} - ${sensor.sensorName || ''}`,
      unit: sensor.unit || undefined,
    }));
    const rows = new Map<number, TableRow>();
    for (const { sensor, points } of results) {
      for (const point of points) {
        let row = rows.get(point.ts);
        if (!row) {
          row = { ts: point.ts };
          rows.set(point.ts, row);
        }
        row[sensor.sensorCode] = point.senval;
      }
    }
    tableRows.value = Array.from(rows.values()).sort((a, b) => b.ts - a.ts);
    currentPage.value = 1;
  } catch (error) {
    ElMessage.error((error as Error).message || t('production.sensorData.loadFailed'));
  } finally {
    loading.value = false;
  }
}

function refresh() {
  void loadHistory();
}

watch(selectedNodeCode, () => {
  void loadHistory();
});

watch(pageSize, () => {
  currentPage.value = 1;
});

onMounted(() => {
  void loadNodes();
});
</script>

<template>
  <div v-loading="loading" class="production-sensor-data">
    <el-card shadow="never">
      <template #header>
        <div class="page-header">
          <span class="page-title">{{ $t('production.sensorData.title') }}</span>
          <div class="page-actions">
            <el-button type="success" plain @click="refresh">{{ $t('production.refresh') }}</el-button>
          </div>
        </div>
      </template>

      <el-form :inline="true" class="filters">
        <el-form-item :label="$t('production.sensorData.node')">
          <el-select
            v-model="selectedNodeCode"
            filterable
            clearable
            :placeholder="$t('production.sensorData.selectNode')"
            style="width: 280px"
          >
            <el-option v-for="node in nodeOptions" :key="node.value" :label="node.label" :value="node.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <span class="hint">{{ $t('production.sensorData.sensorCount', { count: sensors.length }) }}</span>
        </el-form-item>
      </el-form>

      <div v-if="tableColumns.length" class="table-node-header">{{ selectedNodeLabel }}</div>
      <el-table
        v-if="tableColumns.length"
        :data="pagedRows"
        border
        stripe
        max-height="480"
        default-sort="{ prop: 'ts', order: 'descending' }"
      >
        <el-table-column :label="$t('production.sensorData.time')" min-width="180" fixed>
          <template #default="{ row }">{{ new Date(row.ts).toLocaleString('zh-CN') }}</template>
        </el-table-column>
        <el-table-column
          v-for="col in tableColumns"
          :key="col.key"
          :label="col.label"
          min-width="170"
        >
          <template #default="{ row }">{{ row[col.key] ?? '--' }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-else :description="$t('production.sensorData.noData')" :image-size="90" />

      <div class="table-foot">
        <span>{{ $t('production.sensorData.total', { count: tableRows.length }) }}</span>
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="tableRows.length"
          layout="sizes, prev, pager, next, jumper"
          background
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.production-sensor-data :deep(.el-card__header) { padding: 12px 20px; }
.page-header { display: flex; align-items: center; justify-content: space-between; }
.page-title { font-size: 16px; font-weight: 700; letter-spacing: 0.3px; }
.page-actions { display: flex; gap: 8px; }
.filters { margin-bottom: 6px; }
.hint { color: #909399; font-size: 12px; }
.table-node-header {
  background: #409eff;
  color: #fff;
  font-weight: 600;
  text-align: center;
  padding: 8px 0;
  border: 1px solid #409eff;
  border-bottom: 0;
}
.table-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
  color: #909399;
  font-size: 12px;
}
</style>
