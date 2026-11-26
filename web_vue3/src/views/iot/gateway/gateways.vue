<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Search } from '@element-plus/icons-vue';
import { listDevices } from '@/api/iot/gateway';
import { listAreas } from '@/api/iot/area';
import AreaSelect from '@/components/AreaSelect.vue';
import GatewayRecords from '@/views/gateway/records.vue';
import type { Device } from '@/types';

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

const TYPE_NAMES: Record<number, string> = { 1: '4G网关', 2: '5G网关' };

function areaName(id?: string) {
  return (id && areaNameMap.value.get(id)) || '--';
}

function typeName(type?: number) {
  if (type === undefined || type === null) return '--';
  return TYPE_NAMES[type] ?? String(type);
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
    ElMessage.error((error as Error).message || '网关列表加载失败');
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
    <h2 class="page-title">网关管理</h2>

    <div class="filter-bar">
      <el-select v-model="nameField" class="field-select" aria-label="名称搜索字段">
        <el-option label="网关名称" value="name" />
        <el-option label="网关编码" value="deviceId" />
      </el-select>
      <el-input v-model="keyword" class="keyword-input" clearable placeholder="请输入内容">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="onlineFilter" class="status-select" clearable placeholder="是否在线" aria-label="是否在线">
        <el-option label="在线" value="ONLINE" />
        <el-option label="离线" value="OFFLINE" />
      </el-select>
      <el-select v-model="typeFilter" class="type-select" clearable placeholder="请选择网关类型" aria-label="网关类型">
        <el-option label="4G网关" :value="1" />
        <el-option label="5G网关" :value="2" />
      </el-select>
      <AreaSelect v-model="areaFilter" class="area-select" placeholder="按安装区域筛选" />
      <el-button type="primary" @click="load">搜索</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <div class="table-card" v-loading="loading">
      <el-table :data="filtered" border style="width: 100%" empty-text="暂无网关数据">
        <el-table-column label="序号" type="index" width="64" align="center" />
        <el-table-column prop="name" label="网关名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="deviceId" label="网关编码" min-width="190" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ONLINE' ? 'success' : 'danger'" size="small" effect="light">
              {{ row.status === 'ONLINE' ? '在线' : '离线' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="网关类型" width="120" align="center">
          <template #default="{ row }">{{ typeName(row.gatewayType) }}</template>
        </el-table-column>
        <el-table-column label="安装区域" min-width="170" show-overflow-tooltip>
          <template #default="{ row }">{{ areaName(row.areaId) }}</template>
        </el-table-column>
        <el-table-column label="最后上报时间" width="180" align="center">
          <template #default="{ row }">{{ formatTime(row.lastSeen) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="170" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看详情</el-button>
            <el-button link type="primary" @click="openRecords(row)">认证记录</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog
      v-model="recordsVisible"
      :title="`认证记录 - ${recordsGateway?.name || recordsGateway?.deviceId || ''}`"
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
