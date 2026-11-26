<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { listMessages, markRead, unreadCount, getNotifyPref, updateNotifyPref } from '@/api/system/me';
import type { KbMessage } from '@/types/kb';

const loading = ref(false);
const tab = ref('未读');
const messages = ref<KbMessage[]>([]);
const unread = ref(0);

const prefVisible = ref(false);
const prefForm = reactive({ ALARM: true, SYSTEM: false, INAPP: true, WECHAT: false, SMS: false });

const TYPE_LABELS: Record<string, string> = { ALARM: '告警', SYSTEM: '系统' };

const visibleMessages = computed(() => {
  if (tab.value === '未读') return messages.value.filter((m) => !m.readAt);
  if (tab.value === '已读') return messages.value.filter((m) => m.readAt);
  return messages.value;
});

async function refresh() {
  loading.value = true;
  try {
    const [msgRes, unreadRes] = await Promise.all([listMessages(), unreadCount()]);
    messages.value = msgRes.data || [];
    unread.value = unreadRes.data?.count ?? 0;
  } catch (error) {
    ElMessage.error((error as Error).message || '消息加载失败');
  } finally {
    loading.value = false;
  }
}

async function openMessage(m: KbMessage) {
  if (!m.readAt) {
    try {
      await markRead(m.id);
      await refresh();
    } catch (error) {
      ElMessage.error((error as Error).message || '标记已读失败');
    }
  }
}

async function openPrefs() {
  try {
    const res = await getNotifyPref();
    const kinds = JSON.parse(res.data?.receiveKinds || '[]') as string[];
    const channels = JSON.parse(res.data?.channels || '[]') as string[];
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
  const kinds = ['ALARM', 'SYSTEM'].filter((k) => (prefForm as Record<string, boolean>)[k]);
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
          <el-segmented v-model="tab" :options="['未读', '已读', '全部']" />
          <el-badge :value="unread" :hidden="!unread" type="danger">
            <el-button @click="openPrefs">通知偏好</el-button>
          </el-badge>
        </div>
      </template>

      <el-empty v-if="!visibleMessages.length" :description="`暂无${tab}消息`" />
      <div v-else class="msg-list">
        <button v-for="m in visibleMessages" :key="m.id" class="kb-tile msg-item kb-press"
          :class="{ 'is-ok': !!m.readAt }" @click="openMessage(m)">
          <div class="msg-item__main">
            <span class="msg-item__title">{{ TYPE_LABELS[m.type] || m.type }} · {{ m.title }}</span>
            <span class="msg-item__sub">{{ m.content || '—' }}</span>
          </div>
          <span class="msg-item__time">{{ (m.createdAt || '').replace('T', ' ').slice(0, 16) }}</span>
        </button>
      </div>
    </el-card>

    <el-dialog v-model="prefVisible" title="通知偏好（本期仅存配置，不实际推送）" width="460px">
      <h4>接收类型</h4>
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
.msg-item__time { font-size: var(--kb-fs-caption); color: var(--kb-text-muted); }
h4 { margin: 12px 0 6px; color: var(--kb-text-muted); }
</style>
