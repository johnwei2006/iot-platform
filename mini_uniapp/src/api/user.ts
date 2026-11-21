import request from './request';
import type { ApiResponse, LoginPayload, TokenResponse } from '@/types';

/** 账号密码登录（clientId 固定 mini，避免落到默认 web 客户端）。 */
export function login(data: LoginPayload): Promise<ApiResponse<TokenResponse>> {
  return request<TokenResponse>({ url: '/api/auth/login', method: 'POST', data: { ...data, clientId: 'mini' } });
}

/** 微信一键登录：wx.login 的 code 换本平台令牌（首次登录自动创建用户）。 */
export function wechatLogin(code: string, deviceId?: string): Promise<ApiResponse<TokenResponse>> {
  return request<TokenResponse>({
    url: '/api/auth/wechat/login',
    method: 'POST',
    data: { code, clientId: 'mini', deviceId },
  });
}

/** 刷新 access token（轮换 refresh，需保存返回的新 refreshToken）。 */
export function refresh(refreshToken: string): Promise<ApiResponse<TokenResponse>> {
  return request<TokenResponse>({ url: '/api/auth/refresh', method: 'POST', data: { refreshToken } });
}

/** 登出：撤销 refresh、拉黑 access。 */
export function logout(accessToken: string, refreshToken: string): Promise<ApiResponse<null>> {
  return request<null>({ url: '/api/auth/logout', method: 'POST', data: { accessToken, refreshToken } });
}
