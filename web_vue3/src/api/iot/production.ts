import request from '../request';
import type { ApiResponse, Device } from '@/types';
import type { NodeEntity } from './node';
import type { SensorEntity } from './sensor';
import type { DeviceCtrlEntity } from './device';

export type ProductionKind = 'GATEWAY' | 'NODE' | 'SENSOR' | 'CTRL';

/**
 * 生产管理 API：
 * - 列表返回全量设备（含已分配），前端展示分配状态与归属机构；
 * - 生产入库（编号后端自动生成、不属于任何机构）；
 * - 更新/删除仅对未分配设备有效（已分配返回 403）；
 * - 网关级联分配机构。
 */

export interface AssignStat { nodes: number; sensors: number; devices: number }

/* ---------- 生产网关 ---------- */

export function listProductionGateways(): Promise<ApiResponse<Device[]>> {
  return request<Device[]>({ url: '/api/production/gateways', method: 'GET' });
}

/** 未分配网关（归属下拉与分配候选）。 */
export function listUnassignedGateways(): Promise<ApiResponse<Device[]>> {
  return request<Device[]>({ url: '/api/production/gateways/unassigned', method: 'GET' });
}

export function createProductionGateway(data: Partial<Device>): Promise<ApiResponse<Device>> {
  return request<Device>({ url: '/api/production/gateways', method: 'POST', data });
}

export function updateProductionGateway(id: string, data: Partial<Device>): Promise<ApiResponse<Device>> {
  return request<Device>({ url: `/api/production/gateways/${id}`, method: 'PUT', data });
}

export function deleteProductionGateway(id: string): Promise<ApiResponse<null>> {
  return request<null>({ url: `/api/production/gateways/${id}`, method: 'DELETE' });
}

export function assignGatewayOrg(id: string, orgCode: string): Promise<ApiResponse<AssignStat>> {
  return request<AssignStat>({ url: `/api/production/gateways/${id}/assign`, method: 'POST', data: { orgCode } });
}

/* ---------- 生产节点 ---------- */

export function listProductionNodes(): Promise<ApiResponse<NodeEntity[]>> {
  return request<NodeEntity[]>({ url: '/api/production/nodes', method: 'GET' });
}

/** 未分配节点（归属下拉候选）。 */
export function listUnassignedNodes(): Promise<ApiResponse<NodeEntity[]>> {
  return request<NodeEntity[]>({ url: '/api/production/nodes/unassigned', method: 'GET' });
}

export function createProductionNode(data: Partial<NodeEntity>): Promise<ApiResponse<NodeEntity>> {
  return request<NodeEntity>({ url: '/api/production/nodes', method: 'POST', data });
}

export function updateProductionNode(id: string, data: Partial<NodeEntity>): Promise<ApiResponse<NodeEntity>> {
  return request<NodeEntity>({ url: `/api/production/nodes/${id}`, method: 'PUT', data });
}

export function deleteProductionNode(id: string): Promise<ApiResponse<null>> {
  return request<null>({ url: `/api/production/nodes/${id}`, method: 'DELETE' });
}

/* ---------- 生产传感器 ---------- */

export function listProductionSensors(): Promise<ApiResponse<SensorEntity[]>> {
  return request<SensorEntity[]>({ url: '/api/production/sensors', method: 'GET' });
}

export function createProductionSensor(data: Partial<SensorEntity>): Promise<ApiResponse<SensorEntity>> {
  return request<SensorEntity>({ url: '/api/production/sensors', method: 'POST', data });
}

export function updateProductionSensor(id: string, data: Partial<SensorEntity>): Promise<ApiResponse<SensorEntity>> {
  return request<SensorEntity>({ url: `/api/production/sensors/${id}`, method: 'PUT', data });
}

export function deleteProductionSensor(id: string): Promise<ApiResponse<null>> {
  return request<null>({ url: `/api/production/sensors/${id}`, method: 'DELETE' });
}

/* ---------- 生产控制设备 ---------- */

export function listProductionControlDevices(): Promise<ApiResponse<DeviceCtrlEntity[]>> {
  return request<DeviceCtrlEntity[]>({ url: '/api/production/control-devices', method: 'GET' });
}

export function createProductionControlDevice(data: Partial<DeviceCtrlEntity>): Promise<ApiResponse<DeviceCtrlEntity>> {
  return request<DeviceCtrlEntity>({ url: '/api/production/control-devices', method: 'POST', data });
}

export function updateProductionControlDevice(id: string, data: Partial<DeviceCtrlEntity>): Promise<ApiResponse<DeviceCtrlEntity>> {
  return request<DeviceCtrlEntity>({ url: `/api/production/control-devices/${id}`, method: 'PUT', data });
}

export function deleteProductionControlDevice(id: string): Promise<ApiResponse<null>> {
  return request<null>({ url: `/api/production/control-devices/${id}`, method: 'DELETE' });
}

/** 表单打开时获取下一个自动编号（可按类型/大类计算编码片段）。 */
export function getNextProductionCode(kind: ProductionKind, params?: { type?: number; clazz?: string }): Promise<ApiResponse<string>> {
  return request<string>({ url: '/api/production/next-code', method: 'GET', params: { kind, ...params } });
}

/** 编号唯一性校验；id 用于编辑时排除自身。 */
export function checkProductionCode(kind: ProductionKind, code: string, id?: string): Promise<ApiResponse<boolean>> {
  return request<boolean>({ url: '/api/production/check-code', method: 'GET', params: { kind, code, id } });
}
