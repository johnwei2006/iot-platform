<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import { createRole, deleteRole, fetchMenuTree, fetchRoles, fetchScopeEntities, updateRole } from '@/api/admin';
import type { MenuNode, RoleView, ScopeEntity } from '@/types';

const SCOPE_OPTIONS = [
  { value: 'ALL', label: '全部数据' },
  { value: 'ORG_AND_CHILDREN', label: '本机构及以下' },
  { value: 'ORG_ONLY', label: '仅本机构' },
  { value: 'SELF', label: '仅本人' },
];

const roles = ref<RoleView[]>([]);
const menuTree = ref<MenuNode[]>([]);
const entities = ref<ScopeEntity[]>([]);
const loading = ref(false);

const drawer = reactive({ visible: false, mode: 'create' as 'create' | 'edit', id: '', builtIn: false });
const form = reactive({
  code: '',
  name: '',
  menuIds: [] as string[],
  dataScopes: {} as Record<string, string>,
});

async function refresh() {
  loading.value = true;
  try {
    const [r, m, e] = await Promise.all([fetchRoles(), fetchMenuTree(), fetchScopeEntities()]);
    roles.value = r;
    menuTree.value = m;
    entities.value = e;
  } catch (error) {
    ElMessage.error((error as Error).message || '加载角色失败');
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  drawer.mode = 'create';
  drawer.id = '';
  drawer.builtIn = false;
  form.code = '';
  form.name = '';
  form.menuIds = [];
  form.dataScopes = {};
  drawer.visible = true;
}

function openEdit(role: RoleView) {
  drawer.mode = 'edit';
  drawer.id = role.id;
  drawer.builtIn = !!role.builtIn;
  form.code = role.code;
  form.name = role.name;
  form.menuIds = [...(role.menuIds ?? [])];
  form.dataScopes = { ...(role.dataScopes ?? {}) };
  drawer.visible = true;
}

async function submit() {
  if (drawer.mode === 'create' && !form.code.trim()) {
    ElMessage.warning('请输入角色编码（大写，如 ORG_ADMIN）');
    return;
  }
  if (!form.name.trim()) {
    ElMessage.warning('请输入角色名称');
    return;
  }
  try {
    if (drawer.mode === 'create') {
      await createRole({
        code: form.code.trim(),
        name: form.name,
        menuIds: form.menuIds,
        dataScopes: form.dataScopes,
      });
    } else {
      await updateRole(drawer.id, {
        name: form.name,
        menuIds: drawer.builtIn ? undefined : form.menuIds,
        dataScopes: drawer.builtIn ? undefined : form.dataScopes,
      });
    }
    ElMessage.success('保存成功');
    drawer.visible = false;
    await refresh();
  } catch (error) {
    ElMessage.error((error as Error).message || '保存失败');
  }
}

async function handleDelete(role: RoleView) {
  try {
    await ElMessageBox.confirm(`确认删除角色「${role.name}（${role.code}）」？`, '删除确认', {
      type: 'warning',
    });
  } catch {
    return;
  }
  try {
    await deleteRole(role.id);
    ElMessage.success('已删除');
    await refresh();
  } catch (error) {
    ElMessage.error((error as Error).message || '删除失败');
  }
}

onMounted(refresh);
</script>

<template>
  <div v-loading="loading" class="role-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>角色管理（菜单授权 + 按实体数据权限）</span>
          <el-button type="primary" size="small" :icon="Plus" v-permission="'system:role:create'" @click="openCreate">
            新增角色
          </el-button>
        </div>
      </template>

      <el-table :data="roles" stripe>
        <el-table-column prop="code" label="编码" min-width="140">
          <template #default="{ row }">
            <span class="mono">{{ row.code }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="名称" min-width="140" />
        <el-table-column label="数据权限" min-width="220">
          <template #default="{ row }">
            <el-tag
              v-for="(scope, entity) in row.dataScopes"
              :key="entity"
              size="small"
              class="scope-tag"
              :type="scope === 'ALL' ? 'danger' : scope === 'SELF' ? 'info' : 'warning'"
            >
              {{ entity }}: {{ scope }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.builtIn" size="small" type="success">内置</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'DISABLED' ? 'danger' : 'success'" size="small">
              {{ row.status === 'DISABLED' ? '停用' : '启用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" v-permission="'system:role:edit'" @click="openEdit(row)">
              编辑
            </el-button>
            <el-button
              v-if="!row.builtIn"
              link
              type="danger"
              size="small"
              v-permission="'system:role:delete'"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-drawer v-model="drawer.visible" :title="drawer.mode === 'create' ? '新增角色' : '编辑角色'" size="520px">
      <el-form label-width="90px">
        <el-form-item label="角色编码">
          <el-input v-model="form.code" :disabled="drawer.mode === 'edit'" placeholder="大写编码，如 ORG_ADMIN" />
        </el-form-item>
        <el-form-item label="角色名称">
          <el-input v-model="form.name" placeholder="显示名称" />
        </el-form-item>
        <el-form-item label="菜单授权">
          <el-tree
            ref="menuTreeRef"
            :data="menuTree"
            node-key="id"
            show-checkbox
            default-expand-all
            :default-checked-keys="form.menuIds"
            :props="{ label: 'name', children: 'children' }"
            :disabled="drawer.builtIn"
            @check="(_data: any, check: any) => (form.menuIds = check.checkedKeys as string[])"
          />
        </el-form-item>
        <el-form-item v-for="entity in entities" :key="entity.code" :label="`${entity.name}数据`">
          <el-select v-model="form.dataScopes[entity.code]" style="width: 100%" :disabled="drawer.builtIn">
            <el-option v-for="opt in SCOPE_OPTIONS" :key="opt.value" :value="opt.value" :label="opt.label" />
          </el-select>
        </el-form-item>
        <div v-if="drawer.builtIn" class="built-in-tip">内置角色菜单与数据权限已锁定，仅可改名。</div>
      </el-form>
      <template #footer>
        <el-button @click="drawer.visible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.mono {
  font-family: monospace;
}
.scope-tag {
  margin-right: 4px;
  margin-bottom: 2px;
}
.built-in-tip {
  font-size: 12px;
  color: #e6a23c;
  padding-left: 90px;
}
</style>
