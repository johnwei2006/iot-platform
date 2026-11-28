<script setup lang="ts">
import ConfigWorkspace from './components/ConfigWorkspace.vue';
import { configSensor } from '@/api/iot/gatewayManage';
</script>

<template>
  <ConfigWorkspace
    :title="$t('gatewayManage.title')"
    :description="$t('gatewayManage.sensorConfig')"
    :config-title="$t('gatewayManage.sensorConfig')"
    :report-type="16"
    :send="configSensor"
  />
</template>
