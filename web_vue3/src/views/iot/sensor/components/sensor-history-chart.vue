<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { getSensorHistory, type SensorEntity, type SensorHistoryPoint } from '@/api/iot/sensor';
import KbLineChart from '@/components/KbLineChart.vue';

const props = defineProps<{ sensor: SensorEntity }>();

function today() {
  const date = new Date();
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

const date = ref(today());
const points = ref<SensorHistoryPoint[]>([]);
const loading = ref(false);
const metricLabel = computed(() =>
  `${props.sensor.sensorCode}${props.sensor.sensorName ? '-' + props.sensor.sensorName : ''}`);

const chartPoints = computed(() => points.value.map((p) => {
  const time = new Date(p.ts);
  const pad = (n: number) => String(n).padStart(2, '0');
  return { label: `${pad(time.getHours())}:${pad(time.getMinutes())}`, value: p.senval };
}));

async function load() {
  loading.value = true;
  try {
    const from = new Date(`${date.value}T00:00:00`).getTime();
    const to = new Date(`${date.value}T23:59:59`).getTime();
    const res = await getSensorHistory(props.sensor.id, from, to);
    points.value = res.data ?? [];
  } catch (error) {
    points.value = [];
    ElMessage.warning((error as Error).message || '历史数据加载失败，请稍后重试');
  } finally {
    loading.value = false;
  }
}

function exportCsv() {
  if (!points.value.length) {
    ElMessage.warning('当前曲线暂无数据可导出');
    return;
  }
  const rows = points.value.map((p) => {
    const time = new Date(p.ts);
    const pad = (n: number) => String(n).padStart(2, '0');
    const stamp = `${time.getFullYear()}-${pad(time.getMonth() + 1)}-${pad(time.getDate())} ` +
      `${pad(time.getHours())}:${pad(time.getMinutes())}:${pad(time.getSeconds())}`;
    return `${stamp},${p.senval}`;
  });
  const csv = `\uFEFF时间,${metricLabel.value}\n${rows.join('\n')}\n`;
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
  const link = document.createElement('a');
  link.href = url;
  link.download = `${props.sensor.sensorCode}-${date.value}.csv`;
  link.click();
  URL.revokeObjectURL(url);
  ElMessage.success('已导出 CSV');
}

onMounted(load);
</script>

<template>
  <div class="history-card" v-loading="loading">
    <div class="history-toolbar">
      <el-date-picker v-model="date" type="date" format="YYYY年MM月DD日" value-format="YYYY-MM-DD"
                      placeholder="选择日期" @change="load" />
      <el-select :model-value="metricLabel" class="metric-select" aria-label="测点指标">
        <el-option :label="metricLabel" :value="metricLabel" />
      </el-select>
      <div class="toolbar-spacer" />
      <el-button @click="load">刷新</el-button>
      <el-button @click="exportCsv">导出</el-button>
    </div>

    <div class="chart-legend"><span class="legend-dot" />{{ metricLabel }}</div>
    <KbLineChart v-if="chartPoints.length" :points="chartPoints" :unit="sensor.unit || ''" :height="260" area />
    <el-empty v-else description="当日暂无历史数据" :image-size="70" />
  </div>
</template>

<style scoped>
.history-card { margin-top: 20px; padding: 16px; background: #fff; border: 1px solid #ebeef5; }
.history-toolbar { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
.metric-select { width: 240px; }
.toolbar-spacer { flex: 1; }
.chart-legend { display: flex; align-items: center; justify-content: center; gap: 6px; margin-bottom: 6px; color: #606266; font-size: 12px; }
.legend-dot { width: 10px; height: 10px; background: #409eff; border-radius: 2px; }
</style>
