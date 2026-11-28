<script setup lang="ts">
import { onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { listProductionGateways } from '@/api/iot/production';
import type { Device } from '@/types';

const model = defineModel<string>();
const route = useRoute();
const gateways = ref<Device[]>([]);
const dropdownVisible = ref(false);
const selectRef = ref<{ blur?: () => void } | null>(null);

function closeDropdown() {
  dropdownVisible.value = false;
  selectRef.value?.blur?.();
}

watch(model, closeDropdown);

onMounted(async () => {
  try {
    const res = await listProductionGateways();
    gateways.value = res.data ?? [];
    const fromRoute = typeof route.query.gatewayCode === 'string' ? route.query.gatewayCode : '';
    if (fromRoute) model.value = fromRoute;
    else if (!model.value && gateways.value.length) model.value = gateways.value[0].deviceId;
  } catch (error) {
    console.error('gateway selector load failed', error);
  }
});
</script>

<template>
  <el-select ref="selectRef" v-model="model" v-model:visible="dropdownVisible" filterable
    :placeholder="$t('gatewayManage.selectGateway')" class="picker" @change="closeDropdown">
    <el-option v-for="g in gateways" :key="g.deviceId" :value="g.deviceId"
      :label="`${g.deviceId} · ${g.name || '--'}`" @click="closeDropdown" />
  </el-select>
</template>

<style scoped>
.picker { width: 320px; }
@media (max-width: 900px) { .picker { width: 100%; min-width: 220px; } }
</style>
