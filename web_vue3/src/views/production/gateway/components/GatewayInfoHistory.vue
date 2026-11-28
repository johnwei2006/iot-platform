<script setup lang="ts">
import { computed, ref } from 'vue';
import {
  parseGatewayPayload,
  type GatewayInfoHistoryRecord,
  type GatewayInfoPayload,
} from '@/api/iot/gatewayManage';

const props = defineProps<{
  records: GatewayInfoHistoryRecord[];
  total: number;
  page: number;
  size: number;
  loading?: boolean;
}>();

const emit = defineEmits<{
  (e: 'change', page: number, size: number): void;
}>();

interface DetailField { key: string; labelKey: string }
const detailFields: DetailField[] = [
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

const rows = computed(() => props.records.map((record) => {
  const fallback = record.payload ? parseGatewayPayload<GatewayInfoPayload>(record.payload) : {};
  return {
    ...record,
    projectId: record.projectId ?? fallback.projectId,
    rtcTime: record.rtcTime ?? fallback.rtcTime,
    firmwareVersion: record.firmwareVersion ?? fallback.firmwareVersion,
    memoryUsed: record.memoryUsed ?? fallback.memoryUsed,
    flashUsed: record.flashUsed ?? fallback.flashUsed,
    battery: record.battery ?? fallback.battery,
  };
}));

const detailVisible = ref(false);
const detailRow = ref<GatewayInfoHistoryRecord | null>(null);

const detailInfo = computed<Record<string, string>>(() => {
  const record = detailRow.value;
  if (!record) return {};
  const fallback = record.payload ? parseGatewayPayload<GatewayInfoPayload>(record.payload) : {};
  const source: Record<string, unknown> = { ...fallback, ...record };
  const result: Record<string, string> = {};
  for (const field of detailFields) {
    result[field.key] = formatValue(field.key, source[field.key]);
  }
  return result;
});

function formatValue(key: string, value: unknown): string {
  if (value === undefined || value === null || value === '') return '--';
  if (key === 'adapterExists' || key === 'chargingEnabled') {
    return Number(value) === 1 || value === true ? 'Y' : 'N';
  }
  return typeof value === 'boolean' ? (value ? 'Y' : 'N') : String(value);
}

function formatTime(value?: string) {
  if (!value) return '--';
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? value : d.toLocaleString();
}

function indexMethod(index: number) {
  return (props.page - 1) * props.size + index + 1;
}

function openDetail(row: GatewayInfoHistoryRecord) {
  detailRow.value = row;
  detailVisible.value = true;
}
</script>

<template>
  <div class="info-history">
    <el-table :data="rows" size="small" stripe height="250" empty-text=" " v-loading="loading">
      <el-table-column type="index" :index="indexMethod" width="52" />
      <el-table-column :label="$t('gatewayManage.reportTime')" width="160">
        <template #default="{ row }">{{ formatTime(row.reportedAt) }}</template>
      </el-table-column>
      <el-table-column :label="$t('gatewayManage.infoFields.projectId')" prop="projectId" min-width="100" show-overflow-tooltip />
      <el-table-column :label="$t('gatewayManage.infoFields.rtcTime')" prop="rtcTime" min-width="130" show-overflow-tooltip />
      <el-table-column :label="$t('gatewayManage.infoFields.firmwareVersion')" prop="firmwareVersion" min-width="100" show-overflow-tooltip />
      <el-table-column :label="$t('gatewayManage.infoFields.memoryUsed')" prop="memoryUsed" width="88" />
      <el-table-column :label="$t('gatewayManage.infoFields.flashUsed')" prop="flashUsed" width="88" />
      <el-table-column :label="$t('gatewayManage.infoFields.battery')" prop="battery" min-width="100" show-overflow-tooltip />
      <el-table-column :label="$t('gatewayManage.details')" width="76" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openDetail(row)">{{ $t('gatewayManage.details') }}</el-button>
        </template>
      </el-table-column>
      <template #empty><div class="empty">{{ $t('gatewayManage.noReport') }}</div></template>
    </el-table>

    <div class="pager">
      <el-pagination
        background
        layout="total, sizes, prev, pager, next"
        :total="total"
        :current-page="page"
        :page-size="size"
        :page-sizes="[10, 20, 50, 100]"
        @current-change="emit('change', $event, size)"
        @size-change="emit('change', 1, $event)"
      />
    </div>

    <el-dialog v-model="detailVisible" :title="$t('gatewayManage.details')" width="760px" top="8vh">
      <el-descriptions v-if="detailRow" :column="2" border size="small">
        <el-descriptions-item :label="$t('gatewayManage.reportTime')">
          {{ formatTime(detailRow.reportedAt) }}
        </el-descriptions-item>
        <el-descriptions-item v-for="field in detailFields" :key="field.key" :label="$t(field.labelKey)">
          {{ detailInfo[field.key] }}
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button size="small" @click="detailVisible = false">{{ $t('gatewayManage.close') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.info-history { display: flex; flex-direction: column; gap: 10px; min-height: 0; }
.empty { padding: 30px; color: var(--el-text-color-secondary); text-align: center; }
.pager { display: flex; justify-content: flex-end; }
</style>
