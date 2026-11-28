<script setup lang="ts">
import { reactive, watch } from 'vue';
import type { GatewayConfigPayload } from '@/api/iot/gatewayManage';

const props = defineProps<{ modelValue: GatewayConfigPayload; sending?: boolean }>();
const emit = defineEmits<{
  'update:modelValue': [value: GatewayConfigPayload];
  send: [];
  reboot: [];
}>();

type ConfigForm = Required<GatewayConfigPayload>;
const defaults: ConfigForm = {
  configVersion: 1, gatewayId: '', projectId: '', systemServerUrl: '', systemServerPort: 8080,
  businessServerUrl: '', heartbeatInterval: 30, reportInterval: 60, loraFreqGroup: 1,
};
const form = reactive<ConfigForm>({ ...defaults, ...props.modelValue });
let applying = false;

watch(() => props.modelValue, (value) => {
  applying = true;
  Object.assign(form, defaults, value);
  applying = false;
}, { deep: true });

watch(form, (value) => {
  if (!applying) emit('update:modelValue', { ...value });
}, { deep: true });
</script>

<template>
  <div class="config-form">
    <el-alert :title="$t('gatewayManage.loraTimeNote')" type="info" :closable="false" show-icon />
    <el-form :model="form" label-width="140px" class="form">
      <el-form-item :label="$t('gatewayManage.configFields.configVersion')">
        <el-input-number v-model="form.configVersion" :min="0" :max="255" controls-position="right" />
      </el-form-item>
      <el-form-item :label="$t('gatewayManage.configFields.gatewayId')">
        <el-input v-model="form.gatewayId" maxlength="11" show-word-limit />
      </el-form-item>
      <el-form-item :label="$t('gatewayManage.configFields.projectId')">
        <el-input v-model="form.projectId" maxlength="11" show-word-limit />
      </el-form-item>
      <el-form-item :label="$t('gatewayManage.configFields.systemServerUrl')">
        <el-input v-model="form.systemServerUrl" maxlength="40" />
      </el-form-item>
      <el-form-item :label="$t('gatewayManage.configFields.systemServerPort')">
        <el-input-number v-model="form.systemServerPort" :min="1" :max="65535" controls-position="right" />
      </el-form-item>
      <el-form-item :label="$t('gatewayManage.configFields.businessServerUrl')">
        <el-input v-model="form.businessServerUrl" maxlength="30" />
      </el-form-item>
      <el-form-item :label="$t('gatewayManage.configFields.heartbeatInterval')">
        <el-input-number v-model="form.heartbeatInterval" :min="1" :max="65535" controls-position="right" />
      </el-form-item>
      <el-form-item :label="$t('gatewayManage.configFields.reportInterval')">
        <el-input-number v-model="form.reportInterval" :min="1" :max="65535" controls-position="right" />
      </el-form-item>
      <el-form-item :label="$t('gatewayManage.configFields.loraFreqGroup')">
        <el-input-number v-model="form.loraFreqGroup" :min="0" :max="255" controls-position="right" />
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
