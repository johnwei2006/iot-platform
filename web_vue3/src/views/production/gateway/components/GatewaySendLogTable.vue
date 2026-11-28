<script setup lang="ts">
import { computed } from 'vue';
import { parseGatewayPayload, type GatewayCommandRecord, type GatewayConfigPayload } from '@/api/iot/gatewayManage';

const props = defineProps<{ commands: GatewayCommandRecord[] }>();

const statusType: Record<string, string> = {
  PENDING: 'warning', SUCCESS: 'success', FAILED: 'danger', TIMEOUT: 'info',
};

interface LogRow {
  id: string;
  time?: string;
  status: string;
  summary: string;
  payload: string;
}

const rows = computed<LogRow[]>(() => props.commands
  .map((command) => {
    const config = parseGatewayPayload<GatewayConfigPayload>(command.payload);
    return {
      id: command.id,
      time: command.sentAt || command.createdAt,
      status: command.status,
      summary: [
        `v${config.configVersion ?? '--'}`,
        config.gatewayId || '--',
        `HB:${config.heartbeatInterval ?? '--'}s`,
        `RPT:${config.reportInterval ?? '--'}s`,
        `LoRa:${config.loraFreqGroup ?? '--'}`,
      ].join(' · '),
      payload: JSON.stringify(config, null, 2),
    };
  }));

function formatTime(value?: string) {
  if (!value) return '--';
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? value : d.toLocaleString();
}
</script>

<template>
  <el-table :data="rows" size="small" stripe height="250" empty-text=" ">
    <el-table-column type="expand">
      <template #default="{ row }">
        <pre class="payload">{{ row.payload }}</pre>
      </template>
    </el-table-column>
    <el-table-column :label="$t('gatewayManage.sendTime')" width="170">
      <template #default="{ row }">{{ formatTime(row.time) }}</template>
    </el-table-column>
    <el-table-column :label="$t('gatewayManage.status')" width="110">
      <template #default="{ row }">
        <el-tag :type="statusType[row.status] || 'info'" size="small" effect="light">{{ row.status }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column :label="$t('gatewayManage.configSummary')" min-width="280" prop="summary" show-overflow-tooltip />
    <template #empty><div class="empty">{{ $t('gatewayManage.noSendRecord') }}</div></template>
  </el-table>
</template>

<style scoped>
.payload { margin: 0; padding: 10px 14px; background: #f6f9fc; border-radius: 6px; font-size: 12px; color: #4b5563; }
.empty { padding: 30px; color: var(--el-text-color-secondary); text-align: center; }
</style>
