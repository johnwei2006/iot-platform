<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { ArrowLeft, Refresh } from '@element-plus/icons-vue';
import { getNode, type NodeEntity } from '@/api/iot/node';
import { listSensors, listSensorClassType, type SensorEntity, type SensorClassTypeDict } from '@/api/iot/sensor';
import { listControlDevices, listDeviceClassType, type DeviceCtrlEntity, type DeviceClassTypeDict } from '@/api/iot/device';

const { t } = useI18n();
const router = useRouter();
const route = useRoute();
const node = ref<NodeEntity | null>(null);
const loading = ref(false);
const activeTab = ref('sensor');
const sensors = ref<SensorEntity[]>([]);
const devices = ref<DeviceCtrlEntity[]>([]);
const sensorTypeMap = ref(new Map<string, string>());
const deviceTypeMap = ref(new Map<string, string>());

const NODE_TYPES: Record<number, string> = { 1: 'node.type_1', 2: 'node.type_2', 3: 'node.type_3', 4: 'node.type_4' };

const nodeId = computed(() => String(route.params.id || ''));

const title = computed(() => {
  const n = node.value;
  if (!n) return t('production.nodeDetail');
  return `${n.nodeName || n.nodeCode}（${n.nodeCode}）`;
});

function typeName(type?: number) {
  if (type === undefined || type === null) return '--';
  const label = NODE_TYPES[type];
  return label ? t(label) : String(type);
}

function sensorTypeName(row: SensorEntity) {
  const name = row.sensorClassType ? sensorTypeMap.value.get(row.sensorClassType) : undefined;
  return name || row.sensorClassType || '--';
}

function deviceTypeName(row: DeviceCtrlEntity) {
  const name = row.deviceClassType ? deviceTypeMap.value.get(row.deviceClassType) : undefined;
  return name || row.deviceClassType || '--';
}

function formatTime(value?: string) {
  if (!value) return '--';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function val(value: unknown) {
  return value === undefined || value === null || value === '' ? '--' : String(value);
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

async function load() {
  if (!nodeId.value) return;
  loading.value = true;
  try {
    const res = await getNode(nodeId.value);
    node.value = res.data ?? null;
    const nodeCode = node.value?.nodeCode;
    if (nodeCode) {
      const [sensorRes, deviceRes] = await Promise.all([
        listSensors(nodeCode),
        listControlDevices(nodeCode),
      ]);
      sensors.value = sensorRes.data ?? [];
      devices.value = deviceRes.data ?? [];
    } else {
      sensors.value = [];
      devices.value = [];
    }
  } catch (error) {
    ElMessage.error((error as Error).message || t('production.loadFailed'));
  } finally {
    loading.value = false;
  }
}

function refresh() {
  void load();
}

function goBack() {
  void router.push('/production/nodes');
}

onMounted(() => {
  void loadDicts();
  void load();
});
</script>

<template>
  <div class="prod-page" v-loading="loading">
    <template v-if="nodeId">
      <div class="detail-header">
        <div class="header-left">
          <el-button :icon="ArrowLeft" link @click="goBack" />
          <h2 class="page-title">{{ title }}</h2>
          <el-tag v-if="node?.online === 1" type="success" size="small">{{ $t('gatewayManage.online') }}</el-tag>
          <el-tag v-else-if="node" type="info" size="small">{{ $t('gatewayManage.offline') }}</el-tag>
        </div>
        <el-button type="primary" :icon="Refresh" @click="refresh">{{ $t('production.refresh') }}</el-button>
      </div>

      <div class="table-card" v-if="node">
        <h3 class="section-title">{{ $t('production.nodeInfo') }}</h3>
        <el-descriptions :column="4" border size="small">
          <el-descriptions-item :label="$t('node.type')">{{ typeName(node.nodeType) }}</el-descriptions-item>
          <el-descriptions-item :label="$t('node.gatewayCode')"><span class="mono">{{ val(node.gatewayCode) }}</span></el-descriptions-item>
          <el-descriptions-item :label="$t('gateway.firmware')">{{ val(node.firmwareVersion) }}</el-descriptions-item>
          <el-descriptions-item :label="$t('node.reportInterval')">{{ val(node.reportInterval) }}</el-descriptions-item>
          <el-descriptions-item :label="$t('node.rssi')">{{ val(node.rssi) }}</el-descriptions-item>
          <el-descriptions-item :label="$t('node.snr')">{{ val(node.snr) }}</el-descriptions-item>
          <el-descriptions-item :label="$t('node.speed')">{{ val(node.speed) }}</el-descriptions-item>
          <el-descriptions-item :label="$t('node.batteryVoltage')">{{ val(node.batteryVoltage) }}</el-descriptions-item>
          <el-descriptions-item :label="$t('node.status')">
            <el-tag v-if="node.online === 1" type="success" size="small">{{ $t('gatewayManage.online') }}</el-tag>
            <el-tag v-else type="info" size="small">{{ $t('gatewayManage.offline') }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="$t('production.lastReportTime')">{{ formatTime(node.lastReportTime) }}</el-descriptions-item>
          <el-descriptions-item :label="$t('common.createTime')">{{ formatTime(node.createTime) }}</el-descriptions-item>
          <el-descriptions-item :label="$t('node.installLocation')">{{ val(node.address) }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <div class="table-card" v-if="node">
        <el-tabs v-model="activeTab">
          <el-tab-pane :label="$t('gatewayManage.nodePage.sensorTab')" name="sensor">
            <el-table :data="sensors" border :empty-text="$t('common.empty')">
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
            <el-table :data="devices" border :empty-text="$t('common.empty')">
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
      </div>
    </template>

    <div v-else class="empty-hint">
      <el-empty :description="$t('production.selectFromList')" />
    </div>
  </div>
</template>

<style scoped>
.prod-page { padding: 20px 24px 32px; }
.detail-header { display: flex; align-items: center; justify-content: space-between; gap: 8px; margin-bottom: 16px; }
.header-left { display: flex; align-items: center; gap: 10px; }
.page-title { margin: 0; font-size: 20px; font-weight: 600; color: #303133; }
.table-card {
  background: #fff; border: 1px solid #ebeef5; border-radius: 4px; padding: 16px;
  margin-bottom: 16px;
}
.section-title { margin: 0 0 12px; font-size: 15px; font-weight: 600; color: #1f2937; }
.mono { font-family: monospace; }
.empty-hint { padding-top: 60px; }
</style>
