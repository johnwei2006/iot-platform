import request from '../request';
import type { ApiResponse } from '@/types';

/** 传感器实体（对应 iot_sensor） */
export interface SensorEntity {
  id: string;
  orgCode?: string;
  sensorCode: string;
  sensorName?: string;
  nodeCode?: string;
  sensorClass?: string;
  sensorClassType?: string;
  status?: number;
  unit?: string;
  warnMin?: number;
  warnMax?: number;
  scale?: number;
  reportInterval?: number;
  lastReportTime?: string;
  pic?: string;
  channel?: string;
  commandRead?: string;
  createBy?: string;
  createTime?: string;
  updateBy?: string;
  updateTime?: string;
  isDeleted?: number;
}

export interface SensorHistoryPoint {
  ts: number;
  sensorcode?: string;
  senval: number;
  unit?: string;
}

export interface SensorClassDict {
  sensorClass: string;
  className?: string;
  classNameEn?: string;
  unit?: string;
  scale?: number;
}

export interface SensorClassTypeDict {
  classType: string;
  sensorClass?: string;
  classTypeName?: string;
  model?: string;
  description?: string;
}

export function listSensors(nodeCode?: string): Promise<ApiResponse<SensorEntity[]>> {
  return request<SensorEntity[]>({ url: '/api/sensors', method: 'GET', params: nodeCode ? { nodeCode } : undefined });
}

export function getSensor(id: string): Promise<ApiResponse<SensorEntity>> {
  return request<SensorEntity>({ url: `/api/sensors/${id}`, method: 'GET' });
}

export function getSensorHistory(
  id: string,
  from?: number,
  to?: number,
): Promise<ApiResponse<SensorHistoryPoint[]>> {
  return request<SensorHistoryPoint[]>({
    url: `/api/sensors/${id}/history`,
    method: 'GET',
    params: { from, to },
  });
}

export function createSensor(data: Partial<SensorEntity>): Promise<ApiResponse<SensorEntity>> {
  return request<SensorEntity>({ url: '/api/sensors', method: 'POST', data });
}

export function updateSensor(id: string, data: Partial<SensorEntity>): Promise<ApiResponse<SensorEntity>> {
  return request<SensorEntity>({ url: `/api/sensors/${id}`, method: 'PUT', data });
}

export function deleteSensor(id: string): Promise<ApiResponse<null>> {
  return request<null>({ url: `/api/sensors/${id}`, method: 'DELETE' });
}

export function listSensorClass(): Promise<ApiResponse<SensorClassDict[]>> {
  return request<SensorClassDict[]>({ url: '/api/dicts/sensor-class', method: 'GET' });
}

export function listSensorClassType(sensorClass?: string): Promise<ApiResponse<SensorClassTypeDict[]>> {
  return request<SensorClassTypeDict[]>({
    url: '/api/dicts/sensor-class-type',
    method: 'GET',
    params: sensorClass ? { sensorClass } : undefined,
  });
}
