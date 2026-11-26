import type { RoleView, ScopeEntity } from '@/types';
import { adminHttp, unwrap } from './client';

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
