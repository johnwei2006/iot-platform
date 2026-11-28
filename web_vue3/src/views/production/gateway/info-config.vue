<script setup lang="ts">
import { onMounted, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { ElMessage, ElMessageBox } from 'element-plus';
import GatewayConsole from './components/GatewayConsole.vue';
import GatewayInfoPanel from './components/GatewayInfoPanel.vue';
import GatewayInfoHistory from './components/GatewayInfoHistory.vue';
import GatewayConfigForm from './components/GatewayConfigForm.vue';
import GatewaySendLogTable from './components/GatewaySendLogTable.vue';
import {
  GATEWAY_CONFIG_FIELDS,
  configRadio,
  getGatewayInfo,
  listGatewayCommands,
  listGatewayConfigs,
  listGatewayInfoHistory,
  listGatewayReports,
  parseGatewayPayload,
  rebootGateway,
  type GatewayCommandRecord,
  type GatewayConfigPayload,
  type GatewayConfigRecord,
  type GatewayInfoHistoryRecord,
  type GatewayInfoPayload,
  type GatewayReportRecord,
} from '@/api/iot/gatewayManage';

const { t: $t } = useI18n();
const gatewayCode = ref('');
const infoHistory = ref<GatewayInfoHistoryRecord[]>([]);
const infoHistoryPage = ref(1);
const infoHistorySize = ref(10);
const infoHistoryTotal = ref(0);
const infoHistoryLoading = ref(false);
const configReports = ref<GatewayReportRecord[]>([]);
const configRecords = ref<GatewayConfigRecord[]>([]);
const commands = ref<GatewayCommandRecord[]>([]);
const config = ref<GatewayConfigPayload>({});
const info = ref<GatewayInfoPayload>({});
const infoReportedAt = ref<string>();
const sending = ref(false);

async function loadGatewayInfo() {
  try {
    const res = await getGatewayInfo(gatewayCode.value);
    const { reportedAt, ...fields } = res.data ?? {};
    info.value = fields;
    infoReportedAt.value = reportedAt;
  } catch (error) {
    console.error('gateway latest info load failed', error);
  }
}

async function loadInfoHistory() {
  infoHistoryLoading.value = true;
  try {
    const res = await listGatewayInfoHistory({
      gatewayCode: gatewayCode.value,
      page: infoHistoryPage.value,
      size: infoHistorySize.value,
    });
    infoHistory.value = res.data?.records ?? [];
    infoHistoryTotal.value = res.data?.total ?? 0;
  } catch (error) {
    console.error('gateway infoHistory load failed', error);
  } finally {
    infoHistoryLoading.value = false;
  }
}

async function loadConfig() {
  try {
    const [configRes, reportRes] = await Promise.all([
      listGatewayConfigs({ gatewayCode: gatewayCode.value, limit: 100 }),
      listGatewayReports({ gatewayCode: gatewayCode.value, serviceType: 8, limit: 20 }),
    ]);
    configRecords.value = configRes.data ?? [];
    configReports.value = reportRes.data ?? [];
    applyLatestConfig();
  } catch (error) {
    console.error('gateway config load failed', error);
  }
}

function applyLatestConfig() {
  const pending = configRecords.value.find(
    (record) => record.status === 'PENDING' || record.status === 'FAILED');
  const success = configRecords.value.find((record) => record.status === 'SUCCESS');
  const source = pending ?? success;
  if (!source) return applyConfigReport();
  const whitelisted: GatewayConfigPayload = {};
  for (const key of GATEWAY_CONFIG_FIELDS) {
    const value = source[key];
    if (value !== undefined && value !== null) Object.assign(whitelisted, { [key]: value });
  }
  config.value = whitelisted;
}

async function loadCommands() {
  try {
    const commandRes = await listGatewayCommands({ gatewayCode: gatewayCode.value, serviceType: 9, limit: 100 });
    commands.value = commandRes.data ?? [];
  } catch (error) {
    console.error('gateway commands load failed', error);
  }
}

async function load() {
  if (!gatewayCode.value) return;
  await Promise.all([loadGatewayInfo(), loadInfoHistory(), loadConfig(), loadCommands()]);
}

function onInfoHistoryChange(page: number, size: number) {
  infoHistoryPage.value = page;
  infoHistorySize.value = size;
  void loadInfoHistory();
}

function applyConfigReport() {
  const latest = configReports.value[0];
  if (!latest?.payload) return;
  const decoded = parseGatewayPayload<GatewayConfigPayload>(latest.payload);
  const whitelisted: GatewayConfigPayload = {};
  for (const key of GATEWAY_CONFIG_FIELDS) {
    const value = decoded[key];
    if (value !== undefined && value !== null) Object.assign(whitelisted, { [key]: value });
  }
  config.value = whitelisted;
}

async function send() {
  if (!gatewayCode.value) return ElMessage.warning($t('gatewayManage.selectGatewayRequired'));
  try {
    await ElMessageBox.confirm(
      $t('gatewayManage.sendConfigConfirm'),
      $t('gatewayManage.send'),
      { type: 'warning' },
    );
  } catch (error) {
    console.error('config send cancelled', error);
    return;
  }
  sending.value = true;
  try {
    await configRadio({
      gatewayCode: gatewayCode.value,
      deviceId: gatewayCode.value,
      requestId: `GWCFG-${Date.now()}`,
      serviceDataDecoded: { ...config.value },
    });
    ElMessage.success($t('gatewayManage.sent'));
    await load();
  } catch (error) {
    ElMessage.error((error as Error).message || $t('common.failed'));
  } finally {
    sending.value = false;
  }
}

async function reboot() {
  if (!gatewayCode.value) return ElMessage.warning($t('gatewayManage.selectGatewayRequired'));
  try {
    await ElMessageBox.confirm($t('gatewayManage.rebootConfirm'), $t('gatewayManage.reboot'), { type: 'warning' });
  } catch (error) {
    console.error('reboot cancelled', error);
    return;
  }
  await rebootGateway({ gatewayCode: gatewayCode.value, deviceId: gatewayCode.value, appType: 1 });
  ElMessage.success($t('gatewayManage.rebooted'));
}

watch(gatewayCode, () => {
  infoHistoryPage.value = 1;
  load();
});
onMounted(load);
</script>

<template>
  <GatewayConsole v-model:gateway-code="gatewayCode" :title="$t('gatewayManage.infoConfigTitle')"
    :description="$t('gatewayManage.infoConfigDesc')">
    <div class="quad-grid">
      <section class="panel quadrant info-quadrant">
        <h3 class="panel-title">{{ $t('gatewayManage.gatewayInfo') }}</h3>
        <GatewayInfoPanel :info="info" :reported-at="infoReportedAt" />
      </section>
      <section class="panel quadrant config-quadrant">
        <h3 class="panel-title">{{ $t('gatewayManage.config') }}</h3>
        <GatewayConfigForm v-model="config" :sending="sending" @send="send" @reboot="reboot" />
      </section>
      <section class="panel quadrant history-quadrant">
        <h3 class="panel-title">{{ $t('gatewayManage.infoHistory') }}</h3>
        <GatewayInfoHistory
          :records="infoHistory"
          :total="infoHistoryTotal"
          :page="infoHistoryPage"
          :size="infoHistorySize"
          :loading="infoHistoryLoading"
          @change="onInfoHistoryChange"
        />
      </section>
      <section class="panel quadrant sendlog-quadrant">
        <h3 class="panel-title">{{ $t('gatewayManage.sendRecord') }}</h3>
        <GatewaySendLogTable :commands="commands" />
      </section>
    </div>
  </GatewayConsole>
</template>

<style scoped>
.quad-grid {
  display: grid;
  grid-template-columns: minmax(0, 0.92fr) minmax(0, 1.08fr);
  grid-template-areas: 'info config' 'history sendlog';
  gap: 16px;
  align-items: stretch;
}
.info-quadrant { grid-area: info; }
.config-quadrant { grid-area: config; }
.history-quadrant { grid-area: history; }
.sendlog-quadrant { grid-area: sendlog; }
.quadrant { min-width: 0; min-height: 0; }
@media (max-width: 1100px) {
  .quad-grid { grid-template-columns: minmax(0, 1fr); grid-template-areas: 'info' 'config' 'history' 'sendlog'; }
}
</style>
