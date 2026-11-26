<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import DeviceControlPanel from './components/device-control-panel.vue';
import RuleControlPanel from './components/rule-control-panel.vue';
import ControlLogPanel from './components/control-log-panel.vue';

const route = useRoute();
const router = useRouter();

type Tab = 'oneClick' | 'smart' | 'logs';
const validTabs: Tab[] = ['oneClick', 'smart', 'logs'];
const active = ref<Tab>(validTabs.includes(route.query.tab as Tab) ? (route.query.tab as Tab) : 'oneClick');
const areaId = ref<string>((route.query.areaId as string) || '');

function switchTab(tab: Tab) {
  active.value = tab;
  router.replace({
    path: '/iot/control',
    query: tab === 'logs' && areaId.value ? { tab, areaId: areaId.value } : { tab },
  });
}

watch(() => route.query.tab, (tab) => {
  if (validTabs.includes(tab as Tab)) active.value = tab as Tab;
});
watch(() => route.query.areaId, (id) => {
  areaId.value = (id as string) || '';
});

const tabs = computed(() => [
  { key: 'oneClick' as Tab, label: '一键控制' },
  { key: 'smart' as Tab, label: '智能控制' },
  { key: 'logs' as Tab, label: '控制记录' },
]);
</script>

<template>
  <div class="control-page">
    <div class="top-tabs" role="tablist">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        type="button"
        role="tab"
        class="top-tab"
        :class="{ active: active === tab.key }"
        :aria-selected="active === tab.key"
        @click="switchTab(tab.key)"
      >{{ tab.label }}</button>
    </div>
    <div class="tab-body">
      <DeviceControlPanel v-if="active === 'oneClick'" @show-logs="(id) => { areaId = id || ''; switchTab('logs'); }" />
      <RuleControlPanel v-else-if="active === 'smart'" />
      <ControlLogPanel v-else :area-id="areaId" />
    </div>
  </div>
</template>

<style scoped>
.control-page { min-height: 100%; background: var(--kb-bg); }
.top-tabs { display: flex; gap: 28px; align-items: center; height: 44px; padding: 0 2px; border-bottom: 1px solid var(--kb-line); background: var(--kb-bg); }
.top-tab { position: relative; padding: 0 2px; height: 44px; border: 0; background: transparent; cursor: pointer; color: var(--kb-text-muted); font-size: var(--kb-fs-body); font-weight: 500; transition: color 0.2s ease; }
.top-tab:hover { color: var(--kb-primary); }
.top-tab.active { color: var(--kb-primary); font-weight: 700; }
.top-tab.active::after { content: ''; position: absolute; left: 0; right: 0; bottom: 0; height: 2px; border-radius: 2px 2px 0 0; background: var(--kb-primary); }
.tab-body { padding-top: 16px; }
</style>
