<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Search } from '@element-plus/icons-vue';
import type { NodeEntity } from '@/api/iot/node';
import {
  createProductionControlDevice,
  createProductionSensor,
  deleteProductionControlDevice,
  deleteProductionSensor,
  listProductionControlDevices,
  listProductionNodes,
  listProductionSensors,
} from '@/api/iot/production';
import {
  listSensorClass,
  listSensorClassType,
  listSensors,
  type SensorClassDict,
  type SensorClassTypeDict,
  type SensorEntity,
} from '@/api/iot/sensor';
import {
  listControlDevices,
  listDeviceClass,
  listDeviceClassType,
  type DeviceClassDict,
  type DeviceClassTypeDict,
  type DeviceCtrlEntity,
} from '@/api/iot/device';

const { t } = useI18n();
const route = useRoute();

const loading = ref(false);
const nodes = ref<NodeEntity[]>([]);
const typeFilter = ref<number | ''>('');
const codeFilter = ref('');
const nameFilter = ref('');
const sensorCountMap = ref(new Map<string, number>());
const deviceCountMap = ref(new Map<string, number>());

const selected = ref<NodeEntity | null>(null);
const activeTab = ref('sensor');

/* 字典 */
const sensorClasses = ref<SensorClassDict[]>([]);
const sensorClassTypes = ref<SensorClassTypeDict[]>([]);
const deviceClasses = ref<DeviceClassDict[]>([]);
const deviceClassTypes = ref<DeviceClassTypeDict[]>([]);
const sensorTypeNameMap = computed(() => new Map(sensorClassTypes.value.map((d) => [d.classType, d.classTypeName || d.classType])));
const deviceTypeNameMap = computed(() => new Map(deviceClassTypes.value.map((d) => [d.type, d.typeName || d.type])));
const sensorClassNameMap = computed(() => new Map(sensorClasses.value.map((d) => [d.sensorClass, d.className || d.sensorClass])));
const deviceClassNameMap = computed(() => new Map(deviceClasses.value.map((d) => [d.deviceClass, d.className || d.deviceClass])));

/* 当前节点已分配 */
const detailLoading = ref(false);
const sensors = ref<SensorEntity[]>([]);
const devices = ref<DeviceCtrlEntity[]>([]);

/* 分配弹窗 */
const assignDialog = reactive<{
  visible: boolean;
  kind: 'SENSOR' | 'CTRL';
  classType: string;
  classTypeName: string;
  parentClass: string;
  channel: string;
  name: string;
}>({ visible: false, kind: 'SENSOR', classType: '', classTypeName: '', parentClass: '', channel: '', name: '' });
const assigning = ref(false);

const NODE_TYPES: Record<number, string> = { 1: 'node.type_1', 2: 'node.type_2', 3: 'node.type_3', 4: 'node.type_4' };

function typeName(type?: number) {
  if (type === undefined || type === null) return '--';
  const label = NODE_TYPES[type];
  return label ? t(label) : String(type);
}

const filtered = computed(() => nodes.value.filter((n) => {
  const code = codeFilter.value.trim().toLowerCase();
  const name = nameFilter.value.trim().toLowerCase();
  return (!code || (n.nodeCode || '').toLowerCase().includes(code))
    && (!name || (n.nodeName || '').toLowerCase().includes(name))
    && (typeFilter.value === '' || n.nodeType === typeFilter.value);
}));

function resetFilter() {
  typeFilter.value = '';
  codeFilter.value = '';
  nameFilter.value = '';
}

async function load() {
  loading.value = true;
  try {
    const [nodeRes, sensorRes, deviceRes] = await Promise.all([
      listProductionNodes(),
      listProductionSensors(),
      listProductionControlDevices(),
    ]);
    nodes.value = nodeRes.data ?? [];
    const sCount = new Map<string, number>();
    for (const s of sensorRes.data ?? []) {
      if (s.nodeCode) sCount.set(s.nodeCode, (sCount.get(s.nodeCode) || 0) + 1);
    }
    const dCount = new Map<string, number>();
    for (const d of deviceRes.data ?? []) {
      if (d.nodeCode) dCount.set(d.nodeCode, (dCount.get(d.nodeCode) || 0) + 1);
    }
    sensorCountMap.value = sCount;
    deviceCountMap.value = dCount;
    // 列表刷新后保留选中行（按 nodeCode 重新定位）
    if (selected.value) {
      const again = nodes.value.find((n) => n.nodeCode === selected.value?.nodeCode);
      if (again) selectNode(again);
      else clearSelected();
    }
  } catch (error) {
    ElMessage.error((error as Error).message || t('production.loadFailed'));
  } finally {
    loading.value = false;
  }
}

