<script setup lang="ts">
import { computed, ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { listMessages, listTasks, markMessageRead, unreadCount } from '@/api/kb';
import { storage } from '@/utils/storage';
import type { KbMessage, KbTask } from '@/types/kb';

type Tab = 'TODO' | 'DONE' | 'UNREAD' | 'ALL';

const large = ref(storage.get<boolean>('largeFont') || false);
const tab = ref<Tab>('TODO');
const tasks = ref<{ task: KbTask; overdueMark: string }[]>([]);
const messages = ref<KbMessage[]>([]);

const TABS: { key: Tab; label: string }[] = [
  { key: 'TODO', label: '待办' },
  { key: 'DONE', label: '已完成' },
  { key: 'UNREAD', label: '未读' },
  { key: 'ALL', label: '全部' },
];

const TYPE_LABELS: Record<string, string> = { TASK: '任务', ALARM: '告警', SYSTEM: '系统' };

const showTasks = computed(() => tab.value === 'TODO' || tab.value === 'DONE');
const unreadCountValue = ref(0);

function overdueText(mark: string) {
  if (mark === 'OVERDUE') return '超时未完成';
  if (mark === 'LATE_DONE') return '超时完成';
  return '';
}

function fmtTime(s?: string) {
  return (s || '').slice(0, 16).replace('T', ' ');
}

async function loadTasks() {
  const status = tab.value === 'DONE' ? 'DONE' : 'TODO';
  const res = await listTasks({ status });
  tasks.value = res.data || [];
}

async function loadMessages() {
  const res = await listMessages(tab.value === 'UNREAD' ? { unread: true } : {});
  messages.value = res.data || [];
}

function switchTab(t: Tab) {
  tab.value = t;
  load();
}

async function load() {
  if (!storage.get<string>('token')) return;
  try {
    if (showTasks.value) await loadTasks();
    else await loadMessages();
    const c = await unreadCount();
    unreadCountValue.value = c.data?.count || 0;
    if (unreadCountValue.value > 0) {
      uni.setTabBarBadge({ index: 2, text: String(Math.min(unreadCountValue.value, 99)) });
    } else {
      uni.removeTabBarBadge({ index: 2 });
    }
  } catch { /* request 已 toast */ }
}

function openTask(t: KbTask) {
  storage.set('kb.taskDraft', t);
  uni.navigateTo({ url: '/pages/task-detail/task-detail?id=' + t.id });
}

function openMessage(m: KbMessage) {
  if (!m.readAt) markMessageRead(m.id).catch(() => {});
  if (m.type === 'TASK' && m.refId) {
    openTask({ id: m.refId, title: m.title, content: m.content, assigneeUserId: m.userId, status: 'TODO' } as KbTask);
    return;
  }
  uni.showToast({ title: m.content || m.title, icon: 'none' });
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

    <template v-if="showTasks">
      <view v-if="!tasks.length" class="kb-card empty">{{ tab === 'DONE' ? '暂无已完成任务' : '暂无待办任务' }}</view>
      <view v-for="item in tasks" :key="item.task.id" class="kb-card row kb-press" @tap="openTask(item.task)">
        <view class="row-main">
          <text class="row-title">{{ item.task.title }}</text>
          <text class="row-sub">{{ fmtTime(item.task.dueTime) }} 截止 · {{ item.task.content || '无补充说明' }}</text>
        </view>
        <text class="tag" :class="item.task.status === 'DONE' ? 'ok' : item.overdueMark === 'OVERDUE' ? 'bad' : 'warn'">
          {{ item.task.status === 'DONE' ? overdueText(item.overdueMark) || '已完成' : overdueText(item.overdueMark) || '待完成' }}
        </text>
      </view>
    </template>

    <template v-else>
      <view v-if="!messages.length" class="kb-card empty">暂无消息</view>
      <view v-for="m in messages" :key="m.id" class="kb-card row kb-press" @tap="openMessage(m)">
        <view class="row-main">
          <text class="row-title" :class="{ bold: !m.readAt }">{{ m.title }}</text>
          <text class="row-sub">{{ m.content || '' }} · {{ fmtTime(m.createdAt) }}</text>
        </view>
        <text class="tag" :class="m.type === 'ALARM' ? 'bad' : m.type === 'TASK' ? 'warn' : 'plain'">
          {{ TYPE_LABELS[m.type] || '消息' }}
        </text>
      </view>
    </template>
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
.tag.ok { color: $kb-success; background: $kb-success-bg; }
.tag.warn { color: $kb-warning; background: $kb-warning-bg; }
.tag.bad { color: $kb-danger; background: $kb-danger-bg; }
.tag.plain { color: $kb-text-muted; background: $kb-bg-sunken; }
.kb-large .row-title, .kb-large .row-sub { font-size: 31rpx; }
</style>
