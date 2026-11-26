import request from './request';
import type { ApiResponse } from '@/types';

/** 三层架构类型定义（网关→节点→传感器/控制设备） */
export interface NodeEntity {
  id: string;
  orgCode?: string;
  nodeCode: string;
  nodeName?: string;
  gatewayCode?: string;
  status?: number;
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
  createTime?: string;
  updateTime?: string;
  isDeleted?: number;
}

export interface DeviceCtrlEntity {
  id: string;
  orgCode?: string;
  deviceCode: string;
  deviceName?: string;
  deviceClass?: string;
  deviceClassType?: string;
  nodeCode?: string;
  status?: string;
  online?: number;
  auto?: number;
  address?: string;
  longitude?: string;
  latitude?: string;
  pic?: string;
  channel?: string;
  connectType?: number;
  createTime?: string;
  updateTime?: string;
  isDeleted?: number;
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

export interface DeviceClassDict {
  deviceClass: string;
  className?: string;
  switchMode?: number;
  onName?: string;
  offName?: string;
  stopName?: string;
  setName?: string;
}

export interface DeviceClassTypeDict {
  type: string;
  typeName?: string;
  deviceClass?: string;
  model?: string;
  descInfo?: string;
}

export interface GatewayEntryVO {
  deviceId: string;
  nodeId?: string;
  channel?: number;
  deviceName?: string;
  deviceType?: string;
  action?: string;
}

// ===== 节点 =====
export function listNodes(gatewayCode?: string): Promise<ApiResponse<NodeEntity[]>> {
  return request<NodeEntity[]>({ url: '/api/nodes', method: 'GET', params: gatewayCode ? { gatewayCode } : undefined });
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

// ===== 传感器 =====
export function listSensors(nodeCode?: string): Promise<ApiResponse<SensorEntity[]>> {
  return request<SensorEntity[]>({ url: '/api/sensors', method: 'GET', params: nodeCode ? { nodeCode } : undefined });
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

// ===== 控制设备 =====
export function listControlDevices(nodeCode?: string): Promise<ApiResponse<DeviceCtrlEntity[]>> {
  return request<DeviceCtrlEntity[]>({ url: '/api/control-devices', method: 'GET', params: nodeCode ? { nodeCode } : undefined });
}
export function getControlDeviceEntry(id: string): Promise<ApiResponse<GatewayEntryVO>> {
  return request<GatewayEntryVO>({ url: `/api/control-devices/${id}/gateway-entry`, method: 'GET' });
}
export function createControlDevice(data: Partial<DeviceCtrlEntity>): Promise<ApiResponse<DeviceCtrlEntity>> {
  return request<DeviceCtrlEntity>({ url: '/api/control-devices', method: 'POST', data });
}
export function updateControlDevice(id: string, data: Partial<DeviceCtrlEntity>): Promise<ApiResponse<DeviceCtrlEntity>> {
  return request<DeviceCtrlEntity>({ url: `/api/control-devices/${id}`, method: 'PUT', data });
}
export function deleteControlDevice(id: string): Promise<ApiResponse<null>> {
  return request<null>({ url: `/api/control-devices/${id}`, method: 'DELETE' });
}

// ===== 分类字典 =====
export function listSensorClass(): Promise<ApiResponse<SensorClassDict[]>> {
  return request<SensorClassDict[]>({ url: '/api/dicts/sensor-class', method: 'GET' });
}
export function listSensorClassType(sensorClass?: string): Promise<ApiResponse<SensorClassTypeDict[]>> {
  return request<SensorClassTypeDict[]>({ url: '/api/dicts/sensor-class-type', method: 'GET', params: sensorClass ? { sensorClass } : undefined });
}
export function listDeviceClass(): Promise<ApiResponse<DeviceClassDict[]>> {
  return request<DeviceClassDict[]>({ url: '/api/dicts/device-class', method: 'GET' });
}
export function listDeviceClassType(deviceClass?: string): Promise<ApiResponse<DeviceClassTypeDict[]>> {
  return request<DeviceClassTypeDict[]>({ url: '/api/dicts/device-class-type', method: 'GET', params: deviceClass ? { deviceClass } : undefined });
}
