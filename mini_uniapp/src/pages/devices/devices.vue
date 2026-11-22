<script setup lang="ts">
import { ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { listDevices } from '@/api/devices';
import { sendCommand } from '@/api/kb';
import { storage } from '@/utils/storage';
import type { Device } from '@/types';
import type { DeviceEntry } from '@/types/kb';

const large = ref(storage.get<boolean>('largeFont') || false);
const loading = ref(false);
const devices = ref<Device[]>([]);
const busy = ref(false);

function kindOf(d: Device) {
  const text = `${d.name || ''}${d.deviceId}`;
  return text.includes('棉被') || text.includes('blanket') ? 'blanket' : 'fan';
}

function entryOf(d: Device): DeviceEntry {
  return { deviceId: d.deviceId, nodeId: d.nodeId || undefined, channel: 1, deviceName: d.name || d.deviceId, deviceType: kindOf(d) === 'blanket' ? '棉被' : '风机' };
}

async function issue(d: Device, action: string, value?: string) {
  busy.value = true;
  try {
    const res = await sendCommand([entryOf(d)], action, value);
    const first = (res.data?.logs || [])[0];
    if (first?.result === 'FAILED') {
      uni.showToast({ title: first.detail || '指令未下发', icon: 'none' });
    } else {
      uni.showToast({ title: '指令已发送', icon: 'success' });
    }
  } catch {
    /* request 已 toast */
  } finally {
    busy.value = false;
  }
}

function setLevel(d: Device) {
  uni.showModal({
    title: '设置开度',
    editable: true,
    placeholderText: '0-100',
    success: (res) => {
      const v = Math.round(Number(res.content));
      if (Number.isNaN(v) || v < 0 || v > 100) {
        uni.showToast({ title: '开度需为 0-100', icon: 'none' });
        return;
      }
      issue(d, 'LEVEL', String(v));
    },
  });
}

function goRules() { uni.navigateTo({ url: '/pages/control-rules/control-rules' }); }
function goLogs() { uni.navigateTo({ url: '/pages/control-logs/control-logs' }); }

async function refresh() {
  if (!storage.get<string>('token')) return;
  loading.value = true;
  try {
    const res = await listDevices();
    devices.value = res.data || [];
  } catch { /* 静默 */ } finally { loading.value = false; }
}

onShow(refresh);
</script>

<template>
  <scroll-view scroll-y class="page" :class="{ 'kb-large': large }">
    <view class="entry-row">
      <view class="kb-card entry kb-press" @tap="goRules">
        <text class="entry-title">自动控制</text>
        <text class="entry-sub">定时、条件规则与冲突校验</text>
      </view>
      <view class="kb-card entry kb-press" @tap="goLogs">
        <text class="entry-title">控制记录</text>
        <text class="entry-sub">操作与执行结果</text>
      </view>
    </view>

    <view class="section-title">可控制的设备</view>
    <view v-if="!devices.length" class="kb-card empty">暂无可控设备</view>
    <view v-for="d in devices" :key="d.deviceId" class="kb-card dev">
      <view class="dev-head">
        <text class="dev-name">{{ d.name || d.deviceId }}</text>
        <text class="dev-kind">{{ kindOf(d) === 'blanket' ? '棉被' : '风机' }}</text>
      </view>
      <view class="btn-row" v-if="kindOf(d) === 'fan'">
        <view class="btn primary" hover-class="kb-press" :class="{ disabled: busy }" @tap="issue(d, 'ON')">开启风机</view>
        <view class="btn" hover-class="kb-press" :class="{ disabled: busy }" @tap="issue(d, 'OFF')">关闭风机</view>
      </view>
      <template v-else>
        <view class="btn-row">
          <view class="btn primary" hover-class="kb-press" :class="{ disabled: busy }" @tap="issue(d, 'OPEN')">卷起</view>
          <view class="btn" hover-class="kb-press" :class="{ disabled: busy }" @tap="issue(d, 'STOP')">停止</view>
          <view class="btn" hover-class="kb-press" :class="{ disabled: busy }" @tap="issue(d, 'CLOSE')">放下</view>
        </view>
        <view class="btn link" hover-class="kb-press" :class="{ disabled: busy }" @tap="setLevel(d)">设置开度（0-100%）</view>
      </template>
    </view>
  </scroll-view>
</template>

<style scoped lang="scss">
.page { padding: 24rpx; min-height: 100vh; box-sizing: border-box; }
.entry-row { display: flex; gap: 16rpx; }
.entry { flex: 1; padding: 26rpx; display: flex; flex-direction: column; gap: 6rpx; }
.entry-title { font-size: 30rpx; font-weight: 700; color: $kb-brand; }
.entry-sub { font-size: 23rpx; color: $kb-text-muted; }
.section-title { font-size: 30rpx; font-weight: 700; color: $kb-text; margin: 30rpx 6rpx 16rpx; }
.empty { padding: 40rpx; text-align: center; color: $kb-offline; font-size: 26rpx; }
.dev { padding: 26rpx 30rpx; margin-bottom: 16rpx; }
.dev-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 18rpx; }
.dev-name { font-size: 30rpx; font-weight: 700; color: $kb-text; }
.dev-kind { font-size: 22rpx; color: $kb-brand; background: $kb-brand-wash; border-radius: 8rpx; padding: 4rpx 12rpx; }
.btn-row { display: flex; gap: 14rpx; margin-bottom: 10rpx; }
.btn { flex: 1; text-align: center; padding: 20rpx 0; border-radius: $kb-radius-btn; border: 1rpx solid #83a994; color: $kb-brand; font-size: 27rpx; font-weight: 600; background: #fff; }
.btn.primary { background: $kb-brand; color: #fff; }
.btn.link { flex: none; border: 0; padding: 8rpx 0; font-size: 25rpx; }
.btn.disabled { opacity: 0.55; }
.kb-large .dev-name, .kb-large .btn { font-size: 31rpx; }
</style>