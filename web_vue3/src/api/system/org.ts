import type { OrgNode, OrgRecord, ProductRecord } from '@/types';
import { adminHttp, unwrap } from './client';

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

export function fetchOrgProducts(orgId: string): Promise<ProductRecord[]> {
  return unwrap<ProductRecord[]>(adminHttp.get(`/api/admin/orgs/${orgId}/products`));
}

export function assignOrgProducts(orgId: string, productIds: string[]): Promise<string[]> {
  return unwrap<string[]>(adminHttp.put(`/api/admin/orgs/${orgId}/products`, { productIds }));
}
