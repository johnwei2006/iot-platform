import request from './request';
import type { ApiResponse } from '@/types';
import type {
  ConflictResult,
  ControlLog,
  ControlRule,
  DeviceEntry,
} from '@/types/kb';

/* ---------- 控制规则 ---------- */

export function listRules(params?: { kind?: string; trigger?: string; enabled?: boolean }): Promise<ApiResponse<ControlRule[]>> {
  return request<ControlRule[]>({ url: '/api/control/rules', params, method: 'GET' });
}

export function getRule(id: string): Promise<ApiResponse<ControlRule>> {
  return request<ControlRule>({ url: `/api/control/rules/${id}`, method: 'GET' });
}

/** 冲突校验（不落库），服务端唯一权威 */
export function checkRule(rule: Partial<ControlRule>): Promise<ApiResponse<ConflictResult>> {
  return request<ConflictResult>({ url: '/api/control/rules/check', data: rule, method: 'POST' });
}

export function createRule(rule: Partial<ControlRule>, intent: 'DRAFT' | 'ENABLE'): Promise<ApiResponse<{ rule: ControlRule; conflict: ConflictResult }>> {
  return request({ url: '/api/control/rules', params: { intent }, data: rule, method: 'POST' });
}

export function updateRule(id: string, rule: Partial<ControlRule>, intent: 'DRAFT' | 'ENABLE'): Promise<ApiResponse<{ rule: ControlRule; conflict: ConflictResult }>> {
  return request({ url: `/api/control/rules/${id}`, params: { intent }, data: rule, method: 'PUT' });
}

export function toggleRule(id: string): Promise<ApiResponse<ControlRule>> {
  return request<ControlRule>({ url: `/api/control/rules/${id}/toggle`, method: 'POST' });
}

export function executeRule(id: string): Promise<ApiResponse<{ logs: ControlLog[]; result: string }>> {
  return request({ url: `/api/control/rules/${id}/execute`, method: 'POST' });
}

export function deleteRule(id: string): Promise<ApiResponse<null>> {
  return request<null>({ url: `/api/control/rules/${id}`, method: 'DELETE' });
}

/* ---------- 手动下发 ---------- */

export function sendCommand(devices: DeviceEntry[], action: string, value?: string): Promise<ApiResponse<{ logs: ControlLog[]; result: string }>> {
  return request({ url: '/api/control/commands/devices', data: { devices, action, value }, method: 'POST' });
}

/* ---------- 控制记录 ---------- */

export function listControlLogs(params?: { deviceId?: string; ruleId?: string; result?: string; source?: string }): Promise<ApiResponse<ControlLog[]>> {
  return request<ControlLog[]>({ url: '/api/control/logs', params, method: 'GET' });
}

export function getControlLog(id: string): Promise<ApiResponse<ControlLog>> {
  return request<ControlLog>({ url: `/api/control/logs/${id}`, method: 'GET' });
}