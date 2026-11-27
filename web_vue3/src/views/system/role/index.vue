<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import { createRole, deleteRole, fetchRoles, fetchScopeEntities, updateRole } from '@/api/system/role';
import { fetchMenuTree } from '@/api/system/menu';
import { useUserStore } from '@/stores/modules/user';
import type { MenuNode, RoleView, ScopeEntity } from '@/types';

const { t } = useI18n();
const userStore = useUserStore();

const SCOPE_KEYS = [
  { value: 'ALL', key: 'role.scopeAll' },
  { value: 'ORG_AND_CHILDREN', key: 'role.scopeOrgChildren' },
  { value: 'ORG_ONLY', key: 'role.scopeOrgOnly' },
  { value: 'SELF', key: 'role.scopeSelf' },
];

const roles = ref<RoleView[]>([]);
const menuTree = ref<MenuNode[]>([]);
const entities = ref<ScopeEntity[]>([]);
const loading = ref(false);

/** 机构管理员只能在 /me 返回的菜单（产品上限内）继续勾选角色菜单。 */
const allowedMenuIds = computed(() => {
  const ids = new Set<string>();
  const walk = (nodes: MenuNode[]) => nodes.forEach((n) => {
    ids.add(n.id);
    if (n.children) walk(n.children);
  });
  walk(userStore.menus);
  return ids;
});
const visibleMenuTree = computed(() => {
  if (userStore.superAdmin) return menuTree.value;
  const filter = (nodes: MenuNode[]): MenuNode[] => nodes
    .filter((n) => allowedMenuIds.value.has(n.id))
    .map((n) => ({ ...n, children: n.children ? filter(n.children) : undefined }));
  return filter(menuTree.value);
});

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
    ElMessage.error((error as Error).message || t('role.loadFailed'));
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
    ElMessage.warning(t('role.codeRequired'));
    return;
  }
  if (!form.name.trim()) {
    ElMessage.warning(t('role.nameRequired'));
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
    ElMessage.success(t('gateway.saved'));
    drawer.visible = false;
    await refresh();
  } catch (error) {
    ElMessage.error((error as Error).message || t('gateway.saveFailed'));
  }
}

async function handleDelete(role: RoleView) {
  try {
    await ElMessageBox.confirm(t('role.deleteConfirm', { name: role.name, code: role.code }), t('common.deleteTitle'), {
      type: 'warning',
    });
  } catch {
    return;
  }
  try {
    await deleteRole(role.id);
    ElMessage.success(t('control.deleted'));
    await refresh();
  } catch (error) {
    ElMessage.error((error as Error).message || t('control.deleteFailed'));
  }
}

onMounted(refresh);
</script>

<template>
  <div v-loading="loading" class="role-page">
    <el-card v-if="!userStore.superAdmin" shadow="never" class="cap-card">
      <strong>{{ $t('role.productCap') }}</strong>{{ userStore.products.map((p) => p.name).join('、') || $t('system.noProducts') }}
      <span>{{ $t('role.capHint') }}</span>
    </el-card>
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>{{ $t('role.title') }}</span>
          <el-button type="primary" size="small" :icon="Plus" v-permission="'system:role:create'" @click="openCreate">
            {{ $t('role.addRole') }}
          </el-button>
        </div>
      </template>

      <el-table :data="roles" stripe>
        <el-table-column prop="code" :label="$t('common.code')" min-width="140">
          <template #default="{ row }">
            <span class="mono">{{ row.code }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" :label="$t('common.name')" min-width="140" />
        <el-table-column :label="$t('user.dataScope')" min-width="220">
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
        <el-table-column :label="$t('common.type')" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.builtIn" size="small" type="success">{{ $t('role.builtIn') }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('common.status')" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'DISABLED' ? 'danger' : 'success'" size="small">
              {{ row.status === 'DISABLED' ? $t('common.disable') : $t('common.enable') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('common.actions')" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" v-permission="'system:role:edit'" @click="openEdit(row)">
              {{ $t('common.edit') }}
            </el-button>
            <el-button
              v-if="!row.builtIn"
              link
              type="danger"
              size="small"
              v-permission="'system:role:delete'"
              @click="handleDelete(row)"
            >
              {{ $t('common.delete') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-drawer v-model="drawer.visible" :title="drawer.mode === 'create' ? $t('role.addRole') : $t('role.editRole')" size="520px">
      <el-form label-width="90px">
        <el-form-item :label="$t('role.code')">
          <el-input v-model="form.code" :disabled="drawer.mode === 'edit'" :placeholder="$t('role.codePlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('role.name')">
          <el-input v-model="form.name" :placeholder="$t('role.namePlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('role.menuAuth')">
          <el-tree
            ref="menuTreeRef"
            :data="visibleMenuTree"
            node-key="id"
            show-checkbox
            default-expand-all
            :default-checked-keys="form.menuIds"
            :props="{ label: 'name', children: 'children' }"
            :disabled="drawer.builtIn"
            @check="(_data: any, check: any) => (form.menuIds = check.checkedKeys as string[])"
          />
        </el-form-item>
        <el-form-item v-for="entity in entities" :key="entity.code" :label="`${entity.name}${$t('role.dataSuffix')}`">
          <el-select v-model="form.dataScopes[entity.code]" style="width: 100%" :disabled="drawer.builtIn || !userStore.superAdmin">
            <el-option v-for="opt in SCOPE_KEYS" :key="opt.value" :value="opt.value" :label="t(opt.key)" />
          </el-select>
        </el-form-item>
        <div v-if="drawer.builtIn" class="built-in-tip">{{ $t('role.builtInTip') }}</div>
      </el-form>
      <template #footer>
        <el-button @click="drawer.visible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="submit">{{ $t('common.save') }}</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped>
.role-page { display: flex; flex-direction: column; gap: 16px; }
.cap-card { border-color: var(--kb-line); border-radius: var(--kb-radius-card); color: var(--kb-text); }
.cap-card span { color: var(--kb-text-muted); font-size: var(--kb-fs-caption); margin-left: 12px; }
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
