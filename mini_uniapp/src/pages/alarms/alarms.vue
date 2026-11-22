<script setup lang="ts">
import { ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { listAlarms, resolveAlarm } from '@/api/alarms';
import { storage } from '@/utils/storage';
import type { Alarm } from '@/types';

const large = ref(storage.get<boolean>('largeFont') || false);
const loading = ref(false);
const alarms = ref<Alarm[]>([]);
const statusFilter = ref('OPEN');

const STATUS_OPTIONS = [
  { key: '', label: '全部' },
  { key: 'OPEN', label: '持续中' },
  { key: 'RESOLVED', label: '已恢复' },
];

function fmt(s?: string) {
  return (s || '').slice(0, 19).replace('T', ' ');
}

async function load() {
  if (!storage.get<string>('token')) return;
  loading.value = true;
  try {
    const res = await listAlarms(statusFilter.value || undefined);
    alarms.value = (res.data || []).slice().reverse();
  } catch { /* 静默 */ } finally { loading.value = false; }
}

function switchFilter(key: string) {
  statusFilter.value = key;
  void load();
}

function resolve(a: Alarm) {
  if (!a.id) return;
  resolveAlarm(a.id)
    .then(() => {
      uni.showToast({ title: '已标记恢复', icon: 'success' });
      void load();
    })
    .catch(() => { /* request 已 toast */ });
}

onShow(load);
</script>
<template>
  <scroll-view scroll-y class="page" :class="{ 'kb-large': large }">
    <view class="seg-row">
      <view v-for="s in STATUS_OPTIONS" :key="s.key" class="seg" :class="{ active: statusFilter === s.key }" @tap="switchFilter(s.key)">
        {{ s.label }}
      </view>
    </view>

    <view v-if="!alarms.length" class="kb-card empty">{{ loading ? '加载中…' : '暂无告警' }}</view>
    <view v-for="a in alarms" :key="a.id" class="kb-card alarm" :class="{ open: a.status === 'OPEN' }">
      <view class="alarm-head">
        <text class="alarm-msg">{{ a.message || a.rule || '指标异常' }}</text>
        <text class="tag" :class="a.status === 'OPEN' ? 'bad' : 'ok'">{{ a.status === 'OPEN' ? '持续中' : '已恢复' }}</text>
      </view>
      <text class="alarm-sub">{{ a.deviceId }}{{ a.level ? ' · ' + a.level : '' }}</text>
      <view class="alarm-foot">
        <text class="alarm-time">{{ fmt(a.createdAt) }}</text>
        <view v-if="a.status === 'OPEN'" class="mini-btn" hover-class="kb-press" @tap="resolve(a)">标记恢复</view>
      </view>
    </view>
  </scroll-view>
</template>
<style scoped lang="scss">
.page { padding: 24rpx; min-height: 100vh; box-sizing: border-box; }
.seg-row { display: flex; gap: 10rpx; margin-bottom: 20rpx; background: $kb-bg-sunken; border-radius: $kb-radius-pill; padding: 6rpx; }
.seg { flex: 1; text-align: center; padding: 14rpx 0; font-size: 25rpx; color: $kb-text-muted; border-radius: $kb-radius-pill; }
.seg.active { background: #fff; color: $kb-brand; font-weight: 700; }
.empty { padding: 40rpx; text-align: center; color: $kb-offline; font-size: 26rpx; }
.alarm { padding: 24rpx 28rpx; margin-bottom: 16rpx; display: flex; flex-direction: column; gap: 10rpx; }
.alarm.open { background: $kb-danger-bg; border-color: #efd0cf; }
.alarm-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 14rpx; }
.alarm-msg { flex: 1; font-size: 28rpx; color: $kb-text; line-height: 1.45; }
.alarm.open .alarm-msg { color: $kb-danger; font-weight: 600; }
.tag { flex-shrink: 0; font-size: 22rpx; padding: 4rpx 14rpx; border-radius: 8rpx; }
.tag.bad { color: $kb-danger; background: #fff; border: 1rpx solid $kb-danger; }
.tag.ok { color: $kb-success; background: $kb-success-bg; }
.alarm-sub { font-size: 24rpx; color: $kb-text-muted; }
.alarm-foot { display: flex; justify-content: space-between; align-items: center; }
.alarm-time { font-size: 22rpx; color: $kb-offline; }
.mini-btn { font-size: 25rpx; color: $kb-danger; border: 1rpx solid $kb-danger; border-radius: 8rpx; padding: 8rpx 22rpx; background: #fff; }
.kb-large .alarm-msg, .kb-large .alarm-sub { font-size: 31rpx; }
</style>
