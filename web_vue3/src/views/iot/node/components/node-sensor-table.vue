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
    <div class="table-title">已绑定传感器({{ sensors.length }}个)</div>
    <el-table :data="sensors" border style="width: 100%" empty-text="暂无数据">
      <el-table-column label="序号" type="index" width="60" align="center" />
      <el-table-column label="设备组名称" min-width="120"><template #default>--</template></el-table-column>
      <el-table-column label="传感器类型" min-width="140">
        <template #default="{ row }">{{ typeName(row) }}</template>
      </el-table-column>
      <el-table-column label="传感器名称" min-width="150">
        <template #default="{ row }">{{ row.sensorName || '--' }}</template>
      </el-table-column>
      <el-table-column prop="sensorCode" label="传感器编码" min-width="160" show-overflow-tooltip />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small" effect="light">
            {{ row.status === 0 ? '在线' : '离线' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="别名区域" min-width="120"><template #default>--</template></el-table-column>
      <el-table-column label="操作" width="80" align="center"><template #default>--</template></el-table-column>
    </el-table>
  </div>
</template>

<style scoped>
.table-title { padding: 10px 0; color: #606266; font-size: 13px; }
:deep(.el-table th.el-table__cell) { background: #f5f7fa; color: #606266; font-weight: 600; }
:deep(.el-table .cell) { font-size: 13px; }
</style>
