<script setup lang="ts">
import type { DeviceClassDict, DeviceClassTypeDict, DeviceCtrlEntity } from '@/api/iot/device';

const props = defineProps<{
  devices: DeviceCtrlEntity[];
  classes: DeviceClassDict[];
  classTypes: DeviceClassTypeDict[];
}>();

const SWITCH_MODES: Record<number, string> = { 1: '开度', 2: '二开', 3: '三开' };

function typeName(device: DeviceCtrlEntity) {
  const type = props.classTypes.find((t) => t.type === device.deviceClassType)?.typeName;
  if (type) return type;
  const cls = props.classes.find((c) => c.deviceClass === device.deviceClass)?.className;
  return cls || device.deviceClassType || device.deviceClass || '-';
}

function switchMode(device: DeviceCtrlEntity) {
  const mode = props.classes.find((c) => c.deviceClass === device.deviceClass)?.switchMode;
  return mode ? (SWITCH_MODES[mode] ?? String(mode)) : '--';
}
</script>

<template>
  <div class="device-table">
    <div class="table-title">已绑定控制设备({{ devices.length }}个)</div>
    <el-table :data="devices" border style="width: 100%" empty-text="暂无数据">
      <el-table-column label="序号" type="index" width="60" align="center" />
      <el-table-column label="设备组名称" min-width="120"><template #default>--</template></el-table-column>
      <el-table-column label="设备类型" min-width="130">
        <template #default="{ row }">{{ typeName(row) }}</template>
      </el-table-column>
      <el-table-column label="设备名称" min-width="140">
        <template #default="{ row }">{{ row.deviceName || '--' }}</template>
      </el-table-column>
      <el-table-column prop="deviceCode" label="设备编码" min-width="160" show-overflow-tooltip />
      <el-table-column label="开关模式" width="100" align="center">
        <template #default="{ row }">{{ switchMode(row) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.online === 1 ? 'success' : 'danger'" size="small" effect="light">
            {{ row.online === 1 ? '在线' : '离线' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="别名区域" min-width="130">
        <template #default="{ row }">{{ row.address || '--' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="80" align="center"><template #default>--</template></el-table-column>
    </el-table>
  </div>
</template>

<style scoped>
.table-title { padding: 10px 0; color: #606266; font-size: 13px; }
:deep(.el-table th.el-table__cell) { background: #f5f7fa; color: #606266; font-weight: 600; }
:deep(.el-table .cell) { font-size: 13px; }
</style>
