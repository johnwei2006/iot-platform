<script setup lang="ts">
import { computed, ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { listThresholds, saveThreshold } from '@/api/kb';
import { storage } from '@/utils/storage';
import type { MetricThreshold } from '@/types/kb';

const large = ref(storage.get<boolean>('largeFont') || false);
const deviceId = ref('');
const metric = ref('temp');
const minValue = ref('');
const maxValue = ref('');
const saving = ref(false);
const saved = ref<MetricThreshold[]>([]);

const METRICS = [
  { code: 'temp', label: '空气温度', unit: '℃' },
  { code: 'humid', label: '空气湿度', unit: '%' },
  { code: 'press', label: '气压', unit: 'kPa' },
];
const METRIC_NAMES: Record<string, string> = { temp: '空气温度', humid: '空气湿度', press: '气压' };

const current = computed(() => METRICS.find((m) => m.code === metric.value));

function loadForm(t?: MetricThreshold) {
  minValue.value = t?.minValue != null ? String(t.minValue) : '';
  maxValue.value = t?.maxValue != null ? String(t.maxValue) : '';
}

async function refresh() {
  try {
    const res = await listThresholds(deviceId.value);
    saved.value = (res.data || []).filter((t) => t.metric === metric.value);
    loadForm(saved.value[0]);
  } catch { /* request 已 toast */ }
}

function pickMetric(code: string) {
  metric.value = code;
  loadForm(saved.value.find((x) => x.metric === code));
  void refresh();
}

async function save() {
  if (saving.value) return;
  const min = minValue.value === '' ? undefined : Number(minValue.value);
  const max = maxValue.value === '' ? undefined : Number(maxValue.value);
  if (min == null && max == null) {
    uni.showToast({ title: '请至少填写一个阈值', icon: 'none' });
    return;
  }
  if (min != null && max != null && min >= max) {
    uni.showToast({ title: '下限需小于上限', icon: 'none' });
    return;
  }
  saving.value = true;
  try {
    const existing = saved.value[0];
    await saveThreshold({
      id: existing?.id,
      deviceId: deviceId.value,
      metric: metric.value,
      station: existing?.station,
      minValue: min,
      maxValue: max,
    });
    uni.showToast({ title: '已保存，超限将联动告警', icon: 'none' });
    await refresh();
  } catch { /* request 已 toast */ } finally { saving.value = false; }
}

onLoad((options) => {
  deviceId.value = ((options as Record<string, string>)?.deviceId as string) || '';
  void refresh();
});
</script>
<template>
  <scroll-view scroll-y class="page" :class="{ 'kb-large': large }">
    <view class="kb-card form">
      <view class="field">
        <text class="label">测点指标</text>
        <view class="seg-row">
          <view v-for="m in METRICS" :key="m.code" class="seg" :class="{ active: metric === m.code }" @tap="pickMetric(m.code)">
            {{ m.label }}
          </view>
        </view>
      </view>
      <view class="field">
        <text class="label">下限（低于触发告警）</text>
        <input class="input" type="digit" v-model="minValue" :placeholder="'如 5 ' + (current?.unit || '')" />
      </view>
      <view class="field">
        <text class="label">上限（高于触发告警）</text>
        <input class="input" type="digit" v-model="maxValue" :placeholder="'如 35 ' + (current?.unit || '')" />
      </view>
      <view class="btn primary" hover-class="kb-press" :class="{ disabled: saving }" @tap="save">保存阈值</view>
    </view>

    <view class="section-title">已保存阈值</view>
    <view v-if="!saved.length" class="kb-card empty">该指标暂未设置阈值</view>
    <view v-for="t in saved" :key="t.id" class="kb-card th-row">
      <text class="th-name">{{ METRIC_NAMES[t.metric] || t.metric }}{{ t.station ? ' · ' + t.station : '' }}</text>
      <text class="th-range">{{ t.minValue ?? '—' }} ~ {{ t.maxValue ?? '—' }}</text>
    </view>
  </scroll-view>
</template>
<style scoped lang="scss">
.page { padding: 24rpx; min-height: 100vh; box-sizing: border-box; }
.form { padding: 24rpx 28rpx; display: flex; flex-direction: column; gap: 22rpx; }
.field { display: flex; flex-direction: column; gap: 10rpx; }
.label { font-size: 25rpx; font-weight: 600; color: $kb-text; }
.seg-row { display: flex; gap: 8rpx; background: $kb-bg-sunken; border-radius: $kb-radius-pill; padding: 6rpx; }
.seg { flex: 1; text-align: center; padding: 12rpx 0; font-size: 25rpx; color: $kb-text-muted; border-radius: $kb-radius-pill; }
.seg.active { background: #fff; color: $kb-brand; font-weight: 700; }
.input { background: #fff; border: 1rpx solid #a9bdaf; border-radius: 12rpx; padding: 18rpx; font-size: 27rpx; color: $kb-text; }
.btn { text-align: center; padding: 22rpx 0; border-radius: $kb-radius-btn; font-size: 28rpx; font-weight: 600; }
.btn.primary { background: $kb-brand; color: #fff; }
.btn.disabled { opacity: 0.55; }
.section-title { font-size: 30rpx; font-weight: 700; color: $kb-text; margin: 30rpx 6rpx 16rpx; }
.empty { padding: 40rpx; text-align: center; color: $kb-offline; font-size: 26rpx; }
.th-row { display: flex; justify-content: space-between; align-items: center; padding: 22rpx 26rpx; margin-bottom: 14rpx; }
.th-name { font-size: 27rpx; color: $kb-text; font-weight: 600; }
.th-range { font-size: 27rpx; color: $kb-brand; font-weight: 700; }
.kb-large .label, .kb-large .input, .kb-large .seg { font-size: 31rpx; }
</style>
