<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { ArrowLeft, Edit } from '@element-plus/icons-vue';
import { getNode, updateNode, type NodeEntity } from '@/api/iot/node';
import {
  listSensors, listSensorClass, listSensorClassType,
  type SensorClassDict, type SensorClassTypeDict, type SensorEntity,
} from '@/api/iot/sensor';
import {
  listControlDevices, listDeviceClass, listDeviceClassType,
  type DeviceClassDict, type DeviceClassTypeDict, type DeviceCtrlEntity,
} from '@/api/iot/device';
import { fetchOrgList } from '@/api/system/org';
import AreaSelect from '@/components/AreaSelect.vue';
import type { OrgRecord } from '@/types';
import NodeSensorTable from './components/node-sensor-table.vue';
import NodeDeviceTable from './components/node-device-table.vue';

type Field = 'nodeName' | 'longitude' | 'latitude' | 'address';

const route = useRoute();
const router = useRouter();
const id = String(route.params.id);
const node = ref<NodeEntity | null>(null);
const orgs = ref<OrgRecord[]>([]);
const sensors = ref<SensorEntity[]>([]);
const devices = ref<DeviceCtrlEntity[]>([]);
const sensorClasses = ref<SensorClassDict[]>([]);
const sensorClassTypes = ref<SensorClassTypeDict[]>([]);
const deviceClasses = ref<DeviceClassDict[]>([]);
const deviceClassTypes = ref<DeviceClassTypeDict[]>([]);
const loading = ref(false);
const activeTab = ref('sensor');
const activeField = ref<Field | null>(null);
const draftValue = ref('');
const coordDraft = ref({ longitude: '', latitude: '' });
const editInputRef = ref<{ focus: () => void } | null>(null);

const orgName = computed(() => orgs.value.find((o) => o.code === node.value?.orgCode)?.name || '--');

async function loadList<T>(promise: Promise<{ data?: T[] }>, label: string): Promise<T[]> {
  try {
    const res = await promise;
    return res.data ?? [];
  } catch (error) {
    console.warn(`${label}加载失败`, error);
    return [];
  }
}

function startEdit(field: Field) {
  if (!node.value) return;
  activeField.value = field;
  if (field === 'longitude' || field === 'latitude') {
    coordDraft.value = { longitude: node.value.longitude ?? '', latitude: node.value.latitude ?? '' };
  } else {
    draftValue.value = String(node.value[field] ?? '');
  }
  nextTick(() => editInputRef.value?.focus());
}

async function applyPatch(patch: Partial<NodeEntity>) {
  if (!node.value) return;
  try {
    const res = await updateNode(id, patch);
    node.value = res.data ?? node.value;
    activeField.value = null;
    ElMessage.success('已保存');
  } catch (error) {
    ElMessage.error((error as Error).message || '保存失败');
    activeField.value = null;
  }
}

async function saveArea(areaId?: string) {
  try {
    const res = await updateNode(id, { areaId: areaId || '' });
    node.value = res.data ?? node.value;
    ElMessage.success('安装区域已更新');
  } catch (error) {
    ElMessage.error((error as Error).message || '保存失败');
  }
}

async function save(field: Field) {
  if (!node.value || activeField.value !== field) return;
  const value = draftValue.value.trim();
  if (value === (node.value[field] ?? '')) {
    activeField.value = null;
    return;
  }
  await applyPatch({ [field]: value } as Partial<NodeEntity>);
}

async function saveCoords() {
  if (!node.value) return;
  const { longitude, latitude } = coordDraft.value;
  if (longitude === (node.value.longitude ?? '') && latitude === (node.value.latitude ?? '')) {
    activeField.value = null;
    return;
  }
  await applyPatch({ longitude, latitude });
}

async function load() {
  loading.value = true;
  try {
    const detail = await getNode(id);
    node.value = detail.data;
    const nodeCode = detail.data?.nodeCode;
    const [orgList, sensorList, deviceList, sc, sct, dc, dct] = await Promise.all([
      loadList(fetchOrgList().then((data) => ({ data })), '机构'),
      loadList(listSensors(nodeCode), '传感器'),
      loadList(listControlDevices(nodeCode), '控制设备'),
      loadList(listSensorClass(), '传感器大类字典'),
      loadList(listSensorClassType(), '传感器类型字典'),
      loadList(listDeviceClass(), '控制设备大类字典'),
      loadList(listDeviceClassType(), '控制设备类型字典'),
    ]);
    orgs.value = orgList;
    sensors.value = sensorList;
    devices.value = deviceList;
    sensorClasses.value = sc;
    sensorClassTypes.value = sct;
    deviceClasses.value = dc;
    deviceClassTypes.value = dct;
  } catch (error) {
    ElMessage.error((error as Error).message || '节点详情加载失败');
    router.push('/iot/nodes');
  } finally {
    loading.value = false;
  }
}

function showDeviceTab() {
  activeTab.value = 'device';
}

onMounted(load);
</script>

