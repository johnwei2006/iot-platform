<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import type { DeviceClassDict, DeviceClassTypeDict, DeviceCtrlEntity } from '@/api/iot/device';

const { t } = useI18n();
const props = defineProps<{
  devices: DeviceCtrlEntity[];
  classes: DeviceClassDict[];
  classTypes: DeviceClassTypeDict[];
}>();

const SWITCH_MODES: Record<number, string> = { 1: 'device.switchMode_1', 2: 'device.switchMode_2', 3: 'device.switchMode_3' };

function typeName(device: DeviceCtrlEntity) {
  const type = props.classTypes.find((t) => t.type === device.deviceClassType)?.typeName;
  if (type) return type;
  const cls = props.classes.find((c) => c.deviceClass === device.deviceClass)?.className;
  return cls || device.deviceClassType || device.deviceClass || '-';
}

function switchMode(device: DeviceCtrlEntity) {
  const mode = props.classes.find((c) => c.deviceClass === device.deviceClass)?.switchMode;
  if (!mode) return '--';
  const key = SWITCH_MODES[mode];
  return key ? t(key) : String(mode);
}
</script>

<template>
  <div class="device-table">
    <div class="table-title">{{ $t('node.boundDevices', { count: devices.length }) }}</div>
    <el-table :data="devices" border style="width: 100%" :empty-text="$t('common.empty')">
      <el-table-column :label="$t('common.page')" type="index" width="60" align="center" />
      <el-table-column :label="$t('node.groupName')" min-width="120"><template #default>--</template></el-table-column>
      <el-table-column :label="$t('device.type')" min-width="130">
        <template #default="{ row }">{{ typeName(row) }}</template>
      </el-table-column>
      <el-table-column :label="$t('device.name')" min-width="140">
        <template #default="{ row }">{{ row.deviceName || '--' }}</template>
      </el-table-column>
      <el-table-column prop="deviceCode" :label="$t('device.code')" min-width="160" show-overflow-tooltip />
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
      <el-table-column :label="$t('node.aliasArea')" min-width="130">
        <template #default="{ row }">{{ row.address || '--' }}</template>
      </el-table-column>
      <el-table-column :label="$t('common.actions')" width="80" align="center"><template #default>--</template></el-table-column>
    </el-table>
  </div>
</template>

<style scoped>
.table-title { padding: 10px 0; color: #606266; font-size: 13px; }
:deep(.el-table th.el-table__cell) { background: #f5f7fa; color: #606266; font-weight: 600; }
:deep(.el-table .cell) { font-size: 13px; }
</style>
