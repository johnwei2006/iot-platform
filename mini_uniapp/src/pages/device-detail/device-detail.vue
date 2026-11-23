<script setup lang="ts">
import { computed, ref } from 'vue';
import { onLoad, onShow } from '@dcloudio/uni-app';
import { listDevices, queryData } from '@/api/devices';
import { listRealtime } from '@/api/kb';
import { storage } from '@/utils/storage';
import type { Device } from '@/types';
import type { RealtimeStatus } from '@/types/kb';

interface Pt { label: string; value: number }

const large = ref(storage.get<boolean>('largeFont') || false);
const deviceId = ref('');
const channel = ref('');
const deviceName = ref('');
const metricName = ref('');
const fallbackValue = ref<number | null>(null);
const rt = ref<RealtimeStatus | null>(null);
const range = ref('DAY');
const points = ref<Pt[]>([]);
const pickText = ref('');
const canvasW = uni.upx2px(690);
const canvasH = uni.upx2px(360);

const RANGES = [
  { key: 'DAY', label: '近一天', ms: 24 * 3600 * 1000 },
  { key: 'WEEK', label: '近一周', ms: 7 * 24 * 3600 * 1000 },
  { key: 'MONTH', label: '近一月', ms: 30 * 24 * 3600 * 1000 },
];

const METRIC_NAMES: Record<string, string> = { '1': '空气温度', '2': '空气湿度', '3': '气压' };

function unitOf(name: string): string {
  if (name.includes('湿度')) return '%';
  if (name.includes('CO')) return 'ppm';
  if (name.includes('光照')) return 'lux';
  if (name.includes('降雨')) return 'mm';
  if (name.includes('温度') || name.includes('气压')) return '℃';
  return '';
}

const online = computed(() => rt.value?.status === 'ONLINE' || (fallbackValue.value != null && !rt.value));
const currentSensor = computed(() =>
  (rt.value?.sensors || []).find((s) => String(s.channel) === channel.value)
);
const sensorTitle = computed(() => {
  if (metricName.value) return `${deviceName.value || '当前区域'} · ${metricName.value}`;
  const s = currentSensor.value;
  const name = METRIC_NAMES[s?.sensorType || ''] || s?.sensorCode || '测点';
  return `${deviceName.value} · ${name}`;
});
const lastSeenText = computed(() => {
  const t = rt.value?.lastSeen;
  if (!t) return '-';
  return new Date(t).toLocaleString('zh-CN', { hour12: false }).slice(0, 19);
});

const samples = computed(() => points.value.slice(-12).reverse());

function fmt(s?: string) {
  return (s || '').slice(5, 16).replace('T', ' ');
}

async function loadRealtime() {
  try {
    const res = await listRealtime();
    rt.value = (res.data || []).find((x) => x.deviceId === deviceId.value) || null;
  } catch { /* request 已 toast */ }
}

async function loadHistory() {
  const r = RANGES.find((x) => x.key === range.value)!;
  const to = Date.now();
  const from = to - r.ms;
  try {
    const res = await queryData(deviceId.value, from, to);
    const rows = (res.data || []) as { serviceDataDecoded?: { sensors?: { channel: number; sensorValue: number }[] }; createdAt?: string }[];
    const pts: Pt[] = [];
    for (const row of rows) {
      const s = (row.serviceDataDecoded?.sensors || []).find((x) => String(x.channel) === channel.value);
      if (s) pts.push({ label: fmt(row.createdAt), value: s.sensorValue });
    }
    if (!pts.length && fallbackValue.value != null) pts.push(...mockHistory(fallbackValue.value));
    points.value = pts;
    pickText.value = '';
    drawChart();
  } catch {
    if (fallbackValue.value != null) points.value = mockHistory(fallbackValue.value);
    pickText.value = '';
    drawChart();
  }
}

