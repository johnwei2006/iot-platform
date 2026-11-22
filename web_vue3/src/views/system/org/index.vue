<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import { createOrg, createOrgWithAdmin, deleteOrg, fetchOrgList, fetchOrgTree, updateOrg } from '@/api/admin';
import type { OrgNode, OrgRecord } from '@/types';
import ProductAssignDialog from './ProductAssignDialog.vue';

const tree = ref<OrgNode[]>([]);
const records = ref<OrgRecord[]>([]);
const loading = ref(false);

const dialog = reactive({
  visible: false,
  mode: 'create' as 'create' | 'edit',
  id: '',
  parentCode: '',
});
const form = reactive({
  code: '', name: '', leader: '', phone: '', sort: '0', status: 'ENABLED',
  adminUsername: '', adminPassword: '',
});
const assign = reactive({ visible: false, orgId: '', orgName: '' });

async function refresh() {
  loading.value = true;
  try {
    tree.value = await fetchOrgTree();
    records.value = await fetchOrgList();
  } catch (error) {
    ElMessage.error((error as Error).message || '加载机构失败');
  } finally {
    loading.value = false;
  }
}

/** 新增：传父机构编码用于提示编码前缀；编码由用户按层级规则填写（4/6/8/10 位） */
function openCreate(parent?: OrgRecord) {
  dialog.mode = 'create';
  dialog.id = '';
  form.code = '';
  form.name = '';
  form.leader = '';
  form.phone = '';
  form.sort = '0';
  form.status = 'ENABLED';
  form.adminUsername = '';
  form.adminPassword = '';
  dialog.parentCode = parent?.code ?? '';
  dialog.visible = true;
}

function openEdit(record: OrgRecord) {
  dialog.mode = 'edit';
  dialog.id = record.id;
  form.code = record.code;
  form.name = record.name;
  form.leader = record.leader ?? '';
  form.phone = record.phone ?? '';
  form.sort = record.sort ?? '0';
  form.status = record.status ?? 'ENABLED';
  dialog.visible = true;
}

async function submit() {
  if (!form.name.trim()) {
    ElMessage.warning('请输入机构名称');
    return;
  }
  try {
    if (dialog.mode === 'create') {
      const code = form.code.trim();
      if (!/^\d{4}(\d{2}){0,3}$/.test(code)) {
        ElMessage.warning('编码须为 4/6/8/10 位数字，且以父机构编码开头（如 0001、000102）');
        return;
      }
      if (!dialog.parentCode) {
        if (!form.adminUsername.trim() || form.adminPassword.length < 8) {
          ElMessage.warning('一级机构须录入管理员账号，初始密码至少 8 位');
          return;
        }
        await createOrgWithAdmin({
          code, name: form.name, leader: form.leader, phone: form.phone, sort: form.sort,
          adminUsername: form.adminUsername.trim(), adminPassword: form.adminPassword,
        });
        ElMessage.success('机构与管理员账号已创建，请继续分配产品');
      } else {
        await createOrg({ code, name: form.name, leader: form.leader, phone: form.phone, sort: form.sort });
      }
    } else {
      await updateOrg(dialog.id, {
        name: form.name,
        leader: form.leader,
        phone: form.phone,
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

function openAssign(record: OrgRecord) {
  assign.orgId = record.id;
  assign.orgName = record.name;
  assign.visible = true;
}

async function handleDelete(record: OrgRecord) {
  try {
    await ElMessageBox.confirm(`确认删除机构「${record.name}（${record.code}）」？`, '删除确认', {
      type: 'warning',
    });
  } catch {
    return;
  }
  try {
    await deleteOrg(record.id);
    ElMessage.success('已删除');
    await refresh();
  } catch (error) {
    ElMessage.error((error as Error).message || '删除失败');
  }
}

onMounted(refresh);
</script>

<template>
  <div v-loading="loading" class="org-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>机构树（编码 4-2-2-2 层级：0001 → 000101 → 00010103，最深 4 级）</span>
          <el-button
            type="primary"
            size="small"
            :icon="Plus"
            v-permission="'system:org:create'"
            @click="openCreate()"
          >
            新增机构
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
              <el-tag size="small" type="info" class="code-tag">{{ data.code }}</el-tag>
              {{ data.name }}
              <el-tag v-if="data.status === 'DISABLED'" size="small" type="danger">停用</el-tag>
            </span>
            <span class="tree-actions">
              <el-button link type="primary" size="small" @click="openAssign(data)">分配产品</el-button>
              <el-button
                link
                type="primary"
                size="small"
                v-permission="'system:org:create'"
                @click="openCreate(data)"
              >
                新增下级
              </el-button>
              <el-button
                link
                type="primary"
                size="small"
                v-permission="'system:org:edit'"
                @click="openEdit(data)"
              >
                编辑
              </el-button>
              <el-button
                link
                type="danger"
                size="small"
                v-permission="'system:org:delete'"
                @click="handleDelete(data)"
              >
                删除
              </el-button>
            </span>
          </div>
        </template>
      </el-tree>
    </el-card>

    <el-dialog
      v-model="dialog.visible"
      :title="dialog.mode === 'create' ? '新增机构' : '编辑机构'"
      width="460px"
    >
      <el-form label-width="90px">
        <el-form-item v-if="dialog.mode === 'create'" label="机构编码">
          <el-input v-model="form.code" placeholder="4/6/8/10 位数字，如 0001 或 000102" />
          <div class="form-tip" v-if="dialog.parentCode">
            父机构编码：{{ dialog.parentCode }}，子级编码须以其开头
          </div>
        </el-form-item>
        <el-form-item v-else label="机构编码">
          <el-input :model-value="form.code" disabled />
        </el-form-item>
        <el-form-item label="机构名称">
          <el-input v-model="form.name" placeholder="请输入机构名称" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="form.leader" placeholder="选填" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.phone" placeholder="选填" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input v-model="form.sort" placeholder="数字越小越靠前" />
        </el-form-item>
        <template v-if="dialog.mode === 'create' && !dialog.parentCode">
          <div class="section-title">一级机构管理员</div>
          <el-form-item label="管理员账号">
            <el-input v-model="form.adminUsername" placeholder="将作为该机构管理员登录账号" />
          </el-form-item>
          <el-form-item label="初始密码">
            <el-input v-model="form.adminPassword" type="password" show-password placeholder="至少 8 位，交由管理员首次登录后修改" />
          </el-form-item>
        </template>
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

    <ProductAssignDialog
      v-model="assign.visible"
      :org-id="assign.orgId"
      :org-name="assign.orgName"
      @saved="refresh"
    />
  </div>
</template>

<style scoped>
.org-page {
  padding: 4px;
}
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
.code-tag {
  font-family: monospace;
  margin-right: 6px;
}
.tree-actions {
  display: flex;
  gap: 4px;
}
.section-title {
  font-size: var(--kb-fs-subheading);
  font-weight: 600;
  color: var(--kb-text);
  margin: 12px 0 4px 90px;
}
.form-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
  margin-top: 4px;
}
</style>
