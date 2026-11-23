<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { listDevices } from '@/api/devices';
import { listAlarms } from '@/api/alarms';
import { listRealtime } from '@/api/kb';
import { storage } from '@/utils/storage';
import { usePlotStore } from '@/store/modules/plot';
import type { Device } from '@/types';
import type { RealtimeStatus } from '@/types/kb';

const large = ref(storage.get<boolean>('largeFont') || false);
const plot = usePlotStore();
const areaPopShow = ref(false);
const currentPlot = computed(() => plot.currentPlot);

interface Notice { type: 'warn' | 'normal'; text: string }
const notice = ref<Notice | null>({
  type: 'warn',
  text: '【紧急】1#冷棚温度7.2℃，低于8℃阈值，注意防寒！',
});

const outTemp = ref<number>(5);
const outHum = ref<number>(42);
const wind = ref<string>('西北风2级');
const rain = ref<number>(0);
const inTemp = ref<number>(7.2);
const inHum = ref<number>(88);
const co2 = ref<number>(1100);
const light = ref<number>(320);
const tempTag = ref('偏低');
const tempStatus = ref('warn-card');
const humTag = ref('适宜');
const humStatus = ref('ok-card');
const plotInfo = reactive({ seed: 'L808', batch: '秋冬出菇茬', date: '2026-08-02' });

const devices = ref<Device[]>([]);
const activeDevice = ref('');
const realtime = ref<RealtimeStatus | null>(null);

const METRIC_NAMES: Record<string, string> = { '1': '温度', '2': '湿度', '3': '气压' };

function sensorBy(type: string) {
  return (realtime.value?.sensors || []).find((s) => s.sensorType === type);
}

/** 传感器卡片 → 历史数据页（device-detail）。无登录态时用模拟设备 id 占位，保证历史页仍可渲染。 */
function goHistory(type: string, name: string, value: number) {
  const s = sensorBy(type);
  const deviceId = activeDevice.value || 'LOCAL-PLOT';
  const channel = s ? s.channel : type;
  const query = `deviceId=${encodeURIComponent(deviceId)}&channel=${encodeURIComponent(String(channel))}&name=${encodeURIComponent(name)}&value=${value}&metric=${encodeURIComponent(type)}`;
  uni.navigateTo({ url: `/pages/device-detail/device-detail?${query}` });
}

function openAreaPop() { areaPopShow.value = true; }
function closeAreaPop() { areaPopShow.value = false; }
function selectArea(val: string) {
  plot.selectPlot(val);
  areaPopShow.value = false;
  uni.showToast({ title: '已切换：' + val });
}
function onRefresh() {
  uni.showToast({ title: '刷新中', icon: 'loading' });
  void refresh();
  setTimeout(() => uni.hideToast(), 800);
}
function goAlarm() { uni.navigateTo({ url: '/pages/alarms/alarms' }); }

async function refresh() {
  if (!storage.get<string>('token')) return;
  try {
    const devRes = await listDevices();
    devices.value = devRes.data || [];
    if (!activeDevice.value && devices.value.length) {
      const saved = uni.getStorageSync('kb.activeDevice') as string;
      activeDevice.value = saved && devices.value.some((d) => d.deviceId === saved) ? saved : devices.value[0].deviceId;
    }
    const rtRes = await listRealtime();
    realtime.value = (rtRes.data || []).find((x) => x.deviceId === activeDevice.value) || null;
    const t = sensorBy('1');
    const h = sensorBy('2');
    if (t) inTemp.value = t.sensorValue;
    if (h) inHum.value = h.sensorValue;

    const alarmRes = await listAlarms('OPEN');
    const rows = (alarmRes.data || []) as { message?: string }[];
    if (rows.length) notice.value = { type: 'warn', text: rows[0].message || '' };
  } catch { /* 静默，保留 dist 默认展示 */ }
}

onShow(() => { plot.restore(); void refresh(); });
</script>

