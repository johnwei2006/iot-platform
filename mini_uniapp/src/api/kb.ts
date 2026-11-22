import request from './request';
import type { ApiResponse } from '@/types';
import type {
  ConflictResult,
  ControlLog,
  ControlRule,
  KbMessage,
  KbTask,
  MetricThreshold,
  RealtimeStatus,
  DeviceEntry,
} from '@/types/kb';

/* ---------- 实时与阈值 ---------- */

export function listRealtime(): Promise<ApiResponse<RealtimeStatus[]>> {
  return request<RealtimeStatus[]>({ url: '/api/devices/realtime', method: 'GET' });
}

export function listThresholds(deviceId: string): Promise<ApiResponse<MetricThreshold[]>> {
  return request<MetricThreshold[]>({ url: '/api/thresholds', data: { deviceId }, method: 'GET' });
}

export function saveThreshold(t: MetricThreshold): Promise<ApiResponse<MetricThreshold>> {
  return request<MetricThreshold>({ url: '/api/thresholds', data: t as unknown as object, method: 'POST' });
}

/* ---------- 控制规则 ---------- */

export function listRules(params?: Record<string, unknown>): Promise<ApiResponse<ControlRule[]>> {
  return request<ControlRule[]>({ url: '/api/control/rules', data: params, method: 'GET' });
}

export function checkRule(rule: Partial<ControlRule>): Promise<ApiResponse<ConflictResult>> {
  return request<ConflictResult>({ url: '/api/control/rules/check', data: rule as unknown as object, method: 'POST' });
}

export function saveRule(rule: Partial<ControlRule>, intent: 'DRAFT' | 'ENABLE'): Promise<ApiResponse<{ rule: ControlRule; conflict: ConflictResult }>> {
  return request({ url: `/api/control/rules${rule.id ? '/' + rule.id : ''}?intent=${intent}`, data: rule as unknown as object, method: rule.id ? 'PUT' : 'POST' });
}

export function toggleRule(id: string): Promise<ApiResponse<ControlRule>> {
  return request<ControlRule>({ url: `/api/control/rules/${id}/toggle`, method: 'POST' });
}

export function executeRule(id: string): Promise<ApiResponse<{ logs: ControlLog[]; result: string }>> {
  return request({ url: `/api/control/rules/${id}/execute`, method: 'POST' });
}

/* ---------- 手动下发 ---------- */

export function sendCommand(devices: DeviceEntry[], action: string, value?: string): Promise<ApiResponse<{ logs: ControlLog[]; result: string }>> {
  return request({ url: '/api/control/commands/devices', data: { devices, action, value } as unknown as object, method: 'POST' });
}

/* ---------- 控制记录 ---------- */

export function listControlLogs(params?: Record<string, unknown>): Promise<ApiResponse<ControlLog[]>> {
  return request<ControlLog[]>({ url: '/api/control/logs', data: params, method: 'GET' });
}

/* ---------- 消息与任务 ---------- */

export function listMessages(params?: Record<string, unknown>): Promise<ApiResponse<KbMessage[]>> {
  return request<KbMessage[]>({ url: '/api/messages', data: params, method: 'GET' });
}

export function unreadCount(): Promise<ApiResponse<{ count: number }>> {
  return request<{ count: number }>({ url: '/api/messages/unread-count', method: 'GET' });
}

export function markMessageRead(id: string): Promise<ApiResponse<KbMessage>> {
  return request<KbMessage>({ url: `/api/messages/${id}/read`, method: 'POST' });
}

export function listTasks(params?: Record<string, unknown>): Promise<ApiResponse<{ task: KbTask; overdueMark: string }[]>> {
  return request({ url: '/api/tasks', data: params, method: 'GET' });
}

export function completeTask(id: string, doneNote?: string): Promise<ApiResponse<{ task: KbTask; overdueMark: string }>> {
  return request({ url: `/api/tasks/${id}/complete`, data: { doneNote } as unknown as object, method: 'POST' });
}

/* ---------- 通知偏好 ---------- */

export function getNotifyPref(): Promise<ApiResponse<{ receiveKinds?: string; channels?: string }>> {
  return request({ url: '/api/messages/notify-prefs', method: 'GET' });
}

export function updateNotifyPref(pref: { receiveKinds?: string; channels?: string }): Promise<ApiResponse<{ receiveKinds?: string; channels?: string }>> {
  return request({ url: '/api/messages/notify-prefs', data: pref as unknown as object, method: 'POST' });
}