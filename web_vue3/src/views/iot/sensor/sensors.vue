<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { ElMessage } from 'element-plus';
import { Search } from '@element-plus/icons-vue';
import { listNodes, type NodeEntity } from '@/api/iot/node';
import {
  listSensorClass, listSensorClassType, listSensors,
  type SensorClassDict, type SensorClassTypeDict, type SensorEntity,
} from '@/api/iot/sensor';
import AreaSelect from '@/components/AreaSelect.vue';
import { listAreas } from '@/api/iot/area';

const { t } = useI18n();
const router = useRouter();
const loading = ref(false);
const sensors = ref<SensorEntity[]>([]);
const nodes = ref<NodeEntity[]>([]);
const classes = ref<SensorClassDict[]>([]);
const classTypes = ref<SensorClassTypeDict[]>([]);
const codeField = ref('sensorCode');
const nameField = ref('sensorName');
const codeKey = ref('');
const nameKey = ref('');
const areaFilter = ref('');
const moreVisible = ref(false);
const classFilter = ref('');
const typeFilter = ref('');
const channelFilter = ref('');
const unitFilter = ref('');
const statusFilter = ref('');

const nodeByCode = computed(() => new Map(nodes.value.map((n) => [n.nodeCode, n])));
const areaNames = ref(new Map<string, string>());

function areaName(id?: string) {
  return (id && areaNames.value.get(id)) || '--';
}

function nodeOf(code?: string) {
  return code ? nodeByCode.value.get(code) : undefined;
}

function typeName(row: SensorEntity) {
  const type = classTypes.value.find((t) => t.classType === row.sensorClassType)?.classTypeName;
  if (type) return type;
  const cls = classes.value.find((c) => c.sensorClass === row.sensorClass)?.className;
  return cls || row.sensorClassType || row.sensorClass || '--';
}

function normalRange(row: SensorEntity) {
  if (row.warnMin === undefined && row.warnMax === undefined) return '--';
  return `${row.warnMin ?? '--'} ~ ${row.warnMax ?? '--'}${row.unit ? ` ${row.unit}` : ''}`;
}

const filtered = computed(() => sensors.value.filter((s) => {
  const code = codeKey.value.trim().toLowerCase();
  const name = nameKey.value.trim().toLowerCase();
  return (!code || (s.sensorCode ?? '').toLowerCase().includes(code))
    && (!name || (s.sensorName ?? '').toLowerCase().includes(name))
    && (!areaFilter.value || s.areaId === areaFilter.value)
    && (!classFilter.value || s.sensorClass === classFilter.value)
    && (!typeFilter.value || s.sensorClassType === typeFilter.value)
    && (!channelFilter.value || (s.channel ?? '').includes(channelFilter.value.trim()))
    && (!unitFilter.value || s.unit === unitFilter.value)
    && (!statusFilter.value || (s.status === 0 ? 'OK' : 'ERR') === statusFilter.value);
}));

function reset() {
  codeKey.value = '';
  nameKey.value = '';
  areaFilter.value = '';
  classFilter.value = '';
  typeFilter.value = '';
  channelFilter.value = '';
  unitFilter.value = '';
  statusFilter.value = '';
}

async function load() {
  loading.value = true;
  try {
    const [sensorRes, nodeRes, classRes, typeRes, areaRes] = await Promise.all([
      listSensors(), listNodes(), listSensorClass(), listSensorClassType(), listAreas(),
    ]);
    sensors.value = sensorRes.data ?? [];
    nodes.value = nodeRes.data ?? [];
    classes.value = classRes.data ?? [];
    classTypes.value = typeRes.data ?? [];
    areaNames.value = new Map((areaRes.data ?? []).map((a) => [a.id, a.areaName]));
  } catch (error) {
    ElMessage.error((error as Error).message || t('sensor.loadFailed'));
  } finally {
    loading.value = false;
  }
}

function openDetail(row: SensorEntity) {
  router.push(`/iot/sensors/${row.id}`);
}

onMounted(load);
</script>

