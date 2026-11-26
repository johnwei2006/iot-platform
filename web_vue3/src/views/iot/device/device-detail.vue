<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { ArrowLeft, Edit, MoreFilled } from '@element-plus/icons-vue';
import { listNodes, type NodeEntity } from '@/api/iot/node';
import {
  getControlDevice, getControlDeviceEntry, listDeviceClass, listDeviceClassType,
  updateControlDevice, type DeviceClassDict, type DeviceClassTypeDict, type DeviceCtrlEntity,
} from '@/api/iot/device';
import AreaSelect from '@/components/AreaSelect.vue';

type Field = 'deviceName' | 'longitude' | 'latitude';

const route = useRoute();
const router = useRouter();
const id = String(route.params.id);
const device = ref<DeviceCtrlEntity | null>(null);
const node = ref<NodeEntity | null>(null);
const classes = ref<DeviceClassDict[]>([]);
const classTypes = ref<DeviceClassTypeDict[]>([]);
const loading = ref(false);
const bound = ref(true);
const editVisible = ref(false);
const activeField = ref<Field | null>(null);
const draftValue = ref('');
const coordDraft = ref({ longitude: '', latitude: '' });
const editInputRef = ref<{ focus: () => void } | null>(null);
const form = reactive<Partial<DeviceCtrlEntity>>({});

const SWITCH_MODES: Record<number, string> = { 1: '开度', 2: '二开', 3: '三开' };

const typeName = computed(() => {
  if (!device.value) return '--';
  return classTypes.value.find((t) => t.type === device.value?.deviceClassType)?.typeName
    || classes.value.find((c) => c.deviceClass === device.value?.deviceClass)?.className
    || device.value.deviceClassType || device.value.deviceClass || '--';
});

const switchMode = computed(() => {
  const mode = device.value && classes.value.find((c) => c.deviceClass === device.value?.deviceClass)?.switchMode;
  return mode ? (SWITCH_MODES[mode] ?? String(mode)) : '--';
});

function statusLabel(status?: string) {
  if (status === '0' || status === 'open') return '开启';
  if (status === '1' || status === 'stop') return '停止';
  return status || '--';
}

