import { createRouter, createWebHistory } from 'vue-router';
import { ElMessage } from 'element-plus';
import { routes } from './routes';
import { tokens } from '@/utils/token';
import { useUserStore } from '@/stores/modules/user';

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior: () => ({ top: 0 }),
});

// 全局前置守卫：标题设置 + 登录鉴权 + 角色鉴权
router.beforeEach((to) => {
  document.title = (to.meta.title as string) || import.meta.env.VITE_APP_TITLE;
  if (to.meta.requiresAuth && !tokens.access) {
    return { name: 'Login', query: { redirect: to.fullPath } };
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

export default router;
