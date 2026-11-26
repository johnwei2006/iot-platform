<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Search } from '@element-plus/icons-vue';
import { listNodes, type NodeEntity } from '@/api/iot/node';
import {
  listControlDevices, listDeviceClass, listDeviceClassType,
  type DeviceClassDict, type DeviceClassTypeDict, type DeviceCtrlEntity,
} from '@/api/iot/device';
import AreaSelect from '@/components/AreaSelect.vue';
import { listAreas } from '@/api/iot/area';

const router = useRouter();
const loading = ref(false);
const devices = ref<DeviceCtrlEntity[]>([]);
const nodes = ref<NodeEntity[]>([]);
const classes = ref<DeviceClassDict[]>([]);
const classTypes = ref<DeviceClassTypeDict[]>([]);
const codeField = ref('deviceCode');
const nameField = ref('deviceName');
const codeKey = ref('');
const nameKey = ref('');
const areaFilter = ref('');
const moreVisible = ref(false);
const classFilter = ref('');
const typeFilter = ref('');
const connectFilter = ref<number | ''>('');
const onlineFilter = ref('');

const SWITCH_MODES: Record<number, string> = { 1: '开度', 2: '二开', 3: '三开' };
const nodeByCode = computed(() => new Map(nodes.value.map((n) => [n.nodeCode, n])));
const areaNameMap = ref(new Map<string, string>());

function areaOf(row: DeviceCtrlEntity) {
  return (row.areaId && areaNameMap.value.get(row.areaId)) || nodeOf(row.nodeCode)?.address || row.address || '--';
}

function nodeOf(code?: string) {
  return code ? nodeByCode.value.get(code) : undefined;
}

function typeName(row: DeviceCtrlEntity) {
  const type = classTypes.value.find((t) => t.type === row.deviceClassType)?.typeName;
  if (type) return type;
  const cls = classes.value.find((c) => c.deviceClass === row.deviceClass)?.className;
  return cls || row.deviceClassType || row.deviceClass || '--';
}

function switchMode(row: DeviceCtrlEntity) {
  const mode = classes.value.find((c) => c.deviceClass === row.deviceClass)?.switchMode;
  return mode ? (SWITCH_MODES[mode] ?? String(mode)) : '--';
}

const filtered = computed(() => devices.value.filter((d) => {
  const code = codeKey.value.trim().toLowerCase();
  const name = nameKey.value.trim().toLowerCase();
  return (!code || (d.deviceCode ?? '').toLowerCase().includes(code))
    && (!name || (d.deviceName ?? '').toLowerCase().includes(name))
    && (!areaFilter.value || d.areaId === areaFilter.value)
    && (!classFilter.value || d.deviceClass === classFilter.value)
    && (!typeFilter.value || d.deviceClassType === typeFilter.value)
    && (connectFilter.value === '' || d.connectType === connectFilter.value)
    && (!onlineFilter.value || (d.online === 1 ? 'ON' : 'OFF') === onlineFilter.value);
}));

function reset() {
  codeKey.value = '';
  nameKey.value = '';
  areaFilter.value = '';
  classFilter.value = '';
  typeFilter.value = '';
  connectFilter.value = '';
  onlineFilter.value = '';
}

async function load() {
  loading.value = true;
  try {
    const [deviceRes, nodeRes, classRes, typeRes, areaRes] = await Promise.all([
      listControlDevices(), listNodes(), listDeviceClass(), listDeviceClassType(), listAreas(),
    ]);
    devices.value = deviceRes.data ?? [];
    nodes.value = nodeRes.data ?? [];
    classes.value = classRes.data ?? [];
    classTypes.value = typeRes.data ?? [];
    areaNameMap.value = new Map((areaRes.data ?? []).map((a) => [a.id, a.areaName]));
  } catch (error) {
    ElMessage.error((error as Error).message || '控制设备列表加载失败');
  } finally {
    loading.value = false;
  }
}

function openDetail(row: DeviceCtrlEntity) {
  router.push(`/iot/control-devices/${row.id}`);
}

