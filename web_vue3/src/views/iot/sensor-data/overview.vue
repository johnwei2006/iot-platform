<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
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

const { t } = useI18n();
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

function metricName(sensorType: string): string {
  const keys: Record<string, string> = { '1': 'data.metricTemp', '2': 'data.metricHumid', '3': 'data.metricPress' };
  const key = keys[sensorType];
  return key ? t(key) : '';
}

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
function tileValue(item: SensorItem) {
  return item.online && !Number.isNaN(item.sensorValue) ? `${item.sensorValue.toFixed(1)} ${item.unit}` : t('data.noRealtime');
}
function tileStatus(item: SensorItem) {
  if (!item.online) return t('common.offline');
  const th = thresholds.value[`${item.deviceId}:${metricKey(item.sensorType)}`];
  const out = th && ((th.min != null && item.sensorValue < th.min) || (th.max != null && item.sensorValue > th.max));
  return out ? t('data.exceeded') : t('data.normal');
}

async function openChart(item: SensorItem) {
  chartTitle.value = `${item.deviceName} · ${metricName(item.sensorType) || item.sensorCode || t('data.reading')} ${t('data.historySuffix')}`;
  chartUnit.value = item.unit;
  const to = Date.now();
  const from = to - 24 * 3600 * 1000;
  try {
    const res = await queryData(item.deviceId, from, to);
    const rows = (res.data as unknown as { serviceDataDecoded?: { sensors?: { channel: number; sensorValue: number }[] }; createdAt?: string }[]) || [];
    const pts = rows.filter((r) => (r.serviceDataDecoded?.sensors || []).some((s) => String(s.channel) === String(item.channel))).slice(-48).map((r) => ({ label: (r.createdAt || '').slice(5, 16).replace('T', ' '), value: (r.serviceDataDecoded?.sensors || []).find((s) => String(s.channel) === String(item.channel))?.sensorValue ?? 0 }));
    chartPoints.value = pts.length ? pts : [{ label: t('common.empty'), value: 0 }];
    chartVisible.value = true;
  } catch (error) {
    ElMessage.error((error as Error).message || t('sensorData.historyLoadFailed'));
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
      for (const th of thRes.data || []) {
        thresholds.value[`${th.deviceId}:${th.metric}`] = { min: th.minValue, max: th.maxValue };
      }
    }
  } catch (error) {
    ElMessage.error((error as Error).message || t('common.loadFailed'));
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
        <span class="ov-title">{{ $t('data.title') }}</span>
        <span class="ov-sub">{{ $t('data.subtitle') }}</span>
      </template>
      <div class="tile-grid">
        <button v-for="(item, i) in tiles" :key="i" class="kb-tile tile" :class="tileClass(item)" @click="openChart(item)">
          <span class="tile__name">{{ item.deviceName }} · {{ metricName(item.sensorType) || item.sensorCode || $t('device.title') }}</span>
          <b class="tile__value">{{ tileValue(item) }}</b>
          <span class="tile__status">{{ tileStatus(item) }}</span>
        </button>
      </div>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <template #header><span class="ov-title">{{ $t('data.alarmTitle', { count: openAlarms.length }) }}</span></template>
      <el-empty v-if="!openAlarms.length" :description="$t('data.noOpenAlarms')" :image-size="72" />
      <div v-else class="alarm-rows">
        <div v-for="a in openAlarms" :key="a.id" class="alarm-row">
          <span>{{ a.message }}</span>
          <el-tag type="danger" size="small">{{ $t('alarm.kindThreshold') }}</el-tag>
        </div>
      </div>
    </el-card>

    <el-dialog v-model="chartVisible" :title="chartTitle" width="720px">
      <KbLineChart :points="chartPoints" :unit="chartUnit" @pick="(p) => (pickText = `${p.label} · ${p.value} ${chartUnit}`)" />
      <div class="pick">{{ pickText || $t('data.pickHint') }}</div>
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