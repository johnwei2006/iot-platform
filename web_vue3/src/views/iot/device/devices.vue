<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { ElMessage } from 'element-plus';
import { Search } from '@element-plus/icons-vue';
import { listNodes, type NodeEntity } from '@/api/iot/node';
import {
  listControlDevices, listDeviceClass, listDeviceClassType,
  type DeviceClassDict, type DeviceClassTypeDict, type DeviceCtrlEntity,
} from '@/api/iot/device';
import AreaSelect from '@/components/AreaSelect.vue';
import { listAreas } from '@/api/iot/area';

const { t } = useI18n();
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

const SWITCH_KEYS: Record<number, string> = { 1: 'device.switchMode_1', 2: 'device.switchMode_4', 3: 'device.switchMode_5' };
const nodeByCode = computed(() => new Map(nodes.value.map((n) => [n.nodeCode, n])));
const areaNameMap = ref(new Map<string, string>());

function areaOf(row: DeviceCtrlEntity) {
  return (row.areaId && areaNameMap.value.get(row.areaId)) || nodeOf(row.nodeCode)?.address || row.address || '--';
}

function nodeOf(code?: string) {
  return code ? nodeByCode.value.get(code) : undefined;
}

function typeName(row: DeviceCtrlEntity) {
  const type = classTypes.value.find((c) => c.type === row.deviceClassType)?.typeName;
  if (type) return type;
  const cls = classes.value.find((c) => c.deviceClass === row.deviceClass)?.className;
  return cls || row.deviceClassType || row.deviceClass || '--';
}

function switchMode(row: DeviceCtrlEntity) {
  const mode = classes.value.find((c) => c.deviceClass === row.deviceClass)?.switchMode;
  if (!mode) return '--';
  const key = SWITCH_KEYS[mode];
  return key ? t(key) : String(mode);
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
    ElMessage.error((error as Error).message || t('device.loadFailed'));
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
    <h2 class="page-title">{{ $t('device.titleManage') }}</h2>

    <div class="filter-bar">
      <div class="search-group">
        <el-select v-model="codeField" class="field-select" :aria-label="$t('device.codeField')">
          <el-option :label="$t('device.code')" value="deviceCode" />
        </el-select>
        <el-input v-model="codeKey" class="search-input" clearable :placeholder="$t('device.inputPlaceholder')">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </div>
      <div class="search-group">
        <el-select v-model="nameField" class="field-select" :aria-label="$t('device.nameField')">
          <el-option :label="$t('device.name')" value="deviceName" />
        </el-select>
        <el-input v-model="nameKey" class="search-input" clearable :placeholder="$t('device.inputPlaceholder')">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </div>
      <AreaSelect v-model="areaFilter" class="area-select" :placeholder="$t('sensor.areaFilter')" />
      <el-button type="primary" @click="load">{{ $t('common.search') }}</el-button>
      <el-button @click="reset">{{ $t('common.reset') }}</el-button>
      <el-button link type="primary" @click="moreVisible = !moreVisible">
        {{ moreVisible ? $t('sensor.collapseFilters') : $t('sensor.moreFilters') }}
      </el-button>
    </div>

    <div v-if="moreVisible" class="more-filter">
      <el-select v-model="classFilter" clearable :placeholder="$t('device.class')" :aria-label="$t('device.class')">
        <el-option v-for="c in classes" :key="c.deviceClass" :label="c.className" :value="c.deviceClass" />
      </el-select>
      <el-select v-model="typeFilter" clearable :placeholder="$t('device.type')" :aria-label="$t('device.type')">
        <el-option v-for="c in classTypes" :key="c.type" :label="c.typeName" :value="c.type" />
      </el-select>
      <el-select v-model="connectFilter" clearable :placeholder="$t('device.connectType')" :aria-label="$t('device.connectType')">
        <el-option :label="$t('device.connectDirect')" :value="0" />
        <el-option :label="$t('device.connect485')" :value="1" />
      </el-select>
      <el-select v-model="onlineFilter" clearable :placeholder="$t('common.status')" :aria-label="$t('common.status')">
        <el-option :label="$t('common.online')" value="ON" />
        <el-option :label="$t('common.offline')" value="OFF" />
      </el-select>
    </div>

    <div class="table-card" v-loading="loading">
      <el-table :data="filtered" border style="width: 100%" :empty-text="$t('common.empty')">
        <el-table-column :label="$t('common.page')" type="index" width="60" align="center" />
        <el-table-column :label="$t('gateway.code')" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ nodeOf(row.nodeCode)?.gatewayCode || '--' }}</template>
        </el-table-column>
        <el-table-column prop="nodeCode" :label="$t('node.nodeCode')" min-width="160" show-overflow-tooltip />
        <el-table-column prop="deviceCode" :label="$t('device.code')" min-width="160" show-overflow-tooltip />
        <el-table-column :label="$t('device.name')" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.deviceName || '--' }}</template>
        </el-table-column>
        <el-table-column :label="$t('device.type')" min-width="130">
          <template #default="{ row }">{{ typeName(row) }}</template>
        </el-table-column>
        <el-table-column :label="$t('device.switchMode')" width="100" align="center">
          <template #default="{ row }">{{ switchMode(row) }}</template>
        </el-table-column>
        <el-table-column :label="$t('common.status')" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.online === 1 ? 'success' : 'danger'" size="small" effect="light">
              {{ row.online === 1 ? $t('common.online') : $t('common.offline') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('sensor.linkedArea')" min-width="130">
          <template #default="{ row }">{{ areaOf(row) }}</template>
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
