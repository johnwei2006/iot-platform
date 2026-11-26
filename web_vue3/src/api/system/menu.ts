import type { MenuNode } from '@/types';
import { adminHttp, unwrap } from './client';

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
