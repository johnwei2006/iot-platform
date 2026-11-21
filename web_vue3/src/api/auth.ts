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
/** SSO 兑票客户端密钥；与 auth-service 的 AUTH_CLIENT_WEB_SECRET 保持一致（公开客户端，仅作绑定校验）。 */
const CLIENT_SECRET = import.meta.env.VITE_APP_AUTH_CLIENT_SECRET || '';

/** 账号密码登录（BCrypt 校验在 auth-service 完成）。 */
export function login(payload: LoginPayload): Promise<TokenResponse> {
  return authHttp
    .post<ApiResponse<TokenResponse>>('/api/auth/login', { ...payload, clientId: CLIENT_ID })
    .then((r) => r.data.data);
}

/**
 * 用 refresh token 换取新 access token（服务端轮换 refresh）。
 * 参数可为空串：浏览器端 refresh 由 HttpOnly Cookie 自动携带。
 */
export function refresh(refreshToken: string): Promise<TokenResponse> {
  return authHttp
    .post<ApiResponse<TokenResponse>>('/api/auth/refresh', { refreshToken: refreshToken || '' })
    .then((r) => r.data.data);
}

/** 登出：服务端撤销 refresh（Cookie 亦会清除）并拉黑 access。失败不阻断本地清理。 */
export function logout(accessToken: string, refreshToken: string): Promise<void> {
  return authHttp
    .post('/api/auth/logout', { accessToken, refreshToken })
    .then(() => undefined)
    .catch(() => undefined);
}

/** SSO 回调：用一次性 ticket + clientSecret + redirectUri 换取令牌，ticket 用后即焚。 */
export function exchangeSsoTicket(ticket: string, redirectUri: string): Promise<TokenResponse> {
  return authHttp
    .post<ApiResponse<TokenResponse>>('/api/sso/token', {
      ticket,
      clientId: CLIENT_ID,
      clientSecret: CLIENT_SECRET,
      redirectUri,
    })
    .then((r) => r.data.data);
}

export { authHttp };
