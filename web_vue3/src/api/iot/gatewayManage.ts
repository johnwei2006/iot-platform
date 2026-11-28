import request from '../request';
import type { ApiResponse } from '@/types';

export interface GatewayReportRecord {
  id: string;
  gatewayCode: string;
  serviceType: number;
  category: string;
  payload: string;
  reportedAt?: string;
  createdAt?: string;
}

export interface GatewayInfoHistoryRecord {
  id: string;
  gatewayCode: string;
  configVersion?: number;
  gatewayId?: string;
  projectId?: string;
  heartbeatInterval?: number;
  reportInterval?: number;
  rtcTime?: string;
  systemServerUrl?: string;
  systemServerPort?: number;
  hardwareVersion?: string;
  firmwareVersion?: string;
  gatewayMcuid?: string;
  memoryTotal?: number;
  memoryUsed?: number;
  flashTotal?: number;
  flashUsed?: number;
  uplinkMessageSize?: number;
  downlinkMessageSize?: number;
  battery?: string;
  adapterExists?: number;
  chargingEnabled?: number;
  latestRebootTime?: string;
  payload?: string;
  reportedAt?: string;
  createdAt?: string;
}

export interface GatewayInfoHistoryPage {
  records: GatewayInfoHistoryRecord[];
  total: number;
  page: number;
  size: number;
  pages: number;
}

export interface GatewayConfigRecord extends GatewayConfigPayload {
  id: string;
  gatewayCode: string;
  status: 'SUCCESS' | 'PENDING' | 'FAILED';
  configTime?: string;
  sendTime?: string;
  reportedAt?: string;
  createdAt?: string;
}

export interface GatewayCommandRecord {
  id: string;
  gatewayCode: string;
  serviceType: number;
  commandType: string;
  requestId?: string;
  payload: string;
  status: 'PENDING' | 'SUCCESS' | 'FAILED' | 'TIMEOUT';
  result?: string;
  reason?: string;
  sentAt?: string;
  finishedAt?: string;
  createdBy?: string;
  createdAt?: string;
}

export type GatewayInfoPayload = Record<string, string | number | boolean | null | undefined>;
export type GatewayConfigPayload = {
  configVersion?: number;
  gatewayId?: string;
  projectId?: string;
  systemServerUrl?: string;
  systemServerPort?: number;
  businessServerUrl?: string;
  heartbeatInterval?: number;
  reportInterval?: number;
  loraFreqGroup?: number;
};

export const GATEWAY_CONFIG_FIELDS: (keyof GatewayConfigPayload)[] = [
  'configVersion', 'gatewayId', 'projectId', 'systemServerUrl', 'systemServerPort',
  'businessServerUrl', 'heartbeatInterval', 'reportInterval', 'loraFreqGroup',
];

export function parseGatewayPayload<T extends object = GatewayInfoPayload>(payload?: string): T {
  if (!payload) return {} as T;
  try {
    const parsed = JSON.parse(payload);
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed as T : {} as T;
  } catch (error) {
    console.error('gateway payload parse failed', error);
    return {} as T;
  }
}

export interface GatewayTaskEntity {
  id?: string;
  gatewayCode: string;
  taskName: string;
  taskNo: number;
  enabled: boolean;
  triggerType: 'TIME' | 'CONDITION';
  triggerTime?: string;
  triggers?: string;
  actions?: string;
  deployStatus?: string;
  feedbackMsg?: string;
}

/** 网关节点行：附传感器/设备数量统计（joinTime 即节点创建时间） */
export interface GatewayNodeItem {
  nodeCode: string;
  nodeName?: string;
  nodeType?: number;
  gatewayCode?: string;
  joinTime?: string;
  reportInterval?: number;
  sensorCount: number;
  deviceCount: number;
}

export function listGatewayReports(params: { gatewayCode?: string; serviceType?: number; limit?: number }) {
  return request<GatewayReportRecord[]>({ url: '/api/gateway-manage/reports', method: 'GET', params });
}

export function listGatewayInfoHistory(params: { gatewayCode?: string; page?: number; size?: number }) {
  return request<GatewayInfoHistoryPage>({ url: '/api/gateway-manage/info-history', method: 'GET', params });
}

export function listGatewayConfigs(params: { gatewayCode?: string; limit?: number }) {
  return request<GatewayConfigRecord[]>({ url: '/api/gateway-manage/configs', method: 'GET', params });
}

export function saveGatewayConfig(data: Record<string, unknown>) {
  return request<GatewayConfigRecord>({ url: '/api/gateway-manage/configs', method: 'POST', data });
}

export function getGatewayInfo(gatewayCode: string) {
  return request<GatewayInfoPayload & { reportedAt?: string }>({
    url: '/api/gateway-manage/info',
    method: 'GET',
    params: { gatewayCode },
  });
}

export function listGatewayCommands(params: { gatewayCode?: string; serviceType?: number; limit?: number }) {
  return request<GatewayCommandRecord[]>({ url: '/api/gateway-manage/commands', method: 'GET', params });
}

export function listGatewayNodes(gatewayCode: string) {
  return request<GatewayNodeItem[]>({
    url: '/api/gateway-manage/nodes',
    method: 'GET',
    params: { gatewayCode },
  });
}

function sendConfig(path: string, data: Record<string, unknown>): Promise<ApiResponse<GatewayCommandRecord>> {
  return request<GatewayCommandRecord>({ url: `/api/gateway-manage/config/${path}`, method: 'POST', data });
}

export const configRadio = (data: Record<string, unknown>) => sendConfig('radio', data);
export const configNode = (data: Record<string, unknown>) => sendConfig('node', data);
export const configSensor = (data: Record<string, unknown>) => sendConfig('sensor', data);
export const configDevice = (data: Record<string, unknown>) => sendConfig('device', data);

export function rebootGateway(data: Record<string, unknown>) {
  return request<GatewayCommandRecord>({ url: '/api/gateway-manage/reboot', method: 'POST', data });
}

export function listGatewayTasks(gatewayCode?: string) {
  return request<GatewayTaskEntity[]>({ url: '/api/gateway-manage/tasks', method: 'GET', params: { gatewayCode } });
}

export function saveGatewayTask(data: GatewayTaskEntity) {
  return request<GatewayTaskEntity>({ url: '/api/gateway-manage/tasks', method: 'POST', data });
}

export function deleteGatewayTask(id: string) {
  return request<boolean>({ url: `/api/gateway-manage/tasks/${encodeURIComponent(id)}`, method: 'DELETE' });
}

export function deployGatewayTasks(data: Record<string, unknown>) {
  return request<GatewayCommandRecord>({ url: '/api/gateway-manage/tasks/deploy', method: 'POST', data });
}
