<script setup lang="ts">
import { computed, reactive, watch } from 'vue';
import type { GatewayTaskEntity } from '@/api/iot/gatewayManage';

const props = defineProps<{ modelValue: GatewayTaskEntity | null }>();
const emit = defineEmits<{ 'update:modelValue': [GatewayTaskEntity] }>();

const form = reactive<GatewayTaskEntity>({
  gatewayCode: '', taskName: '', taskNo: 1, enabled: true,
  triggerType: 'TIME', triggerTime: '07:00:00', triggers: '[]', actions: '[]',
});

watch(() => props.modelValue, (task) => {
  if (!task) return;
  Object.assign(form, task, {
    triggers: task.triggers || '[]',
    actions: task.actions || '[]',
    triggerTime: task.triggerTime || '07:00:00',
  });
}, { immediate: true });

const invalidJson = computed(() => {
  try { JSON.parse(form.triggers || '[]'); JSON.parse(form.actions || '[]'); return false; }
  catch (error) { console.error('invalid task json', error); return true; }
});

function submit() {
  emit('update:modelValue', { ...form });
}
</script>

<template>
  <el-form :model="form" label-width="110px">
    <el-form-item :label="$t('gatewayManage.taskNo')" required>
      <el-input-number v-model="form.taskNo" :min="1" :max="255" />
    </el-form-item>
    <el-form-item :label="$t('gatewayManage.taskName')" required>
      <el-input v-model="form.taskName" maxlength="128" />
    </el-form-item>
    <el-form-item :label="$t('gatewayManage.enabled')">
      <el-switch v-model="form.enabled" />
    </el-form-item>
    <el-form-item :label="$t('gatewayManage.triggerType')">
      <el-select v-model="form.triggerType" style="width: 180px">
        <el-option value="TIME" label="TIME" />
        <el-option value="CONDITION" label="CONDITION" />
      </el-select>
    </el-form-item>
    <el-form-item :label="$t('gatewayManage.triggerTime')">
      <el-time-picker v-model="form.triggerTime" format="HH:mm:ss" value-format="HH:mm:ss" />
    </el-form-item>
    <el-form-item :label="$t('gatewayManage.triggers')" :error="invalidJson ? $t('gatewayManage.jsonHint') : ''">
      <el-input v-model="form.triggers" type="textarea" :rows="3" />
    </el-form-item>
    <el-form-item :label="$t('gatewayManage.actions')" :error="invalidJson ? $t('gatewayManage.jsonHint') : ''">
      <el-input v-model="form.actions" type="textarea" :rows="4" />
    </el-form-item>
    <el-form-item>
      <el-button type="primary" :disabled="invalidJson || !form.taskName" @click="submit">{{ $t('common.save') }}</el-button>
    </el-form-item>
  </el-form>
</template>
