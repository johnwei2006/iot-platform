import request from '../request';
import type { ApiResponse, DataRecord, Device, DownlinkPayload } from '@/types';
import type { MetricThreshold, RealtimeStatus } from '@/types/kb';

export function listDevices(areaId?: string): Promise<ApiResponse<Device[]>> {
  return request<Device[]>({ url: '/api/devices', method: 'GET', params: areaId ? { areaId } : undefined });
}

export function getDevice(id: string): Promise<ApiResponse<Device>> {
  return request<Device>({ url: `/api/devices/${id}`, method: 'GET' });
}

export function createDevice(data: Partial<Device>): Promise<ApiResponse<Device>> {
  return request<Device>({ url: '/api/devices', method: 'POST', data });
}

export function updateDevice(id: string, data: Partial<Device>): Promise<ApiResponse<Device>> {
  return request<Device>({ url: `/api/devices/${id}`, method: 'PUT', data });
}

export function deleteDevice(id: string): Promise<ApiResponse<null>> {
  return request<null>({ url: `/api/devices/${id}`, method: 'DELETE' });
}

/** 向设备下发指令（经 platform_java -> dtm -> 网关） */
export function sendDownlink(id: string, data: DownlinkPayload): Promise<ApiResponse<null>> {
  return request<null>({ url: `/api/devices/${id}/downlink`, method: 'POST', data });
}

/** 更新网关认证预共享密钥；secret 为空时由后台自动生成随机密钥（返回掩码） */
export function updateDeviceSecret(id: string, secret?: string): Promise<ApiResponse<Device>> {
  return request<Device>({ url: `/api/devices/${id}/secret`, method: 'PUT', data: { authSecret: secret ?? '' } });
}

/* ---------- 历史数据（deviceId 为网关编码） ---------- */

/** 查询设备数据，可选时间范围（毫秒时间戳） */
export function queryData(
  deviceId: string,
  from?: number,
  to?: number,
): Promise<ApiResponse<DataRecord[]>> {
  return request<DataRecord[]>({
    url: '/api/data',
    method: 'GET',
    params: { deviceId, from, to },
  });
}

/** 设备最新一条数据 */
export function latestData(deviceId: string): Promise<ApiResponse<DataRecord>> {
  return request<DataRecord>({
    url: '/api/data/latest',
    method: 'GET',
    params: { deviceId },
  });
}

/* ---------- 实时状态与阈值 ---------- */

export function listRealtime(): Promise<ApiResponse<RealtimeStatus[]>> {
  return request<RealtimeStatus[]>({ url: '/api/devices/realtime', method: 'GET' });
}

export function getRealtime(deviceId: string): Promise<ApiResponse<RealtimeStatus>> {
  return request<RealtimeStatus>({ url: `/api/devices/realtime/${deviceId}`, method: 'GET' });
}

export function listThresholds(deviceId: string): Promise<ApiResponse<MetricThreshold[]>> {
  return request<MetricThreshold[]>({ url: '/api/thresholds', params: { deviceId }, method: 'GET' });
}

export function saveThreshold(t: MetricThreshold): Promise<ApiResponse<MetricThreshold>> {
  return request<MetricThreshold>({ url: '/api/thresholds', data: t, method: 'POST' });
}