function clearSelected() {
  selected.value = null;
  sensors.value = [];
  devices.value = [];
}

async function selectNode(row: NodeEntity | null) {
  if (!row) return; // 表格刷新时 current-change 可能传 null，保留现有选中
  selected.value = row;
  await loadAssignments();
}

async function loadAssignments() {
  const node = selected.value;
  if (!node?.nodeCode) return;
  detailLoading.value = true;
  try {
    const [sensorRes, deviceRes] = await Promise.all([
      listSensors(node.nodeCode),
      listControlDevices(node.nodeCode),
    ]);
    sensors.value = sensorRes.data ?? [];
    devices.value = deviceRes.data ?? [];
  } catch (error) {
    ElMessage.error((error as Error).message || t('production.loadFailed'));
  } finally {
    detailLoading.value = false;
  }
}

async function loadDicts() {
  try {
    const [sc, sct, dc, dct] = await Promise.all([
      listSensorClass(),
      listSensorClassType(),
      listDeviceClass(),
      listDeviceClassType(),
    ]);
    sensorClasses.value = sc.data ?? [];
    sensorClassTypes.value = sct.data ?? [];
    deviceClasses.value = dc.data ?? [];
    deviceClassTypes.value = dct.data ?? [];
  } catch {
    // 字典加载失败时类型列回退显示编码
  }
}

/** 下一空闲通道号：当前页签已分配最大通道 +1，2 位补零。 */
function nextChannel(kind: 'SENSOR' | 'CTRL') {
  const list = kind === 'SENSOR' ? sensors.value : devices.value;
  let max = 0;
  for (const item of list) {
    const ch = Number(item.channel);
    if (!Number.isNaN(ch) && ch > max) max = ch;
  }
  return String(max + 1).padStart(2, '0');
}

function sensorClassOf(classType: string) {
  return sensorClassTypes.value.find((d) => d.classType === classType)?.sensorClass || classType.slice(0, 2);
}

function deviceClassOf(type: string) {
  return deviceClassTypes.value.find((d) => d.type === type)?.deviceClass || type.slice(0, 2);
}

function openAssignSensor(row: SensorClassTypeDict) {
  assignDialog.kind = 'SENSOR';
  assignDialog.classType = row.classType;
  assignDialog.classTypeName = row.classTypeName || row.classType;
  assignDialog.parentClass = row.sensorClass || sensorClassOf(row.classType);
  assignDialog.channel = nextChannel('SENSOR');
  assignDialog.name = `${assignDialog.classTypeName} ${assignDialog.channel}`;
  assignDialog.visible = true;
}

function openAssignDevice(row: DeviceClassTypeDict) {
  assignDialog.kind = 'CTRL';
  assignDialog.classType = row.type;
  assignDialog.classTypeName = row.typeName || row.type;
  assignDialog.parentClass = row.deviceClass || deviceClassOf(row.type);
  assignDialog.channel = nextChannel('CTRL');
  assignDialog.name = `${assignDialog.classTypeName} ${assignDialog.channel}`;
  assignDialog.visible = true;
}

function onAssignChannelInput() {
  assignDialog.name = `${assignDialog.classTypeName} ${assignDialog.channel}`;
}

