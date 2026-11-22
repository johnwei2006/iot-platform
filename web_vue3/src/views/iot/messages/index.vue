<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { completeTask, listMessages, listTasks, markRead, unreadCount, getNotifyPref, updateNotifyPref } from '@/api/kb';
import type { KbMessage, KbTask } from '@/types/kb';

const loading = ref(false);
const tab = ref('待办');
const tasks = ref<{ task: KbTask; overdueMark: string }[]>([]);
const messages = ref<KbMessage[]>([]);
const unread = ref(0);

const taskVisible = ref(false);
const currentTask = ref<KbTask | null>(null);
const currentMark = ref('');
const doneNote = ref('');

const prefVisible = ref(false);
const prefForm = reactive({ TASK: true, ALARM: true, SYSTEM: false, INAPP: true, WECHAT: false, SMS: false });


const visibleTasks = computed(() => {
  if (tab.value === '待办') return tasks.value.filter((t) => t.task.status === 'TODO');
  if (tab.value === '已完成') return tasks.value.filter((t) => t.task.status === 'DONE');
  if (tab.value === '未读') return messages.value.filter((m) => !m.readAt).map((m) => ({ task: { id: m.refId, title: m.title, content: m.content, status: 'TODO' } as KbTask, overdueMark: '', msg: m }));
  return tasks.value;
});

async function refresh() {
  loading.value = true;
  try {
    const [taskRes, msgRes, unreadRes] = await Promise.all([listTasks(), listMessages(), unreadCount()]);
    tasks.value = taskRes.data || [];
    messages.value = msgRes.data || [];
    unread.value = unreadRes.data?.count ?? 0;
  } catch (error) {
    ElMessage.error((error as Error).message || '消息加载失败');
  } finally {
    loading.value = false;
  }
}

function openTask(t: KbTask, mark: string) {
  currentTask.value = t;
  currentMark.value = mark;
  doneNote.value = t.doneNote || '';
  taskVisible.value = true;
  const msg = messages.value.find((m) => m.refId === t.id && !m.readAt);
  if (msg) {
    markRead(msg.id).catch(() => undefined);
  }
}

async function submitComplete() {
  if (!currentTask.value?.id) return;
  try {
    await completeTask(currentTask.value.id, doneNote.value);
    ElMessage.success('已提交完成');
    taskVisible.value = false;
    await refresh();
  } catch (error) {
    ElMessage.error((error as Error).message || '提交失败');
  }
}

function statusText(t: { task: KbTask; overdueMark: string }) {
  if (t.task.status === 'DONE') return t.overdueMark === 'LATE_DONE' ? '已完成 · 曾超时' : '已完成';
  return t.overdueMark === 'OVERDUE' ? '任务已超时' : '任务已分配';
}

async function openPrefs() {
  try {
    const res = await getNotifyPref();
    const kinds = JSON.parse(res.data?.receiveKinds || '[]') as string[];
    const channels = JSON.parse(res.data?.channels || '[]') as string[];
    prefForm.TASK = kinds.includes('TASK');
    prefForm.ALARM = kinds.includes('ALARM');
    prefForm.SYSTEM = kinds.includes('SYSTEM');
    prefForm.INAPP = channels.includes('INAPP');
    prefForm.WECHAT = channels.includes('WECHAT');
    prefForm.SMS = channels.includes('SMS');
    prefVisible.value = true;
  } catch (error) {
    ElMessage.error((error as Error).message || '偏好加载失败');
  }
}

async function savePrefs() {
  const kinds = ['TASK', 'ALARM', 'SYSTEM'].filter((k) => (prefForm as Record<string, boolean>)[k]);
  const channels = ['INAPP', 'WECHAT', 'SMS'].filter((c) => (prefForm as Record<string, boolean>)[c]);
  try {
    await updateNotifyPref({ receiveKinds: JSON.stringify(kinds), channels: JSON.stringify(channels) });
    ElMessage.success('通知偏好已保存');
    prefVisible.value = false;
  } catch (error) {
    ElMessage.error((error as Error).message || '保存失败');
  }
}

