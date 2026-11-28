<script setup lang="ts">
import { computed } from 'vue';
import type { GatewayInfoPayload } from '@/api/iot/gatewayManage';

const props = defineProps<{ info: GatewayInfoPayload; reportedAt?: string }>();

interface InfoField { key: string; labelKey: string }
const fields: InfoField[] = [
  { key: 'configVersion', labelKey: 'gatewayManage.infoFields.configVersion' },
  { key: 'gatewayId', labelKey: 'gatewayManage.infoFields.gatewayId' },
  { key: 'projectId', labelKey: 'gatewayManage.infoFields.projectId' },
  { key: 'heartbeatInterval', labelKey: 'gatewayManage.infoFields.heartbeatInterval' },
  { key: 'reportInterval', labelKey: 'gatewayManage.infoFields.reportInterval' },
  { key: 'rtcTime', labelKey: 'gatewayManage.infoFields.rtcTime' },
  { key: 'systemServerUrl', labelKey: 'gatewayManage.infoFields.systemServerUrl' },
  { key: 'systemServerPort', labelKey: 'gatewayManage.infoFields.systemServerPort' },
  { key: 'hardwareVersion', labelKey: 'gatewayManage.infoFields.hardwareVersion' },
  { key: 'firmwareVersion', labelKey: 'gatewayManage.infoFields.firmwareVersion' },
  { key: 'gatewayMcuid', labelKey: 'gatewayManage.infoFields.gatewayMcuid' },
  { key: 'memoryTotal', labelKey: 'gatewayManage.infoFields.memoryTotal' },
  { key: 'memoryUsed', labelKey: 'gatewayManage.infoFields.memoryUsed' },
  { key: 'flashTotal', labelKey: 'gatewayManage.infoFields.flashTotal' },
  { key: 'flashUsed', labelKey: 'gatewayManage.infoFields.flashUsed' },
  { key: 'uplinkMessageSize', labelKey: 'gatewayManage.infoFields.uplinkMessageSize' },
  { key: 'downlinkMessageSize', labelKey: 'gatewayManage.infoFields.downlinkMessageSize' },
  { key: 'battery', labelKey: 'gatewayManage.infoFields.battery' },
  { key: 'adapterExists', labelKey: 'gatewayManage.infoFields.adapterExists' },
  { key: 'chargingEnabled', labelKey: 'gatewayManage.infoFields.chargingEnabled' },
  { key: 'latestRebootTime', labelKey: 'gatewayManage.infoFields.latestRebootTime' },
];

const hasData = computed(() => fields.some((f) =>
  props.info[f.key] !== undefined && props.info[f.key] !== null && props.info[f.key] !== ''));

function format(key: string, value: unknown): string {
  if (value === undefined || value === null || value === '') return '--';
  if (key === 'adapterExists' || key === 'chargingEnabled') {
    return Number(value) === 1 || value === true ? 'Y' : 'N';
  }
  return typeof value === 'boolean' ? (value ? 'Y' : 'N') : String(value);
}
</script>

<template>
  <div class="info-panel">
    <div class="meta">
      <span>{{ $t('gatewayManage.reportTime') }}：{{ reportedAt ? new Date(reportedAt).toLocaleString() : '--' }}</span>
      <el-tag v-if="hasData" type="success" size="small" effect="light">{{ $t('gatewayManage.infoReceived') }}</el-tag>
      <el-tag v-else type="info" size="small" effect="light">{{ $t('gatewayManage.noReport') }}</el-tag>
    </div>
    <div class="field-grid">
      <div v-for="field in fields" :key="field.key" class="field-item" :title="`${$t(field.labelKey)}：${format(field.key, info[field.key])}`">
        <span class="field-label">{{ $t(field.labelKey) }}：</span>
        <span class="field-value">{{ format(field.key, info[field.key]) }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.info-panel { display: flex; flex-direction: column; gap: 12px; min-height: 0; }
.meta { display: flex; align-items: center; gap: 10px; font-size: 12px; color: var(--el-text-color-secondary); }
.field-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 8px; }
.field-item {
  display: flex; align-items: baseline; min-width: 0; padding: 10px 12px;
  border: 1px solid #edf1f7; border-radius: 8px;
  background: linear-gradient(135deg, #ffffff 0%, #f8fbff 100%);
  font-size: 13px; line-height: 1.5;
}
.field-label { flex: 0 1 auto; color: var(--el-text-color-secondary); }
.field-value {
  flex: 1 1 auto; min-width: 0; margin-left: 4px; font-weight: 600; color: #1f2937;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
@media (max-width: 1400px) { .field-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 760px) { .field-grid { grid-template-columns: minmax(0, 1fr); } }
</style>