<template>
  <div class="sensor-page">
    <h2 class="page-title">{{ $t('sensor.title') }}</h2>

    <div class="filter-bar">
      <div class="search-group">
        <el-select v-model="codeField" class="field-select" :aria-label="$t('sensor.codeField')">
          <el-option :label="$t('sensor.sensorCode')" value="sensorCode" />
        </el-select>
        <el-input v-model="codeKey" class="search-input" clearable :placeholder="$t('common.search')">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </div>
      <div class="search-group">
        <el-select v-model="nameField" class="field-select" :aria-label="$t('sensor.nameField')">
          <el-option :label="$t('sensor.name')" value="sensorName" />
        </el-select>
        <el-input v-model="nameKey" class="search-input" clearable :placeholder="$t('common.search')">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </div>
      <AreaSelect v-model="areaFilter" class="area-select" :placeholder="$t('sensor.areaFilter')" />
      <el-button type="primary" @click="load">{{ $t('common.search') }}</el-button>
      <el-button @click="reset">{{ $t('common.reset') }}</el-button>
      <el-button link type="primary" @click="moreVisible = !moreVisible">
        {{ moreVisible ? $t('sensor.collapseFilters') : $t('sensor.moreFilters') }}
      </el-button>
    </div>

    <div v-if="moreVisible" class="more-filter">
      <el-select v-model="classFilter" clearable :placeholder="$t('sensor.class')" :aria-label="$t('sensor.class')">
        <el-option v-for="c in classes" :key="c.sensorClass" :label="c.className" :value="c.sensorClass" />
      </el-select>
      <el-select v-model="typeFilter" clearable :placeholder="$t('sensor.type')" :aria-label="$t('sensor.type')">
        <el-option v-for="t in classTypes" :key="t.classType" :label="t.classTypeName" :value="t.classType" />
      </el-select>
      <el-input v-model="channelFilter" clearable :placeholder="$t('sensor.channel')" :aria-label="$t('sensor.channel')" />
      <el-input v-model="unitFilter" clearable :placeholder="$t('sensor.unit')" :aria-label="$t('sensor.unit')" />
      <el-select v-model="statusFilter" clearable :placeholder="$t('common.status')" :aria-label="$t('common.status')">
        <el-option :label="$t('sensor.normal')" value="OK" />
        <el-option :label="$t('sensor.abnormal')" value="ERR" />
      </el-select>
    </div>

    <div class="table-card" v-loading="loading">
      <el-table :data="filtered" border style="width: 100%" :empty-text="$t('common.empty')">
        <el-table-column :label="$t('common.page')" type="index" width="60" align="center" />
        <el-table-column :label="$t('node.name')" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ nodeOf(row.nodeCode)?.nodeName || '--' }}</template>
        </el-table-column>
        <el-table-column prop="nodeCode" :label="$t('node.nodeCode')" min-width="150" show-overflow-tooltip />
        <el-table-column :label="$t('sensor.type')" min-width="130">
          <template #default="{ row }">{{ typeName(row) }}</template>
        </el-table-column>
        <el-table-column :label="$t('sensor.name')" min-width="170" show-overflow-tooltip>
          <template #default="{ row }">{{ row.sensorName || '--' }}</template>
        </el-table-column>
        <el-table-column prop="sensorCode" :label="$t('sensor.sensorCode')" min-width="170" show-overflow-tooltip />
        <el-table-column :label="$t('common.status')" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small" effect="light">
              {{ row.status === 0 ? $t('sensor.normal') : $t('sensor.abnormal') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('sensor.linkedArea')" min-width="130">
          <template #default="{ row }">{{ areaName(row.areaId) }}</template>
        </el-table-column>
        <el-table-column :label="$t('sensor.normalRange')" min-width="130">
          <template #default="{ row }">{{ normalRange(row) }}</template>
        </el-table-column>
        <el-table-column :label="$t('common.actions')" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">{{ $t('gateway.viewDetail') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<style scoped>
.sensor-page { padding: 20px 24px 32px; }
.page-title { margin: 0 0 20px; font-size: 20px; font-weight: 600; color: #303133; }
.filter-bar { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin-bottom: 12px; }
.search-group { display: flex; align-items: center; gap: 4px; }
.field-select { width: 110px; }
.search-input { width: 160px; }
.area-select { width: 180px; }
.more-filter { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 12px; padding: 12px; background: #fafafa; border: 1px solid #ebeef5; border-radius: 4px; }
.more-filter > * { width: 170px; }
.table-card { background: #fff; border: 1px solid #ebeef5; border-radius: 4px; padding: 4px; }
:deep(.el-table th.el-table__cell) { background: #f5f7fa; color: #606266; font-weight: 600; }
:deep(.el-table .cell) { font-size: 13px; }
</style>
