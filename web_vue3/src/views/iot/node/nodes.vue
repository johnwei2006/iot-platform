<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { ElMessage } from 'element-plus';
import { Search } from '@element-plus/icons-vue';
import { listNodes, type NodeEntity } from '@/api/iot/node';
import { listDevices } from '@/api/iot/gateway';
import { listAreas } from '@/api/iot/area';
import AreaSelect from '@/components/AreaSelect.vue';

const { t } = useI18n();
const router = useRouter();
const loading = ref(false);
const nodes = ref<NodeEntity[]>([]);
const gatewayNames = ref(new Map<string, string>());
const areaNames = ref(new Map<string, string>());

function areaName(id?: string) {
  return (id && areaNames.value.get(id)) || '--';
}
const gatewayField = ref('gatewayCode');
const nodeField = ref('nodeCode');
const gatewayKey = ref('');
const nodeKey = ref('');
const areaFilter = ref('');
const typeFilter = ref<number | ''>('');
const onlineFilter = ref('');

const NODE_TYPES: Record<number, string> = { 1: 'ACT', 2: 'IRRI', 3: 'DATA', 4: 'node.type_4', 5: 'node.type_5' };

function typeName(type?: number) {
  if (type === undefined || type === null) return '--';
  const label = NODE_TYPES[type];
  if (!label) return String(type);
  return label.startsWith('node.') ? t(label) : label;
}

function gatewayName(code?: string) {
  if (!code) return '--';
  return gatewayNames.value.get(code) || code;
}

function formatTime(value?: string) {
  if (!value) return '--';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ` +
    `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

const filtered = computed(() => nodes.value.filter((n) => {
  const gw = gatewayKey.value.trim().toLowerCase();
  const node = nodeKey.value.trim().toLowerCase();
  return (!gw || (n.gatewayCode ?? '').toLowerCase().includes(gw))
    && (!node || (n.nodeCode ?? '').toLowerCase().includes(node))
    && (!areaFilter.value || n.areaId === areaFilter.value)
    && (typeFilter.value === '' || n.nodeType === typeFilter.value)
    && (!onlineFilter.value || (n.online === 1 ? 'ONLINE' : 'OFFLINE') === onlineFilter.value);
}));

function reset() {
  gatewayKey.value = '';
  nodeKey.value = '';
  areaFilter.value = '';
  typeFilter.value = '';
  onlineFilter.value = '';
}

async function load() {
  loading.value = true;
  try {
    const [nodeRes, gatewayRes, areaRes] = await Promise.all([listNodes(), listDevices(), listAreas()]);
    nodes.value = nodeRes.data ?? [];
    gatewayNames.value = new Map((gatewayRes.data ?? []).map((g) => [g.deviceId, g.name || g.deviceId]));
    areaNames.value = new Map((areaRes.data ?? []).map((a) => [a.id, a.areaName]));
  } catch (error) {
    ElMessage.error((error as Error).message || t('node.loadFailed'));
  } finally {
    loading.value = false;
  }
}

function openDetail(row: NodeEntity) {
  router.push(`/iot/nodes/${row.id}`);
}

onMounted(load);
</script>

<template>
  <div class="node-page">
    <h2 class="page-title">{{ $t('node.title') }}</h2>

    <div class="filter-bar">
      <div class="search-group">
        <el-select v-model="gatewayField" class="field-select" :aria-label="$t('node.gatewayField')">
          <el-option :label="$t('node.gatewayCode')" value="gatewayCode" />
        </el-select>
        <el-input v-model="gatewayKey" class="search-input" clearable :placeholder="$t('common.search')">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </div>
      <div class="search-group">
        <el-select v-model="nodeField" class="field-select" :aria-label="$t('node.nodeField')">
          <el-option :label="$t('node.nodeCode')" value="nodeCode" />
        </el-select>
        <el-input v-model="nodeKey" class="search-input" clearable :placeholder="$t('common.search')">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </div>
      <AreaSelect v-model="areaFilter" class="area-select" :placeholder="$t('gateway.areaFilter')" />
      <el-select v-model="typeFilter" class="type-select" clearable :placeholder="$t('node.typePlaceholder')" :aria-label="$t('node.type')">
        <el-option v-for="(label, value) in NODE_TYPES" :key="value" :label="label.startsWith('node.') ? $t(label) : label" :value="Number(value)" />
      </el-select>
      <el-select v-model="onlineFilter" class="status-select" clearable :placeholder="$t('node.statusPlaceholder')" :aria-label="$t('node.status')">
        <el-option :label="$t('common.online')" value="ONLINE" />
        <el-option :label="$t('common.offline')" value="OFFLINE" />
      </el-select>
      <el-button type="primary" @click="load">{{ $t('common.search') }}</el-button>
      <el-button @click="reset">{{ $t('common.reset') }}</el-button>
    </div>

    <div class="table-card" v-loading="loading">
      <el-table :data="filtered" border style="width: 100%" :empty-text="$t('common.empty')">
        <el-table-column :label="$t('common.page')" type="index" width="60" align="center" />
        <el-table-column :label="$t('node.gatewayName')" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ gatewayName(row.gatewayCode) }}</template>
        </el-table-column>
        <el-table-column prop="gatewayCode" :label="$t('gateway.code')" min-width="160" show-overflow-tooltip />
        <el-table-column prop="nodeName" :label="$t('node.name')" min-width="160" show-overflow-tooltip />
        <el-table-column prop="nodeCode" :label="$t('node.nodeCode')" min-width="160" show-overflow-tooltip />
        <el-table-column :label="$t('common.status')" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.online === 1 ? 'success' : 'danger'" size="small" effect="light">
              {{ row.online === 1 ? $t('common.online') : $t('common.offline') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('node.type')" width="110" align="center">
          <template #default="{ row }">{{ typeName(row.nodeType) }}</template>
        </el-table-column>
        <el-table-column :label="$t('gateway.area')" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ areaName(row.areaId) }}</template>
        </el-table-column>
        <el-table-column :label="$t('gateway.lastSeen')" width="170" align="center">
          <template #default="{ row }">{{ formatTime(row.lastReportTime) }}</template>
        </el-table-column>
        <el-table-column :label="$t('common.actions')" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">{{ $t('gateway.viewDetail') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<style scoped>
.node-page { padding: 20px 24px 32px; }
.page-title { margin: 0 0 20px; font-size: 20px; font-weight: 600; color: #303133; }
.filter-bar { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin-bottom: 16px; }
.search-group { display: flex; align-items: center; gap: 4px; }
.field-select { width: 100px; }
.search-input { width: 150px; }
.address-select { width: 150px; }
.type-select { width: 150px; }
.status-select { width: 150px; }
.table-card { background: #fff; border: 1px solid #ebeef5; border-radius: 4px; padding: 4px; }
:deep(.el-table th.el-table__cell) { background: #f5f7fa; color: #606266; font-weight: 600; }
:deep(.el-table .cell) { font-size: 13px; }
</style>
