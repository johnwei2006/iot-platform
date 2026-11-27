<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { ElMessage } from 'element-plus';
import { Plus, RefreshRight } from '@element-plus/icons-vue';
import {
  assignAdminUser,
  createAdminUser,
  fetchAdminUsers,
  resetAdminUserPassword,
  updateAdminUser,
} from '@/api/system/user';
import { fetchOrgTree } from '@/api/system/org';
import { fetchRoles } from '@/api/system/role';
import type { AdminUser, OrgNode, RoleView } from '@/types';

const { t } = useI18n();
const users = ref<AdminUser[]>([]);
const roles = ref<RoleView[]>([]);
const orgTree = ref<OrgNode[]>([]);
const loading = ref(false);
const keyword = ref('');

const filtered = computed(() =>
  users.value.filter((u) =>
    !keyword.value ? true : u.username.toLowerCase().includes(keyword.value.toLowerCase()),
  ),
);

const createDialog = reactive({ visible: false });
const createForm = reactive({ username: '', password: '', roles: [] as string[] });

const assignDialog = reactive({ visible: false, id: '', username: '' });
const assignForm = reactive({ roles: [] as string[], orgId: '' });

const resetDialog = reactive({ visible: false, id: '', username: '' });
const resetForm = reactive({ password: '' });

