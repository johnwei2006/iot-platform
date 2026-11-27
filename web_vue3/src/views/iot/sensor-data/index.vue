<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { ElMessage } from 'element-plus';
import MultiSensorChart from './components/multi-sensor-chart.vue';
import { listAreas, type AreaEntity } from '@/api/iot/area';
import {
  getSensorHistory,
  listSensorGroups,
  listSensors,
  type SensorEntity,
  type SensorGroupEntity,
} from '@/api/iot/sensor';

interface ChartSeries {
  name: string;
  unit?: string;
  points: { ts: number; value: number }[];
}

interface TableColumn {
  key: string;
  label: string;
  unit?: string;
}

interface TableRow {
  ts: number;
  [key: string]: number | string;
}

const route = useRoute();
const router = useRouter();
const { t } = useI18n();

const loading = ref(false);
const groups = ref<SensorGroupEntity[]>([]);
const sensors = ref<SensorEntity[]>([]);
const areas = ref<AreaEntity[]>([]);
const activeGroupId = ref('');
const selectedAreaId = ref('');
const dataType = ref<'RAW'>('RAW');
const range = ref<number[]>([]);
const activeTab = ref<'chart' | 'table'>('chart');
const chartSeries = ref<ChartSeries[]>([]);
const tableColumns = ref<TableColumn[]>([]);
const tableRows = ref<TableRow[]>([]);

const groupOptions = computed(() => groups.value.map((g) => ({ label: g.groupName, value: g.id })));
const areaOptions = computed(() => areas.value.map((a) => ({ label: a.areaName, value: a.id })));
const selectedGroup = computed(() => groups.value.find((g) => g.id === activeGroupId.value));
const selectedArea = computed(() => areas.value.find((a) => a.id === selectedAreaId.value));
const areaSensors = computed(() => sensors.value.filter((s) => s.areaId === selectedAreaId.value));

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
  const from = new Date();
  from.setHours(0, 0, 0, 0);
  return [from.getTime(), Date.now()];
}

async function loadBase() {
  loading.value = true;
  try {
    const [groupRes, sensorRes, areaRes] = await Promise.all([
      listSensorGroups(), listSensors(), listAreas(),
    ]);
    groups.value = groupRes.data || [];
    sensors.value = sensorRes.data || [];
    areas.value = areaRes.data || [];
    selectedAreaId.value = areas.value[0]?.id || '';
    const queryId = route.query.groupId as string | undefined;
    activeGroupId.value = queryId && groups.value.some((g) => g.id === queryId)
      ? queryId
      : groups.value[0]?.id || '';
  } catch (error) {
    ElMessage.error((error as Error).message || t('sensorData.loadFailed'));
  } finally {
    loading.value = false;
  }
}

async function loadData() {
  const list = selectedSensors.value;
  if (!list.length || range.value.length !== 2) {
    chartSeries.value = [];
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
  } catch (error) {
    ElMessage.error((error as Error).message || t('sensorData.historyLoadFailed'));
  } finally {
    loading.value = false;
  }
}

/** 表格数据与传感器组合无关：按当前区域下全部传感器 + 时间范围透视展示。 */
async function loadTableData() {
  const list = areaSensors.value;
  if (!list.length || range.value.length !== 2) {
    tableColumns.value = [];
    tableRows.value = [];
    return;
  }
  const [from, to] = range.value;
  if (!from || !to || from >= to) return;
  loading.value = true;
  try {
    const results = await Promise.all(
      list.map(async (s) => ({
        sensor: s,
        points: (await getSensorHistory(s.id, from, to)).data || [],
      })),
    );
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
  } catch (error) {
    ElMessage.error((error as Error).message || t('sensorData.historyLoadFailed'));
  } finally {
    loading.value = false;
  }
}

function exportCsv() {
  const hasData = activeTab.value === 'table'
    ? tableRows.value.length > 0
    : chartSeries.value.some((s) => s.points.length > 0);
  if (!hasData) {
    ElMessage.warning(t('sensorData.noExportData'));
    return;
  }
  const head = activeTab.value === 'table'
    ? [t('sensorData.time'), ...tableColumns.value.map((c) => c.label)]
    : [t('sensorData.time'), t('sensor.name'), t('sensorData.value'), t('sensor.unit')];
  const lines = activeTab.value === 'table'
    ? tableRows.value.map((r) =>
      [new Date(r.ts).toLocaleString('zh-CN'), ...tableColumns.value.map((c) => r[c.key] ?? '')]
        .map((v) => `"${String(v).replace(/"/g, '""')}"`)
        .join(','),
    )
    : chartSeries.value.flatMap((s) => s.points.map((p) =>
      [new Date(p.ts).toLocaleString('zh-CN'), s.name, p.value, s.unit || '']
        .map((v) => `"${String(v).replace(/"/g, '""')}"`)
        .join(','),
    ));
  const blob = new Blob(['\uFEFF' + [head.join(','), ...lines].join('\r\n')], {
    type: 'text/csv;charset=utf-8',
  });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `${t('sensorData.title')}_${new Date().toISOString().slice(0, 10)}.csv`;
  link.click();
  URL.revokeObjectURL(url);
}

