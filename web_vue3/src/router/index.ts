import { createRouter, createWebHistory } from 'vue-router';
import { ElMessage } from 'element-plus';
import { routes } from './routes';
import { buildRoutesFromMenus } from './dynamicRoutes';
import { tokens } from '@/utils/token';
import { useUserStore } from '@/stores/modules/user';
import { i18n } from '@/locales';
import { isDynamicRegistered, setDynamicRegistered } from './dynamicState';

const t = i18n.global.t;

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior: () => ({ top: 0 }),
});

/** 注册动态菜单路由（仅一次；退出登录时 resetDynamicRoutes 重置标记）。返回本次是否新增了路由。 */
async function ensureDynamicRoutes(): Promise<boolean> {
  const userStore = useUserStore();
  if (isDynamicRegistered()) {
    return false;
  }
  await userStore.loadMe();
  if (userStore.menus.length > 0) {
    for (const record of buildRoutesFromMenus(userStore.menus)) {
      router.addRoute('Layout', record);
    }
    setDynamicRegistered(true);
    return true;
  }
  return false;
}

// 全局前置守卫：标题设置 + 登录鉴权 + 动态菜单路由 + 角色鉴权
router.beforeEach(async (to) => {
  const title = to.meta.title as string | undefined;
  // 静态路由 title 存 i18n key（route.*）；动态路由 title 为数据库菜单名，原样展示
  document.title = (title && title.startsWith('route.') ? t(title) : title) || import.meta.env.VITE_APP_TITLE;
  if (to.meta.requiresAuth && !tokens.access) {
    return { name: 'Login', query: { redirect: to.fullPath } };
  }
  if (to.meta.requiresAuth && tokens.access) {
    // 登录后（含刷新直链进入）先加载菜单并注册动态路由，避免 404。
    // addRoute 不会让当前导航重新解析，必须重推一次目标地址。
    const added = await ensureDynamicRoutes();
    if (added) {
      return { path: to.path, query: to.query, hash: to.hash, replace: true };
    }
    if (to.matched.length === 0) {
      return { name: 'Home' };
    }
  }
  // RBAC：meta.roles 声明允许访问的角色（与后端角色名大小写不敏感比对）
  // SUPER_ADMIN 为内置超管，恒通过（与 admin-service AdminRoleInterceptor 语义一致）
  const required = to.meta.roles as string[] | undefined;
  if (required && required.length > 0) {
    const userStore = useUserStore();
    const userRoles = (userStore.roles || []).map((r) => String(r).toLowerCase());
    // ORG_ADMIN 为机构管理员，业务页面访问权由服务端菜单/产品上限控制
    if (!userStore.superAdmin && !userRoles.includes('super_admin') && !userRoles.includes('org_admin')) {
      const allowed = required.some((r) => userRoles.includes(String(r).toLowerCase()));
      if (!allowed) {
        ElMessage.error(t('common.noAccessPage'));
        return { name: 'Home' };
      }
    }
  }
  return true;
});

export default router;
