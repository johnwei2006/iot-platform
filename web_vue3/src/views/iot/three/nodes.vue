<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { createNode, deleteNode, listNodes, updateNode, type NodeEntity } from '@/api/threeLayer';

const NODE_TYPES: Record<number, string> = { 1: 'ACT', 2: 'IRRI', 3: 'DATA', 4: '植保机', 5: '开度' };
const loading = ref(false);
const nodes = ref<NodeEntity[]>([]);
const dialogVisible = ref(false);
const editingId = ref<string | null>(null);
const form = reactive<Partial<NodeEntity>>({});

async function refresh() {
  loading.value = true;
  try {
    const res = await listNodes();
    nodes.value = res.data || [];
  } catch (error) {
    ElMessage.error((error as Error).message || '节点加载失败');
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  editingId.value = null;
  Object.keys(form).forEach((k) => delete (form as Record<string, unknown>)[k]);
  form.nodeType = 3;
  form.online = 1;
  dialogVisible.value = true;
}

function openEdit(row: NodeEntity) {
  editingId.value = row.id;
  Object.assign(form, row);
  dialogVisible.value = true;
}

async function submit() {
  if (!form.nodeCode) {
    ElMessage.warning('请填写节点编码');
    return;
  }
  try {
    if (editingId.value) {
      await updateNode(editingId.value, form);
    } else {
      await createNode(form);
    }
    ElMessage.success('保存成功');
    dialogVisible.value = false;
    refresh();
  } catch (error) {
    ElMessage.error((error as Error).message || '保存失败');
  }
}

async function remove(row: NodeEntity) {
  await ElMessageBox.confirm(`确认删除节点 ${row.nodeCode}？`, '提示', { type: 'warning' });
  await deleteNode(row.id);
  ElMessage.success('已删除');
  refresh();
}

onMounted(refresh);
</script>

<template>
  <div v-loading="loading">
    <el-card shadow="never">
      <template #header>
        <div class="toolbar">
          <span>节点管理（挂在网关下）</span>
          <el-button type="primary" @click="openCreate">新增节点</el-button>
        </div>
      </template>
      <el-table :data="nodes" stripe>
        <el-table-column prop="nodeCode" label="节点编码" min-width="120" />
        <el-table-column prop="nodeName" label="节点名称" min-width="120" />
        <el-table-column prop="gatewayCode" label="所属网关" min-width="130" />
        <el-table-column label="节点类型" width="100">
          <template #default="{ row }">{{ NODE_TYPES[row.nodeType] || row.nodeType || '-' }}</template>
        </el-table-column>
        <el-table-column label="在线" width="80">
          <template #default="{ row }">
            <el-tag :type="row.online === 1 ? 'success' : 'info'" size="small">{{ row.online === 1 ? '在线' : '离线' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="address" label="安装地点" min-width="120" />
        <el-table-column prop="firmwareVersion" label="固件" width="90" />
        <el-table-column prop="batteryVoltage" label="电池电压" width="90" />
        <el-table-column label="最后上报" width="160">
          <template #default="{ row }">{{ (row.lastReportTime || '').replace('T', ' ').slice(0, 19) || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑节点' : '新增节点'" width="560px">
      <el-form label-width="90px">
        <el-form-item label="节点编码" required><el-input v-model="form.nodeCode" :disabled="!!editingId" /></el-form-item>
        <el-form-item label="节点名称"><el-input v-model="form.nodeName" /></el-form-item>
        <el-form-item label="网关编码"><el-input v-model="form.gatewayCode" placeholder="iot_gateway.gateway_code" /></el-form-item>
        <el-form-item label="节点类型">
          <el-select v-model="form.nodeType" style="width: 100%">
            <el-option label="ACT" :value="1" /><el-option label="IRRI" :value="2" />
            <el-option label="DATA" :value="3" /><el-option label="植保机" :value="4" />
            <el-option label="开度" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item label="安装地点"><el-input v-model="form.address" /></el-form-item>
        <el-form-item label="经度"><el-input v-model="form.longitude" /></el-form-item>
        <el-form-item label="纬度"><el-input v-model="form.latitude" /></el-form-item>
        <el-form-item label="上报频率"><el-input-number v-model="form.reportInterval" :min="0" style="width: 100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; }
</style>
