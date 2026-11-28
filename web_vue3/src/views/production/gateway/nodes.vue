<script setup lang="ts">
import { nextTick, onMounted, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { ElMessage } from 'element-plus';
import GatewayConsole from './components/GatewayConsole.vue';
import { listGatewayNodes, type GatewayNodeItem } from '@/api/iot/gatewayManage';
import { listSensors, listSensorClassType, type SensorEntity, type SensorClassTypeDict } from '@/api/iot/sensor';
import { listControlDevices, listDeviceClassType, type DeviceCtrlEntity, type DeviceClassTypeDict } from '@/api/iot/device';

const { t } = useI18n();
const gatewayCode = ref('');
const nodes = ref<GatewayNodeItem[]>([]);
const selectedNodeCode = ref('');
const selectedNodeName = ref('');
const nodeTableRef = ref<{ setCurrentRow?: (row?: GatewayNodeItem) => void } | null>(null);
const nodesLoading = ref(false);
const detailLoading = ref(false);
const activeTab = ref('sensor');
const sensors = ref<SensorEntity[]>([]);
const devices = ref<DeviceCtrlEntity[]>([]);
const sensorTypeMap = ref(new Map<string, string>());
const deviceTypeMap = ref(new Map<string, string>());

const NODE_TYPES: Record<number, string> = { 1: 'node.type_1', 2: 'node.type_2', 3: 'node.type_3', 4: 'node.type_4' };

function typeName(type?: number) {
  if (type === undefined || type === null) return '--';
  const label = NODE_TYPES[type];
  return label ? t(label) : String(type);
}

function formatTime(value?: string) {
  if (!value) return '--';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function sensorTypeName(row: SensorEntity) {
  const name = row.sensorClassType ? sensorTypeMap.value.get(row.sensorClassType) : undefined;
  return name || row.sensorClassType || '--';
}

function deviceTypeName(row: DeviceCtrlEntity) {
  const name = row.deviceClassType ? deviceTypeMap.value.get(row.deviceClassType) : undefined;
  return name || row.deviceClassType || '--';
}

async function loadDicts() {
  try {
    const [sensorRes, deviceRes] = await Promise.all([listSensorClassType(), listDeviceClassType()]);
    sensorTypeMap.value = new Map((sensorRes.data ?? []).map((d: SensorClassTypeDict) => [d.classType, d.classTypeName || d.classType]));
    deviceTypeMap.value = new Map((deviceRes.data ?? []).map((d: DeviceClassTypeDict) => [d.type, d.typeName || d.type]));
  } catch {
    // 字典加载失败时类型列回退显示编码
  }
}

async function loadNodes() {
  if (!gatewayCode.value) {
    nodes.value = [];
    selectedNodeCode.value = '';
    selectedNodeName.value = '';
    sensors.value = [];
    devices.value = [];
    return;
  }
  nodesLoading.value = true;
  try {
    const res = await listGatewayNodes(gatewayCode.value);
    nodes.value = res.data ?? [];
    if (nodes.value.length > 0) {
      await selectNode(nodes.value[0].nodeCode);
      await nextTick();
      nodeTableRef.value?.setCurrentRow?.(nodes.value[0]);
    } else {
      selectedNodeCode.value = '';
      selectedNodeName.value = '';
      sensors.value = [];
      devices.value = [];
    }
  } catch (error) {
    ElMessage.error((error as Error).message || t('gatewayManage.nodePage.loadFailed'));
  } finally {
    nodesLoading.value = false;
  }
}

async function selectNode(nodeCode: string) {
  selectedNodeCode.value = nodeCode;
  selectedNodeName.value = nodes.value.find((n) => n.nodeCode === nodeCode)?.nodeName || '';
  detailLoading.value = true;
  try {
    const [sensorRes, deviceRes] = await Promise.all([
      listSensors(nodeCode),
      listControlDevices(nodeCode),
    ]);
    sensors.value = sensorRes.data ?? [];
    devices.value = deviceRes.data ?? [];
  } catch (error) {
    sensors.value = [];
    devices.value = [];
    ElMessage.error((error as Error).message || t('gatewayManage.nodePage.loadFailed'));
  } finally {
    detailLoading.value = false;
  }
}

function onGatewayChange() {
  loadNodes();
}

watch(gatewayCode, onGatewayChange);

onMounted(loadDicts);
</script>

<template>
  <GatewayConsole v-model:gateway-code="gatewayCode" :title="$t('gatewayManage.nodePage.title')">
    <div class="nodes-split">
      <div class="panel node-panel">
        <h3 class="panel-title">{{ $t('gatewayManage.nodePage.nodeList') }}</h3>
        <el-table
          ref="nodeTableRef"
          :data="nodes"
          v-loading="nodesLoading"
          border
          height="100%"
          highlight-current-row
          :empty-text="$t('gatewayManage.noData')"
          @current-change="(row: GatewayNodeItem | undefined) => row && row.nodeCode !== selectedNodeCode && selectNode(row.nodeCode)"
        >
          <el-table-column :label="$t('common.page')" type="index" width="56" align="center" />
          <el-table-column prop="nodeCode" :label="$t('node.nodeCode')" min-width="160">
            <template #default="{ row }"><span class="mono">{{ row.nodeCode }}</span></template>
          </el-table-column>
          <el-table-column prop="nodeName" :label="$t('node.name')" min-width="110" show-overflow-tooltip>
            <template #default="{ row }">{{ row.nodeName || '--' }}</template>
          </el-table-column>
          <el-table-column :label="$t('node.type')" width="90" align="center">
            <template #default="{ row }">{{ typeName(row.nodeType) }}</template>
          </el-table-column>
          <el-table-column :label="$t('gatewayManage.nodePage.joinTime')" width="140" align="center">
            <template #default="{ row }">{{ formatTime(row.joinTime) }}</template>
          </el-table-column>
          <el-table-column :label="$t('gatewayManage.nodePage.reportFrequency')" width="90" align="center">
            <template #default="{ row }">{{ row.reportInterval ?? '--' }}</template>
          </el-table-column>
          <el-table-column :label="$t('gatewayManage.nodePage.sensorCount')" width="90" align="center">
            <template #default="{ row }">{{ row.sensorCount }}</template>
          </el-table-column>
          <el-table-column :label="$t('gatewayManage.nodePage.deviceCount')" width="90" align="center">
            <template #default="{ row }">{{ row.deviceCount }}</template>
          </el-table-column>
        </el-table>
      </div>

      <div class="panel detail-panel">
        <div v-if="selectedNodeCode" class="node-bar">
          <span class="mono node-code">{{ selectedNodeCode }}</span>
          <span class="node-name">{{ selectedNodeName || '--' }}</span>
        </div>
        <el-tabs v-if="selectedNodeCode" v-model="activeTab">
          <el-tab-pane :label="$t('gatewayManage.nodePage.sensorTab')" name="sensor">
            <el-table :data="sensors" v-loading="detailLoading" border height="100%" :empty-text="$t('gatewayManage.noData')">
              <el-table-column prop="channel" :label="$t('gatewayManage.nodePage.channel')" width="90" align="center">
                <template #default="{ row }">{{ row.channel ?? '--' }}</template>
              </el-table-column>
              <el-table-column prop="sensorCode" :label="$t('gatewayManage.nodePage.sensorCode')" min-width="170">
                <template #default="{ row }"><span class="mono">{{ row.sensorCode }}</span></template>
              </el-table-column>
              <el-table-column :label="$t('gatewayManage.nodePage.type')" min-width="130" show-overflow-tooltip>
                <template #default="{ row }">{{ sensorTypeName(row) }}</template>
              </el-table-column>
              <el-table-column prop="commandRead" :label="$t('gatewayManage.nodePage.readCommand')" min-width="150">
                <template #default="{ row }"><span class="mono">{{ row.commandRead || '--' }}</span></template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane :label="$t('gatewayManage.nodePage.deviceTab')" name="device">
            <el-table :data="devices" v-loading="detailLoading" border height="100%" :empty-text="$t('gatewayManage.noData')">
              <el-table-column prop="channel" :label="$t('gatewayManage.nodePage.channel')" width="90" align="center">
                <template #default="{ row }">{{ row.channel ?? '--' }}</template>
              </el-table-column>
              <el-table-column prop="deviceCode" :label="$t('gatewayManage.nodePage.deviceCode')" min-width="170">
                <template #default="{ row }"><span class="mono">{{ row.deviceCode }}</span></template>
              </el-table-column>
              <el-table-column :label="$t('gatewayManage.nodePage.type')" min-width="120" show-overflow-tooltip>
                <template #default="{ row }">{{ deviceTypeName(row) }}</template>
              </el-table-column>
              <el-table-column prop="commandOn" :label="$t('gatewayManage.nodePage.onCommand')" min-width="120">
                <template #default="{ row }"><span class="mono">{{ row.commandOn || '--' }}</span></template>
              </el-table-column>
              <el-table-column prop="commandOff" :label="$t('gatewayManage.nodePage.offCommand')" min-width="120">
                <template #default="{ row }"><span class="mono">{{ row.commandOff || '--' }}</span></template>
              </el-table-column>
              <el-table-column prop="commandSet" :label="$t('gatewayManage.nodePage.setCommand')" min-width="120">
                <template #default="{ row }"><span class="mono">{{ row.commandSet || '--' }}</span></template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
        <div v-else class="empty-hint">
          <span>{{ $t('gatewayManage.nodePage.selectNodeHint') }}</span>
        </div>
      </div>
    </div>
  </GatewayConsole>
</template>

<style scoped>
.nodes-split {
  display: grid;
  grid-template-columns: minmax(0, 3fr) minmax(0, 2fr);
  gap: 16px;
  height: calc(100vh - 260px);
  min-height: 320px;
}
.panel {
  border: 1px solid #e4eaf2;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 2px 10px rgba(31, 41, 55, 0.04);
  padding: 16px;
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.panel-title { margin: 0 0 14px; font-size: 15px; font-weight: 600; color: #1f2937; }
.node-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  padding: 8px 12px;
  border-radius: 8px;
  background: #f0f6ff;
  border: 1px solid #d9e3f0;
}
.node-code { font-size: 14px; font-weight: 600; color: #1d4ed8; }
.node-name { font-size: 13px; color: #374151; }
.panel :deep(.el-table) { flex: 1; }
.detail-panel :deep(.el-tabs) { flex: 1; display: flex; flex-direction: column; min-height: 0; }
.detail-panel :deep(.el-tabs__content) { flex: 1; min-height: 0; }
.detail-panel :deep(.el-tab-pane) { height: 100%; display: flex; flex-direction: column; }
.detail-panel :deep(.el-tab-pane .el-table) { flex: 1; }
.mono { font-family: monospace; }
.empty-hint {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 14px;
}
@media (max-width: 1100px) {
  .nodes-split { grid-template-columns: minmax(0, 1fr); height: auto; }
  .panel :deep(.el-table) { max-height: 400px; }
}
</style>
