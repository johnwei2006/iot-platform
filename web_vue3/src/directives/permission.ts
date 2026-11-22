import type { Directive } from 'vue';
import { useUserStore } from '@/stores/modules/user';

/**
 * v-permission 按钮级权限指令：
 *   v-permission="'system:org:create'" 或 v-permission="['a','b']"（任一满足）
 * 无权限时直接移除元素；SUPER_ADMIN 恒通过。
 */
export const permission: Directive<HTMLElement, string | string[]> = {
  mounted(el, binding) {
    const store = useUserStore();
    const required = binding.value;
    let allowed = store.superAdmin;
    if (!allowed && required) {
      const perms = store.perms;
      const need = Array.isArray(required) ? required : [required];
      allowed = need.some((p) => perms.includes(p));
    }
    if (!allowed) {
      el.parentNode?.removeChild(el);
    }
  },
};

export function setupDirectives(app: import('vue').App) {
  app.directive('permission', permission);
}
