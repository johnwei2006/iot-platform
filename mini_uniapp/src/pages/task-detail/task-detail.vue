<script setup lang="ts">
import { ref } from 'vue';
import { onLoad, onShow } from '@dcloudio/uni-app';
import { completeTask, listTasks } from '@/api/kb';
import { storage } from '@/utils/storage';
import type { KbTask } from '@/types/kb';

const large = ref(storage.get<boolean>('largeFont') || false);
const id = ref('');
const task = ref<KbTask | null>(null);
const overdueMark = ref('');
const note = ref('');
const submitting = ref(false);

function fmt(s?: string) {
  return (s || '').slice(0, 16).replace('T', ' ') || '-';
}

function overdueText() {
  if (overdueMark.value === 'OVERDUE') return '超时未完成';
  if (overdueMark.value === 'LATE_DONE') return '超时完成';
  return '';
}

async function load() {
  try {
    const res = await listTasks({});
    const hit = (res.data || []).find((x) => x.task.id === id.value);
    if (hit) {
      task.value = hit.task;
      overdueMark.value = hit.overdueMark;
    }
  } catch { /* request 已 toast */ }
}

async function submit() {
  if (!task.value?.id || submitting.value) return;
  submitting.value = true;
  try {
    await completeTask(task.value.id, note.value || undefined);
    uni.showToast({ title: '已完成', icon: 'success' });
    setTimeout(() => uni.navigateBack(), 600);
  } catch { /* request 已 toast */ } finally {
    submitting.value = false;
  }
}

onLoad((options) => {
  id.value = ((options as Record<string, string>)?.id as string) || '';
  const draft = storage.get<Partial<KbTask>>('kb.taskDraft');
  if (draft && draft.id === id.value) {
    task.value = { ...draft, status: (draft.status as 'TODO' | 'DONE') || 'TODO' } as KbTask;
  }
});
onShow(load);
</script>
<template>
  <scroll-view scroll-y class="page" :class="{ 'kb-large': large }">
    <view v-if="task" class="kb-card detail">
      <view class="head">
        <text class="title">{{ task.title }}</text>
        <text class="tag" :class="task.status === 'DONE' ? 'ok' : overdueMark === 'OVERDUE' ? 'bad' : 'warn'">
          {{ task.status === 'DONE' ? overdueText() || '已完成' : overdueText() || '待完成' }}
        </text>
      </view>
      <view class="kv"><text class="k">任务说明</text><text class="v">{{ task.content || '无补充说明' }}</text></view>
      <view class="kv"><text class="k">截止时间</text><text class="v">{{ fmt(task.dueTime) }}</text></view>
      <view v-if="task.doneTime" class="kv"><text class="k">完成时间</text><text class="v">{{ fmt(task.doneTime) }}</text></view>
      <view v-if="task.doneNote" class="kv"><text class="k">完成说明</text><text class="v">{{ task.doneNote }}</text></view>
    </view>

    <view v-if="task && task.status === 'TODO'" class="kb-card submit">
      <text class="label">完成说明（选填）</text>
      <textarea v-model="note" class="ta" maxlength="200" placeholder="记录处理方式、结果等信息" />
      <view class="btn primary" hover-class="kb-press" :class="{ disabled: submitting }" @tap="submit">提交完成</view>
    </view>

    <view v-if="task && task.status === 'DONE'" class="kb-card done-hint">任务已完成，无需重复提交。</view>
  </scroll-view>
</template>
<style scoped lang="scss">
.page { padding: 24rpx; min-height: 100vh; box-sizing: border-box; }
.detail { padding: 28rpx 30rpx; display: flex; flex-direction: column; gap: 20rpx; margin-bottom: 20rpx; }
.head { display: flex; justify-content: space-between; align-items: flex-start; gap: 16rpx; }
.title { flex: 1; font-size: 32rpx; font-weight: 700; color: $kb-text; letter-spacing: -0.5rpx; line-height: 1.4; }
.tag { flex-shrink: 0; font-size: 22rpx; padding: 6rpx 14rpx; border-radius: 8rpx; }
.tag.ok { color: $kb-success; background: $kb-success-bg; }
.tag.warn { color: $kb-warning; background: $kb-warning-bg; }
.tag.bad { color: $kb-danger; background: $kb-danger-bg; }
.kv { display: flex; flex-direction: column; gap: 6rpx; }
.k { font-size: 23rpx; color: $kb-text-muted; }
.v { font-size: 28rpx; color: $kb-text; line-height: 1.5; }
.submit { padding: 24rpx 30rpx; display: flex; flex-direction: column; gap: 14rpx; }
.label { font-size: 25rpx; font-weight: 600; color: $kb-text; }
.ta { width: 100%; box-sizing: border-box; background: #fff; border: 1rpx solid #a9bdaf; border-radius: 12rpx; padding: 16rpx; font-size: 27rpx; color: $kb-text; height: 140rpx; }
.btn { text-align: center; padding: 22rpx 0; border-radius: $kb-radius-btn; font-size: 28rpx; font-weight: 600; }
.btn.primary { background: $kb-brand; color: #fff; }
.btn.disabled { opacity: 0.55; }
.done-hint { padding: 30rpx; text-align: center; color: $kb-text-muted; font-size: 26rpx; }
.kb-large .title { font-size: 36rpx; }
.kb-large .v, .kb-large .ta { font-size: 31rpx; }
</style>
