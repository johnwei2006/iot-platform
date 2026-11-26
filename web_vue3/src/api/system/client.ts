import axios, { type AxiosInstance } from 'axios';
import type { ApiResponse } from '@/types';

/** admin-service 客户端：经 /admin-api 代理（生产同域反代），JWT 由拦截器注入 */
export const adminHttp: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_APP_ADMIN_API_BASE_URL || '/admin-api',
  timeout: Number(import.meta.env.VITE_APP_TIMEOUT || 15000),
});

adminHttp.interceptors.request.use((config) => {
  const raw = localStorage.getItem('app_token');
  const token = raw ? JSON.parse(raw) : '';
  if (token) {
    config.headers = config.headers || {};
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export function unwrap<T>(promise: Promise<{ data: ApiResponse<T> }>): Promise<T> {
  return promise.then((r) => {
    const body = r.data;
    if (body.code !== 200) {
      throw new Error(body.message || '请求失败');
    }
    return body.data;
  });
}
