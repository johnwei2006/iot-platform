<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { i18n } from '@/locales';
import { listAreas, type AreaEntity } from '@/api/iot/area';

const props = withDefaults(defineProps<{
  modelValue?: string;
  placeholder?: string;
  clearable?: boolean;
  disabled?: boolean;
  width?: string;
}>(), {
  modelValue: undefined,
  placeholder: () => i18n.global.t('area.selectArea'),
  clearable: true,
  disabled: false,
  width: '100%',
});

const emit = defineEmits<{
  'update:modelValue': [value: string | undefined];
  change: [area: AreaEntity | undefined];
}>();

const areas = ref<AreaEntity[]>([]);
const loading = ref(false);

const selected = computed<string | undefined>({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
});

const nameMap = computed(() => new Map(areas.value.map((a) => [a.id, a.areaName])));

async function load() {
  loading.value = true;
  try {
    const res = await listAreas();
    areas.value = res.data || [];
  } catch (error) {
    console.error('[AreaSelect] 区域列表加载失败:', error);
  } finally {
    loading.value = false;
  }
}

function onChange(id: string | undefined) {
  emit('change', id ? areas.value.find((a) => a.id === id) : undefined);
}

defineExpose({
  nameOf: (id?: string) => (id ? nameMap.value.get(id) || '' : ''),
  refresh: load,
});

onMounted(load);
</script>

<template>
  <el-select
    v-model="selected"
    filterable
    :clearable="clearable"
    :disabled="disabled"
    :loading="loading"
    :placeholder="placeholder"
    :style="{ width }"
    @change="onChange"
  >
    <el-option v-for="a in areas" :key="a.id" :label="a.areaName" :value="a.id" />
  </el-select>
</template>
