import type { MenuNode, ProductRecord } from '@/types';
import { adminHttp, unwrap } from './client';

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

