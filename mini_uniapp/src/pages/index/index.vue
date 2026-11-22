<script setup lang="ts">
import { computed, ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { listDevices } from '@/api/devices';
import { listAlarms } from '@/api/alarms';
import { listRealtime, listThresholds } from '@/api/kb';
import { storage } from '@/utils/storage';
import type { Alarm, Device } from '@/types';
import type { RealtimeStatus } from '@/types/kb';

interface Tile {
  deviceId: string;
  deviceName: string;
  channel: number;
  sensorType: string;
  unit: string;
  value: number;
  online: boolean;
  hot: boolean;
}

const large = ref(storage.get<boolean>('largeFont') || false);
const loading = ref(false);
const devices = ref<Device[]>([]);
const activeDevice = ref('');
const realtime = ref<Record<string, RealtimeStatus>>({});
const thresholds = ref<Record<string, { min?: number; max?: number }>>({});
const alarms = ref<Alarm[]>([]);

const METRIC_NAMES: Record<string, string> = { '1': '温度', '2': '湿度', '3': '气压' };

/** sensorType → 阈值表 metric 键（与后端 ConditionRuleEvaluateHandler.metricKey 一致） */
function metricKey(sensorType: string): string {
  const map: Record<string, string> = { '1': 'temp', '2': 'humid', '3': 'press' };
  return map[sensorType] || '';
}

const deviceNames = computed(() =>
  devices.value.map((d) => ({ id: d.deviceId, name: d.name || d.deviceId }))
);

const currentName = computed(() => {
  const found = deviceNames.value.find((d) => d.id === activeDevice.value);
  return found ? found.name : '选择设备';
});

const tiles = computed<Tile[]>(() => {
  const rt = realtime.value[activeDevice.value];
  const sensors = rt?.sensors || [];
  return sensors.map((s) => {
    const th = thresholds.value[`${activeDevice.value}:${metricKey(s.sensorType || '')}`];
    const hot = rt?.status === 'ONLINE' && !!th && ((th.min != null && s.sensorValue < th.min) || (th.max != null && s.sensorValue > th.max));
    return {
      deviceId: activeDevice.value,
      deviceName: currentName.value,
      channel: s.channel,
      sensorType: s.sensorType || '',
      unit: s.unit || '',
      value: s.sensorValue,
      online: rt?.status === 'ONLINE',
      hot,
    };
  });
});

const openAlarms = computed(() => alarms.value.filter((a) => a.status === 'OPEN'));

function tileClass(t: Tile) {
  if (!t.online) return 'offline';
  return t.hot ? 'hot' : '';
}

function tileValue(t: Tile) {
  return t.online ? `${t.value.toFixed(1)} ${t.unit}` : '暂无实时数据';
}

function pickDevice(id: string) {
  activeDevice.value = id;
  uni.setStorageSync('kb.activeDevice', id);
}

async function refresh() {
  if (!storage.get<string>('token')) return;
  loading.value = true;
  try {
    const devRes = await listDevices();
    devices.value = devRes.data || [];
    if (!activeDevice.value && devices.value.length) {
      const saved = uni.getStorageSync('kb.activeDevice') as string;
      activeDevice.value = saved && devices.value.some((d) => d.deviceId === saved) ? saved : devices.value[0].deviceId;
    }
    const rtRes = await listRealtime();
    const map: Record<string, RealtimeStatus> = {};
    for (const s of rtRes.data || []) map[s.deviceId] = s;
    realtime.value = map;
    const alarmRes = await listAlarms('OPEN');
    alarms.value = (alarmRes.data || []) as unknown as Alarm[];
    for (const d of devices.value) {
      const thRes = await listThresholds(d.deviceId);
      for (const t of thRes.data || []) {
        thresholds.value[`${t.deviceId}:${t.metric}`] = { min: t.minValue, max: t.maxValue };
      }
    }
  } catch {
    /* 静默，页面留空态 */
  } finally {
    loading.value = false;
  }
}

onShow(refresh);

function showSwitch() {
  const names = deviceNames.value.map((d) => d.name);
  uni.showActionSheet({ itemList: names, success: (res) => pickDevice(deviceNames.value[res.tapIndex].id) });
}
function goDetail(t: Tile) {
  uni.navigateTo({ url: `/pages/device-detail/device-detail?deviceId=${t.deviceId}&channel=${t.channel}` });
}
function goAlarms() {
  uni.navigateTo({ url: "/pages/alarms/alarms" });
}
</script>
<template>
  <scroll-view scroll-y class="page" :class="{ 'kb-large': large }">
    <view class="kb-card switch-row" hover-class="kb-press" @tap="showSwitch">
      <view class="switch-main">
        <text class="switch-title">{{ currentName }}</text>
        <text class="switch-sub">点击切换设备 · {{ tiles.length }} 个测点</text>
      </view>
      <text class="chev">›</text>
    </view>

    <view class="section-title">实时数据</view>
    <view v-if="!tiles.length" class="kb-card empty">暂无实时数据</view>
    <view v-else class="tile-grid">
      <view
        v-for="(t, i) in tiles"
        :key="i"
        class="kb-card tile kb-press"
        :class="tileClass(t)"
        @tap="goDetail(t)"
      >
        <text class="tile-name">{{ METRIC_NAMES[t.sensorType] || '测点' }}</text>
        <text class="tile-value">{{ tileValue(t) }}</text>
        <text class="tile-status">{{ t.online ? (t.hot ? '超出阈值' : '正常') : '离线' }}</text>
      </view>
    </view>

    <view class="section-title">异常预警 · 持续中 {{ openAlarms.length }}</view>
    <view v-if="!openAlarms.length" class="kb-card empty">当前没有持续中的告警</view>
    <view
      v-for="a in openAlarms"
      :key="a.id"
      class="kb-card alarm-row kb-press"
      @tap="goAlarms"
    >
      <text class="alarm-msg">{{ a.message }}</text>
      <text class="alarm-tag">指标超限</text>
    </view>
  </scroll-view>
</template>

<style scoped lang="scss">
.page { padding: 24rpx; min-height: 100vh; box-sizing: border-box; }
.switch-row { display: flex; align-items: center; justify-content: space-between; padding: 26rpx 30rpx; }
.switch-main { display: flex; flex-direction: column; gap: 6rpx; }
.switch-title { font-size: 34rpx; font-weight: 700; color: $kb-text; letter-spacing: -0.5rpx; }
.switch-sub { font-size: 24rpx; color: $kb-text-muted; }
.chev { color: $kb-brand; font-size: 40rpx; }
.section-title { font-size: 30rpx; font-weight: 700; color: $kb-text; margin: 30rpx 6rpx 16rpx; }
.tile-grid { display: flex; flex-wrap: wrap; gap: 16rpx; }
.tile { width: calc(50% - 8rpx); display: flex; flex-direction: column; gap: 8rpx; padding: 24rpx; box-sizing: border-box; }
.tile-name { font-size: 24rpx; color: $kb-text-muted; }
.tile-value { font-size: 44rpx; font-weight: 700; color: $kb-brand; letter-spacing: -1rpx; }
.tile.hot .tile-value { color: $kb-danger; }
.tile-status { font-size: 22rpx; color: $kb-text-muted; }
.tile.offline .tile-value { font-size: 30rpx; color: $kb-offline; }
.tile.offline { background: $kb-bg-sunken; }
.tile.hot { background: $kb-danger-bg; border-color: #efd0cf; }
.empty { padding: 40rpx; text-align: center; color: $kb-offline; font-size: 26rpx; }
.alarm-row { display: flex; justify-content: space-between; align-items: center; padding: 22rpx 26rpx; background: $kb-danger-bg; border-color: #efd0cf; margin-bottom: 14rpx; }
.alarm-msg { color: $kb-danger; font-size: 27rpx; flex: 1; }
.alarm-tag { color: $kb-danger; font-size: 22rpx; border: 1rpx solid $kb-danger; border-radius: 8rpx; padding: 2rpx 10rpx; }
.kb-large .switch-title { font-size: 38rpx; }
.kb-large .tile-value { font-size: 50rpx; }
.kb-large .alarm-msg { font-size: 31rpx; }
</style>