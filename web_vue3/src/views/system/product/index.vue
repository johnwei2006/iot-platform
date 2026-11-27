<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import {
  bindProductMenus, createProduct, deleteProduct,
  fetchProductMenus, fetchProducts, updateProduct,
} from '@/api/system/product';
import { fetchMenuTree } from '@/api/system/menu';
import type { MenuNode, ProductRecord } from '@/types';

const { t } = useI18n();
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
    ElMessage.error((error as Error).message || t('product.loadFailed'));
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
    ElMessage.error((error as Error).message || t('product.menusLoadFailed'));
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
    ElMessage.warning(t('product.codeNameRequired'));
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
    ElMessage.success(t('gateway.saved'));
    dialog.visible = false;
    await refreshProducts();
  } catch (error) {
    ElMessage.error((error as Error).message || t('gateway.saveFailed'));
  }
}

async function handleDelete(product: ProductRecord) {
  try {
    await ElMessageBox.confirm(t('product.deleteConfirm', { name: product.name }), t('common.deleteTitle'), { type: 'warning' });
  } catch {
    return;
  }
  try {
    await deleteProduct(product.id);
    ElMessage.success(t('control.deleted'));
    await refreshProducts(false);
  } catch (error) {
    ElMessage.error((error as Error).message || t('control.deleteFailed'));
  }
}

async function saveMenus() {
  if (!selectedId.value) return;
  try {
    await bindProductMenus(selectedId.value, checkedMenuIds.value);
    ElMessage.success(t('product.menusSaved'));
  } catch (error) {
    ElMessage.error((error as Error).message || t('menu.loadFailed'));
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
            <strong>{{ $t('product.title') }}</strong>
            <span class="hint">{{ $t('product.hint') }}</span>
          </div>
          <el-button type="primary" size="small" :icon="Plus" v-permission="'system:product:create'" @click="openCreate">
            {{ $t('product.addProduct') }}
          </el-button>
        </div>
      </template>
      <el-table :data="products" stripe highlight-current-row :current-row-key="selectedId" row-key="id"
        @current-change="(row: ProductRecord) => (selectedId = row?.id || '')">
        <el-table-column prop="code" :label="$t('common.code')" width="120">
          <template #default="{ row }"><span class="mono">{{ row.code }}</span></template>
        </el-table-column>
        <el-table-column prop="name" :label="$t('product.name')" min-width="140" />
        <el-table-column prop="description" :label="$t('product.description')" min-width="220" />
        <el-table-column prop="sort" :label="$t('org.sort')" width="80" />
        <el-table-column :label="$t('common.status')" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 'DISABLED' ? 'danger' : 'success'">
              {{ row.status === 'DISABLED' ? $t('common.disable') : $t('common.enable') }}
            </el-tag>
            <el-tag v-if="row.builtIn" size="small" type="info" class="built-tag">{{ $t('product.builtIn') }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('common.actions')" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" v-permission="'system:product:edit'" @click="openEdit(row)">{{ $t('common.edit') }}</el-button>
            <el-button v-if="!row.builtIn" link type="danger" size="small" v-permission="'system:product:delete'" @click="handleDelete(row)">{{ $t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" class="kb-panel">
      <template #header>
        <div class="card-header">
          <strong>{{ $t('product.menuConfigTitle') }}</strong>
          <span class="hint">{{ $t('product.menuConfigHint') }}</span>
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
        <div v-else class="empty">{{ $t('product.noMenus') }}</div>
      </div>
      <div class="footer">
        <el-button type="primary" v-permission="'system:product:bind-menu'" @click="saveMenus">{{ $t('product.saveMenuScope') }}</el-button>
      </div>
    </el-card>

    <el-dialog v-model="dialog.visible" :title="dialog.mode === 'create' ? $t('product.addTitle') : $t('product.editTitle')" width="480px">
      <el-form label-width="90px">
        <el-form-item :label="$t('product.code')">
          <el-input v-model="form.code" :disabled="dialog.mode === 'edit'" :placeholder="$t('product.codePlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('product.name')"><el-input v-model="form.name" :placeholder="$t('product.namePlaceholder')" /></el-form-item>
        <el-form-item :label="$t('product.description')"><el-input v-model="form.description" :placeholder="$t('product.descPlaceholder')" /></el-form-item>
        <el-form-item :label="$t('org.sort')"><el-input v-model="form.sort" :placeholder="$t('org.sortPlaceholder')" /></el-form-item>
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