<template>
  <div class="node-detail-page" v-loading="loading">
    <header class="detail-header">
      <div class="title-wrap">
        <el-button link :icon="ArrowLeft" aria-label="返回节点列表" @click="router.push('/iot/nodes')" />
        <h2>{{ node?.nodeName || node?.nodeCode || '--' }}</h2>
        <el-tag :type="node?.online === 1 ? 'success' : 'danger'" size="small" effect="light">
          {{ node?.online === 1 ? '在线' : '离线' }}
        </el-tag>
      </div>
      <div class="header-actions">
        <el-button @click="startEdit('address')">安装位置</el-button>
        <el-button type="primary" @click="showDeviceTab">查看控制设备</el-button>
      </div>
    </header>

    <div v-if="node" class="info-grid">
      <div class="info-cell">
        <span class="info-label">节点名称</span>
        <div class="info-value">
          <el-input v-if="activeField === 'nodeName'" ref="editInputRef" v-model="draftValue" size="small"
                    @blur="save('nodeName')" @keyup.enter="save('nodeName')" />
          <template v-else>
            <span>{{ node.nodeName || '--' }}</span>
            <el-button link :icon="Edit" aria-label="编辑节点名称" @click="startEdit('nodeName')" />
          </template>
        </div>
      </div>
      <div class="info-cell"><span class="info-label">机构名称</span><div class="info-value"><span>{{ orgName }}</span></div></div>
      <div class="info-cell"><span class="info-label">机构编码</span><div class="info-value"><span>{{ node.orgCode || '--' }}</span></div></div>
      <div class="info-cell"><span class="info-label">网关编码</span><div class="info-value"><span>{{ node.gatewayCode || '--' }}</span></div></div>
      <div class="info-cell"><span class="info-label">节点编码</span><div class="info-value"><span>{{ node.nodeCode }}</span></div></div>
      <div class="info-cell">
        <span class="info-label">经纬度</span>
        <div class="info-value coord-value">
          <template v-if="activeField === 'longitude' || activeField === 'latitude'">
            <el-input ref="editInputRef" v-model="coordDraft.longitude" size="small" placeholder="经度"
                      @blur="saveCoords" @keyup.enter="saveCoords" />
            <span>,</span>
            <el-input v-model="coordDraft.latitude" size="small" placeholder="纬度"
                      @blur="saveCoords" @keyup.enter="saveCoords" />
          </template>
          <template v-else>
            <span>{{ node.longitude || node.latitude ? `${node.longitude ?? '--'}, ${node.latitude ?? '--'}` : '--' }}</span>
            <el-button link :icon="Edit" aria-label="编辑经纬度" @click="startEdit('longitude')" />
          </template>
        </div>
      </div>
      <div class="info-cell"><span class="info-label">上报周期</span><div class="info-value"><span>{{ node.reportInterval ?? '--' }}</span></div></div>
      <div class="info-cell"><span class="info-label">信号质量</span><div class="info-value"><span>{{ node.rssi ?? '--' }}</span></div></div>
      <div class="info-cell"><span class="info-label">信噪比</span><div class="info-value"><span>{{ node.snr ?? '--' }}</span></div></div>
      <div class="info-cell"><span class="info-label">速率</span><div class="info-value"><span>{{ node.speed ?? '--' }}</span></div></div>
      <div class="info-cell"><span class="info-label">电池电压</span><div class="info-value"><span>{{ node.batteryVoltage ?? '--' }}</span></div></div>
      <div class="info-cell">
        <span class="info-label">安装区域</span>
        <div class="info-value">
          <AreaSelect
            :model-value="node.areaId"
            placeholder="选择安装区域"
            @change="(area) => saveArea(area?.id)"
          />
        </div>
      </div>
      <div class="info-cell">
        <span class="info-label">详细地址</span>
        <div class="info-value">
          <el-input v-if="activeField === 'address'" ref="editInputRef" v-model="draftValue" size="small"
                    @blur="save('address')" @keyup.enter="save('address')" />
          <template v-else><span>{{ node.address || '--' }}</span></template>
        </div>
      </div>
    </div>

    <h3 class="section-title">基本信息</h3>
    <el-tabs v-model="activeTab" class="detail-tabs">
      <el-tab-pane label="传感器" name="sensor">
        <node-sensor-table :sensors="sensors" :classes="sensorClasses" :class-types="sensorClassTypes" />
      </el-tab-pane>
      <el-tab-pane label="控制设备" name="device">
        <node-device-table :devices="devices" :classes="deviceClasses" :class-types="deviceClassTypes" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.node-detail-page { padding: 20px 24px 32px; }
.detail-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.title-wrap { display: flex; align-items: center; gap: 8px; }
.title-wrap h2 { margin: 0; font-size: 20px; font-weight: 600; color: #303133; }
.header-actions { display: flex; gap: 8px; }
.info-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); border: 1px solid #ebeef5; background: #fff; }
.info-cell { display: flex; flex-direction: column; justify-content: center; gap: 8px; min-height: 68px; padding: 12px 16px; border-right: 1px solid #ebeef5; border-bottom: 1px solid #ebeef5; }
.info-cell:nth-child(3n) { border-right: none; }
.info-label { color: #909399; font-size: 12px; }
.info-value { display: flex; align-items: center; gap: 4px; min-height: 24px; color: #303133; font-size: 14px; }
.info-value > span:first-child { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.coord-value :deep(.el-input) { width: 90px; }
.section-title { margin: 24px 0 8px; font-size: 16px; font-weight: 600; color: #303133; }
.detail-tabs :deep(.el-tabs__header) { margin: 0; }
@media (max-width: 900px) { .info-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } .info-cell:nth-child(3n) { border-right: 1px solid #ebeef5; } .info-cell:nth-child(2n) { border-right: none; } }
</style>
