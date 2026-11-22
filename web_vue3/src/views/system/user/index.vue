<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { Plus, RefreshRight } from '@element-plus/icons-vue';
import {
  assignAdminUser,
  createAdminUser,
  fetchAdminUsers,
  fetchOrgTree,
  fetchRoles,
  resetAdminUserPassword,
  updateAdminUser,
} from '@/api/admin';
import type { AdminUser, OrgNode, RoleView } from '@/types';

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
    ElMessage.error((error as Error).message || '加载用户失败');
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
    ElMessage.warning('用户名必填，密码至少 8 位');
    return;
  }
  try {
    await createAdminUser({
      username: createForm.username.trim(),
      password: createForm.password,
      roles: createForm.roles,
    });
    ElMessage.success('创建成功');
    createDialog.visible = false;
    await refresh();
  } catch (error) {
    ElMessage.error((error as Error).message || '创建失败');
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
    ElMessage.success('分配成功（令牌将重新签发生效）');
    assignDialog.visible = false;
    await refresh();
  } catch (error) {
    ElMessage.error((error as Error).message || '分配失败');
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
    ElMessage.warning('密码至少 8 位');
    return;
  }
  try {
    await resetAdminUserPassword(resetDialog.id, resetForm.password);
    ElMessage.success('密码已重置，该用户全部会话已下线');
    resetDialog.visible = false;
    await refresh();
  } catch (error) {
    ElMessage.error((error as Error).message || '重置失败');
  }
}

async function toggleStatus(user: AdminUser) {
  const target = user.status === 'DISABLED' ? 'ENABLED' : 'DISABLED';
  try {
    await updateAdminUser(user.id, { status: target });
    ElMessage.success(target === 'DISABLED' ? '已禁用并强制下线' : '已启用');
    await refresh();
  } catch (error) {
    ElMessage.error((error as Error).message || '操作失败');
  }
}

onMounted(refresh);
</script>

<template>
  <div v-loading="loading" class="user-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <div class="header-actions">
            <el-input
              v-model="keyword"
              placeholder="搜索用户名"
              clearable
              style="width: 200px"
              size="small"
            />
            <el-button type="primary" size="small" :icon="Plus" v-permission="'system:user:create'" @click="openCreate">
              新增用户
            </el-button>
          </div>
        </div>
      </template>

      <el-table :data="filtered" stripe>
        <el-table-column prop="username" label="用户名" min-width="140" />
        <el-table-column label="角色" min-width="180">
          <template #default="{ row }">
            <el-tag v-for="role in row.roles" :key="role" size="small" class="role-tag">
              {{ role }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="所属机构" min-width="120">
          <template #default="{ row }">
            <span class="mono">{{ row.orgCode || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="数据权限" min-width="120">
          <template #default="{ row }">
            <span class="mono">{{ row.dataScopes?.device || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'DISABLED' ? 'danger' : 'success'" size="small">
              {{ row.status === 'DISABLED' ? '禁用' : '启用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" v-permission="'system:user:assign'" @click="openAssign(row)">
              机构/角色
            </el-button>
            <el-button link type="warning" size="small" v-permission="'system:user:reset-password'" @click="openReset(row)">
              <el-icon class="btn-icon"><RefreshRight /></el-icon>
              重置密码
            </el-button>
            <el-button link :type="row.status === 'DISABLED' ? 'success' : 'danger'" size="small"
              v-permission="'system:user:edit'" @click="toggleStatus(row)">
              {{ row.status === 'DISABLED' ? '启用' : '禁用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增用户 -->
    <el-dialog v-model="createDialog.visible" title="新增用户" width="460px">
      <el-form label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="createForm.username" placeholder="登录用户名" />
        </el-form-item>
        <el-form-item label="初始密码">
          <el-input v-model="createForm.password" type="password" show-password placeholder="至少 8 位" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="createForm.roles" multiple placeholder="可多选" style="width: 100%">
            <el-option v-for="role in roles" :key="role.id" :value="role.code" :label="`${role.name}（${role.code}）`" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- 分配机构/角色 -->
    <el-dialog v-model="assignDialog.visible" :title="`分配机构/角色 — ${assignDialog.username}`" width="460px">
      <el-form label-width="80px">
        <el-form-item label="角色">
          <el-select v-model="assignForm.roles" multiple placeholder="可多选" style="width: 100%">
            <el-option v-for="role in roles" :key="role.id" :value="role.code" :label="`${role.name}（${role.code}）`" />
          </el-select>
          <div class="form-tip">多角色时数据权限按实体取最宽范围</div>
        </el-form-item>
        <el-form-item label="所属机构">
          <el-tree-select
            v-model="assignForm.orgId"
            :data="orgTree"
            node-key="id"
            :props="{ label: 'name', children: 'children' }"
            check-strictly
            clearable
            placeholder="选择机构（编码层级树）"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitAssign">保存分配</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码 -->
    <el-dialog v-model="resetDialog.visible" :title="`重置密码 — ${resetDialog.username}`" width="420px">
      <el-form label-width="80px">
        <el-form-item label="新密码">
          <el-input v-model="resetForm.password" type="password" show-password placeholder="至少 8 位" />
        </el-form-item>
      </el-form>
      <div class="form-warn">重置后该用户所有端会话立即失效，需重新登录。</div>
      <template #footer>
        <el-button @click="resetDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitReset">确认重置</el-button>
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