function formatTime(value?: string) {
  if (!value) return '--';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ` +
    `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

function startEdit(field: Field) {
  if (!device.value) return;
  activeField.value = field;
  if (field === 'longitude' || field === 'latitude') {
    coordDraft.value = { longitude: device.value.longitude ?? '', latitude: device.value.latitude ?? '' };
  } else {
    draftValue.value = String(device.value[field] ?? '');
  }
  nextTick(() => editInputRef.value?.focus());
}

async function applyPatch(patch: Partial<DeviceCtrlEntity>) {
  if (!device.value) return;
  try {
    const res = await updateControlDevice(id, patch);
    device.value = res.data ?? device.value;
    activeField.value = null;
    ElMessage.success('已保存');
  } catch (error) {
    ElMessage.error((error as Error).message || '保存失败');
    activeField.value = null;
  }
}

async function saveArea(areaId?: string) {
  try {
    const res = await updateControlDevice(id, { areaId: areaId || '' });
    device.value = res.data ?? device.value;
    ElMessage.success('关联区域已更新');
  } catch (error) {
    ElMessage.error((error as Error).message || '保存失败');
  }
}

async function save(field: Field) {
  if (!device.value || activeField.value !== field) return;
  const value = draftValue.value.trim();
  if (value === (device.value[field] ?? '')) {
    activeField.value = null;
    return;
  }
  await applyPatch({ [field]: value } as Partial<DeviceCtrlEntity>);
}

async function saveCoords() {
  if (!device.value) return;
  const { longitude, latitude } = coordDraft.value;
  if (longitude === (device.value.longitude ?? '') && latitude === (device.value.latitude ?? '')) {
    activeField.value = null;
    return;
  }
  await applyPatch({ longitude, latitude });
}

function openEdit() {
  if (!device.value) return;
  Object.assign(form, {
    deviceName: device.value.deviceName,
    deviceClass: device.value.deviceClass,
    deviceClassType: device.value.deviceClassType,
    address: device.value.address,
    channel: device.value.channel,
    connectType: device.value.connectType,
    auto: device.value.auto,
    online: device.value.online,
  });
  editVisible.value = true;
}

async function submitEdit() {
  try {
    const res = await updateControlDevice(id, form);
    device.value = res.data ?? device.value;
    editVisible.value = false;
    ElMessage.success('已保存');
  } catch (error) {
    ElMessage.error((error as Error).message || '保存失败');
  }
}

async function closeDevice() {
  await ElMessageBox.confirm('确认关闭设备？该操作仅更新设备状态，不下发指令。', '提示', { type: 'warning' });
  await applyPatch({ status: '0' });
}

async function showEntry() {
  try {
    const res = await getControlDeviceEntry(id);
    const entry = res.data;
    if (!entry) {
      ElMessage.warning('未解析到控制三元组');
      return;
    }
    await ElMessageBox.alert(
      `deviceId：${entry.deviceId}<br/>nodeId：${entry.nodeId ?? '-'}<br/>channel：${entry.channel ?? '-'}`,
      '控制三元组', { dangerouslyUseHTMLString: true, confirmButtonText: '知道了' },
    );
  } catch (error) {
    ElMessage.error((error as Error).message || '三元组解析失败');
  }
}

function onBindChange() {
  ElMessage.info('设备绑定功能暂未开放');
  bound.value = true;
}

async function load() {
  loading.value = true;
  try {
    const detail = await getControlDevice(id);
    device.value = detail.data;
    const [nodeRes, classRes, typeRes] = await Promise.all([
      listNodes(), listDeviceClass(), listDeviceClassType(),
    ]);
    node.value = (nodeRes.data ?? []).find((n) => n.nodeCode === detail.data?.nodeCode) ?? null;
    classes.value = classRes.data ?? [];
    classTypes.value = typeRes.data ?? [];
  } catch (error) {
    ElMessage.error((error as Error).message || '控制设备详情加载失败');
    router.push('/iot/control-devices');
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <div class="ctrl-detail-page" v-loading="loading">
    <header class="detail-header">
      <div class="title-wrap">
        <el-button link :icon="ArrowLeft" aria-label="返回控制设备列表" @click="router.push('/iot/control-devices')" />
        <h2>{{ device?.deviceName || device?.deviceCode || '--' }}</h2>
        <el-tag :type="device?.online === 1 ? 'success' : 'danger'" size="small" effect="light">
          {{ device?.online === 1 ? '在线' : '离线' }}
        </el-tag>
        <el-switch v-model="bound" size="small" active-text="已绑定传感器" @change="onBindChange" />
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="closeDevice">关闭设备</el-button>
        <el-dropdown trigger="click">
          <el-button :icon="MoreFilled" aria-label="更多操作" />
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="openEdit">编辑设备</el-dropdown-item>
              <el-dropdown-item @click="showEntry">查看三元组</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <div v-if="device" class="info-grid">
      <div class="info-cell">
        <span class="info-label">设备名称</span>
        <div class="info-value">
          <el-input v-if="activeField === 'deviceName'" ref="editInputRef" v-model="draftValue" size="small"
                    @blur="save('deviceName')" @keyup.enter="save('deviceName')" />
          <template v-else><span>{{ device.deviceName || '--' }}</span><el-button link :icon="Edit" aria-label="编辑设备名称" @click="startEdit('deviceName')" /></template>
        </div>
      </div>
      <div class="info-cell"><span class="info-label">网关编码</span><div class="info-value"><span>{{ node?.gatewayCode || '--' }}</span></div></div>
      <div class="info-cell"><span class="info-label">节点编码</span><div class="info-value"><span>{{ device.nodeCode || '--' }}</span></div></div>
      <div class="info-cell"><span class="info-label">设备编码</span><div class="info-value"><span>{{ device.deviceCode }}</span></div></div>
      <div class="info-cell">
        <span class="info-label">经纬度</span>
        <div class="info-value coord-value">
          <template v-if="activeField === 'longitude' || activeField === 'latitude'">
            <el-input ref="editInputRef" v-model="coordDraft.longitude" size="small" placeholder="经度" @blur="saveCoords" @keyup.enter="saveCoords" />
            <span>,</span>
            <el-input v-model="coordDraft.latitude" size="small" placeholder="纬度" @blur="saveCoords" @keyup.enter="saveCoords" />
          </template>
          <template v-else>
            <span>{{ device.longitude || device.latitude ? `${device.longitude ?? '--'}, ${device.latitude ?? '--'}` : '--' }}</span>
            <el-button link :icon="Edit" aria-label="编辑经纬度" @click="startEdit('longitude')" />
          </template>
        </div>
      </div>
      <div class="info-cell"><span class="info-label">创建时间</span><div class="info-value"><span>{{ formatTime(device.createTime) }}</span></div></div>
      <div class="info-cell"><span class="info-label">更新人</span><div class="info-value"><span>{{ device.updateBy || device.createBy || '--' }}</span></div></div>
      <div class="info-cell"><span class="info-label">设备类型</span><div class="info-value"><span>{{ typeName }}</span></div></div>
      <div class="info-cell"><span class="info-label">更新时间</span><div class="info-value"><span>{{ formatTime(device.updateTime) }}</span></div></div>
      <div class="info-cell"><span class="info-label">开关模式</span><div class="info-value"><span>{{ switchMode }}</span></div></div>
      <div class="info-cell"><span class="info-label">状态</span><div class="info-value"><span>{{ statusLabel(device.status) }}</span></div></div>
      <div class="info-cell"><span class="info-label">最后上报时间</span><div class="info-value"><span>--</span></div></div>
      <div class="info-cell"><span class="info-label">安装位置</span><div class="info-value"><span>{{ device.address || node?.address || '--' }}</span></div></div>
      <div class="info-cell">
        <span class="info-label">关联区域</span>
        <div class="info-value">
          <AreaSelect
            :model-value="device.areaId"
            placeholder="选择关联区域"
            @change="(area) => saveArea(area?.id)"
          />
        </div>
      </div>
      <div class="info-cell">
        <span class="info-label">设备图片</span>
        <div class="info-value">
          <el-image v-if="device.pic" :src="device.pic" :preview-src-list="[device.pic]" fit="cover" class="device-pic" />
          <span v-else>--</span>
        </div>
      </div>
    </div>

    <div class="warning-section">
      <el-tabs model-value="warning">
        <el-tab-pane label="预警记录" name="warning">
          <el-table :data="[]" border style="width: 100%" empty-text="暂无数据">
            <el-table-column label="序号" type="index" width="60" align="center" />
            <el-table-column label="预警时间" min-width="170" />
            <el-table-column label="预警内容" min-width="220" />
            <el-table-column label="接收人" min-width="120" />
            <el-table-column label="确认时间" min-width="170" />
            <el-table-column label="状态" width="100" align="center" />
            <el-table-column label="操作" width="80" align="center" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-dialog v-model="editVisible" title="编辑控制设备" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="设备名称"><el-input v-model="form.deviceName" /></el-form-item>
        <el-form-item label="设备大类">
          <el-select v-model="form.deviceClass" clearable style="width: 100%">
            <el-option v-for="c in classes" :key="c.deviceClass" :label="c.className" :value="c.deviceClass" />
          </el-select>
        </el-form-item>
        <el-form-item label="设备类型">
          <el-select v-model="form.deviceClassType" clearable style="width: 100%">
            <el-option v-for="t in classTypes" :key="t.type" :label="t.typeName" :value="t.type" />
          </el-select>
        </el-form-item>
        <el-form-item label="安装区域"><el-input v-model="form.address" /></el-form-item>
        <el-form-item label="通道"><el-input v-model="form.channel" /></el-form-item>
        <el-form-item label="自动控制"><el-switch v-model="form.auto" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.ctrl-detail-page { padding: 20px 24px 32px; }
.detail-header { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 20px; }
.title-wrap { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.title-wrap h2 { margin: 0; font-size: 20px; font-weight: 600; color: #303133; }
.header-actions { display: flex; gap: 8px; }
.info-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); border: 1px solid #ebeef5; background: #fff; }
.info-cell { display: flex; flex-direction: column; justify-content: center; gap: 8px; min-height: 68px; padding: 12px 16px; border-right: 1px solid #ebeef5; border-bottom: 1px solid #ebeef5; }
.info-cell:nth-child(3n) { border-right: none; }
.info-label { color: #909399; font-size: 12px; }
.info-value { display: flex; align-items: center; gap: 4px; min-height: 24px; color: #303133; font-size: 14px; }
.info-value > span:first-child { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.coord-value :deep(.el-input) { width: 90px; }
.device-pic { width: 56px; height: 40px; border-radius: 4px; }
.warning-section { margin-top: 24px; }
.warning-section :deep(.el-tabs__header) { margin: 0; }
:deep(.el-table th.el-table__cell) { background: #f5f7fa; color: #606266; font-weight: 600; }
@media (max-width: 900px) { .info-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } .info-cell:nth-child(3n) { border-right: 1px solid #ebeef5; } .info-cell:nth-child(2n) { border-right: none; } }
</style>
