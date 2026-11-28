<script setup lang="ts">
import { computed, type WritableComputedRef } from 'vue';
import type { GatewayConfigPayload } from '@/api/iot/gatewayManage';

const model = defineModel<GatewayConfigPayload>({ default: () => ({}) });
defineProps<{ sending?: boolean }>();
const emit = defineEmits<{
  send: [];
  reboot: [];
}>();

type ConfigForm = Required<GatewayConfigPayload>;
const defaults: ConfigForm = {
  configVersion: 1, gatewayId: '', projectId: '', systemServerUrl: '', systemServerPort: 8080,
  businessServerUrl: '', heartbeatInterval: 30, reportInterval: 60, loraFreqGroup: 1,
};

/** 表单完全由父级 modelValue 驱动，避免本地状态与异步预填互相回写。 */
function field<K extends keyof ConfigForm>(key: K): WritableComputedRef<ConfigForm[K]> {
  return computed({
    get: () => (model.value[key] ?? defaults[key]) as ConfigForm[K],
    set: (value: ConfigForm[K]) => { model.value = { ...model.value, [key]: value }; },
  });
}

const configVersion = field('configVersion');
const gatewayId = field('gatewayId');
const projectId = field('projectId');
const systemServerUrl = field('systemServerUrl');
const systemServerPort = field('systemServerPort');
const businessServerUrl = field('businessServerUrl');
const heartbeatInterval = field('heartbeatInterval');
const reportInterval = field('reportInterval');
const loraFreqGroup = field('loraFreqGroup');
</script>

<template>
  <div class="config-form">
    <el-alert :title="$t('gatewayManage.loraTimeNote')" type="info" :closable="false" show-icon />
    <el-form label-width="140px" class="form">
      <el-form-item :label="$t('gatewayManage.configFields.configVersion')">
        <el-input-number v-model="configVersion" :min="0" :max="255" controls-position="right" />
      </el-form-item>
      <el-form-item :label="$t('gatewayManage.configFields.gatewayId')">
        <el-input v-model="gatewayId" maxlength="11" show-word-limit />
      </el-form-item>
      <el-form-item :label="$t('gatewayManage.configFields.projectId')">
        <el-input v-model="projectId" maxlength="11" show-word-limit />
      </el-form-item>
      <el-form-item :label="$t('gatewayManage.configFields.systemServerUrl')">
        <el-input v-model="systemServerUrl" maxlength="40" />
      </el-form-item>
      <el-form-item :label="$t('gatewayManage.configFields.systemServerPort')">
        <el-input-number v-model="systemServerPort" :min="1" :max="65535" controls-position="right" />
      </el-form-item>
      <el-form-item :label="$t('gatewayManage.configFields.businessServerUrl')">
        <el-input v-model="businessServerUrl" maxlength="30" />
      </el-form-item>
      <el-form-item :label="$t('gatewayManage.configFields.heartbeatInterval')">
        <el-input-number v-model="heartbeatInterval" :min="1" :max="65535" controls-position="right" />
      </el-form-item>
      <el-form-item :label="$t('gatewayManage.configFields.reportInterval')">
        <el-input-number v-model="reportInterval" :min="1" :max="65535" controls-position="right" />
      </el-form-item>
      <el-form-item :label="$t('gatewayManage.configFields.loraFreqGroup')">
        <el-input-number v-model="loraFreqGroup" :min="0" :max="255" controls-position="right" />
      </el-form-item>
    </el-form>
    <div class="actions">
      <el-button type="primary" :loading="sending" @click="emit('send')">{{ $t('gatewayManage.send') }}</el-button>
      <el-button type="danger" plain :disabled="sending" @click="emit('reboot')">{{ $t('gatewayManage.reboot') }}</el-button>
    </div>
  </div>
</template>

<style scoped>
.config-form { display: flex; flex-direction: column; gap: 12px; min-height: 0; }
.form { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); column-gap: 16px; }
.form :deep(.el-form-item) { margin-bottom: 14px; }
.actions { display: flex; gap: 8px; justify-content: flex-end; }
@media (max-width: 760px) { .form { grid-template-columns: minmax(0, 1fr); } }
</style>
