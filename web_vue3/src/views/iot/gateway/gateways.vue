<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { ElMessage } from 'element-plus';
import { Search } from '@element-plus/icons-vue';
import { listDevices } from '@/api/iot/gateway';
import { listAreas } from '@/api/iot/area';
import AreaSelect from '@/components/AreaSelect.vue';
import GatewayRecords from './records.vue';
import type { Device } from '@/types';

const { t } = useI18n();
const router = useRouter();
const loading = ref(false);
const gateways = ref<Device[]>([]);
const nameField = ref<'name' | 'deviceId'>('name');
const keyword = ref('');
const onlineFilter = ref('');
const typeFilter = ref<number | ''>('');
const areaFilter = ref('');
const areaNameMap = ref(new Map<string, string>());
const recordsVisible = ref(false);
const recordsGateway = ref<Device | null>(null);

function areaName(id?: string) {
  return (id && areaNameMap.value.get(id)) || '--';
}

function typeName(type?: number) {
  if (type === undefined || type === null) return '--';
  const key = `gateway.type_${type}`;
  const translated = t(key);
  return translated === key ? String(type) : translated;
}

function formatTime(value?: string) {
  if (!value) return '--';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ` +
    `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

const filtered = computed(() => gateways.value.filter((g) => {
  const text = keyword.value.trim().toLowerCase();
  const hitName = !text ||
    String(nameField.value === 'name' ? (g.name ?? '') : (g.deviceId ?? '')).toLowerCase().includes(text);
  const hitStatus = !onlineFilter.value || g.status === onlineFilter.value;
  const hitType = typeFilter.value === '' || g.gatewayType === typeFilter.value;
  const hitArea = !areaFilter.value || g.areaId === areaFilter.value;
  return hitName && hitStatus && hitType && hitArea;
}));

function reset() {
  keyword.value = '';
  nameField.value = 'name';
  onlineFilter.value = '';
  typeFilter.value = '';
  areaFilter.value = '';
}

async function load() {
  loading.value = true;
  try {
    const [deviceRes, areaRes] = await Promise.all([listDevices(), listAreas()]);
    gateways.value = deviceRes.data ?? [];
    areaNameMap.value = new Map((areaRes.data ?? []).map((a) => [a.id, a.areaName]));
  } catch (error) {
    ElMessage.error((error as Error).message || t('gateway.loadFailed'));
  } finally {
    loading.value = false;
  }
}

function openDetail(row: Device) {
  if (row.id) router.push(`/iot/gateways/${row.id}`);
}

function openRecords(row: Device) {
  recordsGateway.value = row;
  recordsVisible.value = true;
}

onMounted(load);
</script>

<template>
  <div class="gateway-page">
    <h2 class="page-title">{{ $t('gateway.title') }}</h2>

    <div class="filter-bar">
      <el-select v-model="nameField" class="field-select" :aria-label="$t('gateway.nameField')">
        <el-option :label="$t('gateway.name')" value="name" />
        <el-option :label="$t('gateway.code')" value="deviceId" />
      </el-select>
      <el-input v-model="keyword" class="keyword-input" clearable :placeholder="$t('common.search')">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="onlineFilter" class="status-select" clearable :placeholder="$t('gateway.onlineFilter')" :aria-label="$t('gateway.onlineFilter')">
        <el-option :label="$t('common.online')" value="ONLINE" />
        <el-option :label="$t('common.offline')" value="OFFLINE" />
      </el-select>
      <el-select v-model="typeFilter" class="type-select" clearable :placeholder="$t('gateway.typePlaceholder')" :aria-label="$t('gateway.type')">
        <el-option :label="$t('gateway.type_1')" :value="1" />
        <el-option :label="$t('gateway.type_2')" :value="2" />
      </el-select>
      <AreaSelect v-model="areaFilter" class="area-select" :placeholder="$t('gateway.areaFilter')" />
      <el-button type="primary" @click="load">{{ $t('common.search') }}</el-button>
      <el-button @click="reset">{{ $t('common.reset') }}</el-button>
    </div>

    <div class="table-card" v-loading="loading">
      <el-table :data="filtered" border style="width: 100%" :empty-text="$t('gateway.empty')">
        <el-table-column :label="$t('common.page')" type="index" width="64" align="center" />
        <el-table-column prop="name" :label="$t('gateway.name')" min-width="180" show-overflow-tooltip />
        <el-table-column prop="deviceId" :label="$t('gateway.code')" min-width="190" show-overflow-tooltip />
        <el-table-column :label="$t('common.status')" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ONLINE' ? 'success' : 'danger'" size="small" effect="light">
              {{ row.status === 'ONLINE' ? $t('common.online') : $t('common.offline') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('gateway.type')" width="120" align="center">
          <template #default="{ row }">{{ typeName(row.gatewayType) }}</template>
        </el-table-column>
        <el-table-column :label="$t('gateway.area')" min-width="170" show-overflow-tooltip>
          <template #default="{ row }">{{ areaName(row.areaId) }}</template>
        </el-table-column>
        <el-table-column :label="$t('gateway.lastSeen')" width="180" align="center">
          <template #default="{ row }">{{ formatTime(row.lastSeen) }}</template>
        </el-table-column>
        <el-table-column :label="$t('common.actions')" width="170" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">{{ $t('gateway.viewDetail') }}</el-button>
            <el-button link type="primary" @click="openRecords(row)">{{ $t('gateway.records') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog
      v-model="recordsVisible"
      :title="`${$t('gateway.records')} - ${recordsGateway?.name || recordsGateway?.deviceId || ''}`"
      width="900px"
      destroy-on-close
    >
      <GatewayRecords v-if="recordsGateway?.deviceId" :device-id="recordsGateway.deviceId" />
    </el-dialog>
  </div>
</template>

<style scoped>
.gateway-page { padding: 20px 24px 32px; }
.page-title { margin: 0 0 20px; font-size: 20px; font-weight: 600; color: #303133; }
.filter-bar { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin-bottom: 16px; }
.field-select { width: 110px; }
.keyword-input { width: 200px; }
.status-select { width: 130px; }
.type-select { width: 160px; }
.table-card { background: #fff; border: 1px solid #ebeef5; border-radius: 4px; padding: 4px; }
:deep(.el-table th.el-table__cell) { background: #f5f7fa; color: #606266; font-weight: 600; }
:deep(.el-table .cell) { font-size: 13px; }
</style>
