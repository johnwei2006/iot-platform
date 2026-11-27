<script setup lang="ts">
import { ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { checkRule, saveRule } from '@/api/kb';
import { listDevices } from '@/api/devices';
import { storage } from '@/utils/storage';
import type { ConflictResult, ControlRule, DeviceEntry } from '@/types/kb';
import type { Device } from '@/types';

const large = ref(storage.get<boolean>('largeFont') || false);
const editingId = ref('');
const devices = ref<Device[]>([]);
const deviceOptions = ref<{ id: string; name: string }[]>([]);
const conflict = ref<ConflictResult | null>(null);
const saving = ref(false);

const form = ref({
  name: '',
  kind: 'TIMED' as 'GROUP' | 'TIMED' | 'CONDITION',
  action: 'ON' as 'ON' | 'OFF',
  selected: [] as string[],
  time: '07:00',
  closeTime: '',
  metric: 'temp',
  metricDeviceId: '',
  operator: '>',
  threshold: 32,
  startTime: '00:00',
  endTime: '23:59',
  useReturn: false,
  returnThreshold: 30,
});

function deviceEntries(): DeviceEntry[] {
  return devices.value
    .filter((d) => form.value.selected.includes(d.deviceId))
    .map((d) => ({ deviceId: d.deviceId, channel: 1, deviceName: d.name || d.deviceId, deviceType: '风机' }));
}

function payload(): Partial<ControlRule> {
  const f = form.value;
  return {
    name: f.name, kind: f.kind, trigger: (f.kind === 'GROUP' ? 'MANUAL' : f.kind) as 'MANUAL' | 'TIMED' | 'CONDITION',
    devices: JSON.stringify(deviceEntries()), action: f.action,
    time: f.kind === 'TIMED' ? f.time : undefined,
    closeTime: f.kind === 'TIMED' && f.closeTime ? f.closeTime : undefined,
    metric: f.kind === 'CONDITION' ? f.metric : undefined,
    metricDeviceId: f.kind === 'CONDITION' ? f.metricDeviceId : undefined,
    operator: f.kind === 'CONDITION' ? f.operator : undefined,
    threshold: f.kind === 'CONDITION' ? f.threshold : undefined,
    startTime: f.kind === 'CONDITION' ? f.startTime : undefined,
    endTime: f.kind === 'CONDITION' ? f.endTime : undefined,
    useReturn: f.kind === 'CONDITION' ? f.useReturn : undefined,
    returnThreshold: f.kind === 'CONDITION' && f.useReturn ? f.returnThreshold : undefined,
  };
}

async function doCheck() {
  try {
    const res = await checkRule(payload());
    conflict.value = res.data || null;
    const hard = (conflict.value?.hits || []).some((h) => h.hard);
    const text = conflict.value?.error || (hard ? '存在控制冲突' : (conflict.value?.hits.length ? '存在同向重复' : '校验通过'));
    uni.showToast({ title: text, icon: 'none' });
  } catch { /* request 已 toast */ }
}

async function doSave(enable: boolean) {
  saving.value = true;
  try {
    await saveRule({ ...payload(), id: editingId.value || undefined }, enable ? 'ENABLE' : 'DRAFT');
    uni.showToast({ title: enable ? '已校验并启用' : '已保存停用草稿', icon: 'success' });
    setTimeout(() => uni.navigateBack(), 600);
  } catch { /* request 已 toast */ } finally { saving.value = false; }
}

function blocked() {
  return (conflict.value?.hits || []).some((h) => h.hard);
}

onLoad(async (options) => {
  editingId.value = (options?.id as string) || '';
  const devRes = await listDevices();
  devices.value = devRes.data || [];
  deviceOptions.value = devices.value.map((d) => ({ id: d.deviceId, name: d.name || d.deviceId }));
  if (!form.value.metricDeviceId && devices.value.length) form.value.metricDeviceId = devices.value[0].deviceId;
  const draft = storage.get<Partial<ControlRule>>('kb.ruleDraft');
  if (draft) {
    storage.remove('kb.ruleDraft');
    const entries = JSON.parse(draft.devices || '[]') as DeviceEntry[];
    form.value = {
      name: draft.name || '', kind: (draft.kind as 'GROUP' | 'TIMED' | 'CONDITION') || 'TIMED',
      action: (draft.action as 'ON' | 'OFF') || 'ON', selected: entries.map((e) => e.deviceId),
      time: draft.time || '07:00', closeTime: draft.closeTime || '', metric: draft.metric || 'temp',
      metricDeviceId: draft.metricDeviceId || form.value.metricDeviceId, operator: draft.operator || '>',
      threshold: Number(draft.threshold) || 32, startTime: draft.startTime || '00:00',
      endTime: draft.endTime || '23:59', useReturn: !!draft.useReturn, returnThreshold: Number(draft.returnThreshold) || 30,
    };
    editingId.value = draft.id || editingId.value;
  }
});
</script>
<template>
  <scroll-view scroll-y class="page" :class="{ 'kb-large': large }">
    <view class="kb-card form">
      <view class="field"><text class="label">名称</text><input class="input" v-model="form.name" maxlength="40" /></view>
      <view class="field"><text class="label">规则类型</text>
        <view class="seg-row">
          <view v-for="k in ['TIMED', 'CONDITION', 'GROUP']" :key="k" class="seg" :class="{ active: form.kind === k }" @tap="form.kind = k as any">{{ k === 'TIMED' ? '定时' : k === 'CONDITION' ? '条件' : '组合' }}</view>
        </view>
      </view>
      <view class="field"><text class="label">执行设备（组合需同类型多台）</text>
        <view v-for="d in deviceOptions" :key="d.id" class="check-row" @tap="() => { const i = form.selected.indexOf(d.id); i >= 0 ? form.selected.splice(i, 1) : form.selected.push(d.id); }">
          <view class="checkbox" :class="{ checked: form.selected.includes(d.id) }">✓</view>
          <text>{{ d.name }}</text>
        </view>
      </view>
      <view class="field"><text class="label">执行动作</text>
        <view class="seg-row">
          <view class="seg" :class="{ active: form.action === 'ON' }" @tap="form.action = 'ON'">开启</view>
          <view class="seg" :class="{ active: form.action === 'OFF' }" @tap="form.action = 'OFF'">关闭</view>
        </view>
      </view>
      <template v-if="form.kind === 'TIMED'">
        <view class="field"><text class="label">每天执行时间</text><input class="input" v-model="form.time" placeholder="HH:mm" /></view>
        <view class="field"><text class="label">反向动作时间（选填）</text><input class="input" v-model="form.closeTime" placeholder="HH:mm" /></view>
      </template>
      <template v-if="form.kind === 'CONDITION'">
        <view class="field"><text class="label">取数设备</text>
          <picker :range="deviceOptions.map(d => d.name)" @change="(e: any) => (form.metricDeviceId = deviceOptions[e.detail.value].id)">
            <view class="input">{{ deviceOptions.find(d => d.id === form.metricDeviceId)?.name || '选择设备' }}</view>
          </picker>
        </view>
        <view class="field"><text class="label">指标</text>
          <view class="seg-row">
            <view class="seg" :class="{ active: form.metric === 'temp' }" @tap="form.metric = 'temp'">空气温度</view>
            <view class="seg" :class="{ active: form.metric === 'humid' }" @tap="form.metric = 'humid'">空气湿度</view>
          </view>
        </view>
        <view class="field"><text class="label">触发条件</text>
          <view class="inline">
            <view class="seg-row" style="flex: 1">
              <view v-for="op in ['>', '>=', '<', '<=']" :key="op" class="seg" :class="{ active: form.operator === op }" @tap="form.operator = op">{{ op }}</view>
            </view>
            <input class="input small" type="digit" v-model="form.threshold" />
          </view>
        </view>
        <view class="field"><text class="label">生效时段</text>
          <view class="inline">
            <input class="input small" v-model="form.startTime" placeholder="开始 HH:mm" />
            <input class="input small" v-model="form.endTime" placeholder="结束 HH:mm" />
          </view>
        </view>
        <view class="check-row" @tap="form.useReturn = !form.useReturn">
          <view class="checkbox" :class="{ checked: form.useReturn }">✓</view>
          <text>设置恢复后的反向动作</text>
        </view>
        <view v-if="form.useReturn" class="field"><text class="label">恢复阈值</text><input class="input small" type="digit" v-model="form.returnThreshold" /></view>
      </template>
    </view>

    <view v-if="conflict" class="verdict" :class="(conflict.hits || []).some(h => h.hard) ? 'bad' : (conflict.hits || []).length ? 'warn' : 'ok'">
      {{ conflict.error || ((conflict.hits || []).some(h => h.hard) ? '存在控制冲突' : (conflict.hits || []).length ? '存在同向重复' : '校验通过') }}
      <text v-for="h in conflict.hits" :key="h.ruleName" class="hit">{{ h.ruleName }} · {{ h.detail }}</text>
    </view>

    <view class="actions">
      <view class="btn" hover-class="kb-press" @tap="doCheck">检查冲突</view>
      <view class="btn" hover-class="kb-press" :class="{ disabled: saving || blocked() }" @tap="doSave(false)">保存停用草稿</view>
    </view>
    <view class="btn primary wide" hover-class="kb-press" :class="{ disabled: saving || blocked() }" @tap="doSave(true)">保存并启用</view>
  </scroll-view>
</template>

<style scoped lang="scss">
.page { padding: 24rpx; min-height: 100vh; box-sizing: border-box; }
.form { padding: 20rpx 26rpx; display: flex; flex-direction: column; gap: 22rpx; }
.field { display: flex; flex-direction: column; gap: 10rpx; }
.label { font-size: 25rpx; font-weight: 600; color: $kb-text; }
.input { background: #fff; border: 1rpx solid #a9bdaf; border-radius: 12rpx; padding: 18rpx; font-size: 27rpx; color: $kb-text; }
.input.small { flex: 1; }
.inline { display: flex; gap: 12rpx; align-items: center; }
.seg-row { display: flex; gap: 8rpx; background: $kb-bg-sunken; border-radius: $kb-radius-pill; padding: 6rpx; }
.seg { flex: 1; text-align: center; padding: 12rpx 0; font-size: 25rpx; color: $kb-text-muted; border-radius: $kb-radius-pill; }
.seg.active { background: #fff; color: $kb-brand; font-weight: 700; }
.check-row { display: flex; align-items: center; gap: 14rpx; font-size: 27rpx; color: $kb-text; padding: 6rpx 0; }
.checkbox { width: 36rpx; height: 36rpx; border: 2rpx solid #a9bdaf; border-radius: 8rpx; display: flex; align-items: center; justify-content: center; font-size: 24rpx; color: #fff; }
.checkbox.checked { background: $kb-brand; border-color: $kb-brand; }
.verdict { padding: 18rpx 24rpx; border-radius: $kb-radius-tile; font-size: 26rpx; margin: 20rpx 0; display: flex; flex-direction: column; gap: 8rpx; }
.verdict.ok { background: $kb-success-bg; color: $kb-success; }
.verdict.warn { background: $kb-warning-bg; color: $kb-warning; }
.verdict.bad { background: $kb-danger-bg; color: $kb-danger; }
.hit { font-size: 23rpx; }
.actions { display: flex; gap: 14rpx; margin-bottom: 16rpx; }
.btn { flex: 1; text-align: center; padding: 22rpx 0; border-radius: $kb-radius-btn; border: 1rpx solid #83a994; color: $kb-brand; font-size: 28rpx; font-weight: 600; background: #fff; }
.btn.primary { background: $kb-brand; color: #fff; border-color: $kb-brand; }
.btn.wide { margin-bottom: 40rpx; }
.btn.disabled { opacity: 0.55; }
.kb-large .label, .kb-large .input, .kb-large .seg { font-size: 31rpx; }
</style>