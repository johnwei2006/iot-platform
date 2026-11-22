import request from './request';
import type { ApiResponse } from '@/types';
import type { KbMessage, KbTask, NotifyPref } from '@/types/kb';

/* ---------- 站内消息 ---------- */

export function listMessages(params?: { type?: string; unread?: boolean }): Promise<ApiResponse<KbMessage[]>> {
  return request<KbMessage[]>({ url: '/api/messages', params, method: 'GET' });
}

export function unreadCount(): Promise<ApiResponse<{ count: number }>> {
  return request<{ count: number }>({ url: '/api/messages/unread-count', method: 'GET' });
}

export function markRead(id: string): Promise<ApiResponse<KbMessage>> {
  return request<KbMessage>({ url: `/api/messages/${id}/read`, method: 'POST' });
}

/* ---------- 任务 ---------- */

export function listTasks(params?: { status?: string; assignee?: string }): Promise<ApiResponse<{ task: KbTask; overdueMark: string }[]>> {
  return request({ url: '/api/tasks', params, method: 'GET' });
}

export function getTask(id: string): Promise<ApiResponse<{ task: KbTask; overdueMark: string }>> {
  return request({ url: `/api/tasks/${id}`, method: 'GET' });
}

export function assignTask(task: Partial<KbTask>): Promise<ApiResponse<KbTask>> {
  return request<KbTask>({ url: '/api/tasks', data: task, method: 'POST' });
}

export function completeTask(id: string, doneNote?: string): Promise<ApiResponse<{ task: KbTask; overdueMark: string }>> {
  return request({ url: `/api/tasks/${id}/complete`, data: { doneNote }, method: 'POST' });
}

/* ---------- 通知偏好 ---------- */

export function getNotifyPref(): Promise<ApiResponse<NotifyPref>> {
  return request<NotifyPref>({ url: '/api/messages/notify-prefs', method: 'GET' });
}

export function updateNotifyPref(pref: { receiveKinds?: string; channels?: string }): Promise<ApiResponse<NotifyPref>> {
  return request<NotifyPref>({ url: '/api/messages/notify-prefs', data: pref, method: 'POST' });
}