<template>
  <view class="container" :class="{ 'kb-large': large }">
    <view class="nav-bar">
      <view class="nav-area-text">{{ currentPlot }}</view>
      <view class="nav-right">
        <view class="area-btn" @tap="openAreaPop">切换</view>
        <view class="refresh" @tap="onRefresh">↻</view>
      </view>
    </view>

    <view v-if="areaPopShow" class="pop-mask" @tap="closeAreaPop">
      <view class="pop area-pop" @tap.stop>
        <view class="pop-title">选择区域</view>
        <view
          v-for="(item, idx) in plot.plotList"
          :key="idx"
          class="area-item"
          :class="{ active: currentPlot === item }"
          @tap="selectArea(item)"
        >{{ item }}</view>
        <view class="pop-cancel" @tap="closeAreaPop">取消</view>
      </view>
    </view>

    <view v-if="notice" class="notice" :class="notice.type === 'warn' ? 'warn-notice' : 'normal-notice'" @tap="goAlarm">
      {{ notice.text }}
    </view>

    <view class="block">
      <view class="block-title">棚外气象</view>
      <view class="card-row">
        <view class="data-card" hover-class="kb-press" @tap="goHistory('OUT_TEMP', '室外温度', outTemp)"><view class="val">{{ outTemp }}℃</view><view class="label">室外温度</view></view>
        <view class="data-card" hover-class="kb-press" @tap="goHistory('OUT_HUM', '室外湿度', outHum)"><view class="val">{{ outHum }}%</view><view class="label">室外湿度</view></view>
        <view class="data-card" hover-class="kb-press" @tap="goHistory('WIND', '风向风速', 0)"><view class="val">{{ wind }}</view><view class="label">风向风速</view></view>
        <view class="data-card" hover-class="kb-press" @tap="goHistory('RAIN', '降雨量', rain)"><view class="val">{{ rain }}mm</view><view class="label">降雨量</view></view>
      </view>
    </view>

    <view class="block">
      <view class="block-title">棚内环境监测</view>
      <view class="card-row">
        <view class="data-card" :class="tempStatus" hover-class="kb-press" @tap="goHistory('1', '棚内温度', inTemp)">
          <view class="val">{{ inTemp }}℃</view><view class="label">棚内温度</view><view class="tag">{{ tempTag }}</view>
        </view>
        <view class="data-card" :class="humStatus" hover-class="kb-press" @tap="goHistory('2', '棚内湿度', inHum)">
          <view class="val">{{ inHum }}%</view><view class="label">棚内湿度</view><view class="tag">{{ humTag }}</view>
        </view>
        <view class="data-card" hover-class="kb-press" @tap="goHistory('4', 'CO₂浓度', co2)">
          <view class="val">{{ co2 }}ppm</view><view class="label">CO₂浓度</view>
        </view>
        <view class="data-card" hover-class="kb-press" @tap="goHistory('5', '光照', light)">
          <view class="val">{{ light }}lux</view><view class="label">光照</view>
        </view>
      </view>
    </view>

    <view class="block note-block">
      <view class="note">番茄：{{ plotInfo.seed }}｜种植：{{ plotInfo.date }}</view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.container { padding: 12rpx; background-color: #f7f8fa; min-height: 100vh; box-sizing: border-box; }
.nav-bar { display: flex; align-items: center; justify-content: space-between; padding: 16rpx 12rpx; background: #fff; border-radius: 12rpx; margin-bottom: 12rpx; }
.nav-area-text { font-size: 32rpx; font-weight: bold; }
.nav-right { display: flex; align-items: center; gap: 16rpx; }
.area-btn { font-size: 28rpx; color: #07c160; padding: 4rpx 8rpx; border: 1rpx solid #07c160; border-radius: 6rpx; }
.refresh { font-size: 32rpx; padding: 0 4rpx; }
.notice { padding: 16rpx; border-radius: 10rpx; margin-bottom: 12rpx; font-size: 28rpx; }
.warn-notice { background: #ff4d4f; color: #fff; }
.normal-notice { background: #eee; color: #333; }
.block { background: #fff; border-radius: 12rpx; padding: 20rpx; margin-bottom: 12rpx; }
.block-title { font-size: 30rpx; font-weight: bold; margin-bottom: 16rpx; }
.card-row { display: grid; grid-template-columns: 1fr 1fr; gap: 12rpx; }
.data-card { border: 1rpx solid #eee; border-radius: 10rpx; padding: 20rpx 10rpx; text-align: center; }
.data-card.ok-card { border-color: #07c160; }
.data-card.warn-card { border-color: #ff7d00; }
.data-card.err-card { border-color: #ff4d4f; }
.data-card.offline { color: #999; border-color: #ddd; }
.val { font-size: 40rpx; font-weight: bold; }
.label { font-size: 24rpx; color: #666; margin-top: 6rpx; }
.tag { font-size: 24rpx; margin-top: 6rpx; }
.note-block { background: #f0f7ff; }
.note { font-size: 26rpx; color: #333; line-height: 1.6; }
.pop-mask { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0, 0, 0, 0.5); display: flex; align-items: center; justify-content: center; z-index: 999; }
.pop { width: 80%; background: #fff; border-radius: 12rpx; padding: 24rpx; }
.area-pop { width: 85%; }
.pop-title { text-align: center; font-size: 32rpx; font-weight: bold; margin-bottom: 20rpx; }
.area-item { padding: 24rpx 12rpx; font-size: 28rpx; border-bottom: 1rpx solid #eee; }
.area-item.active { color: #07c160; font-weight: bold; }
.pop-cancel { text-align: center; padding: 20rpx; font-size: 28rpx; color: #666; margin-top: 12rpx; }
.kb-large .nav-area-text { font-size: 38rpx; }
.kb-large .val { font-size: 46rpx; }
.kb-large .notice, .kb-large .note { font-size: 32rpx; }
</style>
