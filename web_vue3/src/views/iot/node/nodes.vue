<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Search } from '@element-plus/icons-vue';
import { listNodes, type NodeEntity } from '@/api/iot/node';
import { listDevices } from '@/api/iot/gateway';
import { listAreas } from '@/api/iot/area';
import AreaSelect from '@/components/AreaSelect.vue';

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

const NODE_TYPES: Record<number, string> = { 1: 'ACT', 2: 'IRRI', 3: 'DATA', 4: '植保机', 5: '开度' };

function typeName(type?: number) {
  if (type === undefined || type === null) return '--';
  return NODE_TYPES[type] ?? String(type);
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
    ElMessage.error((error as Error).message || '节点列表加载失败');
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
    <h2 class="page-title">节点管理</h2>

    <div class="filter-bar">
      <div class="search-group">
        <el-select v-model="gatewayField" class="field-select" aria-label="网关搜索字段">
          <el-option label="网关编号" value="gatewayCode" />
        </el-select>
        <el-input v-model="gatewayKey" class="search-input" clearable placeholder="请输入内容">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </div>
      <div class="search-group">
        <el-select v-model="nodeField" class="field-select" aria-label="节点搜索字段">
          <el-option label="节点编号" value="nodeCode" />
        </el-select>
        <el-input v-model="nodeKey" class="search-input" clearable placeholder="请输入内容">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </div>
      <AreaSelect v-model="areaFilter" class="area-select" placeholder="按安装区域筛选" />
      <el-select v-model="typeFilter" class="type-select" clearable placeholder="请选择节点类型" aria-label="节点类型">
        <el-option v-for="(label, value) in NODE_TYPES" :key="value" :label="label" :value="Number(value)" />
      </el-select>
      <el-select v-model="onlineFilter" class="status-select" clearable placeholder="请选择节点状态" aria-label="节点状态">
        <el-option label="在线" value="ONLINE" />
        <el-option label="离线" value="OFFLINE" />
      </el-select>
      <el-button type="primary" @click="load">搜索</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <div class="table-card" v-loading="loading">
      <el-table :data="filtered" border style="width: 100%" empty-text="暂无数据">
        <el-table-column label="序号" type="index" width="60" align="center" />
        <el-table-column label="网关名称" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ gatewayName(row.gatewayCode) }}</template>
        </el-table-column>
        <el-table-column prop="gatewayCode" label="网关编码" min-width="160" show-overflow-tooltip />
        <el-table-column prop="nodeName" label="节点名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="nodeCode" label="节点编码" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.online === 1 ? 'success' : 'danger'" size="small" effect="light">
              {{ row.online === 1 ? '在线' : '离线' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="节点类型" width="110" align="center">
          <template #default="{ row }">{{ typeName(row.nodeType) }}</template>
        </el-table-column>
        <el-table-column label="安装区域" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ areaName(row.areaId) }}</template>
        </el-table-column>
        <el-table-column label="最后上报时间" width="170" align="center">
          <template #default="{ row }">{{ formatTime(row.lastReportTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看详情</el-button>
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
