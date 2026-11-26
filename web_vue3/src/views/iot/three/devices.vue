<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  createControlDevice, deleteControlDevice, getControlDeviceEntry,
  listControlDevices, listDeviceClass, listDeviceClassType, updateControlDevice,
  type DeviceClassDict, type DeviceClassTypeDict, type DeviceCtrlEntity, type GatewayEntryVO,
} from '@/api/threeLayer';

const CONNECT_TYPES = [
  { value: 0, label: '通道直连' },
  { value: 1, label: '485 透传' },
];

const loading = ref(false);
const devices = ref<DeviceCtrlEntity[]>([]);
const deviceClasses = ref<DeviceClassDict[]>([]);
const deviceClassTypes = ref<DeviceClassTypeDict[]>([]);
const dialogVisible = ref(false);
const editingId = ref<string | null>(null);
const form = reactive<Partial<DeviceCtrlEntity>>({});

async function refresh() {
  loading.value = true;
  try {
    const res = await listControlDevices();
    devices.value = res.data || [];
  } catch (error) {
    ElMessage.error((error as Error).message || '控制设备加载失败');
  } finally {
    loading.value = false;
  }
}

async function loadDicts() {
  const [cls, types] = await Promise.all([listDeviceClass(), listDeviceClassType()]);
  deviceClasses.value = cls.data || [];
  deviceClassTypes.value = types.data || [];
}

function className(code?: string) {
  return deviceClasses.value.find((c) => c.deviceClass === code)?.className || code || '-';
}

function typeName(code?: string) {
  return deviceClassTypes.value.find((t) => t.type === code)?.typeName || code || '-';
}

function connectTypeLabel(value?: number) {
  return CONNECT_TYPES.find((t) => t.value === value)?.label ?? value ?? '-';
}

function openCreate() {
  editingId.value = null;
  Object.keys(form).forEach((k) => delete (form as Record<string, unknown>)[k]);
  form.connectType = 0;
  form.online = 0;
  form.auto = 0;
  dialogVisible.value = true;
}

function openEdit(row: DeviceCtrlEntity) {
  editingId.value = row.id;
  Object.assign(form, row);
  dialogVisible.value = true;
}

async function showEntry(row: DeviceCtrlEntity) {
  try {
    const res = await getControlDeviceEntry(row.id);
    const entry: GatewayEntryVO | undefined = res.data;
    if (!entry) {
      ElMessage.warning('未解析到控制三元组');
      return;
    }
    await ElMessageBox.alert(
      `deviceId（网关编码）：${entry.deviceId}<br/>nodeId（节点编码）：${entry.nodeId ?? '-'}<br/>channel（通道）：${entry.channel ?? '-'}`,
      `控制三元组：${entry.deviceName || row.deviceCode}`,
      { dangerouslyUseHTMLString: true, confirmButtonText: '知道了' },
    );
  } catch (error) {
    ElMessage.error((error as Error).message || '三元组解析失败');
  }
}

async function submit() {
  if (!form.deviceCode) {
    ElMessage.warning('请填写控制设备编码');
    return;
  }
  if (!form.nodeCode) {
    ElMessage.warning('请填写所属节点编码');
    return;
  }
  try {
    if (editingId.value) {
      await updateControlDevice(editingId.value, form);
    } else {
      await createControlDevice(form);
    }
    ElMessage.success('保存成功');
    dialogVisible.value = false;
    refresh();
  } catch (error) {
    ElMessage.error((error as Error).message || '保存失败');
  }
}

async function remove(row: DeviceCtrlEntity) {
  await ElMessageBox.confirm(`确认删除控制设备 ${row.deviceCode}？`, '提示', { type: 'warning' });
  await deleteControlDevice(row.id);
  ElMessage.success('已删除');
  refresh();
}

onMounted(async () => {
  await Promise.all([refresh(), loadDicts()]);
});
</script>

<template>
  <div v-loading="loading">
    <el-card shadow="never">
      <template #header>
        <div class="toolbar">
          <span>控制设备管理（挂在节点下，下发控制按 网关/节点/通道 三元组寻址）</span>
          <el-button type="primary" @click="openCreate">新增控制设备</el-button>
        </div>
      </template>
      <el-table :data="devices" stripe>
        <el-table-column prop="deviceCode" label="设备编码" min-width="130" />
        <el-table-column prop="deviceName" label="名称" min-width="120" />
        <el-table-column label="大类" width="110">
          <template #default="{ row }">{{ className(row.deviceClass) }}</template>
        </el-table-column>
        <el-table-column label="类型" width="130">
          <template #default="{ row }">{{ typeName(row.deviceClassType) }}</template>
        </el-table-column>
        <el-table-column prop="nodeCode" label="所属节点" min-width="120" />
        <el-table-column prop="channel" label="通道" width="70" />
        <el-table-column label="接入方式" width="110">
          <template #default="{ row }">{{ connectTypeLabel(row.connectType) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'open' ? 'success' : 'info'" size="small">{{ row.status === 'open' ? '开启' : (row.status || '-') }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="在线" width="80">
          <template #default="{ row }">
            <el-tag :type="row.online === 1 ? 'success' : 'danger'" size="small">{{ row.online === 1 ? '在线' : '离线' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="自动控制" width="90">
          <template #default="{ row }">
            <el-tag :type="row.auto === 1 ? 'primary' : 'info'" size="small">{{ row.auto === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showEntry(row)">三元组</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑控制设备' : '新增控制设备'" width="600px">
      <el-form label-width="100px">
        <el-form-item label="设备编码" required><el-input v-model="form.deviceCode" :disabled="!!editingId" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.deviceName" /></el-form-item>
        <el-form-item label="所属节点" required><el-input v-model="form.nodeCode" placeholder="iot_node.node_code" /></el-form-item>
        <el-form-item label="大类">
          <el-select v-model="form.deviceClass" clearable style="width: 100%">
            <el-option v-for="c in deviceClasses" :key="c.deviceClass" :label="c.className" :value="c.deviceClass" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.deviceClassType" clearable style="width: 100%">
            <el-option v-for="t in deviceClassTypes" :key="t.type" :label="t.typeName" :value="t.type" />
          </el-select>
        </el-form-item>
        <el-form-item label="通道"><el-input v-model="form.channel" placeholder="节点下的端口号（数字）" /></el-form-item>
        <el-form-item label="接入方式">
          <el-select v-model="form.connectType" style="width: 100%">
            <el-option v-for="t in CONNECT_TYPES" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="在线"><el-switch v-model="form.online" :active-value="1" :inactive-value="0" /></el-form-item>
        <el-form-item label="自动控制"><el-switch v-model="form.auto" :active-value="1" :inactive-value="0" /></el-form-item>
        <el-form-item label="地址"><el-input v-model="form.address" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; }
</style>
