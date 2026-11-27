<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { ElMessage } from 'element-plus';
import { getAreaRefs, type AreaEntity, type AreaRefCounts } from '@/api/iot/area';
import { listDevices } from '@/api/iot/gateway';
import { listNodes } from '@/api/iot/node';
import { listSensors } from '@/api/iot/sensor';
import { listControlDevices } from '@/api/iot/device';

interface EntityItem {
  id?: string;
  name?: string;
  code: string;
}

const { t } = useI18n();
const props = defineProps<{ area: AreaEntity | null }>();
const emit = defineEmits<{ close: [] }>();

const router = useRouter();
const loading = ref(false);
const refs = ref<AreaRefCounts | null>(null);
const activeTab = ref<'gateway' | 'node' | 'sensor' | 'deviceCtrl'>('gateway');
const lists = ref<Record<string, EntityItem[]>>({ gateway: [], node: [], sensor: [], deviceCtrl: [] });

const tabs = computed(() => [
  { key: 'gateway' as const, label: t('entity.gateway') },
  { key: 'node' as const, label: t('entity.node') },
  { key: 'sensor' as const, label: t('entity.sensor') },
  { key: 'deviceCtrl' as const, label: t('entity.deviceCtrl') },
]);

const detailPath: Record<string, (id: string) => string> = {
  gateway: (id) => `/iot/gateways/${id}`,
  node: (id) => `/iot/nodes/${id}`,
  sensor: (id) => `/iot/sensors/${id}`,
  deviceCtrl: (id) => `/iot/control-devices/${id}`,
};

async function load() {
  if (!props.area) return;
  loading.value = true;
  try {
    const id = props.area.id;
    const [refRes, gatewayRes, nodeRes, sensorRes, deviceRes] = await Promise.all([
      getAreaRefs(id), listDevices(id), listNodes(undefined, id), listSensors(undefined, id), listControlDevices(undefined, id),
    ]);
    refs.value = refRes.data ?? null;
    lists.value = {
      gateway: (gatewayRes.data ?? []).map((d) => ({ id: d.id, name: d.name, code: d.deviceId })),
      node: (nodeRes.data ?? []).map((n) => ({ id: n.id, name: n.nodeName || n.nodeCode, code: n.nodeCode })),
      sensor: (sensorRes.data ?? []).map((s) => ({ id: s.id, name: s.sensorName || s.sensorCode, code: s.sensorCode })),
      deviceCtrl: (deviceRes.data ?? []).map((d) => ({ id: d.id, name: d.deviceName, code: d.deviceCode })),
    };
  } catch (error) {
    ElMessage.error((error as Error).message || t('area.refsLoadFailed'));
  } finally {
    loading.value = false;
  }
}

function openEntity(key: string, id: string) {
  router.push(detailPath[key](id));
}

watch(() => props.area?.id, load);
onMounted(load);
</script>

<template>
  <div v-if="area" v-loading="loading" class="entity-panel">
    <div class="panel-head">
      <div>
        <div class="panel-title">{{ area.areaName }}</div>
        <div class="panel-sub">{{ $t('area.refsSummary', { count: refs?.total ?? 0 }) }}</div>
      </div>
      <el-button :aria-label="$t('common.actions')" @click="emit('close')">✕</el-button>
    </div>
    <el-tabs v-model="activeTab" class="entity-tabs">
      <el-tab-pane v-for="tab in tabs" :key="tab.key" :name="tab.key">
        <template #label>{{ tab.label }} ({{ refs?.[tab.key] ?? 0 }})</template>
        <el-empty v-if="!lists[tab.key]?.length" :description="$t('area.refsEmpty', { name: tab.label })" :image-size="60" />
        <div v-else class="entity-list">
          <div v-for="item in lists[tab.key]" :key="item.id" class="entity-item" @click="item.id && openEntity(tab.key, item.id)">
            <span class="entity-name">{{ item.name }}</span>
            <span class="entity-code">{{ item.code }}</span>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.entity-panel { position: absolute; top: 16px; right: 16px; bottom: 16px; width: 340px; z-index: 10; display: flex; flex-direction: column; background: rgba(255,255,255,0.97); backdrop-filter: blur(12px); border-radius: 12px; box-shadow: 0 8px 32px rgba(0,0,0,0.18); overflow: hidden; }
.panel-head { display: flex; align-items: flex-start; justify-content: space-between; padding: 14px 16px; border-bottom: 1px solid #ebeef5; }
.panel-title { font-size: 16px; font-weight: 700; }
.panel-sub { margin-top: 4px; color: #909399; font-size: 12px; }
.entity-tabs { flex: 1; padding: 0 12px 12px; min-height: 0; overflow: hidden; }
.entity-list { max-height: calc(100vh - 260px); overflow-y: auto; }
.entity-item { display: flex; align-items: center; justify-content: space-between; padding: 9px 6px; cursor: pointer; border-bottom: 1px solid #f0f2f5; transition: background 0.15s; }
.entity-item:hover { background: #f0f6ff; }
.entity-name { color: #303133; font-size: 13px; font-weight: 600; }
.entity-code { color: #909399; font-size: 12px; }
</style>
