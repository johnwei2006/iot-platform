<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { ElMessage } from 'element-plus';
import { Search } from '@element-plus/icons-vue';
import { listAreas, type AreaEntity } from '@/api/iot/area';
import { sendCommand } from '@/api/iot/control';
import {
  getControlDeviceEntry,
  listControlDevices,
  listDeviceClass,
  type DeviceClassDict,
  type DeviceCtrlEntity,
} from '@/api/iot/device';
import { listNodes, type NodeEntity } from '@/api/iot/node';
import AreaSelect from '@/components/AreaSelect.vue';
import type { DeviceEntry } from '@/types/kb';

const props = withDefaults(defineProps<{
  nodeCode?: string;
  groupBy?: 'area' | 'node';
  showAreaFilter?: boolean;
  titleKey?: string;
  permission?: string;
  useGatewayEntry?: boolean;
}>(), {
  nodeCode: '',
  groupBy: 'area',
  showAreaFilter: true,
  titleKey: 'control.controlList',
  permission: 'iot:control:issue',
  useGatewayEntry: false,
});

const { t } = useI18n();
const emit = defineEmits<{ 'show-logs': [{ areaId?: string; deviceId?: string }] }>();
const loading = ref(false);
const devices = ref<DeviceCtrlEntity[]>([]);
const areas = ref<AreaEntity[]>([]);
const nodes = ref<NodeEntity[]>([]);
const deviceClasses = ref<DeviceClassDict[]>([]);
const areaFilter = ref('');
const keyword = ref('');
const applying = ref(new Set<string>());

const areaNameMap = computed(() => new Map(areas.value.map((a) => [a.id, a.areaName])));
const nodeNameMap = computed(() => new Map(
  nodes.value.map((n) => [n.nodeCode, `${n.nodeCode} - ${n.nodeName || ''}`]),
));
const classMap = computed(() => new Map(deviceClasses.value.map((c) => [c.deviceClass, c])));

const groups = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  const filtered = devices.value.filter((d) => {
    const hitNode = !props.nodeCode || d.nodeCode === props.nodeCode;
    const hitArea = !areaFilter.value || d.areaId === areaFilter.value;
    const hitName = !kw || (d.deviceName || d.deviceCode).toLowerCase().includes(kw);
    return hitNode && hitArea && hitName;
  });
  const byGroup = new Map<string, DeviceCtrlEntity[]>();
  for (const d of filtered) {
    const key = props.groupBy === 'node' ? (d.nodeCode || '') : (d.areaId || '');
    if (!byGroup.has(key)) byGroup.set(key, []);
    byGroup.get(key)!.push(d);
  }
  return Array.from(byGroup.entries()).map(([key, list]) => ({
    key,
    areaId: props.groupBy === 'node' ? undefined : key,
    areaName: props.groupBy === 'node'
      ? (nodeNameMap.value.get(key) || key || '--')
      : (areaNameMap.value.get(key) || t('control.unassignedArea')),
    devices: list,
  }));
});

function entryOf(d: DeviceCtrlEntity): DeviceEntry {
  return {
    deviceId: d.deviceCode,
    nodeId: d.nodeCode || undefined,
    channel: Number(d.channel) || 1,
    deviceName: d.deviceName || d.deviceCode,
    deviceType: classMap.value.get(d.deviceClass || '')?.className || t('device.title'),
  };
}

function isOn(d: DeviceCtrlEntity) {
  return d.status === '1' || d.status === 'ON' || d.status === 'OPEN' || (Number(d.status) > 0 && Number(d.status) <= 100);
}

function statusText(d: DeviceCtrlEntity) {
  if (d.online === 0) return t('common.offline');
  const dict = classMap.value.get(d.deviceClass || '');
  if (d.status === '1' || d.status === 'ON' || d.status === 'OPEN') return dict?.onName || t('control.statusOn');
  if (d.status === '0' || d.status === 'OFF' || d.status === 'CLOSE') return dict?.offName || t('control.statusOff');
  if (d.status === '2' || d.status === 'STOP') return dict?.stopName || t('control.statusStop');
  if (d.status === 'AUTO') return t('control.statusAuto');
  return d.status || '--';
}

async function issue(entry: DeviceEntry, action: string, value?: string, key?: string) {
  if (key) applying.value.add(key);
  try {
    const res = await sendCommand([entry], action, value);
    const first = (res.data?.logs || [])[0];
    if (first?.result === 'FAILED') {
      ElMessage.error(first.detail || t('control.commandNotSent'));
    } else {
      ElMessage.success(t('control.commandSent'));
    }
  } catch (error) {
    ElMessage.error((error as Error).message || t('control.issueFailed'));
  } finally {
    if (key) {
      applying.value.delete(key);
      applying.value = new Set(applying.value);
    }
  }
}

async function resolveEntry(d: DeviceCtrlEntity): Promise<DeviceEntry> {
  if (props.useGatewayEntry && d.id) {
    try {
      const res = await getControlDeviceEntry(d.id);
      if (res.data) return res.data;
    } catch (error) {
      ElMessage.error((error as Error).message || t('control.loadFailed'));
    }
  }
  return entryOf(d);
}

async function toggleDevice(d: DeviceCtrlEntity, on: boolean) {
  const key = d.id || d.deviceCode;
  if (applying.value.has(key)) return;
  const entry = await resolveEntry(d);
  void issue(entry, on ? 'ON' : 'OFF', undefined, key);
}

function openAreaLogs(areaId?: string) {
  emit('show-logs', { areaId });
}

