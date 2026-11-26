import request from '../request';
import type { ApiResponse, MeResponse } from '@/types';
import type { KbMessage, NotifyPref } from '@/types/kb';
import { adminHttp, unwrap } from './client';

/** 当前管理员上下文：动态菜单/按钮权限/数据权限 */
export function fetchMe(): Promise<MeResponse> {
  return unwrap<MeResponse>(adminHttp.get('/api/admin/me'));
}

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

/* ---------- 通知偏好 ---------- */

export function getNotifyPref(): Promise<ApiResponse<NotifyPref>> {
  return request<NotifyPref>({ url: '/api/messages/notify-prefs', method: 'GET' });
}

export function updateNotifyPref(pref: { receiveKinds?: string; channels?: string }): Promise<ApiResponse<NotifyPref>> {
  return request<NotifyPref>({ url: '/api/messages/notify-prefs', data: pref, method: 'POST' });
}
