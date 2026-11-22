<script setup lang="ts">
import { computed, ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { listRules, toggleRule } from '@/api/kb';
import { storage } from '@/utils/storage';
import type { ControlRule } from '@/types/kb';

const large = ref(storage.get<boolean>('largeFont') || false);
const loading = ref(false);
const rules = ref<ControlRule[]>([]);
const kindFilter = ref<'ALL' | 'TIMED' | 'CONDITION' | 'GROUP'>('ALL');

const KIND_LABELS: Record<string, string> = { GROUP: '组合', TIMED: '定时', CONDITION: '条件' };

const filtered = computed(() =>
  rules.value.filter((r) => kindFilter.value === 'ALL' || r.kind === kindFilter.value)
);

function parseDevices(json: string): { deviceId: string; deviceName?: string }[] {
  try { return JSON.parse(json || '[]'); } catch { return []; }
}

function summary(r: ControlRule) {
  if (r.trigger === 'MANUAL') return `手动整组${r.action === 'ON' ? '开启' : '关闭'} · ${parseDevices(r.devices).length} 台`;
  if (r.trigger === 'TIMED') {
    const close = r.closeTime ? `，${r.closeTime} ${r.action === 'ON' ? '关闭' : '开启'}` : '';
    return `每天 ${r.time} ${r.action === 'ON' ? '开启' : '关闭'}${close}`;
  }
  const ret = r.useReturn ? `，恢复至 ${r.returnThreshold} 时反向` : '';
  return `${r.startTime}-${r.endTime}，${r.metric} ${r.operator} ${r.threshold} 时${r.action === 'ON' ? '开启' : '关闭'}${ret}`;
}

function create() {
  uni.navigateTo({ url: '/pages/rule-form/rule-form' });
}

function edit(r: ControlRule) {
  storage.set('kb.ruleDraft', r);
  uni.navigateTo({ url: '/pages/rule-form/rule-form?id=' + r.id });
}

async function toggle(r: ControlRule) {
  try {
    await toggleRule(r.id!);
    uni.showToast({ title: r.enabled ? '已停用' : '已校验并启用', icon: 'success' });
    await refresh();
  } catch { /* request 已 toast */ }
}

async function refresh() {
  if (!storage.get<string>('token')) return;
  loading.value = true;
  try {
    const res = await listRules({});
    rules.value = res.data || [];
  } catch { /* 静默 */ } finally { loading.value = false; }
}

onShow(refresh);
</script>

<template>
  <scroll-view scroll-y class="page" :class="{ 'kb-large': large }">
    <view class="seg-row">
      <view
        v-for="opt in ['ALL', 'TIMED', 'CONDITION', 'GROUP']"
        :key="opt"
        class="seg"
        :class="{ active: kindFilter === opt }"
        @tap="kindFilter = opt as any"
      >
        {{ opt === 'ALL' ? '全部' : KIND_LABELS[opt] }}
      </view>
    </view>

    <view class="btn primary wide" hover-class="kb-press" @tap="create">新建规则</view>

    <view v-if="!filtered.length" class="kb-card empty">暂无规则，点击上方按钮创建。</view>
    <view v-for="r in filtered" :key="r.id" class="kb-card rule kb-press" @tap="edit(r)">
      <view class="rule-head">
        <text class="rule-name">{{ r.name }}</text>
        <text class="rule-tag" :class="r.enabled ? 'on' : 'off'">{{ r.enabled ? '已启用' : '已停用' }}</text>
      </view>
      <text class="rule-summary">{{ KIND_LABELS[r.kind] }} · {{ summary(r) }}</text>
      <view class="rule-actions">
        <view class="mini-btn" hover-class="kb-press" @tap.stop="toggle(r)">{{ r.enabled ? '停用' : '校验并启用' }}</view>
      </view>
    </view>
  </scroll-view>
</template>

<style scoped lang="scss">
.page { padding: 24rpx; min-height: 100vh; box-sizing: border-box; }
.seg-row { display: flex; gap: 10rpx; margin-bottom: 20rpx; background: $kb-bg-sunken; border-radius: $kb-radius-pill; padding: 6rpx; }
.seg { flex: 1; text-align: center; padding: 14rpx 0; font-size: 25rpx; color: $kb-text-muted; border-radius: $kb-radius-pill; }
.seg.active { background: #fff; color: $kb-brand; font-weight: 700; }
.btn { text-align: center; padding: 22rpx 0; border-radius: $kb-radius-btn; font-size: 28rpx; font-weight: 600; }
.btn.primary { background: $kb-brand; color: #fff; }
.btn.wide { margin-bottom: 20rpx; }
.empty { padding: 40rpx; text-align: center; color: $kb-offline; font-size: 26rpx; }
.rule { padding: 26rpx 30rpx; margin-bottom: 16rpx; }
.rule-head { display: flex; justify-content: space-between; align-items: center; }
.rule-name { font-size: 30rpx; font-weight: 700; color: $kb-text; }
.rule-tag { font-size: 22rpx; padding: 4rpx 12rpx; border-radius: 8rpx; }
.rule-tag.on { color: $kb-success; background: $kb-success-bg; }
.rule-tag.off { color: $kb-offline; background: $kb-bg-sunken; }
.rule-summary { display: block; margin-top: 10rpx; font-size: 25rpx; color: $kb-text-muted; line-height: 1.5; }
.rule-actions { margin-top: 16rpx; display: flex; justify-content: flex-end; }
.mini-btn { font-size: 25rpx; color: $kb-brand; border: 1rpx solid #83a994; border-radius: 8rpx; padding: 8rpx 22rpx; }
.kb-large .rule-name, .kb-large .rule-summary { font-size: 31rpx; }
</style>