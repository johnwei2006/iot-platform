<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { ElMessage } from 'element-plus';
import { listNodes, type NodeEntity } from '@/api/iot/node';
import DeviceControlPanel from '@/views/iot/control/components/device-control-panel.vue';

const { t } = useI18n();

const loading = ref(false);
const nodes = ref<NodeEntity[]>([]);
const selectedNodeCode = ref('');

const nodeOptions = computed(() => nodes.value.map((node) => ({
  label: `${node.nodeCode} - ${node.nodeName || ''}`,
  value: node.nodeCode,
})));

async function loadNodes() {
  loading.value = true;
  try {
    const res = await listNodes();
    nodes.value = res.data || [];
  } catch (error) {
    ElMessage.error((error as Error).message || t('production.deviceControl.loadFailed'));
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  void loadNodes();
});
</script>

<template>
  <div v-loading="loading" class="production-device-control">
    <el-card shadow="never" class="node-filter-card">
      <el-form :inline="true" class="filters">
        <el-form-item :label="$t('production.deviceControl.node')">
          <el-select
            v-model="selectedNodeCode"
            filterable
            clearable
            :placeholder="$t('production.deviceControl.allNodes')"
            style="width: 280px"
          >
            <el-option v-for="node in nodeOptions" :key="node.value" :label="node.label" :value="node.value" />
          </el-select>
        </el-form-item>
      </el-form>
    </el-card>

    <DeviceControlPanel
      :node-code="selectedNodeCode"
      group-by="node"
      :show-area-filter="false"
      :use-gateway-entry="true"
      permission="production:device_control:issue"
      title-key="production.deviceControl.title"
    />
  </div>
</template>

<style scoped>
.production-device-control :deep(.el-card__header) { padding: 12px 20px; }
.node-filter-card { margin-bottom: 12px; }
.filters { margin-bottom: 0; }
</style>
