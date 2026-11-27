<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import {
  QuestionFilled, Grid, ChatDotRound, SwitchButton, ArrowRight, User,
} from '@element-plus/icons-vue';
import { useUserStore } from '@/stores/modules/user';
import { useThemeStore, type FontSize, type ThemeMode, type ThemeSkin } from '@/stores/modules/theme';
import { useLocaleStore } from '@/stores/modules/locale';
import { LOCALES, type AppLocale } from '@/locales';
import { unreadCount } from '@/api/system/me';
import { buildProducts, productOfPath, type ProductView } from '@/utils/productMenus';
import type { MenuNode } from '@/types';
import { resetDynamicRoutes } from '@/router/dynamicState';

const route = useRoute();
const router = useRouter();
const { t } = useI18n();
const userStore = useUserStore();
const themeStore = useThemeStore();
const localeStore = useLocaleStore();

const username = computed(() => userStore.userInfo?.name ?? t('layout.notLoggedIn'));
const orgName = computed(() => userStore.userInfo?.orgName || userStore.userInfo?.orgCode || t('layout.notAssignedOrg'));
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

function iconOf(node: MenuNode): string {
  if (typeof node.icon === 'string' && node.icon) return node.icon;
  return node.type === 'DIR' ? 'Folder' : 'Document';
}

/** 菜单名：按 path 命中语言包则翻译，否则回退数据库名。 */
function menuLabel(node: MenuNode): string {
  if (node.path) {
    const key = `menus.${node.path}`;
    const translated = t(key);
    if (translated !== key) return translated;
  }
  return node.name;
}

/** 产品名：按根路径命中语言包则翻译，否则回退数据库名。 */
function productLabel(product: ProductView): string {
  if (product.path) {
    const key = `products.${product.path}`;
    const translated = t(key);
    if (translated !== key) return translated;
  }
  return product.name;
}

function loadUnread(): void {
  unreadCount()
    .then((res) => { unread.value = res.data?.count || 0; })
    .catch((error) => console.error('load unread count failed:', error));
}

function goMessages(): void {
  void router.push('/iot/messages');
}

