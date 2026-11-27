<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import { createMenu, deleteMenu, fetchMenuTree, updateMenu } from '@/api/system/menu';
import MenuIconPicker from '@/components/MenuIconPicker.vue';
import type { MenuNode } from '@/types';

const { t } = useI18n();
const TYPE_KEYS: Record<string, string> = { DIR: 'menu.typeDir', MENU: 'menu.typeMenu', BUTTON: 'menu.typeButton' };

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

function typeLabel(type?: string) {
  if (!type) return '--';
  const key = TYPE_KEYS[type];
  return key ? t(key) : type;
}

async function refresh() {
  loading.value = true;
  try {
    tree.value = await fetchMenuTree();
  } catch (error) {
    ElMessage.error((error as Error).message || t('menu.loadFailed'));
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
    ElMessage.warning(t('menu.nameRequired'));
    return;
  }
  if (form.type === 'BUTTON' && !form.perm.trim()) {
    ElMessage.warning(t('menu.permRequired'));
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
    ElMessage.success(t('gateway.saved'));
    dialog.visible = false;
    await refresh();
  } catch (error) {
    ElMessage.error((error as Error).message || t('gateway.saveFailed'));
  }
}

async function handleDelete(node: MenuNode) {
  try {
    await ElMessageBox.confirm(t('menu.deleteConfirm', { name: node.name }), t('common.deleteTitle'), { type: 'warning' });
  } catch {
    return;
  }
  try {
    await deleteMenu(node.id);
    ElMessage.success(t('control.deleted'));
    await refresh();
  } catch (error) {
    ElMessage.error((error as Error).message || t('control.deleteFailed'));
  }
}

onMounted(refresh);
</script>

<template>
  <div v-loading="loading" class="menu-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>{{ $t('menu.title') }}</span>
          <el-button type="primary" size="small" :icon="Plus" v-permission="'system:menu:create'" @click="openCreate()">
            {{ $t('menu.addRoot') }}
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
                {{ typeLabel(data.type) }}
              </el-tag>
              <el-icon v-if="data.type !== 'BUTTON'" class="tree-icon">
                <component :is="data.icon || (data.type === 'DIR' ? 'Folder' : 'Document')" />
              </el-icon>
              {{ data.name }}
              <span v-if="data.perm" class="perm-text">{{ data.perm }}</span>
              <el-tag v-if="data.status === 'DISABLED'" size="small" type="danger">{{ $t('common.disable') }}</el-tag>
            </span>
            <span class="tree-actions">
              <el-button v-if="data.type !== 'BUTTON'" link type="primary" size="small"
                v-permission="'system:menu:create'" @click="openCreate(data)">
                {{ $t('menu.addChild') }}
              </el-button>
              <el-button link type="primary" size="small" v-permission="'system:menu:edit'" @click="openEdit(data)">
                {{ $t('common.edit') }}
              </el-button>
              <el-button link type="danger" size="small" v-permission="'system:menu:delete'" @click="handleDelete(data)">
                {{ $t('common.delete') }}
              </el-button>
            </span>
          </div>
        </template>
      </el-tree>
    </el-card>

    <el-dialog v-model="dialog.visible" :title="dialog.mode === 'create' ? $t('menu.addTitle') : $t('menu.editTitle')" width="500px">
      <el-form label-width="90px">
        <el-form-item :label="$t('common.type')">
          <el-radio-group v-model="form.type" :disabled="dialog.mode === 'edit'">
            <el-radio value="DIR">{{ $t('menu.typeDir') }}</el-radio>
            <el-radio value="MENU">{{ $t('menu.typeMenu') }}</el-radio>
            <el-radio value="BUTTON">{{ $t('menu.typeButton') }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="$t('common.name')">
          <el-input v-model="form.name" :placeholder="$t('role.namePlaceholder')" />
        </el-form-item>
        <el-form-item v-if="form.type === 'MENU'" :label="$t('menu.routePath')">
          <el-input v-model="form.path" :placeholder="$t('menu.routePathPlaceholder')" />
        </el-form-item>
        <el-form-item v-if="form.type === 'MENU'" :label="$t('menu.componentPath')">
          <el-input v-model="form.component" :placeholder="$t('menu.componentPlaceholder')" />
        </el-form-item>
        <el-form-item v-if="form.type === 'BUTTON'" :label="$t('menu.permLabel')">
          <el-input v-model="form.perm" :placeholder="$t('menu.permPlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('menu.icon')">
          <MenuIconPicker v-model="form.icon" />
        </el-form-item>
        <el-form-item :label="$t('org.sort')">
          <el-input v-model="form.sort" :placeholder="$t('org.sortPlaceholder')" />
        </el-form-item>
        <el-form-item v-if="dialog.mode === 'edit'" :label="$t('common.status')">
          <el-radio-group v-model="form.status">
            <el-radio value="ENABLED">{{ $t('common.enable') }}</el-radio>
            <el-radio value="DISABLED">{{ $t('common.disable') }}</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="submit">{{ $t('common.save') }}</el-button>
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