onMounted(refresh);
</script>
<template>
  <div v-loading="loading">
    <el-card shadow="never">
      <template #header>
        <div class="head-bar">
          <el-segmented v-model="tab" :options="['待办', '已完成', '未读', '全部']" />
          <el-badge :value="unread" :hidden="!unread" type="danger">
            <el-button @click="openPrefs">通知偏好</el-button>
          </el-badge>
        </div>
      </template>

      <el-empty v-if="!visibleTasks.length" :description="`暂无${tab}消息`" />
      <div v-else class="msg-list">
        <button v-for="(t, i) in visibleTasks" :key="i" class="kb-tile msg-item kb-press"
          :class="t.task.status === 'DONE' ? 'is-ok' : t.overdueMark === 'OVERDUE' ? 'kb-tile--warn' : ''"
          @click="openTask(t.task, t.overdueMark)">
          <div class="msg-item__main">
            <span class="msg-item__title">{{ statusText(t) }} · {{ t.task.title }}</span>
            <span class="msg-item__sub">
              {{ t.task.status === 'DONE' ? '完成时间 ' + (t.task.doneTime || '') : '截止 ' + (t.task.dueTime || '未设置') }}
            </span>
          </div>
          <el-tag v-if="t.task.status === 'DONE'" type="success" size="small">已完成</el-tag>
          <el-tag v-else-if="t.overdueMark === 'OVERDUE'" type="warning" size="small">已超时</el-tag>
        </button>
      </div>
    </el-card>

    <el-dialog v-model="taskVisible" title="任务详情" width="520px">
      <template v-if="currentTask">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="任务">{{ currentTask.title }}</el-descriptions-item>
          <el-descriptions-item label="要求">{{ currentTask.content || '—' }}</el-descriptions-item>
          <el-descriptions-item label="截止时间">{{ currentTask.dueTime || '未设置' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            {{ currentMark === 'OVERDUE' && currentTask.status === 'TODO' ? '已超时（仍可提交）' : statusText({ task: currentTask, overdueMark: currentMark }) }}
          </el-descriptions-item>
        </el-descriptions>
        <el-input v-if="currentTask.status === 'TODO'" v-model="doneNote" type="textarea" :rows="3" maxlength="300" placeholder="完成说明（选填）" style="margin-top: 12px" />
        <el-descriptions v-else :column="1" border style="margin-top: 12px">
          <el-descriptions-item label="完成说明">{{ currentTask.doneNote || '—' }}</el-descriptions-item>
        </el-descriptions>
      </template>
      <template #footer>
        <el-button v-if="currentTask && currentTask.status === 'TODO'" type="primary" v-permission="'iot:task:complete'" @click="submitComplete">提交完成</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="prefVisible" title="通知偏好（本期仅存配置，不实际推送）" width="460px">
      <h4>接收类型</h4>
      <el-checkbox v-model="prefForm.TASK">任务</el-checkbox>
      <el-checkbox v-model="prefForm.ALARM">告警</el-checkbox>
      <el-checkbox v-model="prefForm.SYSTEM">系统</el-checkbox>
      <h4>接收渠道</h4>
      <el-checkbox v-model="prefForm.INAPP">站内</el-checkbox>
      <el-checkbox v-model="prefForm.WECHAT">微信</el-checkbox>
      <el-checkbox v-model="prefForm.SMS">短信</el-checkbox>
      <template #footer>
        <el-button @click="prefVisible = false">取消</el-button>
        <el-button type="primary" @click="savePrefs">保存设置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.head-bar { display: flex; justify-content: space-between; align-items: center; }
.msg-list { display: flex; flex-direction: column; gap: 10px; }
.msg-item { display: flex; align-items: center; justify-content: space-between; padding: 14px 16px; cursor: pointer; font: inherit; color: inherit; text-align: left; width: 100%; }
.msg-item.is-ok { background: var(--kb-success-bg); border-color: #cfe6d5; }
.msg-item__main { display: flex; flex-direction: column; gap: 4px; }
.msg-item__title { font-weight: 600; }
.msg-item__sub { font-size: var(--kb-fs-caption); color: var(--kb-text-muted); }
h4 { margin: 12px 0 6px; color: var(--kb-text-muted); }
</style>