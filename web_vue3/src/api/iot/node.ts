import request from '../request';
import type { ApiResponse } from '@/types';

/** 节点实体（对应 iot_node） */
export interface NodeEntity {
  id: string;
  orgCode?: string;
  nodeCode: string;
  nodeName?: string;
  gatewayCode?: string;
  status?: number;
  /** 安装区域ID（iot_area.id） */
  areaId?: string;
  address?: string;
  longitude?: string;
  latitude?: string;
  nodeType?: number;
  firmwareVersion?: string;
  rssi?: string;
  snr?: string;
  speed?: string;
  batteryVoltage?: string;
  reportInterval?: number;
  lastReportTime?: string;
  online?: number;
  pic?: string;
  createTime?: string;
  updateTime?: string;
  isDeleted?: number;
}

export function listNodes(gatewayCode?: string, areaId?: string): Promise<ApiResponse<NodeEntity[]>> {
  return request<NodeEntity[]>({
    url: '/api/nodes',
    method: 'GET',
    params: { ...(gatewayCode ? { gatewayCode } : {}), ...(areaId ? { areaId } : {}) },
  });
}

export function getNode(id: string): Promise<ApiResponse<NodeEntity>> {
  return request<NodeEntity>({ url: `/api/nodes/${id}`, method: 'GET' });
}

export function createNode(data: Partial<NodeEntity>): Promise<ApiResponse<NodeEntity>> {
  return request<NodeEntity>({ url: '/api/nodes', method: 'POST', data });
}

export function updateNode(id: string, data: Partial<NodeEntity>): Promise<ApiResponse<NodeEntity>> {
  return request<NodeEntity>({ url: `/api/nodes/${id}`, method: 'PUT', data });
}

export function deleteNode(id: string): Promise<ApiResponse<null>> {
  return request<null>({ url: `/api/nodes/${id}`, method: 'DELETE' });
}
