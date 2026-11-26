<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { ArrowLeft, Edit } from '@element-plus/icons-vue';
import { listNodes, type NodeEntity } from '@/api/iot/node';
import {
  getSensor, listSensorClass, listSensorClassType, updateSensor,
  type SensorClassDict, type SensorClassTypeDict, type SensorEntity,
} from '@/api/iot/sensor';
import AreaSelect from '@/components/AreaSelect.vue';
import SensorHistoryChart from './components/sensor-history-chart.vue';

const route = useRoute();
const router = useRouter();
const id = String(route.params.id);
const sensor = ref<SensorEntity | null>(null);
const node = ref<NodeEntity | null>(null);
const classes = ref<SensorClassDict[]>([]);
const classTypes = ref<SensorClassTypeDict[]>([]);
const loading = ref(false);
const editVisible = ref(false);
const alarmVisible = ref(false);
const bound = ref(true);
const form = reactive<Partial<SensorEntity>>({});
const alarmForm = reactive({ warnMin: 0, warnMax: 100 });

const displayName = computed(() => sensor.value
  ? `${sensor.value.sensorCode}${sensor.value.sensorName ? '-' + sensor.value.sensorName : ''}` : '--');

const typeName = computed(() => {
  if (!sensor.value) return '--';
  return classTypes.value.find((t) => t.classType === sensor.value?.sensorClassType)?.classTypeName
    || classes.value.find((c) => c.sensorClass === sensor.value?.sensorClass)?.className
    || sensor.value.sensorClassType || sensor.value.sensorClass || '--';
});

