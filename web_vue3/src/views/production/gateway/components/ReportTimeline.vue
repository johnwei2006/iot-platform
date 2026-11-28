<script setup lang="ts">
import type { GatewayReportRecord } from '@/api/iot/gatewayManage';

defineProps<{ reports: GatewayReportRecord[] }>();

function formatTime(value?: string) {
  if (!value) return '--';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return value;
  return d.toLocaleString();
}
</script>

<template>
  <el-table :data="reports" size="small" stripe empty-text=" ">
    <el-table-column :label="$t('gatewayManage.reportTime')" width="175">
      <template #default="{ row }">{{ formatTime(row.reportedAt || row.createdAt) }}</template>
    </el-table-column>
    <el-table-column :label="$t('gatewayManage.serviceType')" width="110" prop="serviceType" />
    <el-table-column :label="$t('gatewayManage.category')" width="130" prop="category" />
    <el-table-column :label="$t('gatewayManage.payload')" min-width="320" show-overflow-tooltip>
      <template #default="{ row }"><code>{{ row.payload }}</code></template>
    </el-table-column>
    <template #empty><div class="empty">{{ $t('gatewayManage.noData') }}</div></template>
  </el-table>
</template>

<style scoped>
code { font-family: ui-monospace, SFMono-Regular, Consolas, monospace; font-size: 12px; color: var(--el-text-color-regular); }
.empty { padding: 24px; color: var(--el-text-color-secondary); text-align: center; }
</style>
