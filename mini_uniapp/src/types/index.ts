/** 后端统一响应结构（platform_java 约定：code=200 成功） */
export interface ApiResponse<T = unknown> {
  code: number;
  message: string;
  data: T;
}

/**
 * auth-service 签发的令牌组（登录/微信登录/刷新共用）。
 * 注意：小程序端 refresh token 仍走响应体（浏览器端才由 HttpOnly Cookie 承载）。
 */
export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInMs: number;
  user?: {
    id: string;
    username: string;
    roles: string[];
  };
}

export interface UserInfo {
  nickname: string;
  roles: string[];
  username?: string;
  role?: string;
}

export interface LoginPayload {
  username: string;
  password: string;
}

/** 设备 */
export interface Device {
  id?: string;
  deviceId: string;
  name?: string;
  identifierType?: string;
  status?: string;
  lastSeen?: string;
  createdAt?: string;
  updatedAt?: string;
}

/** 告警 */
export interface Alarm {
  id?: string;
  deviceId: string;
  rule?: string;
  level?: string;
  message?: string;
  status?: string;
  createdAt?: string;
  resolvedAt?: string;
}
