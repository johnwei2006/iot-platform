<script setup lang="ts">
import type { GatewayCommandRecord } from '@/api/iot/gatewayManage';

defineProps<{ commands: GatewayCommandRecord[] }>();

const statusType: Record<string, string> = {
  PENDING: 'warning', SUCCESS: 'success', FAILED: 'danger', TIMEOUT: 'info',
};

function formatTime(value?: string) {
  if (!value) return '--';
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? value : d.toLocaleString();
}
</script>

<template>
  <el-table :data="commands" size="small" stripe>
    <el-table-column :label="$t('gatewayManage.reportTime')" width="175">
      <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
    </el-table-column>
    <el-table-column :label="$t('gatewayManage.serviceType')" width="110" prop="serviceType" />
    <el-table-column label="Command" width="120" prop="commandType" />
    <el-table-column :label="$t('gatewayManage.status')" width="110">
      <template #default="{ row }">
        <el-tag :type="statusType[row.status] || 'info'" size="small" effect="light">{{ row.status }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column :label="$t('gatewayManage.result')" width="100" prop="result" />
    <el-table-column :label="$t('gatewayManage.reason')" min-width="220" show-overflow-tooltip prop="reason" />
    <template #empty><div class="empty">{{ $t('gatewayManage.noData') }}</div></template>
  </el-table>
</template>

<style scoped>
.empty { padding: 24px; color: var(--el-text-color-secondary); text-align: center; }
</style>
