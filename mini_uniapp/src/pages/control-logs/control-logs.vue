<script setup lang="ts">
import { computed, ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { listControlLogs } from '@/api/kb';
import { storage } from '@/utils/storage';
import type { ControlLog } from '@/types/kb';

const large = ref(storage.get<boolean>('largeFont') || false);
const loading = ref(false);
const logs = ref<ControlLog[]>([]);
const resultFilter = ref('ALL');

const RESULT_OPTIONS = ['ALL', 'PENDING', 'SUCCESS', 'TIMEOUT', 'FAILED', 'PARTIAL'];
const RESULT_LABELS: Record<string, string> = {
  ALL: '全部', PENDING: '执行中', SUCCESS: '成功', TIMEOUT: '超时', FAILED: '失败', PARTIAL: '部分成功',
};
const RESULT_COLORS: Record<string, string> = {
  PENDING: 'warn', SUCCESS: 'ok', TIMEOUT: 'off', FAILED: 'bad', PARTIAL: 'warn',
};
const SOURCE_LABELS: Record<string, string> = { MANUAL: '手动', RULE: '规则', GROUP: '组合' };

const filtered = computed(() =>
  logs.value.filter((l) => resultFilter.value === 'ALL' || l.result === resultFilter.value)
);

function fmt(s?: string) {
  return (s || '').slice(0, 19).replace('T', ' ');
}

function actionText(a: string) {
  const map: Record<string, string> = { ON: '开启', OFF: '关闭', OPEN: '卷起', CLOSE: '放下', STOP: '停止', LEVEL: '设置开度' };
  return map[a] || a;
}

async function refresh() {
  if (!storage.get<string>('token')) return;
  loading.value = true;
  try {
    const res = await listControlLogs({});
    logs.value = (res.data || []).slice().reverse();
  } catch { /* 静默 */ } finally { loading.value = false; }
}

onShow(refresh);
</script>
<template>
  <scroll-view scroll-y class="page" :class="{ 'kb-large': large }">
    <scroll-view scroll-x class="seg-scroll">
      <view class="seg-row">
        <view v-for="r in RESULT_OPTIONS" :key="r" class="seg" :class="{ active: resultFilter === r }" @tap="resultFilter = r">
          {{ RESULT_LABELS[r] }}
        </view>
      </view>
    </scroll-view>

    <view v-if="!filtered.length" class="kb-card empty">{{ loading ? '加载中…' : '暂无控制记录' }}</view>
    <view v-for="l in filtered" :key="l.id" class="kb-card log">
      <view class="log-head">
        <text class="log-name">{{ l.deviceName || l.deviceId }}</text>
        <text class="tag" :class="RESULT_COLORS[l.result]">{{ RESULT_LABELS[l.result] }}</text>
      </view>
      <text class="log-sub">{{ SOURCE_LABELS[l.source] || l.source }}下发 · {{ actionText(l.action) }}{{ l.ruleName ? ' · ' + l.ruleName : '' }}</text>
      <text v-if="l.detail" class="log-detail">{{ l.detail }}</text>
      <text class="log-time">{{ fmt(l.createdAt) }}</text>
    </view>
  </scroll-view>
</template>
<style scoped lang="scss">
.page { padding: 24rpx; min-height: 100vh; box-sizing: border-box; }
.seg-scroll { white-space: nowrap; margin-bottom: 20rpx; }
.seg-row { display: inline-flex; gap: 10rpx; background: $kb-bg-sunken; border-radius: $kb-radius-pill; padding: 6rpx; }
.seg { display: inline-block; padding: 14rpx 26rpx; font-size: 25rpx; color: $kb-text-muted; border-radius: $kb-radius-pill; }
.seg.active { background: #fff; color: $kb-brand; font-weight: 700; }
.empty { padding: 40rpx; text-align: center; color: $kb-offline; font-size: 26rpx; }
.log { padding: 24rpx 28rpx; margin-bottom: 16rpx; display: flex; flex-direction: column; gap: 8rpx; }
.log-head { display: flex; justify-content: space-between; align-items: center; }
.log-name { font-size: 29rpx; font-weight: 700; color: $kb-text; }
.tag { font-size: 22rpx; padding: 4rpx 14rpx; border-radius: 8rpx; }
.tag.ok { color: $kb-success; background: $kb-success-bg; }
.tag.warn { color: $kb-warning; background: $kb-warning-bg; }
.tag.bad { color: $kb-danger; background: $kb-danger-bg; }
.tag.off { color: $kb-offline; background: $kb-bg-sunken; }
.log-sub { font-size: 25rpx; color: $kb-text-muted; }
.log-detail { font-size: 24rpx; color: $kb-text-muted; line-height: 1.5; }
.log-time { font-size: 22rpx; color: $kb-offline; }
.kb-large .log-name, .kb-large .log-sub, .kb-large .log-detail { font-size: 31rpx; }
</style>
