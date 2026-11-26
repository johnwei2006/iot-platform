<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import {
  bindProductMenus, createProduct, deleteProduct,
  fetchProductMenus, fetchProducts, updateProduct,
} from '@/api/system/product';
import { fetchMenuTree } from '@/api/system/menu';
import type { MenuNode, ProductRecord } from '@/types';

const products = ref<ProductRecord[]>([]);
const menuTree = ref<MenuNode[]>([]);
const selectedId = ref('');
const checkedMenuIds = ref<string[]>([]);
const loading = ref(false);
const binding = ref(false);

const dialog = reactive({ visible: false, mode: 'create' as 'create' | 'edit', id: '' });
const form = reactive({ code: '', name: '', description: '', sort: '0', status: 'ENABLED' });

async function refreshProducts(keepSelection = true) {
  loading.value = true;
  try {
    products.value = await fetchProducts();
    if (!keepSelection || !products.value.some((p) => p.id === selectedId.value)) {
      selectedId.value = products.value[0]?.id || '';
    }
  } catch (error) {
    ElMessage.error((error as Error).message || '加载产品失败');
  } finally {
    loading.value = false;
  }
}

async function loadMenus(productId: string) {
  if (!productId) return;
  binding.value = true;
  try {
    const [all, bound] = await Promise.all([fetchMenuTree(), fetchProductMenus(productId)]);
    menuTree.value = all;
    checkedMenuIds.value = bound.map((m) => m.id);
  } catch (error) {
    ElMessage.error((error as Error).message || '加载产品菜单失败');
  } finally {
    binding.value = false;
  }
}

function openCreate() {
  dialog.mode = 'create';
  dialog.id = '';
  Object.assign(form, { code: '', name: '', description: '', sort: '0', status: 'ENABLED' });
  dialog.visible = true;
}

function openEdit(product: ProductRecord) {
  dialog.mode = 'edit';
  dialog.id = product.id;
  Object.assign(form, {
    code: product.code,
    name: product.name,
    description: product.description || '',
    sort: product.sort || '0',
    status: product.status || 'ENABLED',
  });
  dialog.visible = true;
}

async function submit() {
  if (!form.name.trim() || (dialog.mode === 'create' && !form.code.trim())) {
    ElMessage.warning('请填写产品编码与名称');
    return;
  }
  try {
    if (dialog.mode === 'create') {
      await createProduct({ code: form.code.trim(), name: form.name.trim(), description: form.description, sort: form.sort });
    } else {
      await updateProduct(dialog.id, {
        name: form.name.trim(), description: form.description, sort: form.sort, status: form.status,
      });
    }
    ElMessage.success('保存成功');
    dialog.visible = false;
    await refreshProducts();
  } catch (error) {
    ElMessage.error((error as Error).message || '保存失败');
  }
}

async function handleDelete(product: ProductRecord) {
  try {
    await ElMessageBox.confirm(`确认删除产品「${product.name}」？该产品对机构的授权会一并移除。`, '删除确认', { type: 'warning' });
  } catch {
    return;
  }
  try {
    await deleteProduct(product.id);
    ElMessage.success('已删除');
    await refreshProducts(false);
  } catch (error) {
    ElMessage.error((error as Error).message || '删除失败');
  }
}

async function saveMenus() {
  if (!selectedId.value) return;
  try {
    await bindProductMenus(selectedId.value, checkedMenuIds.value);
    ElMessage.success('产品菜单范围已更新，机构角色授权上限同步生效');
  } catch (error) {
    ElMessage.error((error as Error).message || '保存菜单失败');
  }
}

watch(selectedId, (id) => { void loadMenus(id); });
onMounted(() => refreshProducts(false).then(() => loadMenus(selectedId.value)));
</script>
<template>
  <div v-loading="loading" class="product-page">
    <el-card shadow="never" class="kb-panel">
      <template #header>
        <div class="card-header">
          <div>
            <strong>产品管理</strong>
            <span class="hint">产品是机构功能上限，角色菜单只能在产品范围内细分</span>
          </div>
          <el-button type="primary" size="small" :icon="Plus" v-permission="'system:product:create'" @click="openCreate">
            新增产品
          </el-button>
        </div>
      </template>
      <el-table :data="products" stripe highlight-current-row :current-row-key="selectedId" row-key="id"
        @current-change="(row: ProductRecord) => (selectedId = row?.id || '')">
        <el-table-column prop="code" label="编码" width="120">
          <template #default="{ row }"><span class="mono">{{ row.code }}</span></template>
        </el-table-column>
        <el-table-column prop="name" label="产品名称" min-width="140" />
        <el-table-column prop="description" label="说明" min-width="220" />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 'DISABLED' ? 'danger' : 'success'">
              {{ row.status === 'DISABLED' ? '停用' : '启用' }}
            </el-tag>
            <el-tag v-if="row.builtIn" size="small" type="info" class="built-tag">预置</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" v-permission="'system:product:edit'" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="!row.builtIn" link type="danger" size="small" v-permission="'system:product:delete'" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" class="kb-panel">
      <template #header>
        <div class="card-header">
          <strong>产品-菜单配置</strong>
          <span class="hint">勾选该产品包含的菜单与按钮；机构管理员仅能在此范围内分配角色菜单</span>
        </div>
      </template>
      <div v-loading="binding" class="menu-area">
        <el-tree
          v-if="menuTree.length"
          :data="menuTree"
          node-key="id"
          show-checkbox
          default-expand-all
          :props="{ label: 'name', children: 'children' }"
          :default-checked-keys="checkedMenuIds"
          :key="selectedId"
          @check="(_data: any, check: any) => (checkedMenuIds = check.checkedKeys as string[])"
        />
        <div v-else class="empty">暂无菜单，请先在菜单管理中创建</div>
      </div>
      <div class="footer">
        <el-button type="primary" v-permission="'system:product:bind-menu'" @click="saveMenus">保存菜单范围</el-button>
      </div>
    </el-card>

    <el-dialog v-model="dialog.visible" :title="dialog.mode === 'create' ? '新增产品' : '编辑产品'" width="480px">
      <el-form label-width="90px">
        <el-form-item label="产品编码">
          <el-input v-model="form.code" :disabled="dialog.mode === 'edit'" placeholder="大写英文，如 IOT" />
        </el-form-item>
        <el-form-item label="产品名称"><el-input v-model="form.name" placeholder="如 物联网" /></el-form-item>
        <el-form-item label="产品说明"><el-input v-model="form.description" placeholder="简要描述该产品的功能范围" /></el-form-item>
        <el-form-item label="排序"><el-input v-model="form.sort" placeholder="数字越小越靠前" /></el-form-item>
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
.product-page { padding: 4px; display: flex; flex-direction: column; gap: 16px; }
.kb-panel { border-radius: var(--kb-radius-card); border-color: var(--kb-line); }
.card-header { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.hint { font-size: var(--kb-fs-caption); color: var(--kb-text-muted); font-weight: 400; margin-left: 12px; }
.mono { font-family: ui-monospace, monospace; }
.built-tag { margin-left: 6px; }
.menu-area { max-height: 380px; overflow: auto; }
.empty { padding: 28px; text-align: center; color: var(--kb-text-muted); }
.footer { display: flex; justify-content: flex-end; padding-top: 12px; border-top: 1px solid var(--kb-line); margin-top: 12px; }
</style>
