<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { listDevices } from '@/api/iot/gateway';
import { listRealtime, listThresholds, queryData } from '@/api/iot/gateway';
import { listAlarms } from '@/api/iot/alarms';
import KbLineChart from '@/components/KbLineChart.vue';
import type { Device } from '@/types';

interface SensorItem {
  deviceId: string;
  deviceName: string;
  channel: number;
  sensorCode: string;
  sensorType: string;
  unit: string;
  sensorValue: number;
  online: boolean;
}

const loading = ref(false);
const devices = ref<Device[]>([]);
const realtime = ref<Record<string, { online: boolean; sensors: SensorItem[]; lastSeen?: string }>>({});
const thresholds = ref<Record<string, { min?: number; max?: number }>>({});
const alarms = ref<{ id: string; deviceId?: string; rule?: string; message?: string; status?: string }[]>([]);

const chartVisible = ref(false);
const chartTitle = ref('');
const chartUnit = ref('');
const chartPoints = ref<{ label: string; value: number }[]>([]);
const pickText = ref('');

const METRIC_NAMES: Record<string, string> = { '1': '温度', '2': '湿度', '3': '气压' };

/** sensorType → 阈值表 metric 键（与后端 ConditionRuleEvaluateHandler.metricKey 一致） */
function metricKey(sensorType: string): string {
  const map: Record<string, string> = { '1': 'temp', '2': 'humid', '3': 'press' };
  return map[sensorType] || '';
}

const tiles = computed<SensorItem[]>(() => {
  const rows: SensorItem[] = [];
  for (const d of devices.value) {
    const rt = realtime.value[d.deviceId];
    const sensors = rt?.sensors ?? [];
    if (sensors.length) {
      for (const s of sensors) {
        rows.push({ ...s, deviceId: d.deviceId, deviceName: d.name || d.deviceId, online: rt.online });
      }
    } else {
      rows.push({ deviceId: d.deviceId, deviceName: d.name || d.deviceId, channel: 0, sensorCode: '', sensorType: '', unit: '', sensorValue: NaN, online: false });
    }
  }
  return rows;
});

const openAlarms = computed(() => alarms.value.filter((a) => a.status === 'OPEN'));

function tileClass(t: SensorItem) {
  if (!t.online || Number.isNaN(t.sensorValue)) return 'kb-tile--offline';
  const th = thresholds.value[`${t.deviceId}:${metricKey(t.sensorType)}`];
  if (th && ((th.min != null && t.sensorValue < th.min) || (th.max != null && t.sensorValue > th.max))) return 'kb-tile--hot';
  return '';
}
function tileValue(t: SensorItem) {
  return t.online && !Number.isNaN(t.sensorValue) ? `${t.sensorValue.toFixed(1)} ${t.unit}` : '暂无实时数据';
}
function tileStatus(t: SensorItem) {
  if (!t.online) return '离线';
  const th = thresholds.value[`${t.deviceId}:${metricKey(t.sensorType)}`];
  const out = th && ((th.min != null && t.sensorValue < th.min) || (th.max != null && t.sensorValue > th.max));
  return out ? '超出阈值' : '正常';
}