async function submitAssign() {
  const node = selected.value;
  if (!node?.nodeCode) return;
  const channel = assignDialog.channel.trim();
  if (!channel) {
    ElMessage.warning(t('production.channelRequired'));
    return;
  }
  if (!assignDialog.name.trim()) {
    ElMessage.warning(t('production.nameRequired'));
    return;
  }
  assigning.value = true;
  try {
    if (assignDialog.kind === 'SENSOR') {
      const unit = sensorClasses.value.find((c) => c.sensorClass === assignDialog.parentClass)?.unit || '';
      const res = await createProductionSensor({
        nodeCode: node.nodeCode,
        channel,
        sensorName: assignDialog.name.trim(),
        sensorClass: assignDialog.parentClass,
        sensorClassType: assignDialog.classType,
        unit,
      });
      ElMessage.success(t('production.created', { code: res.data?.sensorCode || '' }));
    } else {
      const res = await createProductionControlDevice({
        nodeCode: node.nodeCode,
        channel,
        deviceName: assignDialog.name.trim(),
        deviceClass: assignDialog.parentClass,
        deviceClassType: assignDialog.classType,
        connectType: 0,
      });
      ElMessage.success(t('production.created', { code: res.data?.deviceCode || '' }));
    }
    assignDialog.visible = false;
    await Promise.all([loadAssignments(), load()]);
  } catch (error) {
    ElMessage.error((error as Error).message || t('gateway.saveFailed'));
  } finally {
    assigning.value = false;
  }
}

async function removeSensor(row: SensorEntity) {
  try {
    await ElMessageBox.confirm(t('production.deleteConfirm', { code: row.sensorCode }), t('common.deleteTitle'), { type: 'warning' });
  } catch {
    return;
  }
  try {
    await deleteProductionSensor(row.id);
    ElMessage.success(t('control.deleted'));
    await Promise.all([loadAssignments(), load()]);
  } catch (error) {
    ElMessage.error((error as Error).message || t('control.deleteFailed'));
  }
}

async function removeDevice(row: DeviceCtrlEntity) {
  try {
    await ElMessageBox.confirm(t('production.deleteConfirm', { code: row.deviceCode }), t('common.deleteTitle'), { type: 'warning' });
  } catch {
    return;
  }
  try {
    await deleteProductionControlDevice(row.id);
    ElMessage.success(t('control.deleted'));
    await Promise.all([loadAssignments(), load()]);
  } catch (error) {
    ElMessage.error((error as Error).message || t('control.deleteFailed'));
  }
}

onMounted(async () => {
  void loadDicts();
  await load();
  const preselect = String(route.query.nodeCode || '');
  if (preselect) {
    const target = nodes.value.find((n) => n.nodeCode === preselect);
    if (target) selectNode(target);
  }
});
</script>

