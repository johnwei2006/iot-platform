<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import GatewaySelector from './GatewaySelector.vue';

defineProps<{ gatewayCode: string; title: string; description?: string }>();
defineEmits<{ 'update:gatewayCode': [string] }>();
const { t } = useI18n();
</script>

<template>
  <section class="gateway-console">
    <header class="console-header">
      <div>
        <h2>{{ title }}</h2>
      </div>
      <div class="selector-group">
        <span class="selector-label">{{ t('gatewayManage.selectGateway') }}：</span>
        <GatewaySelector :model-value="gatewayCode" @update:model-value="(code: string | undefined) => code && $emit('update:gatewayCode', code)" />
      </div>
    </header>
    <div class="console-body">
      <slot />
    </div>
  </section>
</template>

<style scoped>
.gateway-console { min-height: calc(100vh - 150px); }
.console-header {
  display: flex; align-items: center; justify-content: space-between; gap: 24px;
  padding: 18px 22px; margin-bottom: 16px; border: 1px solid #d9e3f0;
  border-radius: 12px; background: linear-gradient(120deg, #ffffff 0%, #eef5ff 100%);
  box-shadow: 0 6px 18px rgba(15, 76, 145, 0.06);
}
h2 { margin: 0; font-size: 20px; font-weight: 650; color: #1f2937; }
.selector-group { display: flex; align-items: center; gap: 8px; }
.selector-label { color: #4b5563; font-size: 13px; white-space: nowrap; }
.console-body :deep(.panel) {
  border: 1px solid #e4eaf2; border-radius: 12px; background: #fff;
  box-shadow: 0 2px 10px rgba(31, 41, 55, .04); padding: 16px;
}
.console-body :deep(.panel-title) {
  margin: 0 0 14px; font-size: 15px; font-weight: 600; color: #1f2937;
}
.console-body :deep(.split) { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: 16px; }
@media (max-width: 1100px) {
  .console-header { flex-direction: column; align-items: stretch; }
  .console-body :deep(.split) { grid-template-columns: minmax(0, 1fr); }
}
</style>
