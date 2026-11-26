<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { listDevices } from '@/api/iot/gateway';
import { checkRule, createRule } from '@/api/iot/control';
import type { ConflictResult } from '@/types/kb';
import type { Device } from '@/types';

const loading = ref(false);
const devices = ref<Device[]>([]);
const kind = ref<'TIMED' | 'CONDITION'>('TIMED');
const conflict = ref<ConflictResult | null>(null);
const savedDrafts = ref(0);
const enabledNow = ref<string[]>([]);

const form = reactive({
  deviceId: '',
  action: 'ON' as 'ON' | 'OFF',
  time: '07:20',
  startTime: '07:10',
  endTime: '07:30',
  threshold: 32,
});

const candidate = () => ({
  name: `管理员校验 ${new Date().toLocaleTimeString('zh-CN', { hour12: false })}`,
  kind: kind.value,
  trigger: kind.value,
  devices: JSON.stringify(devices.value.filter((d: Device) => d.deviceId === form.deviceId).map((d: Device) => ({
    deviceId: d.deviceId, nodeId: d.nodeId || undefined, channel: 1, deviceName: d.name || d.deviceId, deviceType: '风机',
  }))),
  action: form.action,
  time: kind.value === 'TIMED' ? form.time : undefined,
  startTime: kind.value === 'CONDITION' ? form.startTime : undefined,
  endTime: kind.value === 'CONDITION' ? form.endTime : undefined,
  metric: kind.value === 'CONDITION' ? 'temp' : undefined,
  metricDeviceId: kind.value === 'CONDITION' ? form.deviceId : undefined,
  operator: kind.value === 'CONDITION' ? '>=' : undefined,
  threshold: kind.value === 'CONDITION' ? form.threshold : undefined,
});

async function doCheck() {
  try {
    const res = await checkRule(candidate());
    conflict.value = res.data || null;
  } catch (error) {
    ElMessage.error((error as Error).message || '校验失败');
  }
}

async function doSave(enable: boolean) {
  try {
    await createRule(candidate(), enable ? 'ENABLE' : 'DRAFT');
    if (enable) {
      enabledNow.value.push(form.deviceId + ' · ' + (kind.value === 'TIMED' ? `每天 ${form.time} ${form.action === 'ON' ? '开启' : '关闭'}` : `temp>=${form.threshold}`));
    } else {
      savedDrafts.value += 1;
    }
    ElMessage.success(enable ? '规则已启用' : '已保存停用草稿');
    conflict.value = null;
  } catch (error) {
    ElMessage.error((error as Error).message || '保存失败');
  }
}

const blocked = () => (conflict?.value?.hits.some((h) => h.hard) ?? false);

async function refresh() {
  loading.value = true;
  try {
    const res = await listDevices();
    devices.value = res.data || [];
    if (devices.value.length && !form.deviceId) form.deviceId = devices.value[0].deviceId;
  } catch (error) {
    ElMessage.error((error as Error).message || '设备加载失败');
  } finally {
    loading.value = false;
  }
}

onMounted(refresh);
</script>
<template>
  <div v-loading="loading" class="admin-wrap">
    <el-card shadow="never">
      <template #header>
        <span class="t">智能控制冲突校验</span>
        <span class="s">选择执行设备和动作，核对已启用的定时和条件规则。服务端为唯一权威。</span>
      </template>

      <el-segmented v-model="kind" :options="['TIMED', 'CONDITION']" style="margin-bottom: 16px" />
      <el-form label-width="120px" style="max-width: 560px">
        <el-form-item label="执行设备">
          <el-select v-model="form.deviceId" style="width: 100%">
            <el-option v-for="d in devices" :key="d.deviceId" :value="d.deviceId" :label="d.name || d.deviceId" />
          </el-select>
        </el-form-item>
        <el-form-item label="执行动作">
          <el-radio-group v-model="form.action">
            <el-radio value="ON">开启</el-radio>
            <el-radio value="OFF">关闭</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="kind === 'TIMED'" label="每天执行时间"><el-time-picker v-model="form.time" format="HH:mm" /></el-form-item>
        <template v-else>
          <el-form-item label="允许触发时段">
            <el-time-picker v-model="form.startTime" format="HH:mm" style="width: 130px" />
            <span style="margin: 0 6px">至</span>
            <el-time-picker v-model="form.endTime" format="HH:mm" style="width: 130px" />
          </el-form-item>
          <el-form-item label="空气温度达到（℃）"><el-input-number v-model="form.threshold" :step="0.1" /></el-form-item>
        </template>
      </el-form>

      <el-alert v-if="conflict" :type="conflict.error ? 'warning' : conflict.hits.some((h) => h.hard) ? 'error' : conflict.hits.length ? 'warning' : 'success'" :closable="false" show-icon
        :title="conflict.error || (conflict.hits.some((h) => h.hard) ? '与已有规则冲突' : conflict.hits.length ? '与已有规则动作重复' : '校验通过')" style="margin-bottom: 12px; max-width: 560px" />
      <div v-for="h in conflict?.hits || []" :key="h.ruleName" class="hit-row">{{ h.ruleName }} · {{ h.detail }}</div>

      <div class="actions">
        <el-button v-permission="'iot:rule:check'" @click="doCheck">检查冲突</el-button>
        <el-button v-permission="'iot:rule:edit'" :disabled="blocked()" @click="doSave(false)">保存停用草稿</el-button>
        <el-button type="primary" v-permission="'iot:rule:edit'" :disabled="blocked()" @click="doSave(true)">保存并启用</el-button>
      </div>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <template #header><span class="t">判定规则说明</span></template>
      <div class="kv"><span>执行对象</span><b>定时和条件按实际设备校验；组合由用户手动启动。</b></div>
      <div class="kv"><span>时间和动作</span><b>时间存在交集且动作相反时阻止生效。同向重复发出提示。</b></div>
      <div class="kv"><span>生效复核</span><b>保存并启用时服务端重新校验最新规则。停用草稿不参与执行。</b></div>
      <div v-if="enabledNow.length || savedDrafts" class="summary">
        <div v-for="(s, i) in enabledNow" :key="i" class="kv"><span>本次已启用 {{ i + 1 }}</span><b>{{ s }}</b></div>
        <div v-if="savedDrafts" class="kv"><span>停用草稿</span><b>已保存 {{ savedDrafts }} 条</b></div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.t { font-size: var(--kb-fs-subheading); font-weight: 700; letter-spacing: var(--kb-tracking-heading); margin-right: 12px; }
.s { color: var(--kb-text-muted); font-size: var(--kb-fs-caption); }
.hit-row { padding: 6px 0; border-bottom: 1px solid var(--kb-line); font-size: var(--kb-fs-caption); color: var(--kb-text-muted); max-width: 560px; }
.actions { display: flex; gap: 8px; margin-top: 16px; }
.kv { padding: 8px 0; border-bottom: 1px solid var(--kb-line); display: flex; flex-direction: column; gap: 2px; }
.kv span { font-size: var(--kb-fs-caption); color: var(--kb-text-muted); }
.kv b { font-weight: 600; }
</style>