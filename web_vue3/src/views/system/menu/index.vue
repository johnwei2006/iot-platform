<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import { createMenu, deleteMenu, fetchMenuTree, updateMenu } from '@/api/system/menu';
import MenuIconPicker from '@/components/MenuIconPicker.vue';
import type { MenuNode } from '@/types';

const TYPE_LABELS: Record<string, string> = { DIR: '目录', MENU: '菜单', BUTTON: '按钮' };

const tree = ref<MenuNode[]>([]);
const loading = ref(false);

const dialog = reactive({ visible: false, mode: 'create' as 'create' | 'edit', id: '' });
const form = reactive({
  parentId: '' as string | null,
  name: '',
  type: 'MENU',
  path: '',
  component: '',
  perm: '',
  icon: '',
  sort: '0',
  status: 'ENABLED',
});

async function refresh() {
  loading.value = true;
  try {
    tree.value = await fetchMenuTree();
  } catch (error) {
    ElMessage.error((error as Error).message || '加载菜单失败');
  } finally {
    loading.value = false;
  }
}

function openCreate(parent?: MenuNode) {
  dialog.mode = 'create';
  dialog.id = '';
  form.parentId = parent?.id ?? null;
  // 按钮的父节点通常是菜单；目录下可挂目录/菜单
  form.type = parent?.type === 'MENU' ? 'BUTTON' : parent ? 'MENU' : 'DIR';
  form.name = '';
  form.path = '';
  form.component = '';
  form.perm = '';
  form.icon = '';
  form.sort = '0';
  form.status = 'ENABLED';
  dialog.visible = true;
}

function openEdit(node: MenuNode) {
  dialog.mode = 'edit';
  dialog.id = node.id;
  form.parentId = node.parentId;
  form.type = node.type;
  form.name = node.name;
  form.path = node.path ?? '';
  form.component = node.component ?? '';
  form.perm = node.perm ?? '';
  form.icon = node.icon ?? '';
  form.sort = node.sort ?? '0';
  form.status = node.status ?? 'ENABLED';
  dialog.visible = true;
}

async function submit() {
  if (!form.name.trim()) {
    ElMessage.warning('请输入名称');
    return;
  }
  if (form.type === 'BUTTON' && !form.perm.trim()) {
    ElMessage.warning('按钮节点必须填写权限标识（如 system:org:create）');
    return;
  }
  try {
    if (dialog.mode === 'create') {
      await createMenu({
        parentId: form.parentId || null,
        name: form.name,
        type: form.type,
        path: form.path || undefined,
        component: form.component || undefined,
        perm: form.perm || undefined,
        icon: form.icon || undefined,
        sort: form.sort,
      });
    } else {
      await updateMenu(dialog.id, {
        name: form.name,
        path: form.path || undefined,
        component: form.component || undefined,
        perm: form.perm || undefined,
        icon: form.icon || undefined,
        sort: form.sort,
        status: form.status,
      });
    }
    ElMessage.success('保存成功');
    dialog.visible = false;
    await refresh();
  } catch (error) {
    ElMessage.error((error as Error).message || '保存失败');
  }
}

async function handleDelete(node: MenuNode) {
  try {
    await ElMessageBox.confirm(`确认删除菜单「${node.name}」？`, '删除确认', { type: 'warning' });
  } catch {
    return;
  }
  try {
    await deleteMenu(node.id);
    ElMessage.success('已删除');
    await refresh();
  } catch (error) {
    ElMessage.error((error as Error).message || '删除失败');
  }
}

onMounted(refresh);
</script>

<template>
  <div v-loading="loading" class="menu-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>菜单管理（目录 / 菜单 / 按钮三级树）</span>
          <el-button type="primary" size="small" :icon="Plus" v-permission="'system:menu:create'" @click="openCreate()">
            新增根菜单
          </el-button>
        </div>
      </template>
      <el-tree
        :data="tree"
        node-key="id"
        :props="{ label: 'name', children: 'children' }"
        default-expand-all
        :expand-on-click-node="false"
      >
        <template #default="{ data }">
          <div class="tree-node">
            <span>
              <el-tag size="small" :type="data.type === 'BUTTON' ? 'warning' : 'primary'" class="type-tag">
                {{ TYPE_LABELS[data.type] || data.type }}
              </el-tag>
              <el-icon v-if="data.type !== 'BUTTON'" class="tree-icon">
                <component :is="data.icon || (data.type === 'DIR' ? 'Folder' : 'Document')" />
              </el-icon>
              {{ data.name }}
              <span v-if="data.perm" class="perm-text">{{ data.perm }}</span>
              <el-tag v-if="data.status === 'DISABLED'" size="small" type="danger">停用</el-tag>
            </span>
            <span class="tree-actions">
              <el-button v-if="data.type !== 'BUTTON'" link type="primary" size="small"
                v-permission="'system:menu:create'" @click="openCreate(data)">
                新增子级
              </el-button>
              <el-button link type="primary" size="small" v-permission="'system:menu:edit'" @click="openEdit(data)">
                编辑
              </el-button>
              <el-button link type="danger" size="small" v-permission="'system:menu:delete'" @click="handleDelete(data)">
                删除
              </el-button>
            </span>
          </div>
        </template>
      </el-tree>
    </el-card>

    <el-dialog v-model="dialog.visible" :title="dialog.mode === 'create' ? '新增菜单' : '编辑菜单'" width="500px">
      <el-form label-width="90px">
        <el-form-item label="类型">
          <el-radio-group v-model="form.type" :disabled="dialog.mode === 'edit'">
            <el-radio value="DIR">目录</el-radio>
            <el-radio value="MENU">菜单</el-radio>
            <el-radio value="BUTTON">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="form.name" placeholder="显示名称" />
        </el-form-item>
        <el-form-item v-if="form.type === 'MENU'" label="路由路径">
          <el-input v-model="form.path" placeholder="如 /system/org" />
        </el-form-item>
        <el-form-item v-if="form.type === 'MENU'" label="组件路径">
          <el-input v-model="form.component" placeholder="views 下路径，如 system/org/index" />
        </el-form-item>
        <el-form-item v-if="form.type === 'BUTTON'" label="权限标识">
          <el-input v-model="form.perm" placeholder="如 system:org:create" />
        </el-form-item>
        <el-form-item label="图标">
          <MenuIconPicker v-model="form.icon" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input v-model="form.sort" placeholder="数字越小越靠前" />
        </el-form-item>
        <el-form-item v-if="dialog.mode === 'edit'" label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="ENABLED">启用</el-radio>
            <el-radio value="DISABLED">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.tree-node {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  padding-right: 8px;
}
.type-tag {
  margin-right: 6px;
}
.tree-icon { margin-right: 6px; color: var(--kb-brand); vertical-align: -2px; }
.perm-text {
  font-family: monospace;
  color: #909399;
  font-size: 12px;
  margin-left: 6px;
}
.tree-actions {
  display: flex;
  gap: 4px;
}
</style>