<template>
  <div class="prod-page" v-loading="loading">
    <h2 class="page-title">{{ $t('route.productionNodeConfig') }}</h2>

    <div class="filter-bar">
      <el-select v-model="typeFilter" class="type-select" clearable :placeholder="$t('production.nodeConfig.searchNodeType')" :aria-label="$t('production.nodeConfig.searchNodeType')">
        <el-option v-for="(label, value) in NODE_TYPES" :key="value" :label="$t(label)" :value="Number(value)" />
      </el-select>
      <el-input v-model="codeFilter" class="code-input" clearable :placeholder="$t('production.nodeConfig.searchNodeCode')" :aria-label="$t('production.nodeConfig.searchNodeCode')">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-input v-model="nameFilter" class="name-input" clearable :placeholder="$t('production.nodeConfig.searchName')" :aria-label="$t('production.nodeConfig.searchName')" />
      <el-button type="primary" @click="load">{{ $t('common.search') }}</el-button>
      <el-button @click="resetFilter">{{ $t('common.reset') }}</el-button>
    </div>

    <div class="table-card" v-loading="loading">
      <h3 class="section-title">{{ $t('production.nodeConfig.nodeList') }}</h3>
      <el-table
        :data="filtered"
        border
        highlight-current-row
        style="width: 100%"
        :empty-text="$t('production.empty')"
        @current-change="selectNode"
      >
        <el-table-column :label="$t('common.page')" type="index" width="64" align="center" />
        <el-table-column prop="nodeCode" :label="$t('node.nodeCode')" min-width="200">
          <template #default="{ row }"><span class="mono">{{ row.nodeCode }}</span></template>
        </el-table-column>
        <el-table-column prop="nodeName" :label="$t('node.name')" min-width="140" show-overflow-tooltip />
        <el-table-column :label="$t('node.type')" width="110" align="center">
          <template #default="{ row }">{{ typeName(row.nodeType) }}</template>
        </el-table-column>
        <el-table-column :label="$t('production.boundGateway')" min-width="190">
          <template #default="{ row }"><span class="mono">{{ row.gatewayCode || '--' }}</span></template>
        </el-table-column>
        <el-table-column :label="$t('production.sensorCount')" width="100" align="center">
          <template #default="{ row }">{{ sensorCountMap.get(row.nodeCode) ?? 0 }}</template>
        </el-table-column>
        <el-table-column :label="$t('production.deviceCount')" width="100" align="center">
          <template #default="{ row }">{{ deviceCountMap.get(row.nodeCode) ?? 0 }}</template>
        </el-table-column>
      </el-table>
    </div>

    <div class="table-card lower-card" v-if="selected" v-loading="detailLoading">
      <el-tabs v-model="activeTab">
        <el-tab-pane :label="$t('production.nodeConfig.sensorConfigList')" name="sensor">
          <div class="assign-layout">
            <div class="assign-left">
              <h4 class="block-title">{{ $t('production.nodeConfig.assignedList') }}</h4>
              <el-table :data="sensors" border style="width: 100%" :empty-text="$t('common.empty')">
                <el-table-column prop="channel" :label="$t('gatewayManage.nodePage.channel')" width="90" align="center">
                  <template #default="{ row }">{{ row.channel ?? '--' }}</template>
                </el-table-column>
                <el-table-column prop="sensorCode" :label="$t('sensor.sensorCode')" min-width="200" show-overflow-tooltip>
                  <template #default="{ row }"><span class="mono">{{ row.sensorCode || '--' }}</span></template>
                </el-table-column>
                <el-table-column :label="$t('production.nodeConfig.sensorType')" min-width="160" show-overflow-tooltip>
                  <template #default="{ row }">
                    {{ (row.sensorClassType && sensorTypeNameMap.get(row.sensorClassType)) || row.sensorClassType || '--' }}
                  </template>
                </el-table-column>
                <el-table-column :label="$t('common.actions')" width="90" align="center">
                  <template #default="{ row }">
                    <el-button link type="danger" v-permission="'production:sensor:delete'" @click="removeSensor(row)">{{ $t('common.delete') }}</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
            <div class="assign-right">
              <h4 class="block-title">{{ $t('production.nodeConfig.availableTypes') }}</h4>
              <el-table :data="sensorClassTypes" border height="360" style="width: 100%" :empty-text="$t('common.empty')">
                <el-table-column prop="classType" :label="$t('production.nodeConfig.typeCode')" width="100" align="center">
                  <template #default="{ row }"><span class="mono">{{ row.classType }}</span></template>
                </el-table-column>
                <el-table-column prop="classTypeName" :label="$t('production.nodeConfig.typeName')" min-width="160" show-overflow-tooltip>
                  <template #default="{ row }">{{ row.classTypeName || '--' }}</template>
                </el-table-column>
                <el-table-column :label="$t('production.nodeConfig.parentClass')" min-width="120" show-overflow-tooltip>
                  <template #default="{ row }">{{ sensorClassNameMap.get(row.sensorClass || '') || row.sensorClass || '--' }}</template>
                </el-table-column>
                <el-table-column :label="$t('common.actions')" width="90" align="center" fixed="right">
                  <template #default="{ row }">
                    <el-button link type="primary" v-permission="'production:sensor:create'" @click="openAssignSensor(row)">{{ $t('production.nodeConfig.assign') }}</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </el-tab-pane>
        <el-tab-pane :label="$t('production.nodeConfig.deviceConfigList')" name="device">
          <div class="assign-layout">
            <div class="assign-left">
              <h4 class="block-title">{{ $t('production.nodeConfig.assignedList') }}</h4>
              <el-table :data="devices" border style="width: 100%" :empty-text="$t('common.empty')">
                <el-table-column prop="channel" :label="$t('gatewayManage.nodePage.channel')" width="90" align="center">
                  <template #default="{ row }">{{ row.channel ?? '--' }}</template>
                </el-table-column>
                <el-table-column prop="deviceCode" :label="$t('device.code')" min-width="200" show-overflow-tooltip>
                  <template #default="{ row }"><span class="mono">{{ row.deviceCode || '--' }}</span></template>
                </el-table-column>
                <el-table-column :label="$t('production.nodeConfig.deviceType')" min-width="160" show-overflow-tooltip>
                  <template #default="{ row }">
                    {{ (row.deviceClassType && deviceTypeNameMap.get(row.deviceClassType)) || row.deviceClassType || '--' }}
                  </template>
                </el-table-column>
                <el-table-column :label="$t('common.actions')" width="90" align="center">
                  <template #default="{ row }">
                    <el-button link type="danger" v-permission="'production:ctrl:delete'" @click="removeDevice(row)">{{ $t('common.delete') }}</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
            <div class="assign-right">
              <h4 class="block-title">{{ $t('production.nodeConfig.availableTypes') }}</h4>
              <el-table :data="deviceClassTypes" border height="360" style="width: 100%" :empty-text="$t('common.empty')">
                <el-table-column prop="type" :label="$t('production.nodeConfig.typeCode')" width="100" align="center">
                  <template #default="{ row }"><span class="mono">{{ row.type }}</span></template>
                </el-table-column>
                <el-table-column prop="typeName" :label="$t('production.nodeConfig.typeName')" min-width="160" show-overflow-tooltip>
                  <template #default="{ row }">{{ row.typeName || '--' }}</template>
                </el-table-column>
                <el-table-column :label="$t('production.nodeConfig.parentClass')" min-width="120" show-overflow-tooltip>
                  <template #default="{ row }">{{ deviceClassNameMap.get(row.deviceClass || '') || row.deviceClass || '--' }}</template>
                </el-table-column>
                <el-table-column :label="$t('common.actions')" width="90" align="center" fixed="right">
                  <template #default="{ row }">
                    <el-button link type="primary" v-permission="'production:ctrl:create'" @click="openAssignDevice(row)">{{ $t('production.nodeConfig.assign') }}</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <div v-else class="empty-hint">
      <el-empty :description="$t('production.nodeConfig.selectNodeFirst')" />
    </div>

    <el-dialog v-model="assignDialog.visible" :title="$t('production.nodeConfig.assignTitle', { type: assignDialog.classTypeName })" width="460px">
      <el-form label-width="110px">
        <el-form-item :label="$t('production.nodeConfig.typeName')">
          <el-input :model-value="`${assignDialog.classTypeName}（${assignDialog.classType}）`" disabled />
        </el-form-item>
        <el-form-item :label="$t('gatewayManage.nodePage.channel')">
          <el-input v-model="assignDialog.channel" style="width: 140px" :placeholder="$t('production.channelPlaceholder')" @input="onAssignChannelInput" />
          <span class="form-tip">{{ $t('production.channelTip') }}</span>
        </el-form-item>
        <el-form-item :label="$t('node.name')">
          <el-input v-model="assignDialog.name" :placeholder="$t('production.nodeConfig.namePlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialog.visible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="assigning" @click="submitAssign">{{ $t('production.nodeConfig.assign') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.prod-page { padding: 20px 24px 32px; }
.page-title { margin: 0 0 20px; font-size: 20px; font-weight: 600; color: #303133; }
.filter-bar { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin-bottom: 16px; }
.type-select { width: 150px; }
.code-input { width: 200px; }
.name-input { width: 180px; }
.table-card {
  background: #fff; border: 1px solid #ebeef5; border-radius: 4px; padding: 16px;
  margin-bottom: 16px;
}
.section-title { margin: 0 0 12px; font-size: 15px; font-weight: 600; color: #1f2937; }
.block-title { margin: 0 0 8px; font-size: 13px; font-weight: 600; color: #606266; }
.lower-card { min-height: 320px; }
.assign-layout { display: flex; gap: 16px; align-items: flex-start; }
.assign-left { flex: 1 1 50%; min-width: 0; }
.assign-right { flex: 1 1 50%; min-width: 0; }
.mono { font-family: monospace; }
.form-tip { font-size: 12px; color: #909399; line-height: 1.4; margin-left: 8px; }
.empty-hint { padding-top: 60px; }
:deep(.el-table th.el-table__cell) { background: #f5f7fa; color: #606266; font-weight: 600; }
:deep(.el-table .cell) { font-size: 13px; }
@media (max-width: 900px) { .assign-layout { flex-direction: column; } }
</style>