async function openChart(t: SensorItem) {
  chartTitle.value = `${t.deviceName} · ${METRIC_NAMES[t.sensorType] || t.sensorCode || '读数'} 历史`;
  chartUnit.value = t.unit;
  const to = Date.now();
  const from = to - 24 * 3600 * 1000;
  try {
    const res = await queryData(t.deviceId, from, to);
    const rows = (res.data as unknown as { serviceDataDecoded?: { sensors?: { channel: number; sensorValue: number }[] }; createdAt?: string }[]) || [];
    const pts = rows.filter((r) => (r.serviceDataDecoded?.sensors || []).some((s) => String(s.channel) === String(t.channel))).slice(-48).map((r) => ({ label: (r.createdAt || '').slice(5, 16).replace('T', ' '), value: (r.serviceDataDecoded?.sensors || []).find((s) => String(s.channel) === String(t.channel))?.sensorValue ?? 0 }));
    chartPoints.value = pts.length ? pts : [{ label: '暂无数据', value: 0 }];
    chartVisible.value = true;
  } catch (error) {
    ElMessage.error((error as Error).message || '历史数据加载失败');
  }
}
async function refresh() {
  loading.value = true;
  try {
    const [devRes, rtRes, alarmRes] = await Promise.all([listDevices(), listRealtime(), listAlarms('OPEN')]);
    devices.value = devRes.data || [];
    const map: typeof realtime.value = {};
    for (const s of rtRes.data || []) {
      map[s.deviceId] = {
        online: s.status === 'ONLINE',
        sensors: (s.sensors || []).map((x: any) => ({
          deviceId: s.deviceId, deviceName: '', channel: x.channel, sensorCode: x.sensorCode,
          sensorType: x.sensorType, unit: x.unit, sensorValue: x.sensorValue, online: s.status === 'ONLINE',
        })),
        lastSeen: s.lastSeen ? new Date(s.lastSeen).toLocaleTimeString() : undefined,
      };
    }
    realtime.value = map;
    alarms.value = (alarmRes.data || []) as unknown as typeof alarms.value;
    for (const d of devices.value) {
      const thRes = await listThresholds(d.deviceId);
      for (const t of thRes.data || []) {
        thresholds.value[`${t.deviceId}:${t.metric}`] = { min: t.minValue, max: t.maxValue };
      }
    }
  } catch (error) {
    ElMessage.error((error as Error).message || '加载失败');
  } finally {
    loading.value = false;
  }
}
onMounted(refresh);
</script>

<template>
  <div v-loading="loading">
    <el-card shadow="never">
      <template #header>
        <span class="ov-title">数据总览 · 实时瓦片</span>
        <span class="ov-sub">点击瓦片查看近一天历史曲线；红色为超限</span>
      </template>
      <div class="tile-grid">
        <button v-for="(t, i) in tiles" :key="i" class="kb-tile tile" :class="tileClass(t)" @click="openChart(t)">
          <span class="tile__name">{{ t.deviceName }} · {{ METRIC_NAMES[t.sensorType] || t.sensorCode || '设备' }}</span>
          <b class="tile__value">{{ tileValue(t) }}</b>
          <span class="tile__status">{{ tileStatus(t) }}</span>
        </button>
      </div>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <template #header><span class="ov-title">异常预警 · 持续中 {{ openAlarms.length }} 条</span></template>
      <el-empty v-if="!openAlarms.length" description="当前没有持续中的告警" :image-size="72" />
      <div v-else class="alarm-rows">
        <div v-for="a in openAlarms" :key="a.id" class="alarm-row">
          <span>{{ a.message }}</span>
          <el-tag type="danger" size="small">指标超限</el-tag>
        </div>
      </div>
    </el-card>

    <el-dialog v-model="chartVisible" :title="chartTitle" width="720px">
      <KbLineChart :points="chartPoints" :unit="chartUnit" @pick="(p) => (pickText = `${p.label} · ${p.value} ${chartUnit}`)" />
      <div class="pick">{{ pickText || '点击曲线查看某次采集值' }}</div>
    </el-dialog>
  </div>
</template>

<style scoped>
.ov-title { font-size: var(--kb-fs-subheading); font-weight: 700; letter-spacing: var(--kb-tracking-heading); margin-right: 12px; }
.ov-sub { color: var(--kb-text-muted); font-size: var(--kb-fs-caption); }
.tile-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 12px; }
.tile { display: flex; flex-direction: column; gap: 4px; padding: 14px 16px; text-align: left; cursor: pointer; font: inherit; color: inherit; }
.tile__name { font-size: var(--kb-fs-caption); color: var(--kb-text-muted); }
.tile__value { font-size: 26px; color: var(--kb-brand); letter-spacing: -0.4px; }
.kb-tile--hot .tile__value, .kb-tile--warn .tile__value { color: inherit; }
.tile__status { font-size: var(--kb-fs-caption); color: var(--kb-text-muted); }
.alarm-rows { display: flex; flex-direction: column; gap: 8px; }
.alarm-row { display: flex; justify-content: space-between; align-items: center; padding: 10px 14px; border: 1px solid #efd0cf; background: var(--kb-danger-bg); border-radius: var(--kb-radius-tile); color: var(--kb-danger); }
.pick { margin-top: 8px; color: var(--kb-brand); font-size: var(--kb-fs-caption); min-height: 18px; }
</style>