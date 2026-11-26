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
    :title="mode === 'create' ? '新建' : '编辑'"
    width="440px"
    :close-on-click-modal="false"
    @update:model-value="emit('update:visible', Boolean($event))"
  >
    <el-form :model="form" label-width="90px">
      <el-form-item label="区域名称" required>
        <el-input v-model="form.areaName" maxlength="128" placeholder="请输入" />
      </el-form-item>
      <el-form-item label="绘制面积" required>
        <span class="readonly-area">{{ (drawAreaMu ?? 0).toFixed(2) }}亩</span>
      </el-form-item>
      <el-form-item v-if="mode === 'create'" label="边界">
        <div class="boundary-row">
          <span class="point-hint">已绘制 {{ boundaryPoints.length }} 个边界点</span>
          <el-button link type="primary" @click="emit('clear-boundary')">清空边界数据</el-button>
        </div>
      </el-form-item>
      <el-form-item label="标注面积">
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
      <el-button @click="emit('update:visible', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="emit('save')">保存</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.readonly-area { color: #606266; font-size: 14px; font-weight: 600; }
.boundary-row { display: flex; align-items: center; justify-content: space-between; width: 100%; }
.point-hint { color: #909399; font-size: 12px; }
</style>
