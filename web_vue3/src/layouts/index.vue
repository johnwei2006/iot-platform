<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
  QuestionFilled, Grid, ChatDotRound, SwitchButton, ArrowRight, User,
} from '@element-plus/icons-vue';
import { useUserStore } from '@/stores/modules/user';
import { useThemeStore, type FontSize, type ThemeMode } from '@/stores/modules/theme';
import { unreadCount } from '@/api/kb';
import { buildProducts, productOfPath, type ProductView } from '@/utils/productMenus';
import type { MenuNode } from '@/types';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const themeStore = useThemeStore();

const username = computed(() => userStore.userInfo?.name ?? '未登录');
const orgName = computed(() => userStore.userInfo?.orgName || userStore.userInfo?.orgCode || '未分配机构');
const activeMenu = computed(() => route.path);
const products = computed<ProductView[]>(() => buildProducts(userStore.menus, userStore.products));

const activeProduct = ref<ProductView | null>(null);
const unread = ref(0);

function syncProduct(): void {
  const hit = productOfPath(products.value, route.path);
  if (hit && hit.menus.some((m) => m.path && route.path.startsWith(m.path))) {
    activeProduct.value = hit;
  } else if (!activeProduct.value) {
    activeProduct.value = hit || products.value[0] || null;
  }
}

function selectProduct(product: ProductView): void {
  activeProduct.value = product;
  const first = product.menus.find((m) => m.type === 'MENU' && m.path);
  if (first?.path) void router.push(first.path);
}

function iconOf(node: MenuNode): string | undefined {
  return typeof node.icon === 'string' && node.icon ? node.icon : undefined;
}

function loadUnread(): void {
  unreadCount()
    .then((res) => { unread.value = res.data?.count || 0; })
    .catch((error) => console.error('load unread count failed:', error));
}

function goMessages(): void {
  void router.push('/iot/messages');
}

function goConflictCheck(): void {
  void router.push('/iot/conflict-check');
}

async function handleLogout(): Promise<void> {
  await userStore.logout();
  await router.push({ name: 'Login' });
}

