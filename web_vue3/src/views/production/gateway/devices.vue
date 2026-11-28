<script setup lang="ts">
import ConfigWorkspace from './components/ConfigWorkspace.vue';
import { configDevice } from '@/api/iot/gatewayManage';
</script>

<template>
  <ConfigWorkspace
    :title="$t('gatewayManage.title')"
    :description="$t('gatewayManage.deviceConfig')"
    :config-title="$t('gatewayManage.deviceConfig')"
    :report-type="20"
    :send="configDevice"
  />
</template>