async function handleLogout(): Promise<void> {
  await userStore.logout();
  resetDynamicRoutes();
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
        <span class="kb-brand__name">{{ $t('layout.brand') }}</span>
      </div>

      <nav class="kb-products" aria-label="products">
        <button
          v-for="product in products"
          :key="product.id"
          type="button"
          class="kb-product"
          :class="{ 'is-active': activeProduct?.id === product.id }"
          :aria-current="activeProduct?.id === product.id ? 'page' : undefined"
          @click="selectProduct(product)"
        >
          {{ productLabel(product) }}
        </button>
      </nav>

      <div class="kb-tools">
        <el-popover placement="bottom-end" :width="240" trigger="click">
          <template #reference>
            <button type="button" class="kb-tool" :aria-label="$t('layout.help')">
              <el-icon :size="18"><QuestionFilled /></el-icon>
            </button>
          </template>
          <div class="kb-popover-title">{{ $t('layout.helpTitle') }}</div>
          <p class="kb-popover-text">{{ $t('layout.helpText') }}</p>
        </el-popover>

        <el-popover placement="bottom-end" :width="300" trigger="click">
          <template #reference>
            <button type="button" class="kb-tool kb-tool--theme" :aria-label="$t('layout.theme')">
              <el-icon :size="18"><Grid /></el-icon>
              <span>{{ $t('layout.theme') }}</span>
            </button>
          </template>
          <div class="kb-popover-title">{{ $t('layout.appearance') }}</div>
          <div class="kb-setting-row">
            <span>{{ $t('layout.skin') }}</span>
            <el-radio-group
              :model-value="themeStore.skin"
              size="small"
              @update:model-value="(v: ThemeSkin) => themeStore.setSkin(v)"
            >
              <el-radio-button value="blue">{{ $t('layout.skinBlue') }}</el-radio-button>
              <el-radio-button value="green">{{ $t('layout.skinGreen') }}</el-radio-button>
              <el-radio-button value="gray">{{ $t('layout.skinGray') }}</el-radio-button>
            </el-radio-group>
          </div>
          <div class="kb-setting-row">
            <span>{{ $t('layout.mode') }}</span>
            <el-radio-group
              :model-value="themeStore.mode"
              size="small"
              @update:model-value="(v: ThemeMode) => themeStore.setMode(v)"
            >
              <el-radio-button value="light">{{ $t('layout.light') }}</el-radio-button>
              <el-radio-button value="dark">{{ $t('layout.dark') }}</el-radio-button>
            </el-radio-group>
          </div>
          <div class="kb-setting-row">
            <span>{{ $t('layout.fontSize') }}</span>
            <el-radio-group
              :model-value="themeStore.fontSize"
              size="small"
              @update:model-value="(v: FontSize) => themeStore.setFontSize(v)"
            >
              <el-radio-button value="standard">{{ $t('layout.fontStandard') }}</el-radio-button>
              <el-radio-button value="large">{{ $t('layout.fontLarge') }}</el-radio-button>
            </el-radio-group>
          </div>
          <div class="kb-setting-row">
            <span>{{ $t('layout.language') }}</span>
            <el-radio-group
              :model-value="localeStore.locale"
              size="small"
              @update:model-value="(v: AppLocale) => localeStore.setLocale(v)"
            >
              <el-radio-button v-for="l in LOCALES" :key="l.value" :value="l.value">{{ l.label }}</el-radio-button>
            </el-radio-group>
          </div>
        </el-popover>

        <el-popover placement="bottom-end" :width="260" trigger="click">
          <template #reference>
            <button type="button" class="kb-tool kb-tool--msg" :aria-label="$t('layout.messages')">
              <el-icon :size="19"><ChatDotRound /></el-icon>
              <span v-if="unread" class="kb-msg-badge">{{ unread > 99 ? '99+' : unread }}</span>
            </button>
          </template>
          <div class="kb-popover-title">{{ $t('layout.messageCenter') }}</div>
          <p class="kb-popover-text">{{ $t('layout.unreadText', { count: unread }) }}</p>
          <el-button link type="primary" @click="goMessages">{{ $t('layout.viewAll') }}</el-button>
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
                <el-icon><SwitchButton /></el-icon>{{ $t('layout.logout') }}
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <el-container class="kb-body">
      <el-aside :width="'var(--kb-sidebar-width)'" class="kb-sidebar">
        <div class="kb-sidebar__title">{{ activeProduct ? productLabel(activeProduct) : $t('layout.workbench') }}</div>
        <el-scrollbar class="kb-sidebar__scroll">
          <el-menu :default-active="activeMenu" router class="kb-menu">
            <template v-for="node in activeProduct?.menus || []" :key="node.id">
              <el-sub-menu v-if="node.type === 'DIR' && node.children?.length" :index="node.id">
                <template #title>
                  <el-icon><component :is="iconOf(node)" /></el-icon>
                  <span>{{ menuLabel(node) }}</span>
                </template>
                <el-menu-item v-for="child in node.children" :key="child.id" :index="child.path || child.id">
                  <el-icon><component :is="iconOf(child)" /></el-icon>
                  <span>{{ menuLabel(child) }}</span>
                </el-menu-item>
              </el-sub-menu>
              <el-menu-item v-else-if="node.type === 'MENU' && node.path" :index="node.path">
                <el-icon><component :is="iconOf(node)" /></el-icon>
                <span>{{ menuLabel(node) }}</span>
                <el-icon class="kb-menu__arrow"><ArrowRight /></el-icon>
              </el-menu-item>
            </template>
          </el-menu>
          <div v-if="!activeProduct?.menus.length" class="kb-sidebar__empty">
            {{ $t('layout.sidebarEmpty') }}
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
