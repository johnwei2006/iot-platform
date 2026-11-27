<script setup lang="ts">
import { ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { ElMessage } from 'element-plus';
import { assignOrgProducts, fetchOrgProducts } from '@/api/system/org';
import { fetchProducts } from '@/api/system/product';
import type { ProductRecord } from '@/types';

const { t } = useI18n();
const props = defineProps<{ modelValue: boolean; orgId: string; orgName: string }>();
const emit = defineEmits<{ (e: 'update:modelValue', value: boolean): void; (e: 'saved'): void }>();

const products = ref<ProductRecord[]>([]);
const selected = ref<string[]>([]);
const loading = ref(false);

async function load() {
  if (!props.orgId) return;
  loading.value = true;
  try {
    const [all, assigned] = await Promise.all([fetchProducts(), fetchOrgProducts(props.orgId)]);
    products.value = all.filter((p) => !p.status || p.status === 'ENABLED');
    selected.value = assigned.map((p) => p.id);
  } catch (error) {
    ElMessage.error((error as Error).message || t('system.assignLoadFailed'));
  } finally {
    loading.value = false;
  }
}

async function save() {
  try {
    await assignOrgProducts(props.orgId, selected.value);
    ElMessage.success(t('system.assignSaved'));
    emit('saved');
    emit('update:modelValue', false);
  } catch (error) {
    ElMessage.error((error as Error).message || t('gateway.saveFailed'));
  }
}

watch(() => props.modelValue, (visible) => { if (visible) void load(); });
</script>
<template>
  <el-dialog
    :model-value="modelValue"
    :title="`${$t('org.assignProducts')} · ${orgName}`"
    width="520px"
    @update:model-value="(v: boolean) => emit('update:modelValue', v)"
  >
    <div v-loading="loading" class="assign-body">
      <p class="tip">{{ $t('system.assignTip') }}</p>
      <el-checkbox-group v-model="selected" class="options">
        <el-checkbox v-for="p in products" :key="p.id" :value="p.id" border class="option">
          <strong>{{ p.name }}</strong>
          <span class="code">{{ p.code }}</span>
        </el-checkbox>
      </el-checkbox-group>
      <div v-if="!products.length && !loading" class="empty">{{ $t('system.noProducts') }}</div>
    </div>
    <template #footer>
      <el-button @click="emit('update:modelValue', false)">{{ $t('common.cancel') }}</el-button>
      <el-button type="primary" @click="save">{{ $t('system.saveAssign') }}</el-button>
    </template>
  </el-dialog>
</template>
<style scoped>
.assign-body { min-height: 140px; }
.tip { color: var(--kb-text-muted); font-size: var(--kb-fs-caption); line-height: 1.6; margin: 0 0 12px; }
.options { display: flex; flex-direction: column; gap: 8px; }
.option { width: 100%; margin: 0; border-radius: var(--kb-radius-btn); }
.code { color: var(--kb-text-muted); font-size: 12px; margin-left: 8px; }
.empty { padding: 20px; text-align: center; color: var(--kb-text-muted); }
</style>
