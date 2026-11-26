import request from '../request';
import type { ApiResponse } from '@/types';

/** 控制设备实体（对应 iot_device_ctrl） */
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
  /** 关联区域ID（iot_area.id） */
  areaId?: string;
  address?: string;
  longitude?: string;
  latitude?: string;
  pic?: string;
  channel?: string;
  connectType?: number;
  createBy?: string;
  createTime?: string;
  updateBy?: string;
  updateTime?: string;
  isDeleted?: number;
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

export function listControlDevices(nodeCode?: string, areaId?: string): Promise<ApiResponse<DeviceCtrlEntity[]>> {
  return request<DeviceCtrlEntity[]>({
    url: '/api/control-devices',
    method: 'GET',
    params: { ...(nodeCode ? { nodeCode } : {}), ...(areaId ? { areaId } : {}) },
  });
}

export function getControlDevice(id: string): Promise<ApiResponse<DeviceCtrlEntity>> {
  return request<DeviceCtrlEntity>({ url: `/api/control-devices/${id}`, method: 'GET' });
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

export function listDeviceClass(): Promise<ApiResponse<DeviceClassDict[]>> {
  return request<DeviceClassDict[]>({ url: '/api/dicts/device-class', method: 'GET' });
}

export function listDeviceClassType(deviceClass?: string): Promise<ApiResponse<DeviceClassTypeDict[]>> {
  return request<DeviceClassTypeDict[]>({
    url: '/api/dicts/device-class-type',
    method: 'GET',
    params: deviceClass ? { deviceClass } : undefined,
  });
}
