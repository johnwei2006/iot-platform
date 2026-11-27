<script setup lang="ts">
import { computed, ref } from 'vue';
import * as ElementPlusIconsVue from '@element-plus/icons-vue';
import type { Component } from 'vue';

const props = defineProps<{ modelValue?: string }>();
const emit = defineEmits<{ (e: 'update:modelValue', value: string): void }>();

const icons = ElementPlusIconsVue as unknown as Record<string, Component>;
const keyword = ref('');
const pickerVisible = ref(false);

const names = Object.keys(icons).filter((name) => name !== 'default').sort();
const filtered = computed(() => {
  const word = keyword.value.trim().toLowerCase();
  return word ? names.filter((name) => name.toLowerCase().includes(word)) : names;
});

function pick(name: string): void {
  emit('update:modelValue', name);
  pickerVisible.value = false;
}

function clear(): void {
  emit('update:modelValue', '');
}
</script>
<template>
  <div class="icon-picker">
    <el-popover v-model:visible="pickerVisible" placement="bottom-start" :width="380" trigger="click">
      <template #reference>
        <button type="button" class="picker-trigger" :aria-label="$t('system.pickIcon')">
          <el-icon v-if="props.modelValue" :size="18"><component :is="icons[props.modelValue]" /></el-icon>
          <span>{{ props.modelValue || $t('system.pickIcon') }}</span>
        </button>
      </template>
      <el-input v-model="keyword" size="small" clearable :placeholder="$t('system.searchIcon')" />
      <div class="icon-grid">
        <button
          v-for="name in filtered"
          :key="name"
          type="button"
          class="icon-item"
          :class="{ active: props.modelValue === name }"
          :title="name"
          @click="pick(name)"
        >
          <el-icon :size="18"><component :is="icons[name]" /></el-icon>
          <span>{{ name }}</span>
        </button>
        <div v-if="!filtered.length" class="empty">{{ $t('system.noIconMatch') }}</div>
      </div>
    </el-popover>
    <el-button v-if="props.modelValue" link type="danger" size="small" @click="clear">{{ $t('system.clearIcon') }}</el-button>
  </div>
</template>
<style scoped>
.icon-picker { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.picker-trigger {
  display: inline-flex; align-items: center; gap: 8px; min-width: 160px;
  padding: 7px 12px; border: 1px solid var(--kb-line); border-radius: var(--kb-radius-btn);
  background: var(--kb-bg-card); color: var(--kb-text); cursor: pointer; font: inherit; font-size: var(--kb-fs-body);
}
.picker-trigger:hover { border-color: var(--kb-brand-bright); }
.icon-grid {
  display: grid; grid-template-columns: repeat(6, 1fr); gap: 6px;
  max-height: 260px; overflow: auto; margin-top: 10px;
}
.icon-item {
  display: flex; flex-direction: column; align-items: center; gap: 4px;
  padding: 8px 2px; border: 1px solid transparent; border-radius: 8px;
  background: var(--kb-bg-sunken); color: var(--kb-text-muted); cursor: pointer; font-size: 10px;
}
.icon-item:hover, .icon-item.active { color: var(--kb-brand); border-color: var(--kb-brand-bright); background: var(--kb-brand-wash); }
.icon-item span { max-width: 54px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.empty { grid-column: 1 / -1; text-align: center; padding: 18px; color: var(--kb-text-muted); }
</style>
