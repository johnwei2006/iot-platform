<script setup lang="ts">
import type { SensorClassDict, SensorClassTypeDict, SensorEntity } from '@/api/iot/sensor';

const props = defineProps<{
  sensors: SensorEntity[];
  classes: SensorClassDict[];
  classTypes: SensorClassTypeDict[];
}>();

function typeName(sensor: SensorEntity) {
  const type = props.classTypes.find((t) => t.classType === sensor.sensorClassType)?.classTypeName;
  if (type) return type;
  const cls = props.classes.find((c) => c.sensorClass === sensor.sensorClass)?.className;
  return cls || sensor.sensorClassType || sensor.sensorClass || '-';
}
</script>

<template>
  <div class="sensor-table">
    <div class="table-title">{{ $t('node.boundSensors', { count: sensors.length }) }}</div>
    <el-table :data="sensors" border style="width: 100%" :empty-text="$t('common.empty')">
      <el-table-column :label="$t('common.page')" type="index" width="60" align="center" />
      <el-table-column :label="$t('node.groupName')" min-width="120"><template #default>--</template></el-table-column>
      <el-table-column :label="$t('sensor.type')" min-width="140">
        <template #default="{ row }">{{ typeName(row) }}</template>
      </el-table-column>
      <el-table-column :label="$t('sensor.name')" min-width="150">
        <template #default="{ row }">{{ row.sensorName || '--' }}</template>
      </el-table-column>
      <el-table-column prop="sensorCode" :label="$t('sensor.code')" min-width="160" show-overflow-tooltip />
      <el-table-column :label="$t('common.status')" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small" effect="light">
            {{ row.status === 0 ? $t('common.online') : $t('common.offline') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="$t('node.aliasArea')" min-width="120"><template #default>--</template></el-table-column>
      <el-table-column :label="$t('common.actions')" width="80" align="center"><template #default>--</template></el-table-column>
    </el-table>
  </div>
</template>

<style scoped>
.table-title { padding: 10px 0; color: #606266; font-size: 13px; }
:deep(.el-table th.el-table__cell) { background: #f5f7fa; color: #606266; font-weight: 600; }
:deep(.el-table .cell) { font-size: 13px; }
</style>
