<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { listDevices } from '@/api/iot/gateway';
import { sendCommand } from '@/api/iot/control';
import type { Device } from '@/types';
import type { DeviceEntry } from '@/types/kb';

const loading = ref(false);
const devices = ref<Device[]>([]);
const busy = ref(false);

const levelVisible = ref(false);
const levelTarget = ref<DeviceEntry | null>(null);
const levelValue = ref(50);

function kindOf(d: Device) {
  const text = `${d.name || ''}${d.deviceId}`;
  if (text.includes('blanket') || text.includes('棉被')) return 'blanket';
  return 'fan';
}

function entryOf(d: Device): DeviceEntry {
  return { deviceId: d.deviceId, nodeId: d.nodeId || undefined, channel: 1, deviceName: d.name || d.deviceId, deviceType: kindOf(d) === 'blanket' ? '棉被' : '风机' };
}

async function issue(entry: DeviceEntry, action: string, value?: string) {
  busy.value = true;
  try {
    const res = await sendCommand([entry], action, value);
    const first = (res.data?.logs || [])[0];
    if (first?.result === 'FAILED') {
      ElMessage.error(first.detail || '指令未下发');
    } else {
      ElMessage.success('指令已发送，等待设备回报');
    }
  } catch (error) {
    ElMessage.error((error as Error).message || '下发失败');
  } finally {
    busy.value = false;
  }
}

function openLevel(d: Device) {
  levelTarget.value = entryOf(d);
  levelValue.value = 50;
  levelVisible.value = true;
}

function confirmLevel() {
  const v = Math.round(Number(levelValue.value));
  if (Number.isNaN(v) || v < 0 || v > 100) {
    ElMessage.warning('开度需为 0-100');
    return;
  }
  levelVisible.value = false;
  if (levelTarget.value) issue(levelTarget.value, 'LEVEL', String(v));
}

async function refresh() {
  loading.value = true;
  try {
    const res = await listDevices();
    devices.value = res.data || [];
  } catch (error) {
    ElMessage.error((error as Error).message || '设备加载失败');
  } finally {
    loading.value = false;
  }
}

onMounted(refresh);
</script>
<template>
  <div v-loading="loading">
    <el-alert type="info" :closable="false" show-icon title="按授权过滤的可控设备；下发后可在控制记录页查看执行状态流转。" style="margin-bottom: 16px" />
    <div class="dev-grid">
      <el-card v-for="d in devices" :key="d.deviceId" shadow="never" class="dev-card">
        <template #header>
          <span class="dev-name">{{ d.name || d.deviceId }}</span>
          <el-tag size="small" style="margin-left: 8px">{{ kindOf(d) === 'blanket' ? '棉被' : '风机' }}</el-tag>
        </template>
        <template v-if="kindOf(d) === 'blanket'">
          <div class="btn-row">
            <el-button type="primary" :disabled="busy" v-permission="'iot:control:issue'" @click="issue(entryOf(d), 'OPEN')">卷起</el-button>
            <el-button :disabled="busy" v-permission="'iot:control:issue'" @click="issue(entryOf(d), 'STOP')">停止</el-button>
            <el-button :disabled="busy" v-permission="'iot:control:issue'" @click="issue(entryOf(d), 'CLOSE')">放下</el-button>
          </div>
          <el-button link type="primary" v-permission="'iot:control:issue'" @click="openLevel(d)">设置开度（0-100%）</el-button>
        </template>
        <template v-else>
          <div class="btn-row">
            <el-button type="primary" :disabled="busy" v-permission="'iot:control:issue'" @click="issue(entryOf(d), 'ON')">开启风机</el-button>
            <el-button :disabled="busy" v-permission="'iot:control:issue'" @click="issue(entryOf(d), 'OFF')">关闭风机</el-button>
          </div>
        </template>
      </el-card>
    </div>

    <el-dialog v-model="levelVisible" title="设置棉被开度" width="360px">
      <el-input-number v-model="levelValue" :min="0" :max="100" style="width: 100%" />
      <template #footer>
        <el-button @click="levelVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmLevel">下发</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.dev-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 16px; }
.dev-name { font-weight: 700; letter-spacing: var(--kb-tracking-heading); }
.btn-row { display: flex; gap: 8px; margin-bottom: 8px; }
</style>