watch(() => route.path, () => {
  syncProduct();
  loadUnread();
});
watch(products, syncProduct);
onMounted(() => {
  syncProduct();
  loadUnread();
});
</script>
<template>
  <el-container class="kb-shell">
    <header class="kb-topbar">
      <div class="kb-brand">
        <span class="kb-brand__mark" aria-hidden="true">科</span>
        <span class="kb-brand__name">科百云田</span>
      </div>

      <nav class="kb-products" aria-label="产品导航">
        <button
          v-for="product in products"
          :key="product.id"
          type="button"
          class="kb-product"
          :class="{ 'is-active': activeProduct?.id === product.id }"
          :aria-current="activeProduct?.id === product.id ? 'page' : undefined"
          @click="selectProduct(product)"
        >
          {{ product.name }}
        </button>
      </nav>

      <div class="kb-tools">
        <el-popover placement="bottom-end" :width="240" trigger="click">
          <template #reference>
            <button type="button" class="kb-tool" aria-label="帮助">
              <el-icon :size="18"><QuestionFilled /></el-icon>
            </button>
          </template>
          <div class="kb-popover-title">帮助与支持</div>
          <p class="kb-popover-text">可按产品切换左侧菜单；消息角标显示未读任务与告警。</p>
          <el-button link type="primary" @click="goConflictCheck">打开冲突校验工具</el-button>
        </el-popover>

        <el-popover placement="bottom-end" :width="254" trigger="click">
          <template #reference>
            <button type="button" class="kb-tool kb-tool--theme" aria-label="主题设置">
              <el-icon :size="18"><Grid /></el-icon>
              <span>主题</span>
            </button>
          </template>
          <div class="kb-popover-title">外观设置</div>
          <div class="kb-setting-row">
            <span>模式</span>
            <el-radio-group
              :model-value="themeStore.mode"
              size="small"
              @update:model-value="(v: ThemeMode) => themeStore.setMode(v)"
            >
              <el-radio-button value="light">浅色</el-radio-button>
              <el-radio-button value="dark">深色</el-radio-button>
            </el-radio-group>
          </div>
          <div class="kb-setting-row">
            <span>字号</span>
            <el-radio-group
              :model-value="themeStore.fontSize"
              size="small"
              @update:model-value="(v: FontSize) => themeStore.setFontSize(v)"
            >
              <el-radio-button value="standard">标准</el-radio-button>
              <el-radio-button value="large">大字号</el-radio-button>
            </el-radio-group>
          </div>
        </el-popover>

        <el-popover placement="bottom-end" :width="260" trigger="click">
          <template #reference>
            <button type="button" class="kb-tool kb-tool--msg" aria-label="消息">
              <el-icon :size="19"><ChatDotRound /></el-icon>
              <span v-if="unread" class="kb-msg-badge">{{ unread > 99 ? '99+' : unread }}</span>
            </button>
          </template>
          <div class="kb-popover-title">消息中心</div>
          <p class="kb-popover-text">当前有 {{ unread }} 条未读消息，包含任务与告警。</p>
          <el-button link type="primary" @click="goMessages">查看全部消息</el-button>
        </el-popover>

        <el-dropdown trigger="click" @command="(cmd: string) => cmd === 'logout' && handleLogout()">
          <button type="button" class="kb-user">
            <span class="kb-avatar"><el-icon :size="15"><User /></el-icon></span>
            <span class="kb-user__name">{{ username }}</span>
            <span class="kb-user__sep">|</span>
            <span class="kb-user__org">{{ orgName }}</span>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">
                <el-icon><SwitchButton /></el-icon>退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <el-container class="kb-body">
      <el-aside :width="'var(--kb-sidebar-width)'" class="kb-sidebar">
        <div class="kb-sidebar__title">{{ activeProduct?.name || '工作台' }}</div>
        <el-scrollbar class="kb-sidebar__scroll">
          <el-menu :default-active="activeMenu" router class="kb-menu">
            <template v-for="node in activeProduct?.menus || []" :key="node.id">
              <el-sub-menu v-if="node.type === 'DIR' && node.children?.length" :index="node.id">
                <template #title>
                  <el-icon v-if="iconOf(node)"><component :is="iconOf(node)" /></el-icon>
                  <span>{{ node.name }}</span>
                </template>
                <el-menu-item v-for="child in node.children" :key="child.id" :index="child.path || child.id">
                  <el-icon v-if="iconOf(child)"><component :is="iconOf(child)" /></el-icon>
                  <span>{{ child.name }}</span>
                </el-menu-item>
              </el-sub-menu>
              <el-menu-item v-else-if="node.type === 'MENU' && node.path" :index="node.path">
                <el-icon v-if="iconOf(node)"><component :is="iconOf(node)" /></el-icon>
                <span>{{ node.name }}</span>
                <el-icon class="kb-menu__arrow"><ArrowRight /></el-icon>
              </el-menu-item>
            </template>
          </el-menu>
          <div v-if="!activeProduct?.menus.length" class="kb-sidebar__empty">
            该产品暂无可用菜单，请联系管理员分配。
          </div>
        </el-scrollbar>
      </el-aside>
      <el-main class="kb-content">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>
