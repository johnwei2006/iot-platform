<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import MultiSensorChart from './components/multi-sensor-chart.vue';
import {
  getSensorHistory,
  listSensorGroups,
  listSensors,
  type SensorEntity,
  type SensorGroupEntity,
  type SensorHistoryPoint,
} from '@/api/iot/sensor';

interface ChartSeries {
  name: string;
  unit?: string;
  points: { ts: number; value: number }[];
}

interface TableRow extends SensorHistoryPoint {
  sensorName?: string;
}

const route = useRoute();
const router = useRouter();

const loading = ref(false);
const groups = ref<SensorGroupEntity[]>([]);
const sensors = ref<SensorEntity[]>([]);
const activeGroupId = ref('');
const range = ref<number[]>([]);
const activeTab = ref<'chart' | 'table'>('chart');
const chartSeries = ref<ChartSeries[]>([]);
const tableRows = ref<TableRow[]>([]);

const groupOptions = computed(() => groups.value.map((g) => ({ label: g.groupName, value: g.id })));
const selectedGroup = computed(() => groups.value.find((g) => g.id === activeGroupId.value));

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

const selectedSensors = computed<SensorEntity[]>(() => {
  const codes = parseCodes(selectedGroup.value?.sensorCodes);
  const list: SensorEntity[] = [];
  for (const code of codes) {
    const s = sensors.value.find((x) => x.sensorCode === code);
    if (s) list.push(s);
  }
  return list;
});

function defaultRange(): number[] {
  const to = Date.now();
  return [to - 24 * 60 * 60 * 1000, to];
}

async function loadBase() {
  loading.value = true;
  try {
    const [groupRes, sensorRes] = await Promise.all([listSensorGroups(), listSensors()]);
    groups.value = groupRes.data || [];
    sensors.value = sensorRes.data || [];
    const queryId = route.query.groupId as string | undefined;
    activeGroupId.value = queryId && groups.value.some((g) => g.id === queryId)
      ? queryId
      : groups.value[0]?.id || '';
  } catch (error) {
    ElMessage.error((error as Error).message || '加载组合或传感器失败');
  } finally {
    loading.value = false;
  }
}

async function loadData() {
  const list = selectedSensors.value;
  if (!list.length || range.value.length !== 2) {
    chartSeries.value = [];
    tableRows.value = [];
    return;
  }
  const [from, to] = range.value;
  if (!from || !to || from >= to) return;
  loading.value = true;
  try {
    const results = await Promise.all(
      list.map(async (s) => {
        const res = await getSensorHistory(s.id, from, to);
        return { sensor: s, points: res.data || [] };
      }),
    );
    chartSeries.value = results.map(({ sensor, points }) => ({
      name: sensor.sensorName || sensor.sensorCode,
      unit: sensor.unit || undefined,
      points: points.map((p) => ({ ts: p.ts, value: p.senval })),
    }));
    tableRows.value = results
      .flatMap(({ sensor, points }) =>
        points.map((p) => ({
          ...p,
          sensorName: sensor.sensorName || sensor.sensorCode,
          unit: p.unit || sensor.unit,
        })),
      )
      .sort((a, b) => a.ts - b.ts);
  } catch (error) {
    ElMessage.error((error as Error).message || '历史数据加载失败');
  } finally {
    loading.value = false;
  }
}

function exportCsv() {
  if (!tableRows.value.length) {
    ElMessage.warning('暂无数据可导出');
    return;
  }
  const head = ['时间', '传感器名称', '传感器编码', '数值', '单位'];
  const lines = tableRows.value.map((r) =>
    [new Date(r.ts).toLocaleString('zh-CN'), r.sensorName || '', r.sensorcode || '', r.senval, r.unit || '']
      .map((v) => `"${String(v).replace(/"/g, '""')}"`)
      .join(','),
  );
  const blob = new Blob(['\uFEFF' + [head.join(','), ...lines].join('\r\n')], {
    type: 'text/csv;charset=utf-8',
  });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `传感器数据_${new Date().toISOString().slice(0, 10)}.csv`;
  link.click();
  URL.revokeObjectURL(url);
}

function goGroups() {
  router.push({ name: 'SensorGroupManage' });
}

watch([activeGroupId, () => range.value[0], () => range.value[1]], () => {
  void loadData();
});

onMounted(() => {
  range.value = defaultRange();
  void loadBase();
});
</script>

<template>
  <div v-loading="loading" class="sensor-data">
    <el-card shadow="never">
      <template #header>
        <div class="page-header">
          <span class="page-title">传感器数据</span>
          <div class="page-actions">
            <el-button v-permission="'iot:sensor_data:group'" plain @click="goGroups">组合管理</el-button>
            <el-button v-permission="'iot:sensor_data:export'" @click="exportCsv">导出数据</el-button>
            <el-button type="success" plain @click="loadData">刷新</el-button>
          </div>
        </div>
      </template>

      <el-form :inline="true" class="filters">
        <el-form-item label="传感器组合">
          <el-select
            v-model="activeGroupId"
            placeholder="请选择组合"
            clearable
            style="width: 240px"
          >
            <el-option v-for="g in groupOptions" :key="g.value" :label="g.label" :value="g.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="range"
            type="datetimerange"
            value-format="timestamp"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            :clearable="false"
            style="width: 360px"
          />
        </el-form-item>
        <el-form-item>
          <span class="hint">已选 {{ selectedSensors.length }} 个测点</span>
        </el-form-item>
      </el-form>

      <el-empty v-if="!groups.length" description="暂无传感器组合，请先到组合管理创建" :image-size="90" />
      <el-tabs v-else v-model="activeTab">
        <el-tab-pane label="图表数据" name="chart">
          <el-empty v-if="!chartSeries.length" description="当前组合在所选时间范围内暂无数据" :image-size="90" />
          <MultiSensorChart v-else :series="chartSeries" :height="480" />
        </el-tab-pane>
        <el-tab-pane label="表格数据" name="table">
          <el-table
            :data="tableRows"
            border
            stripe
            max-height="480"
            default-sort="{ prop: 'ts', order: 'ascending' }"
          >
            <el-table-column type="index" label="#" width="60" />
            <el-table-column label="时间" min-width="180">
              <template #default="{ row }">{{ new Date(row.ts).toLocaleString('zh-CN') }}</template>
            </el-table-column>
            <el-table-column label="传感器名称" prop="sensorName" min-width="140" />
            <el-table-column label="传感器编码" prop="sensorcode" min-width="140" />
            <el-table-column label="数值" prop="senval" min-width="100" sortable />
            <el-table-column label="单位" prop="unit" min-width="80" />
          </el-table>
          <div class="table-foot">共 {{ tableRows.length }} 条记录</div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<style scoped>
.sensor-data :deep(.el-card__header) { padding: 12px 20px; }
.page-header { display: flex; align-items: center; justify-content: space-between; }
.page-title { font-size: 16px; font-weight: 700; letter-spacing: 0.3px; }
.page-actions { display: flex; gap: 8px; }
.filters { margin-bottom: 6px; }
.hint { color: #909399; font-size: 12px; }
.table-foot { margin-top: 8px; color: #909399; font-size: 12px; text-align: right; }
</style>
