<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { listControlLogs } from '@/api/iot/control';
import type { ControlLog } from '@/types/kb';

const loading = ref(false);
const logs = ref<ControlLog[]>([]);
const resultFilter = ref('全部');

const SOURCE_LABELS: Record<string, string> = { MANUAL: '手动', RULE: '自动规则', GROUP: '组合' };
const RESULT_TYPES: Record<string, 'success' | 'info' | 'danger' | 'warning'> = {
  SUCCESS: 'success', PENDING: 'info', FAILED: 'danger', TIMEOUT: 'warning', PARTIAL: 'warning',
};

const filtered = ref<ControlLog[]>([]);

async function refresh() {
  loading.value = true;
  try {
    const res = await listControlLogs();
    logs.value = res.data || [];
    applyFilter();
  } catch (error) {
    ElMessage.error((error as Error).message || '控制记录加载失败');
  } finally {
    loading.value = false;
  }
}

function applyFilter() {
  filtered.value = resultFilter.value === '全部'
    ? logs.value
    : logs.value.filter((l) => l.result === resultFilter.value);
}

onMounted(refresh);
</script>

<template>
  <div v-loading="loading">
    <el-card shadow="never">
      <template #header>
        <el-segmented v-model="resultFilter" :options="['全部', 'PENDING', 'SUCCESS', 'TIMEOUT', 'FAILED', 'PARTIAL']" @change="applyFilter" />
      </template>
      <el-table :data="filtered" stripe>
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ (row.createdAt || '').replace('T', ' ').slice(0, 19) }}</template>
        </el-table-column>
        <el-table-column label="设备" min-width="140">
          <template #default="{ row }">{{ row.deviceName || row.deviceId }}</template>
        </el-table-column>
        <el-table-column label="动作" width="90">
          <template #default="{ row }">{{ row.action }}</template>
        </el-table-column>
        <el-table-column label="来源" width="110">
          <template #default="{ row }">{{ SOURCE_LABELS[row.source] || row.source }}<template v-if="row.ruleName"> · {{ row.ruleName }}</template></template>
        </el-table-column>
        <el-table-column label="结果" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="RESULT_TYPES[row.result] || 'info'">{{ row.result }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="设备反馈" min-width="220">
          <template #default="{ row }">{{ row.detail }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>