function goGroups() {
  router.push({ name: 'SensorGroupManage' });
}

function refresh() {
  if (activeTab.value === 'table') {
    void loadTableData();
  } else {
    void loadData();
  }
}

watch([activeGroupId, () => range.value[0], () => range.value[1]], () => {
  if (activeTab.value === 'table') {
    void loadTableData();
  } else {
    void loadData();
  }
});

watch(selectedAreaId, () => {
  if (activeTab.value === 'table') void loadTableData();
});

watch(activeTab, (tab) => {
  if (tab === 'table') void loadTableData();
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
          <span class="page-title">{{ $t('sensorData.title') }}</span>
          <div class="page-actions">
            <el-button v-permission="'iot:sensor_data:group'" plain @click="goGroups">{{ $t('sensorData.groupManage') }}</el-button>
            <el-button v-permission="'iot:sensor_data:export'" @click="exportCsv">{{ $t('common.export') }}</el-button>
            <el-button type="success" plain @click="refresh">{{ $t('common.refresh') }}</el-button>
          </div>
        </div>
      </template>

      <el-form :inline="true" class="filters">
        <el-form-item v-if="activeTab === 'chart'" :label="$t('sensorData.group')">
          <el-select
            v-model="activeGroupId"
            :placeholder="$t('sensorData.selectGroup')"
            clearable
            style="width: 240px"
          >
            <el-option v-for="g in groupOptions" :key="g.value" :label="g.label" :value="g.value" />
          </el-select>
        </el-form-item>
        <el-form-item v-else :label="$t('sensorData.area')">
          <el-select
            v-model="selectedAreaId"
            :placeholder="$t('sensorData.selectArea')"
            clearable
            style="width: 240px"
          >
            <el-option v-for="a in areaOptions" :key="a.value" :label="a.label" :value="a.value" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="activeTab === 'table'" :label="$t('sensorData.dataType')">
          <el-select v-model="dataType" style="width: 140px">
            <el-option :label="$t('sensorData.rawData')" value="RAW" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('sensorData.timeRange')">
          <el-date-picker
            v-model="range"
            type="datetimerange"
            value-format="timestamp"
            :range-separator="$t('sensorData.to')"
            :start-placeholder="$t('sensorData.startTime')"
            :end-placeholder="$t('sensorData.endTime')"
            :clearable="false"
            style="width: 360px"
          />
        </el-form-item>
        <el-form-item>
          <span class="hint">
            {{ activeTab === 'chart'
              ? $t('sensorData.selectedPoints', { count: selectedSensors.length })
              : $t('sensorData.selectedPoints', { count: areaSensors.length }) }}
          </span>
        </el-form-item>
      </el-form>

      <el-tabs v-model="activeTab">
        <el-tab-pane :label="$t('sensorData.tabChart')" name="chart">
          <el-empty v-if="!chartSeries.length" :description="$t('sensorData.noChartData')" :image-size="90" />
          <MultiSensorChart v-else :series="chartSeries" :height="480" />
        </el-tab-pane>
        <el-tab-pane :label="$t('sensorData.tabTable')" name="table">
          <div v-if="tableColumns.length" class="table-area-header">{{ selectedArea?.areaName || '--' }}</div>
          <el-table
            v-if="tableColumns.length"
            :data="tableRows"
            border
            stripe
            max-height="480"
            default-sort="{ prop: 'ts', order: 'descending' }"
          >
            <el-table-column :label="$t('sensorData.time')" min-width="180" fixed>
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
          <el-empty v-else :description="$t('sensorData.noTableData')" :image-size="90" />
          <div class="table-foot">{{ $t('sensorData.totalRecords', { count: tableRows.length }) }}</div>
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
.table-area-header {
  background: #409eff;
  color: #fff;
  font-weight: 600;
  text-align: center;
  padding: 8px 0;
  border: 1px solid #409eff;
  border-bottom: 0;
}
.table-foot { margin-top: 8px; color: #909399; font-size: 12px; text-align: right; }
</style>
