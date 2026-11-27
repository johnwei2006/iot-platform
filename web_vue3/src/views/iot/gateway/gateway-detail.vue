<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { ElMessage } from 'element-plus';
import { ArrowLeft, Edit } from '@element-plus/icons-vue';
import { getDevice, updateDevice } from '@/api/iot/gateway';
import { fetchOrgList } from '@/api/system/org';
import AreaSelect from '@/components/AreaSelect.vue';
import type { Device, OrgRecord } from '@/types';

type Field = 'name' | 'orgCode' | 'longitude' | 'latitude' | 'address' | 'areaId' | 'gatewayType' | 'firmwareVersion' | 'pic';

const { t } = useI18n();
const route = useRoute();
const router = useRouter();
const id = String(route.params.id);
const device = ref<Device | null>(null);
const orgs = ref<OrgRecord[]>([]);
const loading = ref(false);
const activeField = ref<Field | null>(null);
const draftValue = ref<string | number>('');
const coordDraft = ref({ longitude: '', latitude: '' });
const editInputRef = ref<{ focus: () => void } | null>(null);

const orgName = computed(() => orgs.value.find((o) => o.code === device.value?.orgCode)?.name || '--');

function typeName(type?: number) {
  if (type === undefined || type === null) return '--';
  const key = `gateway.type_${type}`;
  const translated = t(key);
  return translated === key ? String(type) : translated;
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
    coordDraft.value = {
      longitude: device.value.longitude ?? '',
      latitude: device.value.latitude ?? '',
    };
  } else {
    draftValue.value = device.value[field] ?? (field === 'gatewayType' ? 1 : '');
  }
  nextTick(() => editInputRef.value?.focus());
}

async function applyPatch(patch: Partial<Device>, message = t('gateway.saved')) {
  if (!device.value) return;
  try {
    const res = await updateDevice(id, patch);
    device.value = res.data ?? device.value;
    activeField.value = null;
    ElMessage.success(message);
  } catch (error) {
    ElMessage.error((error as Error).message || t('gateway.saveFailed'));
    activeField.value = null;
  }
}

async function saveArea(areaId?: string) {
  try {
    const res = await updateDevice(id, { areaId: areaId || '' });
    device.value = res.data ?? device.value;
    ElMessage.success(t('gateway.areaUpdated'));
  } catch (error) {
    ElMessage.error((error as Error).message || t('gateway.saveFailed'));
  }
}

