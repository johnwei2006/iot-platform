<script setup lang="ts">
import { ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { getNotifyPref, updateNotifyPref } from '@/api/kb';
import { useUserStore } from '@/store/modules/user';
import { storage } from '@/utils/storage';

const userStore = useUserStore();
const large = ref(storage.get<boolean>('largeFont') || false);

const KIND_OPTIONS = [
  { key: 'TASK', label: '任务提醒' },
  { key: 'ALARM', label: '告警通知' },
  { key: 'SYSTEM', label: '系统消息' },
];
const CHANNEL_OPTIONS = [
  { key: 'INAPP', label: '站内' },
  { key: 'WECHAT', label: '微信' },
  { key: 'SMS', label: '短信' },
];

const receiveKinds = ref<string[]>(['TASK', 'ALARM']);
const channels = ref<string[]>(['INAPP']);
const prefSaving = ref(false);

function parseList(json?: string): string[] {
  try { return JSON.parse(json || '[]'); } catch { return []; }
}

async function loadPref() {
  try {
    const res = await getNotifyPref();
    receiveKinds.value = parseList(res.data?.receiveKinds);
    channels.value = parseList(res.data?.channels);
    if (!receiveKinds.value.length) receiveKinds.value = ['TASK', 'ALARM'];
    if (!channels.value.length) channels.value = ['INAPP'];
  } catch { /* request 已 toast */ }
}

function toggle(list: string[], key: string) {
  const i = list.indexOf(key);
  if (i >= 0) list.splice(i, 1);
  else list.push(key);
}

async function savePref() {
  if (prefSaving.value) return;
  prefSaving.value = true;
  try {
    await updateNotifyPref({ receiveKinds: JSON.stringify(receiveKinds.value), channels: JSON.stringify(channels.value) });
    uni.showToast({ title: '偏好已保存', icon: 'success' });
  } catch { /* request 已 toast */ } finally { prefSaving.value = false; }
}

function onLargeChange(e: any) {
  large.value = !!e?.detail?.value;
  storage.set('largeFont', large.value);
  uni.showToast({ title: large.value ? '已开启大字号' : '已恢复标准字号', icon: 'none' });
}

function goControlLogs() {
  uni.navigateTo({ url: '/pages/control-logs/control-logs' });
}

function goAlarms() {
  uni.navigateTo({ url: '/pages/alarms/alarms' });
}

function logout() {
  uni.showModal({
    title: '退出登录',
    content: '确定要退出当前账号吗？',
    success: (res) => {
      if (!res.confirm) return;
      userStore.logout();
      uni.reLaunch({ url: '/pages/login/login' });
    },
  });
}

onShow(() => {
  large.value = storage.get<boolean>('largeFont') || false;
  void loadPref();
});
</script>
<template>
  <scroll-view scroll-y class="page" :class="{ 'kb-large': large }">
    <view class="kb-card user-card">
      <view class="avatar">{{ (userStore.userInfo?.nickname || '未')[0] }}</view>
      <view class="user-main">
        <text class="user-name">{{ userStore.userInfo?.nickname || '未登录' }}</text>
        <text class="user-sub">{{ userStore.userInfo?.username || '-' }}</text>
      </view>
    </view>

    <view class="section-title">常用</view>
    <view class="kb-card link-row kb-press" @tap="goControlLogs">
      <text>控制记录</text><text class="chev">›</text>
    </view>
    <view class="kb-card link-row kb-press" @tap="goAlarms">
      <text>告警中心</text><text class="chev">›</text>
    </view>

    <view class="section-title">显示</view>
    <view class="kb-card link-row">
      <view class="link-main">
        <text>大字号模式</text>
        <text class="link-sub">适老显示，全局放大文字</text>
      </view>
      <switch :checked="large" color="#17573F" @change="onLargeChange" />
    </view>

    <view class="section-title">通知偏好（本期仅保存设置）</view>
    <view class="kb-card pref">
      <view class="pref-group">
        <text class="pref-label">接收类型</text>
        <view v-for="k in KIND_OPTIONS" :key="k.key" class="check-row" @tap="toggle(receiveKinds, k.key)">
          <view class="checkbox" :class="{ checked: receiveKinds.includes(k.key) }">✓</view>
          <text>{{ k.label }}</text>
        </view>
      </view>
      <view class="pref-group">
        <text class="pref-label">接收渠道</text>
        <view v-for="c in CHANNEL_OPTIONS" :key="c.key" class="check-row" @tap="toggle(channels, c.key)">
          <view class="checkbox" :class="{ checked: channels.includes(c.key) }">✓</view>
          <text>{{ c.label }}</text>
        </view>
      </view>
      <view class="btn primary" hover-class="kb-press" :class="{ disabled: prefSaving }" @tap="savePref">保存偏好</view>
    </view>

    <view class="btn danger" hover-class="kb-press" @tap="logout">退出登录</view>
  </scroll-view>
</template>
<style scoped lang="scss">
.page { padding: 24rpx; min-height: 100vh; box-sizing: border-box; }
.user-card { display: flex; align-items: center; gap: 22rpx; padding: 28rpx 30rpx; }
.avatar { width: 96rpx; height: 96rpx; border-radius: 50%; background: $kb-brand; color: #fff; font-size: 40rpx; display: flex; align-items: center; justify-content: center; }
.user-main { display: flex; flex-direction: column; gap: 6rpx; }
.user-name { font-size: 32rpx; font-weight: 700; color: $kb-text; }
.user-sub { font-size: 24rpx; color: $kb-text-muted; }
.section-title { font-size: 30rpx; font-weight: 700; color: $kb-text; margin: 30rpx 6rpx 16rpx; }
.link-row { display: flex; align-items: center; justify-content: space-between; padding: 26rpx 30rpx; margin-bottom: 14rpx; font-size: 28rpx; color: $kb-text; }
.link-main { display: flex; flex-direction: column; gap: 6rpx; }
.link-sub { font-size: 23rpx; color: $kb-text-muted; }
.chev { color: $kb-brand; font-size: 36rpx; }
.pref { padding: 24rpx 30rpx; display: flex; flex-direction: column; gap: 24rpx; }
.pref-group { display: flex; flex-direction: column; gap: 10rpx; }
.pref-label { font-size: 25rpx; font-weight: 600; color: $kb-text; }
.check-row { display: flex; align-items: center; gap: 14rpx; font-size: 27rpx; color: $kb-text; padding: 6rpx 0; }
.checkbox { width: 36rpx; height: 36rpx; border: 2rpx solid #a9bdaf; border-radius: 8rpx; display: flex; align-items: center; justify-content: center; font-size: 24rpx; color: #fff; }
.checkbox.checked { background: $kb-brand; border-color: $kb-brand; }
.btn { text-align: center; padding: 22rpx 0; border-radius: $kb-radius-btn; font-size: 28rpx; font-weight: 600; }
.btn.primary { background: $kb-brand; color: #fff; }
.btn.danger { background: #fff; color: $kb-danger; border: 1rpx solid #efd0cf; margin-top: 30rpx; }
.btn.disabled { opacity: 0.55; }
.kb-large .link-row, .kb-large .check-row { font-size: 31rpx; }
</style>
