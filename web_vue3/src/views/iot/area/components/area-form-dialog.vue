<script setup lang="ts">
import type { AreaPoint } from '@/api/iot/area';

defineProps<{
  visible: boolean;
  mode: 'create' | 'edit';
  form: { areaName: string; labelAreaMu?: number };
  drawAreaMu?: number;
  boundaryPoints: AreaPoint[];
  saving: boolean;
}>();

const emit = defineEmits<{
  'update:visible': [value: boolean];
  save: [];
  'clear-boundary': [];
}>();
</script>

<template>
  <el-dialog
    :model-value="visible"
    :title="mode === 'create' ? $t('area.createTitle') : $t('area.editTitle')"
    width="440px"
    :close-on-click-modal="false"
    @update:model-value="emit('update:visible', Boolean($event))"
  >
    <el-form :model="form" label-width="90px">
      <el-form-item :label="$t('area.name')" required>
        <el-input v-model="form.areaName" maxlength="128" :placeholder="$t('area.namePlaceholder')" />
      </el-form-item>
      <el-form-item :label="$t('area.drawArea')" required>
        <span class="readonly-area">{{ (drawAreaMu ?? 0).toFixed(2) }}{{ $t('area.mu') }}</span>
      </el-form-item>
      <el-form-item v-if="mode === 'create'" :label="$t('area.boundary')">
        <div class="boundary-row">
          <span class="point-hint">{{ $t('area.boundaryPoints', { count: boundaryPoints.length }) }}</span>
          <el-button link type="primary" @click="emit('clear-boundary')">{{ $t('area.clearBoundary') }}</el-button>
        </div>
      </el-form-item>
      <el-form-item :label="$t('area.labelArea')">
        <el-input-number
          v-model="form.labelAreaMu"
          :min="0"
          :precision="2"
          :step="0.1"
          controls-position="right"
          style="width: 100%"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="emit('update:visible', false)">{{ $t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="saving" @click="emit('save')">{{ $t('common.save') }}</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.readonly-area { color: #606266; font-size: 14px; font-weight: 600; }
.boundary-row { display: flex; align-items: center; justify-content: space-between; width: 100%; }
.point-hint { color: #909399; font-size: 12px; }
</style>