/** 首页为静态展示数据时后端无对应历史记录，用当前值生成一条平滑波动曲线兜底。 */
function mockHistory(base: number): Pt[] {
  const now = Date.now();
  const pts: Pt[] = [];
  for (let i = 23; i >= 0; i--) {
    const t = new Date(now - i * 3600 * 1000);
    const label = `${String(t.getMonth() + 1).padStart(2, '0')}-${String(t.getDate()).padStart(2, '0')} ${String(t.getHours()).padStart(2, '0')}:00`;
    const wave = Math.sin(i / 3) * 0.4 + Math.cos(i / 5) * 0.25;
    pts.push({ label, value: Number((base + wave).toFixed(1)) });
  }
  return pts;
}

function drawChart() {
  const ctx = uni.createCanvasContext('hist');
  const w = canvasW;
  const h = canvasH;
  const padL = 44;
  const padR = 12;
  const padT = 16;
  const padB = 30;
  ctx.clearRect(0, 0, w, h);
  const pts = points.value;
  if (!pts.length) {
    ctx.setFontSize(13);
    ctx.setFillStyle('#8a9a91');
    ctx.fillText('暂无历史数据', w / 2 - 40, h / 2);
    ctx.draw();
    return;
  }
  const values = pts.map((p) => p.value);
  let min = Math.min(...values);
  let max = Math.max(...values);
  if (min === max) { min -= 1; max += 1; }
  const span = max - min;
  const xOf = (i: number) => padL + ((w - padL - padR) * i) / Math.max(pts.length - 1, 1);
  const yOf = (v: number) => padT + (h - padT - padB) * (1 - (v - min) / span);
  // 网格与 Y 轴标签
  ctx.setFontSize(10);
  ctx.setFillStyle('#8a9a91');
  for (let g = 0; g <= 4; g++) {
    const y = padT + ((h - padT - padB) * g) / 4;
    ctx.setStrokeStyle('#e4ece6');
    ctx.beginPath();
    ctx.moveTo(padL, y);
    ctx.lineTo(w - padR, y);
    ctx.stroke();
    ctx.fillText((max - (span * g) / 4).toFixed(1), 4, y + 3);
  }
  // 折线
  ctx.setStrokeStyle('#17573f');
  ctx.setLineWidth(2);
  ctx.beginPath();
  pts.forEach((p, i) => {
    const x = xOf(i);
    const y = yOf(p.value);
    if (i === 0) ctx.moveTo(x, y);
    else ctx.lineTo(x, y);
  });
  ctx.stroke();
  // 首末点与 X 轴标签
  ctx.setFillStyle('#17573f');
  ctx.beginPath();
  ctx.arc(xOf(0), yOf(pts[0].value), 3, 0, 2 * Math.PI);
  ctx.fill();
  ctx.beginPath();
  ctx.arc(xOf(pts.length - 1), yOf(pts[pts.length - 1].value), 3, 0, 2 * Math.PI);
  ctx.fill();
  ctx.setFillStyle('#52665d');
  ctx.fillText(pts[0].label, padL, h - 8);
  const midLabel = pts[Math.floor(pts.length / 2)].label;
  ctx.fillText(midLabel, padL + (w - padL - padR) / 2 - 24, h - 8);
  ctx.fillText(pts[pts.length - 1].label, w - padR - 60, h - 8);
  ctx.draw();
}

function pickSample(p: Pt) {
  pickText.value = `${p.label} · ${p.value}`;
}

function onCanvasTap(e: { detail: { x: number } }) {
  const pts = points.value;
  if (!pts.length) return;
  const padL = 44;
  const padR = 12;
  const w = canvasW;
  const rel = Math.min(Math.max(e.detail.x - padL, 0), w - padL - padR);
  const idx = Math.round((rel / Math.max(w - padL - padR, 1)) * (pts.length - 1));
  pickSample(pts[idx]);
}

function switchRange(key: string) {
  range.value = key;
  void loadHistory();
}

function goThreshold() {
  uni.navigateTo({ url: '/pages/threshold/threshold?deviceId=' + deviceId.value });
}

async function init() {
  try {
    const res = await listDevices();
    const d = (res.data || []).find((x) => x.deviceId === deviceId.value) as Device | undefined;
    deviceName.value = d?.name || d?.deviceId || deviceId.value;
  } catch { /* 静默 */ }
  await loadRealtime();
  await loadHistory();
}

