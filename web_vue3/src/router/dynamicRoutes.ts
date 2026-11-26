import type { RouteRecordRaw } from 'vue-router';
import type { MenuNode } from '@/types';

/**
 * 动态路由注册：后端菜单树（admin-service /api/admin/me 返回）中的 MENU 节点
 * 映射为 Layout children 路由，component 字段指向 views 下的组件路径。
 * 静态业务路由（home/网关详情等）仍由 routes.ts 维护，动态部分仅追加。
 */
const viewModules = import.meta.glob('../views/**/*.vue');

function resolveView(component?: string): RouteRecordRaw['component'] | null {
  if (!component) {
    return null;
  }
  const key = `../views/${component}.vue`;
  const loader = viewModules[key];
  if (!loader) {
    console.error(`[dynamicRoutes] view component not found: ${key}`);
    return null;
  }
  return loader as RouteRecordRaw['component'];
}

function routeNameOf(menu: MenuNode): string {
  const path = (menu.path || menu.component || menu.id).replace(/^\//, '');
  return (
    'D' +
    path
      .split(/[^a-zA-Z0-9]+/)
      .filter(Boolean)
      .map((seg) => seg.charAt(0).toUpperCase() + seg.slice(1))
      .join('')
  );
}

/** 收集菜单树中的 MENU 节点并生成路由记录（path 需以 / 开头挂到 Layout 下）。 */
export function buildRoutesFromMenus(menus: MenuNode[]): RouteRecordRaw[] {
  const routes: RouteRecordRaw[] = [];
  const walk = (nodes: MenuNode[]) => {
    for (const node of nodes) {
      if (node.type === 'MENU' && node.path && !node.path.startsWith('http')) {
        const component = resolveView(node.component);
        if (component) {
          routes.push({
            path: node.path.startsWith('/') ? node.path : `/${node.path}`,
            name: routeNameOf(node),
            component,
            meta: { title: node.name, requiresAuth: true, icon: node.icon },
          });
        }
      }
      if (node.children?.length) {
        walk(node.children);
      }
    }
  };
  walk(menus);
  return routes;
}