async function refresh() {
  loading.value = true;
  try {
    const [u, r, tree] = await Promise.all([fetchAdminUsers(), fetchRoles(), fetchOrgTree()]);
    users.value = u;
    roles.value = r;
    orgTree.value = tree;
  } catch (error) {
    ElMessage.error((error as Error).message || t('user.loadFailed'));
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  createForm.username = '';
  createForm.password = '';
  createForm.roles = [];
  createDialog.visible = true;
}

async function submitCreate() {
  if (!createForm.username.trim() || createForm.password.length < 8) {
    ElMessage.warning(t('user.createInvalid'));
    return;
  }
  try {
    await createAdminUser({
      username: createForm.username.trim(),
      password: createForm.password,
      roles: createForm.roles,
    });
    ElMessage.success(t('sensorGroups.created'));
    createDialog.visible = false;
    await refresh();
  } catch (error) {
    ElMessage.error((error as Error).message || t('user.createFailed'));
  }
}

function openAssign(user: AdminUser) {
  assignDialog.id = user.id;
  assignDialog.username = user.username;
  assignForm.roles = [...user.roles];
  assignForm.orgId = user.orgId ?? '';
  assignDialog.visible = true;
}

async function submitAssign() {
  try {
    await assignAdminUser(assignDialog.id, {
      roles: assignForm.roles,
      orgId: assignForm.orgId || undefined,
    });
    ElMessage.success(t('user.assignSaved'));
    assignDialog.visible = false;
    await refresh();
  } catch (error) {
    ElMessage.error((error as Error).message || t('user.assignFailed'));
  }
}

function openReset(user: AdminUser) {
  resetDialog.id = user.id;
  resetDialog.username = user.username;
  resetForm.password = '';
  resetDialog.visible = true;
}

async function submitReset() {
  if (resetForm.password.length < 8) {
    ElMessage.warning(t('user.passwordTooShort'));
    return;
  }
  try {
    await resetAdminUserPassword(resetDialog.id, resetForm.password);
    ElMessage.success(t('user.resetSaved'));
    resetDialog.visible = false;
    await refresh();
  } catch (error) {
    ElMessage.error((error as Error).message || t('user.resetFailed'));
  }
}

async function toggleStatus(user: AdminUser) {
  const target = user.status === 'DISABLED' ? 'ENABLED' : 'DISABLED';
  try {
    await updateAdminUser(user.id, { status: target });
    ElMessage.success(target === 'DISABLED' ? t('user.disabledForced') : t('user.enabled'));
    await refresh();
  } catch (error) {
    ElMessage.error((error as Error).message || t('common.failed'));
  }
}

onMounted(refresh);
</script>

<template>
  <div v-loading="loading" class="user-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>{{ $t('user.title') }}</span>
          <div class="header-actions">
            <el-input
              v-model="keyword"
              :placeholder="$t('user.searchPlaceholder')"
              clearable
              style="width: 200px"
              size="small"
            />
            <el-button type="primary" size="small" :icon="Plus" v-permission="'system:user:create'" @click="openCreate">
              {{ $t('user.addUser') }}
            </el-button>
          </div>
        </div>
      </template>

      <el-table :data="filtered" stripe>
        <el-table-column prop="username" :label="$t('login.username')" min-width="140" />
        <el-table-column :label="$t('user.role')" min-width="180">
          <template #default="{ row }">
            <el-tag v-for="role in row.roles" :key="role" size="small" class="role-tag">
              {{ role }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('user.org')" min-width="120">
          <template #default="{ row }">
            <span class="mono">{{ row.orgCode || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('user.dataScope')" min-width="120">
          <template #default="{ row }">
            <span class="mono">{{ row.dataScopes?.device || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('common.status')" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'DISABLED' ? 'danger' : 'success'" size="small">
              {{ row.status === 'DISABLED' ? $t('user.disabled') : $t('user.enabled') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('common.actions')" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" v-permission="'system:user:assign'" @click="openAssign(row)">
              {{ $t('user.assignOrgRole') }}
            </el-button>
            <el-button link type="warning" size="small" v-permission="'system:user:reset-password'" @click="openReset(row)">
              <el-icon class="btn-icon"><RefreshRight /></el-icon>
              {{ $t('user.resetPassword') }}
            </el-button>
            <el-button link :type="row.status === 'DISABLED' ? 'success' : 'danger'" size="small"
              v-permission="'system:user:edit'" @click="toggleStatus(row)">
              {{ row.status === 'DISABLED' ? $t('user.enabled') : $t('user.disabled') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增用户 -->
    <el-dialog v-model="createDialog.visible" :title="$t('user.addUser')" width="460px">
      <el-form label-width="80px">
        <el-form-item :label="$t('login.username')">
          <el-input v-model="createForm.username" :placeholder="$t('user.usernamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('user.initialPassword')">
          <el-input v-model="createForm.password" type="password" show-password :placeholder="$t('user.passwordPlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('user.role')">
          <el-select v-model="createForm.roles" multiple :placeholder="$t('user.multiSelect')" style="width: 100%">
            <el-option v-for="role in roles" :key="role.id" :value="role.code" :label="`${role.name}（${role.code}）`" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialog.visible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="submitCreate">{{ $t('system.confirmCreate') }}</el-button>
      </template>
    </el-dialog>

    <!-- 分配机构/角色 -->
    <el-dialog v-model="assignDialog.visible" :title="`${$t('user.assignOrgRole')} — ${assignDialog.username}`" width="460px">
      <el-form label-width="80px">
        <el-form-item :label="$t('user.role')">
          <el-select v-model="assignForm.roles" multiple :placeholder="$t('user.multiSelect')" style="width: 100%">
            <el-option v-for="role in roles" :key="role.id" :value="role.code" :label="`${role.name}（${role.code}）`" />
          </el-select>
          <div class="form-tip">{{ $t('user.multiRoleTip') }}</div>
        </el-form-item>
        <el-form-item :label="$t('user.org')">
          <el-tree-select
            v-model="assignForm.orgId"
            :data="orgTree"
            node-key="id"
            :props="{ label: 'name', children: 'children' }"
            check-strictly
            clearable
            :placeholder="$t('user.orgPlaceholder')"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialog.visible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="submitAssign">{{ $t('system.saveAssign') }}</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码 -->
    <el-dialog v-model="resetDialog.visible" :title="`${$t('user.resetPassword')} — ${resetDialog.username}`" width="420px">
      <el-form label-width="80px">
        <el-form-item :label="$t('user.newPassword')">
          <el-input v-model="resetForm.password" type="password" show-password :placeholder="$t('user.passwordPlaceholder')" />
        </el-form-item>
      </el-form>
      <div class="form-warn">{{ $t('user.resetWarn') }}</div>
      <template #footer>
        <el-button @click="resetDialog.visible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="submitReset">{{ $t('system.confirmReset') }}</el-button>
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
.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}
.role-tag {
  margin-right: 4px;
}
.mono {
  font-family: monospace;
}
.btn-icon {
  margin-right: 2px;
}
.form-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
  margin-top: 4px;
}
.form-warn {
  font-size: 13px;
  color: #e6a23c;
  padding-left: 80px;
}
</style>
