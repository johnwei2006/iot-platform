import axios, {
  type AxiosInstance,
  type AxiosRequestConfig,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios';
import { ElMessage } from 'element-plus';
import type { ApiResponse } from '@/types';
import { tokens } from '@/utils/token';
import { refresh as refreshTokenApi } from '@/api/system/auth';
import { i18n } from '@/locales';

const t = i18n.global.t;

/** 带重试标记的请求配置：避免刷新失败后无限重试。 */
interface RetryableConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
}

interface PendingRequest {
  resolve: (value: unknown) => void;
  reject: (reason?: unknown) => void;
  config: RetryableConfig;
}

/** Axios 实例：已配置 baseURL、超时与拦截器 */
const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_APP_API_BASE_URL,
  timeout: Number(import.meta.env.VITE_APP_TIMEOUT) || 15000,
});

// 单飞刷新：同一时刻只有一个 refresh 请求，其余 401 请求排队等待重放。
let isRefreshing = false;
let pendingQueue: PendingRequest[] = [];
let redirecting = false;

function applyToken(config: InternalAxiosRequestConfig, token: string): void {
  config.headers.set('Authorization', `Bearer ${token}`);
}

function flushQueue(error: unknown, token?: string): void {
  const waiting = pendingQueue;
  pendingQueue = [];
  waiting.forEach(({ resolve, reject, config }) => {
    if (error) {
      reject(error);
      return;
    }
    if (token) applyToken(config, token);
    resolve(service(config));
  });
}

function expireSession(): void {
  tokens.clear();
  if (redirecting) return;
  redirecting = true;
  ElMessage.error(t('common.sessionExpired'));
  const loginPath = `${import.meta.env.BASE_URL}login`;
  if (window.location.pathname !== loginPath) {
    const redirect = `${window.location.pathname}${window.location.search}${window.location.hash}`;
    window.location.assign(`${loginPath}?redirect=${encodeURIComponent(redirect)}`);
  }
}

// 请求拦截器：统一附加鉴权头（从响应式 token 源读取，不再直接读 localStorage）
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    if (tokens.access) {
      applyToken(config, tokens.access);
    }
    return config;
  },
  (error) => Promise.reject(error),
);

// 响应拦截器：解包 ApiResponse、统一错误提示、401 单飞刷新并重试
service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const res = response.data;
    if (res && res.code !== undefined && res.code !== 0 && res.code !== 200) {
      // 业务码 401 与 HTTP 401 走同一条刷新链路
      return Promise.reject({
        config: response.config,
        response: { status: res.code, data: res },
        message: res.message || t('common.requestFailed'),
      });
    }
    return res as unknown as AxiosResponse<ApiResponse>;
  },
  (error) => {
    const config = error?.config as RetryableConfig | undefined;
    const status = error?.response?.status as number | undefined;

    if (status === 401 && config && !config._retry) {
      config._retry = true;
      if (!tokens.refresh) {
        expireSession();
        return Promise.reject(error);
      }
      if (!isRefreshing) {
        isRefreshing = true;
        const currentRefresh = tokens.refresh;
        refreshTokenApi(currentRefresh)
          .then((data) => {
            tokens.set(data.accessToken, data.refreshToken || currentRefresh);
            flushQueue(null, data.accessToken);
          })
          .catch((refreshError) => {
            flushQueue(refreshError);
            expireSession();
          })
          .finally(() => {
            isRefreshing = false;
          });
      }
      return new Promise((resolve, reject) => {
        pendingQueue.push({ resolve, reject, config });
      });
    }

    const messages: Record<number, string> = {
      401: t('common.httpUnauthorized'),
      403: t('common.httpForbidden'),
      404: t('common.httpNotFound'),
      500: t('common.httpServerError'),
    };
    ElMessage.error(
      (status && messages[status]) || error?.response?.data?.message || error?.message || t('common.requestFailed'),
    );
    return Promise.reject(error);
  },
);

/**
 * 通用请求方法。返回 Promise<ApiResponse<T>>，
 * 业务层通过 res.data 获取真实数据。
 */
export function request<T = unknown>(config: AxiosRequestConfig): Promise<ApiResponse<T>> {
  return service.request<unknown, ApiResponse<T>>(config);
}

/** 便捷方法：直接返回响应体中的 data 字段 */
export const http = {
  get<T = unknown>(url: string, params?: object, config?: AxiosRequestConfig): Promise<T> {
    return request<T>({ method: 'GET', url, params, ...config }).then((r) => r.data);
  },
  post<T = unknown>(url: string, data?: object, config?: AxiosRequestConfig): Promise<T> {
    return request<T>({ method: 'POST', url, data, ...config }).then((r) => r.data);
  },
  put<T = unknown>(url: string, data?: object, config?: AxiosRequestConfig): Promise<T> {
    return request<T>({ method: 'PUT', url, data, ...config }).then((r) => r.data);
  },
  delete<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return request<T>({ method: 'DELETE', url, ...config }).then((r) => r.data);
  },
};

export { service };
export default request;