<style scoped>
.kb-shell { height: 100vh; flex-direction: column; }
.kb-topbar {
  height: 56px; flex: 0 0 56px; display: flex; align-items: center; gap: 28px;
  padding: 0 20px; background: var(--kb-topbar-bg); color: var(--kb-topbar-text);
  border-bottom: 2px solid var(--kb-brand);
}
.kb-brand { display: flex; align-items: center; gap: 10px; flex: 0 0 auto; }
.kb-brand__mark {
  width: 28px; height: 28px; border-radius: 50%; display: grid; place-items: center;
  background: var(--kb-brand-bright); color: #fff; font-size: 14px; font-weight: 700;
}
.kb-brand__name { color: #fff; font-size: var(--kb-fs-subheading); font-weight: 700; letter-spacing: var(--kb-tracking-heading); }
.kb-products { display: flex; align-items: stretch; height: 100%; gap: 4px; flex: 1; min-width: 0; overflow-x: auto; }
.kb-product {
  appearance: none; border: 0; background: transparent; color: inherit; cursor: pointer;
  font: inherit; font-size: var(--kb-fs-body); padding: 0 16px; white-space: nowrap;
  border-bottom: 2px solid transparent; transition: color var(--kb-duration) var(--kb-ease);
}
.kb-product:hover { color: #fff; }
.kb-product.is-active { color: var(--kb-topbar-active); font-weight: 700; border-bottom-color: var(--kb-brand-bright); }
.kb-tools { display: flex; align-items: center; gap: 6px; flex: 0 0 auto; }
.kb-tool {
  appearance: none; border: 1px solid transparent; background: transparent; color: inherit;
  height: 34px; min-width: 34px; padding: 0 8px; border-radius: var(--kb-radius-btn);
  display: inline-flex; align-items: center; justify-content: center; gap: 6px; cursor: pointer;
  font: inherit; font-size: var(--kb-fs-caption); transition: background var(--kb-duration) var(--kb-ease);
}
.kb-tool:hover { background: rgba(255, 255, 255, 0.1); }
.kb-tool--theme { background: rgba(255, 255, 255, 0.08); }
.kb-msg-badge {
  position: absolute; transform: translate(11px, -10px); min-width: 16px; height: 16px; padding: 0 4px;
  border-radius: var(--kb-radius-pill); background: var(--kb-danger); color: #fff;
  font-size: 10px; line-height: 16px; text-align: center;
}
.kb-user {
  appearance: none; border: 0; background: transparent; color: inherit; cursor: pointer;
  display: flex; align-items: center; gap: 8px; font: inherit; font-size: var(--kb-fs-caption);
  padding: 4px 6px; border-radius: var(--kb-radius-pill);
}
.kb-user:hover { background: rgba(255, 255, 255, 0.1); }
.kb-avatar {
  width: 26px; height: 26px; border-radius: 50%; display: grid; place-items: center;
  background: var(--kb-brand); color: #fff; font-size: 12px; font-weight: 700;
}
.kb-user__name { color: #fff; font-weight: 600; }
.kb-user__sep { opacity: 0.4; }
.kb-body { min-height: 0; }
.kb-sidebar { background: var(--kb-bg-card); border-right: 1px solid var(--kb-line); }
.kb-sidebar__title {
  height: 48px; display: flex; align-items: center; padding: 0 18px;
  color: var(--kb-brand); font-weight: 700; font-size: var(--kb-fs-subheading);
  border-bottom: 1px solid var(--kb-line);
}
.kb-sidebar__scroll { height: calc(100vh - 56px - 48px); }
.kb-menu { border: 0; background: transparent; }
.kb-menu :deep(.el-menu-item), .kb-menu :deep(.el-sub-menu__title) { height: 42px; line-height: 42px; font-size: var(--kb-fs-body); }
.kb-menu :deep(.el-menu-item.is-active) { background: var(--kb-brand-wash); color: var(--kb-brand); font-weight: 600; }
.kb-menu__arrow { margin-left: auto; opacity: 0.45; }
.kb-sidebar__empty { padding: 24px 18px; color: var(--kb-text-muted); font-size: var(--kb-fs-caption); line-height: 1.6; }
.kb-content { background: var(--kb-bg-page); padding: 16px; overflow: auto; }
.kb-popover-title { font-weight: 700; color: var(--kb-text); margin-bottom: 8px; }
.kb-popover-text { color: var(--kb-text-muted); font-size: var(--kb-fs-caption); line-height: 1.6; margin: 0 0 8px; }
.kb-setting-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 8px 0; font-size: var(--kb-fs-body); }
</style>
