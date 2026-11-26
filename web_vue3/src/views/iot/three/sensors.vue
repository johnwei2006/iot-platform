<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  createSensor, deleteSensor, listSensorClass, listSensorClassType,
  listSensors, updateSensor,
  type SensorClassDict, type SensorClassTypeDict, type SensorEntity,
} from '@/api/threeLayer';

const loading = ref(false);
const sensors = ref<SensorEntity[]>([]);
const classes = ref<SensorClassDict[]>([]);
const classTypes = ref<SensorClassTypeDict[]>([]);
const dialogVisible = ref(false);
const editingId = ref<string | null>(null);
const form = reactive<Partial<SensorEntity>>({});

async function refresh() {
  loading.value = true;
  try {
    const res = await listSensors();
    sensors.value = res.data || [];
  } catch (error) {
    ElMessage.error((error as Error).message || '传感器加载失败');
  } finally {
    loading.value = false;
  }
}

async function loadDicts() {
  const [cls, types] = await Promise.all([listSensorClass(), listSensorClassType()]);
  classes.value = cls.data || [];
  classTypes.value = types.data || [];
}

function className(code?: string) {
  return classes.value.find((c) => c.sensorClass === code)?.className || code || '-';
}

function typeName(code?: string) {
  return classTypes.value.find((t) => t.classType === code)?.classTypeName || code || '-';
}

function openCreate() {
  editingId.value = null;
  Object.keys(form).forEach((k) => delete (form as Record<string, unknown>)[k]);
  form.scale = 2;
  form.status = 0;
  dialogVisible.value = true;
}

function openEdit(row: SensorEntity) {
  editingId.value = row.id;
  Object.assign(form, row);
  dialogVisible.value = true;
}

async function submit() {
  if (!form.sensorCode) {
    ElMessage.warning('请填写传感器编码');
    return;
  }
  try {
    if (editingId.value) {
      await updateSensor(editingId.value, form);
    } else {
      await createSensor(form);
    }
    ElMessage.success('保存成功');
    dialogVisible.value = false;
    refresh();
  } catch (error) {
    ElMessage.error((error as Error).message || '保存失败');
  }
}

async function remove(row: SensorEntity) {
  await ElMessageBox.confirm(`确认删除传感器 ${row.sensorCode}？`, '提示', { type: 'warning' });
  await deleteSensor(row.id);
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
          <span>传感器管理（挂在节点下，按通道区分）</span>
          <el-button type="primary" @click="openCreate">新增传感器</el-button>
        </div>
      </template>
      <el-table :data="sensors" stripe>
        <el-table-column prop="sensorCode" label="传感器编码" min-width="130" />
        <el-table-column prop="sensorName" label="名称" min-width="120" />
        <el-table-column prop="nodeCode" label="所属节点" min-width="120" />
        <el-table-column label="大类" width="110">
          <template #default="{ row }">{{ className(row.sensorClass) }}</template>
        </el-table-column>
        <el-table-column label="类型" width="130">
          <template #default="{ row }">{{ typeName(row.sensorClassType) }}</template>
        </el-table-column>
        <el-table-column prop="channel" label="通道" width="70" />
        <el-table-column prop="unit" label="单位" width="70" />
        <el-table-column label="阈值下限" width="90">
          <template #default="{ row }">{{ row.warnMin ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="阈值上限" width="90">
          <template #default="{ row }">{{ row.warnMax ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small">{{ row.status === 0 ? '正常' : '异常' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑传感器' : '新增传感器'" width="600px">
      <el-form label-width="100px">
        <el-form-item label="传感器编码" required><el-input v-model="form.sensorCode" :disabled="!!editingId" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.sensorName" /></el-form-item>
        <el-form-item label="节点编码"><el-input v-model="form.nodeCode" placeholder="iot_node.node_code" /></el-form-item>
        <el-form-item label="大类">
          <el-select v-model="form.sensorClass" clearable style="width: 100%">
            <el-option v-for="c in classes" :key="c.sensorClass" :label="c.className" :value="c.sensorClass" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.sensorClassType" clearable style="width: 100%">
            <el-option v-for="t in classTypes" :key="t.classType" :label="t.classTypeName" :value="t.classType" />
          </el-select>
        </el-form-item>
        <el-form-item label="通道"><el-input v-model="form.channel" placeholder="节点下的端口号" /></el-form-item>
        <el-form-item label="单位"><el-input v-model="form.unit" /></el-form-item>
        <el-form-item label="阈值下限"><el-input-number v-model="form.warnMin" style="width: 100%" /></el-form-item>
        <el-form-item label="阈值上限"><el-input-number v-model="form.warnMax" style="width: 100%" /></el-form-item>
        <el-form-item label="小数位"><el-input-number v-model="form.scale" :min="0" :max="6" style="width: 100%" /></el-form-item>
        <el-form-item label="读数指令"><el-input v-model="form.commandRead" placeholder="485 Hex 读数指令（虚拟节点 485 传感器非空）" /></el-form-item>
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
