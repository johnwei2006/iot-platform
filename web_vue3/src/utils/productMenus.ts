import type { MenuNode, ProductRecord } from '@/types';

export interface ProductView {
  id: string;
  name: string;
  path: string;
  sort: number;
  menus: MenuNode[];
}

/** 静态业务页面归入"物联网"产品，保证 admin-service 不可用时侧栏仍可用。 */
// 图标不在前端写死：由 sys_menu.icon 中同路径的菜单节点提供
const IOT_STATIC: MenuNode[] = [
  { id: 'static-home', parentId: null, name: '首页', type: 'MENU', path: '/home' },
];

const PRODUCT_PATHS: Record<string, string> = {
  SYS: '/system',
  SCREEN: '/screen',
  IOT: '/iot',
  FARM: '/farm',
  PRODUCTION: '/production',
};

const DEFAULT_PRODUCTS: ProductView[] = [
  { id: 'product-screen', name: '大屏', path: '/screen', sort: 5, menus: [] },
  { id: 'product-iot', name: '物联网', path: '/iot', sort: 10, menus: IOT_STATIC },
  { id: 'product-farm', name: '农事管理', path: '/farm', sort: 20, menus: [] },
  { id: 'product-system', name: '系统管理', path: '/system', sort: 99, menus: [] },
];

function menuIconByPath(nodes: MenuNode[], path: string): string | undefined {
  for (const node of nodes) {
    if (node.path === path) return node.icon;
    const found = menuIconByPath(node.children || [], path);
    if (found) return found;
  }
  return undefined;
}

/** 静态入口只在动态菜单未覆盖时补充，图标取自 sys_menu.icon。 */
function mergeIotStatic(product: ProductView, menus: MenuNode[]): ProductView {
  const paths = new Set(product.menus.map((m) => m.path));
  const added = IOT_STATIC
    .filter((m) => !paths.has(m.path || ''))
    .map((m) => ({ ...m, icon: menuIconByPath(menus, m.path || '') }));
  return { ...product, menus: [...product.menus, ...added] };
}

/**
 * 产品导航：优先使用服务端 /me.products（机构授权上限），
 * 兼容旧版以顶级 DIR 表示产品的菜单树；admin-service 不可用时回退默认产品。
 */
export function buildProducts(menus: MenuNode[], serverProducts?: ProductRecord[]): ProductView[] {
  const roots = menus.filter((node) => node.type === 'DIR' && !node.parentId);
  if (serverProducts && serverProducts.length) {
    const products = serverProducts
      .filter((p) => !p.status || p.status === 'ENABLED')
      .map((p) => {
        const path = PRODUCT_PATHS[p.code]
          || roots.find((root) => root.name === p.name)?.path
          || '/' + p.code.toLowerCase();
        const root = roots.find((node) => node.path === path);
        return {
          id: p.id,
          name: p.name,
          path,
          sort: Number(p.sort || 99),
          menus: root ? (root.children || []).filter((m) => m.type === 'MENU' || m.type === 'DIR') : [],
        } as ProductView;
      });
    return products.map((p) => (p.path === '/iot' ? mergeIotStatic(p, menus) : p))
      .sort((a, b) => a.sort - b.sort);
  }
  const products = new Map<string, ProductView>();
  for (const node of roots) {
    products.set(node.path || node.id, {
      id: node.id,
      name: node.name,
      path: node.path || node.id,
      sort: Number(node.sort || 99),
      menus: (node.children || []).filter((child) => child.type === 'MENU' || child.type === 'DIR'),
    });
  }
  if (products.size === 0) return DEFAULT_PRODUCTS;
  const iot = products.get('/iot');
  if (iot) products.set('/iot', mergeIotStatic(iot, menus));
  return [...products.values()].sort((a, b) => a.sort - b.sort);
}

/** 当前路由属于哪个产品：优先按产品 path 前缀，其次是产品菜单 path。 */
export function productOfPath(products: ProductView[], path: string): ProductView | undefined {
  const exact = products.find((p) => path === p.path || path.startsWith(p.path + '/'));
  if (exact) return exact;
  const inMenus = products.find((p) => p.menus.some((m) => m.path && path.startsWith(m.path)));
  return inMenus || products.find((p) => p.path === '/iot') || products[0];
}
