import axios, { type AxiosInstance } from 'axios';
import type { ApiResponse, LoginPayload, TokenResponse } from '@/types';

/**
 * 直连 auth-service 的独立客户端。
 *
 * 不复用业务请求实例：业务实例的 401 拦截器会触发 refresh，
 * 若 refresh 本身也走它就会递归，因此这里保持「裸」客户端。
 * 生产环境需把 auth-service 反代到同域或为其配置 CORS。
 */
const authHttp: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_APP_AUTH_API_BASE_URL,
  timeout: Number(import.meta.env.VITE_APP_TIMEOUT) || 15000,
});

authHttp.interceptors.response.use(
  (response) => {
    const res = response.data as ApiResponse<unknown>;
    if (res && res.code !== undefined && res.code !== 200 && res.code !== 0) {
      return Promise.reject(new Error(res.message || '认证失败'));
    }
    return response;
  },
  (error) => Promise.reject(error),
);

const CLIENT_ID = 'web';

/** 账号密码登录（BCrypt 校验在 auth-service 完成）。 */
export function login(payload: LoginPayload): Promise<TokenResponse> {
  return authHttp
    .post<ApiResponse<TokenResponse>>('/api/auth/login', { ...payload, clientId: CLIENT_ID })
    .then((r) => r.data.data);
}

/** 用 refresh token 换取新的 access token；refresh token 本身保持不变。 */
export function refresh(refreshToken: string): Promise<TokenResponse> {
  return authHttp
    .post<ApiResponse<TokenResponse>>('/api/auth/refresh', { refreshToken })
    .then((r) => r.data.data);
}

/** 登出：把两个令牌加入黑名单/撤销 refresh。失败不阻断本地清理。 */
export function logout(accessToken: string, refreshToken: string): Promise<void> {
  return authHttp
    .post('/api/auth/logout', { accessToken, refreshToken })
    .then(() => undefined)
    .catch(() => undefined);
}

/** SSO 回调：用一次性 ticket 换取令牌。ticket 用后即焚，失败需重新登录。 */
export function exchangeSsoTicket(ticket: string): Promise<TokenResponse> {
  return authHttp
    .post<ApiResponse<TokenResponse>>('/api/sso/token', { ticket, clientId: CLIENT_ID })
    .then((r) => r.data.data);
}

export { authHttp };
