<script setup lang="ts">
import { onMounted, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import GatewayConsole from './components/GatewayConsole.vue';
import CommandLogTable from './components/CommandLogTable.vue';
import TaskEditor from './components/TaskEditor.vue';
import { deleteGatewayTask, deployGatewayTasks, listGatewayCommands, listGatewayTasks,
  saveGatewayTask, type GatewayCommandRecord, type GatewayTaskEntity } from '@/api/iot/gatewayManage';

const gatewayCode = ref('');
const tasks = ref<GatewayTaskEntity[]>([]);
const commands = ref<GatewayCommandRecord[]>([]);
const editorVisible = ref(false);
const editing = ref<GatewayTaskEntity | null>(null);

async function load() {
  if (!gatewayCode.value) return;
  try {
    const [taskRes, commandRes] = await Promise.all([
      listGatewayTasks(gatewayCode.value),
      listGatewayCommands({ gatewayCode: gatewayCode.value, serviceType: 37, limit: 100 }),
    ]);
    tasks.value = taskRes.data ?? [];
    commands.value = commandRes.data ?? [];
  } catch (error) { console.error('gateway tasks load failed', error); }
}

function create() {
  editing.value = { gatewayCode: gatewayCode.value, taskName: '', taskNo: tasks.value.length + 1,
    enabled: true, triggerType: 'TIME', triggerTime: '07:00:00', triggers: '[]', actions: '[]' };
  editorVisible.value = true;
}

function edit(row: GatewayTaskEntity) {
  editing.value = { ...row };
  editorVisible.value = true;
}

async function save(task: GatewayTaskEntity) {
  if (!gatewayCode.value) return ElMessage.warning('gatewayManage.selectGatewayRequired');
  await saveGatewayTask({ ...task, gatewayCode: gatewayCode.value });
  editorVisible.value = false;
  ElMessage.success('common.success');
  await load();
}

async function remove(row: GatewayTaskEntity) {
  if (!row.id) return;
  try { await ElMessageBox.confirm('common.deleteConfirm', 'common.delete', { type: 'warning' }); }
  catch (error) { console.error('delete cancelled', error); return; }
  await deleteGatewayTask(row.id);
  await load();
}

async function deploy() {
  if (!gatewayCode.value) return ElMessage.warning('gatewayManage.selectGatewayRequired');
  await deployGatewayTasks({ gatewayCode: gatewayCode.value });
  ElMessage.success('gatewayManage.sent');
  await load();
}

watch(gatewayCode, load);
onMounted(load);
</script>

<template>
  <GatewayConsole v-model:gateway-code="gatewayCode" :title="$t('gatewayManage.title')"
    :description="$t('gatewayManage.taskTitle')">
    <div class="split">
      <div class="panel">
        <div class="toolbar">
          <h3 class="panel-title">{{ $t('gatewayManage.taskTitle') }}</h3>
          <div>
            <el-button type="primary" size="small" @click="create">{{ $t('gatewayManage.addTask') }}</el-button>
            <el-button size="small" @click="deploy">{{ $t('gatewayManage.deployTasks') }}</el-button>
          </div>
        </div>
        <el-table :data="tasks" size="small">
          <el-table-column prop="taskNo" :label="$t('gatewayManage.taskNo')" width="90" />
          <el-table-column prop="taskName" :label="$t('gatewayManage.taskName')" min-width="140" show-overflow-tooltip />
          <el-table-column :label="$t('gatewayManage.enabled')" width="90">
            <template #default="{ row }"><el-tag :type="row.enabled ? 'success' : 'info'" size="small">{{ row.enabled ? 'Y' : 'N' }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="triggerType" :label="$t('gatewayManage.triggerType')" width="120" />
          <el-table-column prop="triggerTime" :label="$t('gatewayManage.triggerTime')" width="120" />
          <el-table-column prop="deployStatus" :label="$t('gatewayManage.deployStatus')" width="120" />
          <el-table-column :label="$t('common.actions')" width="140" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="edit(row)">{{ $t('common.edit') }}</el-button>
              <el-button link type="danger" @click="remove(row)">{{ $t('common.delete') }}</el-button>
            </template>
          </el-table-column>
          <template #empty><div class="empty">{{ $t('gatewayManage.noData') }}</div></template>
        </el-table>
      </div>
      <div class="panel">
        <h3 class="panel-title">{{ $t('gatewayManage.commands') }}</h3>
        <CommandLogTable :commands="commands" />
      </div>
    </div>
    <el-dialog v-model="editorVisible" :title="editing?.id ? $t('gatewayManage.editTask') : $t('gatewayManage.addTask')" width="620px">
      <TaskEditor v-if="editing" :model-value="editing" @update:model-value="save" />
    </el-dialog>
  </GatewayConsole>
</template>

<style scoped>
.toolbar { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.toolbar > div { display: flex; gap: 8px; }
.empty { padding: 24px; color: var(--el-text-color-secondary); text-align: center; }
</style>