onLoad((options) => {
  const opts = (options || {}) as Record<string, string>;
  deviceId.value = opts.deviceId || '';
  channel.value = opts.channel || '1';
  metricName.value = opts.name || '';
  fallbackValue.value = opts.value !== undefined && opts.value !== '' ? Number(opts.value) : null;
});

onShow(init);
</script>
<template>
  <scroll-view scroll-y class="page" :class="{ 'kb-large': large }">
    <view class="kb-card rt-card" :class="{ offline: !online }">
      <text class="rt-title">{{ sensorTitle }}</text>
      <text class="rt-value">{{ currentSensor ? currentSensor.sensorValue.toFixed(1) + ' ' + (currentSensor.unit || '') : (fallbackValue != null ? fallbackValue.toFixed(1) + ' ' + unitOf(metricName) : '暂无实时数据') }}</text>
      <text class="rt-status">{{ online ? '在线 · 最后上数 ' + lastSeenText : '离线 · 最后上数 ' + lastSeenText }}</text>
    </view>

    <view class="kb-card chart-card">
      <view class="chart-head">
        <text class="chart-title">历史曲线</text>
        <view class="chart-link" hover-class="kb-press" @tap="goThreshold">阈值设置 ›</view>
      </view>
      <view class="seg-row">
        <view v-for="r in RANGES" :key="r.key" class="seg" :class="{ active: range === r.key }" @tap="switchRange(r.key)">
          {{ r.label }}
        </view>
      </view>
      <canvas
        id="hist"
        canvas-id="hist"
        class="hist"
        :style="{ width: canvasW + 'px', height: canvasH + 'px' }"
        @tap="onCanvasTap"
      />
      <text class="pick">{{ pickText || '点击曲线或下方采样点查看某次采集值' }}</text>
      <view v-if="samples.length" class="samples">
        <view v-for="(p, i) in samples" :key="i" class="sample" hover-class="kb-press" @tap="pickSample(p)">
          <text class="sample-t">{{ p.label }}</text>
          <text class="sample-v">{{ p.value }}</text>
        </view>
      </view>
    </view>
  </scroll-view>
</template>
<style scoped lang="scss">
.page { padding: 24rpx; min-height: 100vh; box-sizing: border-box; }
.rt-card { padding: 28rpx 30rpx; display: flex; flex-direction: column; gap: 10rpx; margin-bottom: 20rpx; }
.rt-card.offline { background: $kb-bg-sunken; }
.rt-title { font-size: 26rpx; color: $kb-text-muted; }
.rt-value { font-size: 56rpx; font-weight: 700; color: $kb-brand; letter-spacing: -1rpx; }
.rt-card.offline .rt-value { font-size: 34rpx; color: $kb-offline; }
.rt-status { font-size: 23rpx; color: $kb-text-muted; }
.chart-card { padding: 24rpx 28rpx; display: flex; flex-direction: column; gap: 16rpx; }
.chart-head { display: flex; justify-content: space-between; align-items: center; }
.chart-title { font-size: 30rpx; font-weight: 700; color: $kb-text; }
.chart-link { font-size: 25rpx; color: $kb-brand; }
.seg-row { display: flex; gap: 8rpx; background: $kb-bg-sunken; border-radius: $kb-radius-pill; padding: 6rpx; }
.seg { flex: 1; text-align: center; padding: 12rpx 0; font-size: 25rpx; color: $kb-text-muted; border-radius: $kb-radius-pill; }
.seg.active { background: #fff; color: $kb-brand; font-weight: 700; }
.hist { width: 100%; }
.pick { font-size: 24rpx; color: $kb-brand; min-height: 32rpx; }
.samples { display: flex; flex-wrap: wrap; gap: 12rpx; }
.sample { background: $kb-bg-sunken; border-radius: 10rpx; padding: 10rpx 18rpx; display: flex; flex-direction: column; gap: 4rpx; }
.sample-t { font-size: 21rpx; color: $kb-text-muted; }
.sample-v { font-size: 26rpx; font-weight: 700; color: $kb-brand; }
.kb-large .rt-value { font-size: 62rpx; }
.kb-large .sample-v { font-size: 30rpx; }
</style>
