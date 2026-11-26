<script setup lang="ts">
import { computed, ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { listMessages, markMessageRead, unreadCount } from '@/api/kb';
import { storage } from '@/utils/storage';
import type { KbMessage } from '@/types/kb';

type Tab = 'UNREAD' | 'READ' | 'ALL';

const large = ref(storage.get<boolean>('largeFont') || false);
const tab = ref<Tab>('UNREAD');
const messages = ref<KbMessage[]>([]);

const TABS: { key: Tab; label: string }[] = [
  { key: 'UNREAD', label: '未读' },
  { key: 'READ', label: '已读' },
  { key: 'ALL', label: '全部' },
];

const TYPE_LABELS: Record<string, string> = { ALARM: '告警', SYSTEM: '系统' };
const unreadCountValue = ref(0);

const visibleMessages = computed(() => {
  if (tab.value === 'UNREAD') return messages.value.filter((m) => !m.readAt);
  if (tab.value === 'READ') return messages.value.filter((m) => m.readAt);
  return messages.value;
});

function fmtTime(s?: string) {
  return (s || '').slice(0, 16).replace('T', ' ');
}

function switchTab(t: Tab) {
  tab.value = t;
}

async function openMessage(m: KbMessage) {
  if (!m.readAt) {
    markMessageRead(m.id).catch(() => {});
    m.readAt = new Date().toISOString();
    unreadCountValue.value = Math.max(0, unreadCountValue.value - 1);
  }
  uni.showToast({ title: m.content || m.title, icon: 'none' });
}

async function load() {
  if (!storage.get<string>('token')) return;
  try {
    const res = await listMessages();
    messages.value = res.data || [];
    const c = await unreadCount();
    unreadCountValue.value = c.data?.count || 0;
    if (unreadCountValue.value > 0) {
      uni.setTabBarBadge({ index: 2, text: String(Math.min(unreadCountValue.value, 99)) });
    } else {
      uni.removeTabBarBadge({ index: 2 });
    }
  } catch { /* request 已 toast */ }
}

onShow(load);
</script>
<template>
  <scroll-view scroll-y class="page" :class="{ 'kb-large': large }">
    <view class="seg-row">
      <view
        v-for="t in TABS"
        :key="t.key"
        class="seg"
        :class="{ active: tab === t.key }"
        @tap="switchTab(t.key)"
      >
        {{ t.label }}
        <text v-if="t.key === 'UNREAD' && unreadCountValue" class="dot">{{ unreadCountValue }}</text>
      </view>
    </view>

    <view v-if="!visibleMessages.length" class="kb-card empty">暂无消息</view>
    <view v-for="m in visibleMessages" :key="m.id" class="kb-card row kb-press" @tap="openMessage(m)">
      <view class="row-main">
        <text class="row-title" :class="{ bold: !m.readAt }">{{ m.title }}</text>
        <text class="row-sub">{{ m.content || '' }} · {{ fmtTime(m.createdAt) }}</text>
      </view>
      <text class="tag" :class="m.type === 'ALARM' ? 'bad' : 'plain'">
        {{ TYPE_LABELS[m.type] || '消息' }}
      </text>
    </view>
  </scroll-view>
</template>
<style scoped lang="scss">
.page { padding: 24rpx; min-height: 100vh; box-sizing: border-box; }
.seg-row { display: flex; gap: 10rpx; margin-bottom: 20rpx; background: $kb-bg-sunken; border-radius: $kb-radius-pill; padding: 6rpx; }
.seg { flex: 1; text-align: center; padding: 14rpx 0; font-size: 25rpx; color: $kb-text-muted; border-radius: $kb-radius-pill; position: relative; }
.seg.active { background: #fff; color: $kb-brand; font-weight: 700; }
.dot { position: absolute; top: 2rpx; right: 10rpx; background: $kb-danger; color: #fff; font-size: 20rpx; min-width: 30rpx; height: 30rpx; line-height: 30rpx; border-radius: $kb-radius-pill; padding: 0 6rpx; box-sizing: border-box; }
.empty { padding: 40rpx; text-align: center; color: $kb-offline; font-size: 26rpx; }
.row { display: flex; align-items: center; justify-content: space-between; gap: 16rpx; padding: 24rpx 28rpx; margin-bottom: 16rpx; }
.row-main { flex: 1; display: flex; flex-direction: column; gap: 8rpx; min-width: 0; }
.row-title { font-size: 29rpx; color: $kb-text; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.row-title.bold { font-weight: 700; }
.row-sub { font-size: 24rpx; color: $kb-text-muted; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.tag { flex-shrink: 0; font-size: 22rpx; padding: 6rpx 14rpx; border-radius: 8rpx; }
.tag.bad { color: $kb-danger; background: $kb-danger-bg; }
.tag.plain { color: $kb-text-muted; background: $kb-bg-sunken; }
.kb-large .row-title, .kb-large .row-sub { font-size: 31rpx; }
</style>
