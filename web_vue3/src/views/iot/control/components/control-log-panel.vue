<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { ElMessage } from 'element-plus';
import { listControlLogs } from '@/api/iot/control';
import type { ControlLog } from '@/types/kb';

const { t } = useI18n();
const props = defineProps<{ areaId?: string }>();

const loading = ref(false);
const logs = ref<ControlLog[]>([]);
const filtered = ref<ControlLog[]>([]);
const resultFilter = ref('');

const SOURCE_KEYS: Record<string, string> = { MANUAL: 'control.sourceManual', RULE: 'control.sourceRule', GROUP: 'control.sourceGroup' };
const RESULT_TYPES: Record<string, 'success' | 'info' | 'danger' | 'warning'> = {
  SUCCESS: 'success', PENDING: 'info', FAILED: 'danger', TIMEOUT: 'warning', PARTIAL: 'warning',
};

const filterOptions = computed(() => [
  { value: '', label: t('common.all') },
  { value: 'PENDING', label: 'PENDING' },
  { value: 'SUCCESS', label: 'SUCCESS' },
  { value: 'TIMEOUT', label: 'TIMEOUT' },
  { value: 'FAILED', label: 'FAILED' },
  { value: 'PARTIAL', label: 'PARTIAL' },
]);

function sourceLabel(source?: string) {
  if (!source) return '--';
  const key = SOURCE_KEYS[source];
  return key ? t(key) : source;
}

function applyFilter() {
  filtered.value = !resultFilter.value
    ? logs.value
    : logs.value.filter((l) => l.result === resultFilter.value);
}

async function refresh() {
  loading.value = true;
  try {
    const res = await listControlLogs();
    logs.value = res.data || [];
    applyFilter();
  } catch (error) {
    ElMessage.error((error as Error).message || t('control.logsLoadFailed'));
  } finally {
    loading.value = false;
  }
}

watch(() => props.areaId, () => {
  applyFilter();
});

onMounted(refresh);
</script>

<template>
  <div v-loading="loading">
    <el-card shadow="never">
      <template #header>
        <div class="head-row">
          <span class="head-title">{{ $t('control.logsTitle') }}<span v-if="areaId"> · {{ $t('control.currentArea') }}</span></span>
          <el-segmented v-model="resultFilter" :options="filterOptions" :props="{ value: 'value', label: 'label' }" @change="applyFilter" />
        </div>
      </template>
      <el-table :data="filtered" stripe>
        <el-table-column :label="$t('control.logTime')" width="170">
          <template #default="{ row }">{{ (row.createdAt || '').replace('T', ' ').slice(0, 19) }}</template>
        </el-table-column>
        <el-table-column :label="$t('device.title')" min-width="140">
          <template #default="{ row }">{{ row.deviceName || row.deviceId }}</template>
        </el-table-column>
        <el-table-column :label="$t('control.action')" width="90">
          <template #default="{ row }">{{ row.action }}</template>
        </el-table-column>
        <el-table-column :label="$t('control.source')" width="110">
          <template #default="{ row }">{{ sourceLabel(row.source) }}<template v-if="row.ruleName"> · {{ row.ruleName }}</template></template>
        </el-table-column>
        <el-table-column :label="$t('gatewayRecords.result')" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="RESULT_TYPES[row.result] || 'info'">{{ row.result }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('control.deviceFeedback')" min-width="220">
          <template #default="{ row }">{{ row.detail }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.head-row { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.head-title { font-size: var(--kb-fs-subheading); font-weight: 600; }
</style>
