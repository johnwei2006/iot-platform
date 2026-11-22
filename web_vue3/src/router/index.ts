import { createRouter, createWebHistory } from 'vue-router';
import { ElMessage } from 'element-plus';
import { routes } from './routes';
import { buildRoutesFromMenus } from './dynamicRoutes';
import { tokens } from '@/utils/token';
import { useUserStore } from '@/stores/modules/user';

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior: () => ({ top: 0 }),
});

let dynamicRegistered = false;

/** 注册动态菜单路由（仅一次；重新登录后 resetMe 会重置标记）。 */
async function ensureDynamicRoutes() {
  const userStore = useUserStore();
  if (dynamicRegistered) {
    return;
  }
  await userStore.loadMe();
  if (userStore.menus.length > 0) {
    for (const record of buildRoutesFromMenus(userStore.menus)) {
      router.addRoute('Layout', record);
    }
    dynamicRegistered = true;
  }
}

// 全局前置守卫：标题设置 + 登录鉴权 + 动态菜单路由 + 角色鉴权
router.beforeEach(async (to) => {
  document.title = (to.meta.title as string) || import.meta.env.VITE_APP_TITLE;
  if (to.meta.requiresAuth && !tokens.access) {
    return { name: 'Login', query: { redirect: to.fullPath } };
  }
  if (to.meta.requiresAuth && tokens.access) {
    // 登录后（含刷新直链进入）先加载菜单并注册动态路由，避免 404
    await ensureDynamicRoutes();
    if (to.matched.length === 0) {
      return { name: 'Home' };
    }
  }
  // RBAC：meta.roles 声明允许访问的角色（与后端角色名大小写不敏感比对）
  const required = to.meta.roles as string[] | undefined;
  if (required && required.length > 0) {
    const userStore = useUserStore();
    const userRoles = (userStore.roles || []).map((r) => String(r).toLowerCase());
    const allowed = required.some((r) => userRoles.includes(String(r).toLowerCase()));
    if (!allowed) {
      ElMessage.error('当前账号无权访问该页面');
      return { name: 'Home' };
    }
  }
  return true;
});

/** 重新登录后重置动态路由标记（由 user store 的 resetMe 触发场景使用）。 */
export function resetDynamicRoutes() {
  dynamicRegistered = false;
}

export default router;
