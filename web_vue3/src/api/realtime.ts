import request from './request';
import type { ApiResponse } from '@/types';
import type { RealtimeStatus } from '@/types/kb';

export function listRealtime(): Promise<ApiResponse<RealtimeStatus[]>> {
  return request<RealtimeStatus[]>({ url: '/api/devices/realtime', method: 'GET' });
}

export function getRealtime(deviceId: string): Promise<ApiResponse<RealtimeStatus>> {
  return request<RealtimeStatus>({ url: `/api/devices/realtime/${deviceId}`, method: 'GET' });
}

export function listThresholds(deviceId: string): Promise<ApiResponse<import('@/types/kb').MetricThreshold[]>> {
  return request<import('@/types/kb').MetricThreshold[]>({ url: '/api/thresholds', params: { deviceId }, method: 'GET' });
}

export function saveThreshold(t: import('@/types/kb').MetricThreshold): Promise<ApiResponse<import('@/types/kb').MetricThreshold>> {
  return request<import('@/types/kb').MetricThreshold>({ url: '/api/thresholds', data: t, method: 'POST' });
}