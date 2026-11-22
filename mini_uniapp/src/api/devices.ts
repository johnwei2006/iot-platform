import request from './request';
import type { ApiResponse, Device } from '@/types';

export function listDevices(): Promise<ApiResponse<Device[]>> {
  return request<Device[]>({ url: '/api/devices', method: 'GET' });
}

export function getDevice(id: string): Promise<ApiResponse<Device>> {
  return request<Device>({ url: `/api/devices/${id}`, method: 'GET' });
}

export function latestData(deviceId: string): Promise<ApiResponse<unknown>> {
  return request<unknown>({ url: '/api/data/latest', method: 'GET', data: { deviceId } });
}

/** 历史数据：from/to 为毫秒时间戳（与 web 端 /api/data 同构） */
export function queryData(deviceId: string, from?: number, to?: number): Promise<ApiResponse<unknown[]>> {
  return request<unknown[]>({ url: '/api/data', method: 'GET', data: { deviceId, from, to } });
}
