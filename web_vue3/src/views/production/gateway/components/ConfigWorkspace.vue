<script setup lang="ts">
import { ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import GatewayConsole from './GatewayConsole.vue';
import ReportTimeline from './ReportTimeline.vue';
import CommandLogTable from './CommandLogTable.vue';
import { listGatewayCommands, listGatewayReports, type GatewayCommandRecord, type GatewayReportRecord } from '@/api/iot/gatewayManage';

const props = defineProps<{
  title: string; description: string; configTitle: string;
  reportType: number; send: (payload: Record<string, unknown>) => Promise<unknown>;
}>();

const gatewayCode = ref('');
const reports = ref<GatewayReportRecord[]>([]);
const commands = ref<GatewayCommandRecord[]>([]);
const json = ref('{}');
const jsonInvalid = ref(false);

async function load() {
  if (!gatewayCode.value) return;
  try {
    const [reportRes, commandRes] = await Promise.all([
      listGatewayReports({ gatewayCode: gatewayCode.value, serviceType: props.reportType, limit: 100 }),
      listGatewayCommands({ gatewayCode: gatewayCode.value, limit: 100 }),
    ]);
    reports.value = reportRes.data ?? [];
    commands.value = commandRes.data ?? [];
  } catch (error) {
    console.error('gateway config load failed', error);
  }
}

function useReport(row: GatewayReportRecord) {
  try { json.value = JSON.stringify(JSON.parse(row.payload), null, 2); jsonInvalid.value = false; }
  catch (error) { console.error('invalid report payload', error); }
}

async function send() {
  if (!gatewayCode.value) return ElMessage.warning('gatewayManage.selectGatewayRequired');
  let decoded: Record<string, unknown>;
  try {
    decoded = JSON.parse(json.value);
    if (!decoded || typeof decoded !== 'object' || Array.isArray(decoded)) throw new Error('payload must be an object');
    jsonInvalid.value = false;
  } catch (error) {
    console.error('invalid config json', error);
    jsonInvalid.value = true;
    return ElMessage.error('gatewayManage.jsonHint');
  }
  await props.send({ gatewayCode: gatewayCode.value, serviceDataDecoded: decoded });
  ElMessage.success('gatewayManage.sent');
  await load();
}

watch(gatewayCode, load);
</script>

<template>
  <GatewayConsole v-model:gateway-code="gatewayCode" :title="title" :description="description">
    <div class="split">
      <div class="panel">
        <h3 class="panel-title">{{ configTitle }}</h3>
        <el-alert :title="$t('gatewayManage.jsonHint')" type="info" :closable="false" show-icon class="hint" />
        <el-input v-model="json" type="textarea" :rows="16" :class="{ invalid: jsonInvalid }" spellcheck="false" />
        <div class="actions">
          <el-button type="primary" @click="send">{{ $t('gatewayManage.send') }}</el-button>
          <el-button @click="load">{{ $t('gatewayManage.refresh') }}</el-button>
        </div>
      </div>
      <div class="panel">
        <h3 class="panel-title">{{ $t('gatewayManage.reports') }}</h3>
        <el-table :data="reports" size="small" @row-click="useReport" highlight-current-row>
          <el-table-column :label="$t('gatewayManage.reportTime')" width="165">
            <template #default="{ row }">{{ new Date(row.reportedAt || row.createdAt || '').toLocaleString() }}</template>
          </el-table-column>
          <el-table-column :label="$t('gatewayManage.category')" prop="category" width="130" />
          <el-table-column :label="$t('gatewayManage.payload')" min-width="200" show-overflow-tooltip prop="payload" />
          <template #empty><div class="empty">{{ $t('gatewayManage.noData') }}</div></template>
        </el-table>
        <ReportTimeline :reports="reports" style="margin-top: 14px" />
      </div>
    </div>
    <div class="panel" style="margin-top: 16px">
      <h3 class="panel-title">{{ $t('gatewayManage.commands') }}</h3>
      <CommandLogTable :commands="commands" />
    </div>
  </GatewayConsole>
</template>

<style scoped>
.hint { margin-bottom: 12px; }
.actions { margin-top: 12px; display: flex; gap: 8px; }
.invalid :deep(textarea) { border-color: var(--el-color-danger); }
.empty { padding: 24px; color: var(--el-text-color-secondary); text-align: center; }
</style>
