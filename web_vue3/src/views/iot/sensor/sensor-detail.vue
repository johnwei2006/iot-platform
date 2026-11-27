<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { ElMessage } from 'element-plus';
import { ArrowLeft, Edit } from '@element-plus/icons-vue';
import { listNodes, type NodeEntity } from '@/api/iot/node';
import {
  getSensor, listSensorClass, listSensorClassType, updateSensor,
  type SensorClassDict, type SensorClassTypeDict, type SensorEntity,
} from '@/api/iot/sensor';
import AreaSelect from '@/components/AreaSelect.vue';
import SensorHistoryChart from './components/sensor-history-chart.vue';

const { t } = useI18n();
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
    ElMessage.success(t('gateway.saved'));
  } catch (error) {
    ElMessage.error((error as Error).message || t('gateway.saveFailed'));
  }
}

async function submitAlarm() {
  try {
    const res = await updateSensor(id, { warnMin: alarmForm.warnMin, warnMax: alarmForm.warnMax });
    sensor.value = res.data ?? sensor.value;
    alarmVisible.value = false;
    ElMessage.success(t('sensor.alarmSaved'));
  } catch (error) {
    ElMessage.error((error as Error).message || t('gateway.saveFailed'));
  }
}

async function saveArea(areaId?: string) {
  try {
    const res = await updateSensor(id, { areaId: areaId || '' });
    sensor.value = res.data ?? sensor.value;
    ElMessage.success(t('sensor.areaUpdated'));
  } catch (error) {
    ElMessage.error((error as Error).message || t('gateway.saveFailed'));
  }
}

function placeholder(feature: string) {
  ElMessage.info(t('gateway.notAvailable', { feature }));
}

function onBindChange() {
  placeholder(t('sensor.bind'));
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
    ElMessage.error((error as Error).message || t('sensor.detailLoadFailed'));
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
        <el-button link :icon="ArrowLeft" :aria-label="$t('sensor.backToList')" @click="router.push('/iot/sensors')" />
        <h2>{{ displayName }}</h2>
        <el-tag :type="sensor?.status === 0 ? 'success' : 'danger'" size="small" effect="light">
          {{ sensor?.status === 0 ? $t('sensor.normal') : $t('sensor.abnormal') }}
        </el-tag>
        <el-switch v-model="bound" :active-text="$t('sensor.boundSwitch')" size="small" @change="onBindChange" />
      </div>
      <div class="header-actions">
        <el-button @click="openEdit">{{ $t('common.edit') }}</el-button>
        <el-button @click="openAlarm">{{ $t('sensor.alarmConfig') }}</el-button>
        <el-button type="primary" @click="placeholder($t('sensor.issueNow'))">{{ $t('sensor.issueNow') }}</el-button>
      </div>
    </header>

    <div v-if="sensor" class="info-grid">
      <div class="info-cell">
        <span class="info-label">{{ $t('sensor.name') }}</span>
        <div class="info-value"><span>{{ sensor.sensorName || '--' }}</span><el-button link :icon="Edit" :aria-label="$t('common.edit')" @click="openEdit" /></div>
      </div>
      <div class="info-cell"><span class="info-label">{{ $t('gateway.code') }}</span><div class="info-value"><span>{{ node?.gatewayCode || '--' }}</span></div></div>
      <div class="info-cell"><span class="info-label">{{ $t('sensor.sensorCode') }}</span><div class="info-value"><span>{{ sensor.sensorCode }}</span></div></div>
      <div class="info-cell"><span class="info-label">{{ $t('gateway.updatedBy') }}</span><div class="info-value"><span>{{ sensor.updateBy || sensor.createBy || '--' }}</span></div></div>
      <div class="info-cell"><span class="info-label">{{ $t('sensor.type') }}</span><div class="info-value"><span>{{ typeName }}</span></div></div>
      <div class="info-cell"><span class="info-label">{{ $t('common.updateTime') }}</span><div class="info-value"><span>{{ formatTime(sensor.updateTime) }}</span></div></div>
      <div class="info-cell"><span class="info-label">{{ $t('sensor.minValue') }}</span><div class="info-value"><span>{{ sensor.warnMin ?? '--' }}</span></div></div>
      <div class="info-cell"><span class="info-label">{{ $t('sensor.maxValue') }}</span><div class="info-value"><span>{{ sensor.warnMax ?? '--' }}</span></div></div>
      <div class="info-cell"><span class="info-label">{{ $t('sensor.unit') }}</span><div class="info-value"><span>{{ sensor.unit || '--' }}</span></div></div>
      <div class="info-cell"><span class="info-label">{{ $t('sensor.nodeLocation') }}</span><div class="info-value"><span>{{ node?.address || '--' }}</span></div></div>
      <div class="info-cell">
        <span class="info-label">{{ $t('sensor.linkedArea') }}</span>
        <div class="info-value">
          <AreaSelect
            :model-value="sensor.areaId"
            :placeholder="$t('sensor.selectArea')"
            @change="(area) => saveArea(area?.id)"
          />
        </div>
      </div>
      <div class="info-cell"><span class="info-label">{{ $t('common.createTime') }}</span><div class="info-value"><span>{{ formatTime(sensor.createTime) }}</span></div></div>
    </div>

    <sensor-history-chart v-if="sensor" :sensor="sensor" />

    <el-dialog v-model="editVisible" :title="$t('sensor.editTitle')" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item :label="$t('sensor.name')"><el-input v-model="form.sensorName" /></el-form-item>
        <el-form-item :label="$t('sensor.class')">
          <el-select v-model="form.sensorClass" clearable style="width: 100%">
            <el-option v-for="c in classes" :key="c.sensorClass" :label="c.className" :value="c.sensorClass" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('sensor.type')">
          <el-select v-model="form.sensorClassType" clearable style="width: 100%">
            <el-option v-for="t in classTypes" :key="t.classType" :label="t.classTypeName" :value="t.classType" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('sensor.unit')"><el-input v-model="form.unit" /></el-form-item>
        <el-form-item :label="$t('sensor.channel')"><el-input v-model="form.channel" /></el-form-item>
        <el-form-item :label="$t('sensor.readCommand')"><el-input v-model="form.commandRead" /></el-form-item>
        <el-form-item :label="$t('sensor.decimals')"><el-input-number v-model="form.scale" :min="0" :max="6" /></el-form-item>
        <el-form-item :label="$t('sensor.reportFrequency')"><el-input-number v-model="form.reportInterval" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="submitEdit">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="alarmVisible" :title="$t('sensor.alarmConfig')" width="420px">
      <el-form label-width="90px">
        <el-form-item :label="$t('sensor.minValue')"><el-input-number v-model="alarmForm.warnMin" style="width: 100%" /></el-form-item>
        <el-form-item :label="$t('sensor.maxValue')"><el-input-number v-model="alarmForm.warnMax" style="width: 100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="alarmVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="submitAlarm">{{ $t('common.save') }}</el-button>
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
