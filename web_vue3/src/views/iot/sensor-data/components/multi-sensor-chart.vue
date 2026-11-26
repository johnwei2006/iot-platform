<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue';
import * as echarts from 'echarts';

interface ChartSeries {
  name: string;
  unit?: string;
  points: { ts: number; value: number }[];
}

const props = defineProps<{ series: ChartSeries[]; height?: number }>();
const host = ref<HTMLDivElement | null>(null);
let chart: echarts.ECharts | null = null;

const COLORS = ['#409EFF', '#F56C6C', '#67C23A', '#E6A23C', '#909399', '#b37feb'];

function buildOption(): echarts.EChartsOption {
  const units = [...new Set(props.series.map((s) => s.unit || '').filter(Boolean))];
  const leftUnit = units[0] || '';
  const rightUnit = units[1] || '';
  const axis = (unit: string, position: 'left' | 'right') => ({
    type: 'value' as const,
    name: unit,
    position,
    scale: true,
    splitLine: { show: position === 'left' },
    axisLine: { show: true },
  });
  return {
    color: COLORS,
    animationDuration: 300,
    tooltip: { trigger: 'axis', axisPointer: { type: 'cross' } },
    legend: { top: 0, type: 'scroll', textStyle: { fontSize: 12, color: '#606266' } },
    grid: { left: 56, right: rightUnit ? 56 : 24, top: 44, bottom: 74 },
    xAxis: {
      type: 'time',
      axisLabel: { formatter: (value: number) => new Date(value).toTimeString().slice(0, 8), fontSize: 11 },
    },
    yAxis: [
      axis(leftUnit, 'left'),
      ...(rightUnit ? [axis(rightUnit, 'right')] : []),
    ],
    dataZoom: [
      { type: 'inside', throttle: 50 },
      { type: 'slider', height: 18, bottom: 10, borderColor: '#dcdfe6' },
    ],
    series: props.series.map((s, i) => ({
      name: s.name,
      type: 'line' as const,
      smooth: true,
      showSymbol: false,
      yAxisIndex: rightUnit && s.unit === rightUnit ? 1 : 0,
      data: s.points.map((p) => [p.ts, p.value]),
      lineStyle: { width: 2 },
      itemStyle: { color: COLORS[i % COLORS.length] },
    })),
  };
}

function render() {
  if (!chart) return;
  chart.setOption(buildOption(), true);
}

function onResize() {
  chart?.resize();
}

onMounted(() => {
  if (!host.value) return;
  chart = echarts.init(host.value);
  render();
  window.addEventListener('resize', onResize);
});

watch(() => props.series, render, { deep: true });
onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize);
  chart?.dispose();
  chart = null;
});
</script>

<template>
  <div ref="host" class="multi-chart" :style="{ height: (height || 460) + 'px' }" />
</template>

<style scoped>
.multi-chart { width: 100%; background: #fff; }
</style>