function openDeviceLogs(d: DeviceCtrlEntity) {
  emit('show-logs', { areaId: d.areaId, deviceId: d.deviceCode });
}

function reset() {
  areaFilter.value = '';
  keyword.value = '';
}

async function load() {
  loading.value = true;
  try {
    const [deviceRes, areaRes, classRes] = await Promise.all([
      listControlDevices(props.nodeCode || undefined), listAreas(), listDeviceClass(),
    ]);
    devices.value = deviceRes.data || [];
    areas.value = areaRes.data || [];
    deviceClasses.value = classRes.data || [];
    if (props.groupBy === 'node') {
      try {
        nodes.value = (await listNodes()).data || [];
      } catch {
        nodes.value = [];
      }
    }
  } catch (error) {
    ElMessage.error((error as Error).message || t('control.loadFailed'));
  } finally {
    loading.value = false;
  }
}

onMounted(load);
watch(() => props.nodeCode, () => {
  void load();
});
</script>

<template>
  <div v-loading="loading" class="panel">
    <el-card shadow="never">
      <template #header>
        <div class="head-row">
          <span class="head-title">{{ $t(titleKey) }}</span>
          <div class="filter-row">
            <AreaSelect v-if="showAreaFilter" v-model="areaFilter" class="area-select" :placeholder="$t('control.selectArea')" />
            <el-input v-model="keyword" class="search-input" :placeholder="$t('control.searchDevice')" clearable>
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-button type="primary" @click="load">{{ $t('common.search') }}</el-button>
            <el-button @click="reset">{{ $t('common.reset') }}</el-button>
          </div>
        </div>
      </template>

      <el-empty v-if="!groups.length" :description="$t('control.noDevices')" :image-size="90" />
      <section v-for="group in groups" :key="group.key || 'none'" class="group-section">
        <div class="group-head">
          <span class="group-title">{{ group.areaName }}</span>
          <el-button link type="primary" class="log-link" @click="openAreaLogs(group.areaId || undefined)">{{ $t('control.tabLogs') }}</el-button>
        </div>
        <div class="dev-grid">
          <article v-for="d in group.devices" :key="d.id || d.deviceCode" class="dev-card"
                   :class="{ offline: d.online === 0 }">
            <div class="dev-icon">
              <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="4" width="18" height="16" rx="3" /><path d="M7 9h6M7 13h10M7 17h4" />
              </svg>
            </div>
            <div class="dev-main">
              <div class="dev-name" :title="d.deviceName || d.deviceCode">{{ d.deviceName || d.deviceCode }}</div>
              <div class="dev-status" :class="{ on: isOn(d), offline: d.online === 0 }">{{ statusText(d) }}</div>
            </div>
            <div class="dev-actions">
              <el-switch
                :model-value="isOn(d)"
                :disabled="d.online === 0 || applying.has(d.id || d.deviceCode)"
                :loading="applying.has(d.id || d.deviceCode)"
                v-permission="permission"
                @change="(val: boolean) => toggleDevice(d, val)"
              />
              <el-dropdown trigger="click" @command="openDeviceLogs(d)">
                <button type="button" class="more-btn" :aria-label="$t('control.more')">···</button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="logs">{{ $t('control.tabLogs') }}</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </article>
        </div>
      </section>
    </el-card>

  </div>
</template>

<style scoped>
.panel { min-height: 400px; }
.head-row { display: flex; align-items: center; justify-content: space-between; gap: 16px; flex-wrap: wrap; }
.head-title { font-size: var(--kb-fs-heading); font-weight: 700; letter-spacing: var(--kb-tracking-heading); }
.filter-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.area-select { width: 200px; }
.search-input { width: 200px; }
.group-section { margin-top: 20px; }
.group-head { display: flex; align-items: center; justify-content: space-between; padding: 0 0 10px; border-bottom: 1px solid var(--kb-line); }
.group-title { font-size: var(--kb-fs-subheading); font-weight: 600; color: var(--kb-text); }
.log-link { font-size: var(--kb-fs-caption); }
.dev-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 12px; margin-top: 12px; }
.dev-card { display: flex; align-items: center; gap: 12px; min-width: 0; padding: 14px 14px; border: 1px solid var(--kb-line); border-radius: var(--kb-r-md); background: #fff; box-shadow: var(--kb-shadow-1); transition: border-color 0.2s ease, transform 0.2s cubic-bezier(0.22, 1, 0.36, 1); }
.dev-card:hover { border-color: var(--kb-primary); transform: translateY(-2px); }
.dev-card.offline { opacity: 0.7; }
.dev-icon { display: grid; place-items: center; width: 38px; height: 38px; flex: 0 0 38px; border-radius: 10px; color: #fff; background: linear-gradient(145deg, #409eff, #0958d9); }
.dev-main { min-width: 0; flex: 1; }
.dev-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 13px; font-weight: 600; color: var(--kb-text); }
.dev-status { margin-top: 3px; font-size: 12px; color: var(--kb-text-muted); }
.dev-status.on { color: #67c23a; }
.dev-status.offline { color: #f56c6c; }
.dev-actions { display: flex; align-items: center; gap: 10px; flex: 0 0 auto; }
.more-btn { width: 24px; height: 24px; padding: 0; border: 0; border-radius: 6px; background: transparent; color: var(--kb-text-muted); cursor: pointer; letter-spacing: 1px; font-size: 16px; line-height: 1; transition: background 0.15s ease; }
.more-btn:hover { background: var(--kb-primary-soft); color: var(--kb-primary); }
</style>