async function save(field: Field) {
  if (!device.value || activeField.value !== field) return;
  const value = field === 'gatewayType' ? Number(draftValue.value) : String(draftValue.value ?? '');
  if (value === (device.value[field] ?? '')) {
    activeField.value = null;
    return;
  }
  const patch: Partial<Device> = {};
  (patch as Record<string, unknown>)[field] = value;
  await applyPatch(patch);
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

function placeholder(feature: string) {
  ElMessage.info(t('gateway.notAvailable', { feature }));
}

async function load() {
  loading.value = true;
  try {
    const [detail, orgList] = await Promise.all([getDevice(id), fetchOrgList()]);
    device.value = detail.data;
    orgs.value = orgList;
  } catch (error) {
    ElMessage.error((error as Error).message || t('gateway.detailLoadFailed'));
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <div class="detail-page" v-loading="loading">
    <header class="detail-header">
      <div class="title-wrap">
        <el-button link :icon="ArrowLeft" :aria-label="$t('gateway.backToList')" @click="router.push('/iot/gateways')" />
        <h2>{{ device?.name || '--' }}</h2>
        <el-tag :type="device?.status === 'ONLINE' ? 'success' : 'danger'" size="small" effect="light">
          {{ device?.status === 'ONLINE' ? $t('common.online') : $t('common.offline') }}
        </el-tag>
      </div>
      <div class="header-actions">
        <el-button @click="placeholder($t('gateway.installConfig'))">{{ $t('gateway.installConfig') }}</el-button>
        <el-button type="primary" @click="placeholder($t('gateway.bindNow'))">{{ $t('gateway.bindNow') }}</el-button>
      </div>
    </header>

    <div v-if="device" class="info-grid">
      <div class="info-cell">
        <span class="info-label">{{ $t('gateway.name') }}</span>
        <div class="info-value">
          <el-input v-if="activeField === 'name'" ref="editInputRef" v-model="draftValue" size="small"
                    @blur="save('name')" @keyup.enter="save('name')" />
          <template v-else>
            <span>{{ device.name || '--' }}</span>
            <el-button link :icon="Edit" :aria-label="$t('gateway.editName')" @click="startEdit('name')" />
          </template>
        </div>
      </div>

      <div class="info-cell">
        <span class="info-label">{{ $t('gateway.code') }}</span>
        <div class="info-value"><span>{{ device.deviceId || '--' }}</span></div>
      </div>

      <div class="info-cell">
        <span class="info-label">{{ $t('gateway.type') }}</span>
        <div class="info-value">
          <el-select v-if="activeField === 'gatewayType'" v-model="draftValue" size="small" @change="save('gatewayType')">
            <el-option :label="$t('gateway.type_1')" :value="1" />
            <el-option :label="$t('gateway.type_2')" :value="2" />
            <el-option :label="$t('gateway.type_3')" :value="3" />
            <el-option :label="$t('gateway.type_4')" :value="4" />
          </el-select>
          <template v-else>
            <span>{{ typeName(device.gatewayType) }}</span>
            <el-button link :icon="Edit" :aria-label="$t('gateway.editType')" @click="startEdit('gatewayType')" />
          </template>
        </div>
      </div>

      <div class="info-cell">
        <span class="info-label">{{ $t('gateway.orgName') }}</span>
        <div class="info-value"><span>{{ orgName }}</span></div>
      </div>

      <div class="info-cell">
        <span class="info-label">{{ $t('gateway.orgCode') }}</span>
        <div class="info-value">
          <el-select v-if="activeField === 'orgCode'" v-model="draftValue" size="small" filterable @change="save('orgCode')">
            <el-option v-for="org in orgs" :key="org.id" :label="org.name" :value="org.code" />
          </el-select>
          <template v-else>
            <span>{{ device.orgCode || '--' }}</span>
            <el-button link :icon="Edit" :aria-label="$t('gateway.editOrgCode')" @click="startEdit('orgCode')" />
          </template>
        </div>
      </div>

      <div class="info-cell">
        <span class="info-label">{{ $t('gateway.coords') }}</span>
        <div class="info-value coord-value">
          <template v-if="activeField === 'longitude' || activeField === 'latitude'">
            <el-input ref="editInputRef" v-model="coordDraft.longitude" size="small" :placeholder="$t('gateway.longitude')"
                      @blur="saveCoords" @keyup.enter="saveCoords" />
            <span>,</span>
            <el-input v-model="coordDraft.latitude" size="small" :placeholder="$t('gateway.latitude')"
                      @blur="saveCoords" @keyup.enter="saveCoords" />
          </template>
          <template v-else>
            <span>{{ device.longitude || device.latitude ? `${device.longitude ?? '--'}, ${device.latitude ?? '--'}` : '--' }}</span>
            <el-button link :icon="Edit" :aria-label="$t('gateway.editCoords')" @click="startEdit('longitude')" />
          </template>
        </div>
      </div>

      <div class="info-cell">
        <span class="info-label">{{ $t('gateway.updatedBy') }}</span>
        <div class="info-value"><span>--</span></div>
      </div>

      <div class="info-cell">
        <span class="info-label">{{ $t('gateway.firmware') }}</span>
        <div class="info-value">
          <el-input v-if="activeField === 'firmwareVersion'" ref="editInputRef" v-model="draftValue" size="small"
                    @blur="save('firmwareVersion')" @keyup.enter="save('firmwareVersion')" />
          <template v-else>
            <span>{{ device.firmwareVersion || '--' }}</span>
            <el-button link :icon="Edit" :aria-label="$t('gateway.editFirmware')" @click="startEdit('firmwareVersion')" />
          </template>
        </div>
      </div>

      <div class="info-cell">
        <span class="info-label">{{ $t('gateway.sim') }}</span>
        <div class="info-value"><span>--</span></div>
      </div>

      <div class="info-cell">
        <span class="info-label">{{ $t('common.updateTime') }}</span>
        <div class="info-value"><span>{{ formatTime(device.updatedAt) }}</span></div>
      </div>

      <div class="info-cell">
        <span class="info-label">{{ $t('gateway.lastSeen') }}</span>
        <div class="info-value"><span>{{ formatTime(device.lastSeen) }}</span></div>
      </div>

      <div class="info-cell">
        <span class="info-label">{{ $t('gateway.area') }}</span>
        <div class="info-value">
          <AreaSelect
            :model-value="device.areaId"
            :placeholder="$t('gateway.selectArea')"
            @change="(area) => saveArea(area?.id)"
          />
        </div>
      </div>

      <div class="info-cell">
        <span class="info-label">{{ $t('gateway.address') }}</span>
        <div class="info-value">
          <el-input v-if="activeField === 'address'" ref="editInputRef" v-model="draftValue" size="small"
                    @blur="save('address')" @keyup.enter="save('address')" />
          <template v-else>
            <span>{{ device.address || '--' }}</span>
            <el-button link :icon="Edit" :aria-label="$t('gateway.editAddress')" @click="startEdit('address')" />
          </template>
        </div>
      </div>

      <div class="info-cell">
        <span class="info-label">{{ $t('gateway.pic') }}</span>
        <div class="info-value">
          <el-image v-if="device.pic" :src="device.pic" :preview-src-list="[device.pic]" fit="cover" class="gateway-pic" />
          <span v-else>--</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.detail-page { padding: 20px 24px 32px; }
.detail-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.title-wrap { display: flex; align-items: center; gap: 8px; }
.title-wrap h2 { margin: 0; font-size: 20px; font-weight: 600; color: #303133; }
.header-actions { display: flex; gap: 8px; }
.info-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); border: 1px solid #ebeef5; background: #fff; }
.info-cell { display: flex; flex-direction: column; justify-content: center; gap: 8px; min-height: 72px; padding: 12px 16px; border-right: 1px solid #ebeef5; border-bottom: 1px solid #ebeef5; }
.info-cell:nth-child(3n) { border-right: none; }
.info-label { color: #909399; font-size: 12px; }
.info-value { display: flex; align-items: center; gap: 4px; min-height: 24px; color: #303133; font-size: 14px; }
.info-value > span:first-child { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.coord-value :deep(.el-input) { width: 90px; }
.gateway-pic { width: 56px; height: 40px; border-radius: 4px; }
@media (max-width: 900px) { .info-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } .info-cell:nth-child(3n) { border-right: 1px solid #ebeef5; } .info-cell:nth-child(2n) { border-right: none; } }
</style>
