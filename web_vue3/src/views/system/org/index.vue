<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import { createOrg, createOrgWithAdmin, deleteOrg, fetchOrgList, fetchOrgTree, updateOrg } from '@/api/system/org';
import type { OrgNode, OrgRecord } from '@/types';
import ProductAssignDialog from './ProductAssignDialog.vue';

const { t } = useI18n();
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
    ElMessage.error((error as Error).message || t('org.loadFailed'));
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
    ElMessage.warning(t('org.nameRequired'));
    return;
  }
  try {
    if (dialog.mode === 'create') {
      const code = form.code.trim();
      if (!/^\d{4}(\d{2}){0,3}$/.test(code)) {
        ElMessage.warning(t('org.codeInvalid'));
        return;
      }
      if (!dialog.parentCode) {
        if (!form.adminUsername.trim() || form.adminPassword.length < 8) {
          ElMessage.warning(t('org.adminRequired'));
          return;
        }
        await createOrgWithAdmin({
          code, name: form.name, leader: form.leader, phone: form.phone, sort: form.sort,
          adminUsername: form.adminUsername.trim(), adminPassword: form.adminPassword,
        });
        ElMessage.success(t('org.createdWithAdmin'));
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
    ElMessage.success(t('gateway.saved'));
    dialog.visible = false;
    await refresh();
  } catch (error) {
    ElMessage.error((error as Error).message || t('gateway.saveFailed'));
  }
}

function openAssign(record: OrgRecord) {
  assign.orgId = record.id;
  assign.orgName = record.name;
  assign.visible = true;
}

async function handleDelete(record: OrgRecord) {
  try {
    await ElMessageBox.confirm(t('org.deleteConfirm', { name: record.name, code: record.code }), t('common.deleteTitle'), {
      type: 'warning',
    });
  } catch {
    return;
  }
  try {
    await deleteOrg(record.id);
    ElMessage.success(t('control.deleted'));
    await refresh();
  } catch (error) {
    ElMessage.error((error as Error).message || t('control.deleteFailed'));
  }
}

onMounted(refresh);
</script>

<template>
  <div v-loading="loading" class="org-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>{{ $t('org.treeHint') }}</span>
          <el-button
            type="primary"
            size="small"
            :icon="Plus"
            v-permission="'system:org:create'"
            @click="openCreate()"
          >
            {{ $t('org.addOrg') }}
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
              <el-tag v-if="data.status === 'DISABLED'" size="small" type="danger">{{ $t('common.disable') }}</el-tag>
            </span>
            <span class="tree-actions">
              <el-button link type="primary" size="small" @click="openAssign(data)">{{ $t('org.assignProducts') }}</el-button>
              <el-button
                link
                type="primary"
                size="small"
                v-permission="'system:org:create'"
                @click="openCreate(data)"
              >
                {{ $t('org.addChild') }}
              </el-button>
              <el-button
                link
                type="primary"
                size="small"
                v-permission="'system:org:edit'"
                @click="openEdit(data)"
              >
                {{ $t('common.edit') }}
              </el-button>
              <el-button
                link
                type="danger"
                size="small"
                v-permission="'system:org:delete'"
                @click="handleDelete(data)"
              >
                {{ $t('common.delete') }}
              </el-button>
            </span>
          </div>
        </template>
      </el-tree>
    </el-card>

    <el-dialog
      v-model="dialog.visible"
      :title="dialog.mode === 'create' ? $t('org.addOrg') : $t('org.editOrg')"
      width="460px"
    >
      <el-form label-width="90px">
        <el-form-item v-if="dialog.mode === 'create'" :label="$t('org.code')">
          <el-input v-model="form.code" :placeholder="$t('org.codePlaceholder')" />
          <div class="form-tip" v-if="dialog.parentCode">
            {{ $t('org.parentCodeTip', { code: dialog.parentCode }) }}
          </div>
        </el-form-item>
        <el-form-item v-else :label="$t('org.code')">
          <el-input :model-value="form.code" disabled />
        </el-form-item>
        <el-form-item :label="$t('org.name')">
          <el-input v-model="form.name" :placeholder="$t('org.namePlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('org.leader')">
          <el-input v-model="form.leader" :placeholder="$t('org.optional')" />
        </el-form-item>
        <el-form-item :label="$t('org.phone')">
          <el-input v-model="form.phone" :placeholder="$t('org.optional')" />
        </el-form-item>
        <el-form-item :label="$t('org.sort')">
          <el-input v-model="form.sort" :placeholder="$t('org.sortPlaceholder')" />
        </el-form-item>
        <template v-if="dialog.mode === 'create' && !dialog.parentCode">
          <div class="section-title">{{ $t('org.adminSection') }}</div>
          <el-form-item :label="$t('org.adminUsername')">
            <el-input v-model="form.adminUsername" :placeholder="$t('org.adminUsernamePlaceholder')" />
          </el-form-item>
          <el-form-item :label="$t('org.adminPassword')">
            <el-input v-model="form.adminPassword" type="password" show-password :placeholder="$t('org.adminPasswordPlaceholder')" />
          </el-form-item>
        </template>
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