onMounted(load);
</script>

<template>
  <div class="ctrl-page">
    <h2 class="page-title">控制设备管理</h2>

    <div class="filter-bar">
      <div class="search-group">
        <el-select v-model="codeField" class="field-select" aria-label="编码搜索字段">
          <el-option label="设备编码" value="deviceCode" />
        </el-select>
        <el-input v-model="codeKey" class="search-input" clearable placeholder="请输入内容">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </div>
      <div class="search-group">
        <el-select v-model="nameField" class="field-select" aria-label="名称搜索字段">
          <el-option label="设备名称" value="deviceName" />
        </el-select>
        <el-input v-model="nameKey" class="search-input" clearable placeholder="请输入内容">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </div>
      <AreaSelect v-model="areaFilter" class="area-select" placeholder="按关联区域筛选" />
      <el-button type="primary" @click="load">搜索</el-button>
      <el-button @click="reset">重置</el-button>
      <el-button link type="primary" @click="moreVisible = !moreVisible">
        {{ moreVisible ? '收起筛选' : '更多筛选' }}
      </el-button>
    </div>

    <div v-if="moreVisible" class="more-filter">
      <el-select v-model="classFilter" clearable placeholder="设备大类" aria-label="设备大类">
        <el-option v-for="c in classes" :key="c.deviceClass" :label="c.className" :value="c.deviceClass" />
      </el-select>
      <el-select v-model="typeFilter" clearable placeholder="设备类型" aria-label="设备类型">
        <el-option v-for="t in classTypes" :key="t.type" :label="t.typeName" :value="t.type" />
      </el-select>
      <el-select v-model="connectFilter" clearable placeholder="接入方式" aria-label="接入方式">
        <el-option label="通道直连" :value="0" />
        <el-option label="485 透传" :value="1" />
      </el-select>
      <el-select v-model="onlineFilter" clearable placeholder="设备状态" aria-label="设备状态">
        <el-option label="在线" value="ON" />
        <el-option label="离线" value="OFF" />
      </el-select>
    </div>

    <div class="table-card" v-loading="loading">
      <el-table :data="filtered" border style="width: 100%" empty-text="暂无数据">
        <el-table-column label="序号" type="index" width="60" align="center" />
        <el-table-column label="网关编码" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ nodeOf(row.nodeCode)?.gatewayCode || '--' }}</template>
        </el-table-column>
        <el-table-column prop="nodeCode" label="节点编码" min-width="160" show-overflow-tooltip />
        <el-table-column prop="deviceCode" label="设备编码" min-width="160" show-overflow-tooltip />
        <el-table-column label="设备名称" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.deviceName || '--' }}</template>
        </el-table-column>
        <el-table-column label="设备类型" min-width="130">
          <template #default="{ row }">{{ typeName(row) }}</template>
        </el-table-column>
        <el-table-column label="开关模式" width="100" align="center">
          <template #default="{ row }">{{ switchMode(row) }}</template>
        </el-table-column>
        <el-table-column label="设备状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.online === 1 ? 'success' : 'danger'" size="small" effect="light">
              {{ row.online === 1 ? '在线' : '离线' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="关联区域" min-width="130">
          <template #default="{ row }">{{ areaOf(row) }}</template>
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
.ctrl-page { padding: 20px 24px 32px; }
.page-title { margin: 0 0 20px; font-size: 20px; font-weight: 600; color: #303133; }
.filter-bar { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin-bottom: 12px; }
.search-group { display: flex; align-items: center; gap: 4px; }
.field-select { width: 100px; }
.search-input { width: 155px; }
.area-select { width: 180px; }
.more-filter { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 12px; padding: 12px; background: #fafafa; border: 1px solid #ebeef5; border-radius: 4px; }
.more-filter > * { width: 170px; }
.table-card { background: #fff; border: 1px solid #ebeef5; border-radius: 4px; padding: 4px; }
:deep(.el-table th.el-table__cell) { background: #f5f7fa; color: #606266; font-weight: 600; }
:deep(.el-table .cell) { font-size: 13px; }
</style>
