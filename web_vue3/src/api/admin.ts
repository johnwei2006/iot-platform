import axios, { type AxiosInstance } from 'axios';
import type {
  AdminUser,
  ApiResponse,
  MeResponse,
  MenuNode,
  OrgNode,
  OrgRecord,
  ProductRecord,
  RoleView,
  ScopeEntity,
} from '@/types';

/** admin-service 客户端：经 /admin-api 代理（生产同域反代），JWT 由拦截器注入 */
const adminHttp: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_APP_ADMIN_API_BASE_URL || '/admin-api',
  timeout: Number(import.meta.env.VITE_APP_TIMEOUT || 15000),
});

adminHttp.interceptors.request.use((config) => {
  const raw = localStorage.getItem('app_token');
  const token = raw ? JSON.parse(raw) : '';
  if (token) {
    config.headers = config.headers || {};
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

function unwrap<T>(promise: Promise<{ data: ApiResponse<T> }>): Promise<T> {
  return promise.then((r) => {
    const body = r.data;
    if (body.code !== 200) {
      throw new Error(body.message || '请求失败');
    }
    return body.data;
  });
}

/** 当前管理员上下文：动态菜单/按钮权限/数据权限 */
export function fetchMe(): Promise<MeResponse> {
  return unwrap<MeResponse>(adminHttp.get('/api/admin/me'));
}

/** ============ 机构管理 ============ */
export function fetchOrgTree(): Promise<OrgNode[]> {
  return unwrap<OrgNode[]>(adminHttp.get('/api/admin/orgs/tree'));
}

export function fetchOrgList(): Promise<OrgRecord[]> {
  return unwrap<OrgRecord[]>(adminHttp.get('/api/admin/orgs'));
}

export function createOrg(payload: {
  code: string;
  name: string;
  leader?: string;
  phone?: string;
  sort?: string;
}): Promise<OrgRecord> {
  return unwrap<OrgRecord>(adminHttp.post('/api/admin/orgs', payload));
}

export function updateOrg(
  id: string,
  payload: { name?: string; leader?: string; phone?: string; sort?: string; status?: string },
): Promise<OrgRecord> {
  return unwrap<OrgRecord>(adminHttp.put(`/api/admin/orgs/${id}`, payload));
}

export function createOrgWithAdmin(payload: {
  code: string;
  name: string;
  leader?: string;
  phone?: string;
  sort?: string;
  adminUsername: string;
  adminPassword: string;
}): Promise<OrgRecord> {
  return unwrap<OrgRecord>(adminHttp.post('/api/admin/orgs/create-with-admin', payload));
}

export function deleteOrg(id: string): Promise<void> {
  return unwrap<void>(adminHttp.delete(`/api/admin/orgs/${id}`));
}

/** ============ 产品与授权关系 ============ */
export function fetchProducts(): Promise<ProductRecord[]> {
  return unwrap<ProductRecord[]>(adminHttp.get('/api/admin/products'));
}

export function createProduct(payload: {
  code: string; name: string; description?: string; sort?: string;
}): Promise<ProductRecord> {
  return unwrap<ProductRecord>(adminHttp.post('/api/admin/products', payload));
}

export function updateProduct(
  id: string,
  payload: { name?: string; description?: string; sort?: string; status?: string },
): Promise<ProductRecord> {
  return unwrap<ProductRecord>(adminHttp.put(`/api/admin/products/${id}`, payload));
}

export function deleteProduct(id: string): Promise<void> {
  return unwrap<void>(adminHttp.delete(`/api/admin/products/${id}`));
}

export function fetchProductMenus(id: string): Promise<MenuNode[]> {
  return unwrap<MenuNode[]>(adminHttp.get(`/api/admin/products/${id}/menus`));
}

export function bindProductMenus(id: string, menuIds: string[]): Promise<string[]> {
  return unwrap<string[]>(adminHttp.put(`/api/admin/products/${id}/menus`, { menuIds }));
}

export function fetchOrgProducts(orgId: string): Promise<ProductRecord[]> {
  return unwrap<ProductRecord[]>(adminHttp.get(`/api/admin/orgs/${orgId}/products`));
}

export function assignOrgProducts(orgId: string, productIds: string[]): Promise<string[]> {
  return unwrap<string[]>(adminHttp.put(`/api/admin/orgs/${orgId}/products`, { productIds }));
}

/** ============ 用户管理（凭证在 auth-service，admin 代理） ============ */
export function fetchAdminUsers(): Promise<AdminUser[]> {
  return unwrap<AdminUser[]>(adminHttp.get('/api/admin/users'));
}

export function createAdminUser(payload: {
  username: string;
  password: string;
  roles: string[];
}): Promise<AdminUser> {
  return unwrap<AdminUser>(adminHttp.post('/api/admin/users', payload));
}

export function updateAdminUser(
  id: string,
  payload: { username?: string; status?: string },
): Promise<AdminUser> {
  return unwrap<AdminUser>(adminHttp.put(`/api/admin/users/${id}`, payload));
}

export function resetAdminUserPassword(id: string, password: string): Promise<AdminUser> {
  return unwrap<AdminUser>(adminHttp.post(`/api/admin/users/${id}/password`, { password }));
}

/** 分配机构与/或角色（合并数据权限后同步 auth-service） */
export function assignAdminUser(
  id: string,
  payload: { roles?: string[]; orgId?: string },
): Promise<AdminUser> {
  return unwrap<AdminUser>(adminHttp.post(`/api/admin/users/${id}/assign`, payload));
}

/** ============ 角色管理 ============ */
export function fetchRoles(): Promise<RoleView[]> {
  return unwrap<RoleView[]>(adminHttp.get('/api/admin/roles'));
}

export function createRole(payload: {
  code: string;
  name: string;
  menuIds: string[];
  dataScopes: Record<string, string>;
}): Promise<RoleView> {
  return unwrap<RoleView>(adminHttp.post('/api/admin/roles', payload));
}

export function updateRole(
  id: string,
  payload: { name?: string; menuIds?: string[]; dataScopes?: Record<string, string>; status?: string },
): Promise<RoleView> {
  return unwrap<RoleView>(adminHttp.put(`/api/admin/roles/${id}`, payload));
}

export function deleteRole(id: string): Promise<void> {
  return unwrap<void>(adminHttp.delete(`/api/admin/roles/${id}`));
}

export function fetchScopeEntities(): Promise<ScopeEntity[]> {
  return unwrap<ScopeEntity[]>(adminHttp.get('/api/admin/roles/entities'));
}

/** ============ 菜单管理 ============ */
export function fetchMenuTree(): Promise<MenuNode[]> {
  return unwrap<MenuNode[]>(adminHttp.get('/api/admin/menus/tree'));
}

export function fetchMenuList(): Promise<MenuNode[]> {
  return unwrap<MenuNode[]>(adminHttp.get('/api/admin/menus'));
}

export function createMenu(payload: {
  parentId?: string | null;
  name: string;
  type: string;
  path?: string;
  component?: string;
  perm?: string;
  icon?: string;
  sort?: string;
}): Promise<MenuNode> {
  return unwrap<MenuNode>(adminHttp.post('/api/admin/menus', payload));
}

export function updateMenu(
  id: string,
  payload: {
    name?: string;
    path?: string;
    component?: string;
    perm?: string;
    icon?: string;
    sort?: string;
    status?: string;
  },
): Promise<MenuNode> {
  return unwrap<MenuNode>(adminHttp.put(`/api/admin/menus/${id}`, payload));
}

export function deleteMenu(id: string): Promise<void> {
  return unwrap<void>(adminHttp.delete(`/api/admin/menus/${id}`));
}