function formatTime(value?: string) {
  if (!value) return '--';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ` +
    `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

function openEdit() {
  if (!sensor.value) return;
  Object.assign(form, {
    sensorName: sensor.value.sensorName,
    sensorClass: sensor.value.sensorClass,
    sensorClassType: sensor.value.sensorClassType,
    unit: sensor.value.unit,
    channel: sensor.value.channel,
    commandRead: sensor.value.commandRead,
    scale: sensor.value.scale,
    reportInterval: sensor.value.reportInterval,
  });
  editVisible.value = true;
}

function openAlarm() {
  if (!sensor.value) return;
  alarmForm.warnMin = sensor.value.warnMin ?? 0;
  alarmForm.warnMax = sensor.value.warnMax ?? 100;
  alarmVisible.value = true;
}

async function submitEdit() {
  try {
    const res = await updateSensor(id, form);
    sensor.value = res.data ?? sensor.value;
    editVisible.value = false;
    ElMessage.success('已保存');
  } catch (error) {
    ElMessage.error((error as Error).message || '保存失败');
  }
}

async function submitAlarm() {
  try {
    const res = await updateSensor(id, { warnMin: alarmForm.warnMin, warnMax: alarmForm.warnMax });
    sensor.value = res.data ?? sensor.value;
    alarmVisible.value = false;
    ElMessage.success('告警阈值已保存');
  } catch (error) {
    ElMessage.error((error as Error).message || '保存失败');
  }
}

async function saveArea(areaId?: string) {
  try {
    const res = await updateSensor(id, { areaId: areaId || '' });
    sensor.value = res.data ?? sensor.value;
    ElMessage.success('关联区域已更新');
  } catch (error) {
    ElMessage.error((error as Error).message || '保存失败');
  }
}

function placeholder(feature: string) {
  ElMessage.info(`${feature}功能暂未开放`);
}

function onBindChange() {
  placeholder('传感器绑定');
  bound.value = true;
}

async function load() {
  loading.value = true;
  try {
    const detail = await getSensor(id);
    sensor.value = detail.data;
    const [nodeRes, classRes, typeRes] = await Promise.all([
      listNodes(), listSensorClass(), listSensorClassType(),
    ]);
    node.value = (nodeRes.data ?? []).find((n) => n.nodeCode === detail.data?.nodeCode) ?? null;
    classes.value = classRes.data ?? [];
    classTypes.value = typeRes.data ?? [];
  } catch (error) {
    ElMessage.error((error as Error).message || '传感器详情加载失败');
    router.push('/iot/sensors');
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <div class="sensor-detail-page" v-loading="loading">
    <header class="detail-header">
      <div class="title-wrap">
        <el-button link :icon="ArrowLeft" aria-label="返回传感器列表" @click="router.push('/iot/sensors')" />
        <h2>{{ displayName }}</h2>
        <el-tag :type="sensor?.status === 0 ? 'success' : 'danger'" size="small" effect="light">
          {{ sensor?.status === 0 ? '正常' : '异常' }}
        </el-tag>
        <el-switch v-model="bound" active-text="已绑定传感器" size="small" @change="onBindChange" />
      </div>
      <div class="header-actions">
        <el-button @click="openEdit">编辑</el-button>
        <el-button @click="openAlarm">告警配置</el-button>
        <el-button type="primary" @click="placeholder('立即下发')">立即下发</el-button>
      </div>
    </header>

    <div v-if="sensor" class="info-grid">
      <div class="info-cell">
        <span class="info-label">传感器名称</span>
        <div class="info-value"><span>{{ sensor.sensorName || '--' }}</span><el-button link :icon="Edit" aria-label="编辑" @click="openEdit" /></div>
      </div>
      <div class="info-cell"><span class="info-label">网关编码</span><div class="info-value"><span>{{ node?.gatewayCode || '--' }}</span></div></div>
      <div class="info-cell"><span class="info-label">传感器编码</span><div class="info-value"><span>{{ sensor.sensorCode }}</span></div></div>
      <div class="info-cell"><span class="info-label">更新人</span><div class="info-value"><span>{{ sensor.updateBy || sensor.createBy || '--' }}</span></div></div>
      <div class="info-cell"><span class="info-label">传感器类型</span><div class="info-value"><span>{{ typeName }}</span></div></div>
      <div class="info-cell"><span class="info-label">更新时间</span><div class="info-value"><span>{{ formatTime(sensor.updateTime) }}</span></div></div>
      <div class="info-cell"><span class="info-label">最小值</span><div class="info-value"><span>{{ sensor.warnMin ?? '--' }}</span></div></div>
      <div class="info-cell"><span class="info-label">最大值</span><div class="info-value"><span>{{ sensor.warnMax ?? '--' }}</span></div></div>
      <div class="info-cell"><span class="info-label">单位</span><div class="info-value"><span>{{ sensor.unit || '--' }}</span></div></div>
      <div class="info-cell"><span class="info-label">节点位置</span><div class="info-value"><span>{{ node?.address || '--' }}</span></div></div>
      <div class="info-cell">
        <span class="info-label">关联区域</span>
        <div class="info-value">
          <AreaSelect
            :model-value="sensor.areaId"
            placeholder="选择关联区域"
            @change="(area) => saveArea(area?.id)"
          />
        </div>
      </div>
      <div class="info-cell"><span class="info-label">创建时间</span><div class="info-value"><span>{{ formatTime(sensor.createTime) }}</span></div></div>
    </div>

    <sensor-history-chart v-if="sensor" :sensor="sensor" />

    <el-dialog v-model="editVisible" title="编辑传感器" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="传感器名称"><el-input v-model="form.sensorName" /></el-form-item>
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
        <el-form-item label="单位"><el-input v-model="form.unit" /></el-form-item>
        <el-form-item label="通道"><el-input v-model="form.channel" /></el-form-item>
        <el-form-item label="读数指令"><el-input v-model="form.commandRead" /></el-form-item>
        <el-form-item label="小数位"><el-input-number v-model="form.scale" :min="0" :max="6" /></el-form-item>
        <el-form-item label="上报频率"><el-input-number v-model="form.reportInterval" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="alarmVisible" title="告警配置" width="420px">
      <el-form label-width="90px">
        <el-form-item label="最小值"><el-input-number v-model="alarmForm.warnMin" style="width: 100%" /></el-form-item>
        <el-form-item label="最大值"><el-input-number v-model="alarmForm.warnMax" style="width: 100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="alarmVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAlarm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.sensor-detail-page { padding: 20px 24px 32px; }
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
@media (max-width: 900px) { .info-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } .info-cell:nth-child(3n) { border-right: 1px solid #ebeef5; } .info-cell:nth-child(2n) { border-right: none; } }
</style>
