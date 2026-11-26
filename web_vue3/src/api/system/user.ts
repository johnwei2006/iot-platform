import type { AdminUser } from '@/types';
import { adminHttp, unwrap } from './client';

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
