import { ref } from 'vue';
import { storage } from './storage';

/**
 * 令牌的单一数据源。access/refresh 的读写都必须经过这里，
 * 避免各处直接用 localStorage.getItem 取到 JSON 序列化后的带引号字符串。
 *
 * access 持久化在 localStorage（短有效期，默认 30 分钟）；
 * refresh 只保存在内存，浏览器端由 auth-service 下发的 HttpOnly Cookie
 * 承载并在请求 /api/auth/* 时自动携带，不再落 localStorage。
 */
const accessToken = ref<string>(storage.get<string>('token') || '');
const refreshToken = ref<string>('');

// 清理历史版本遗留的持久化 refresh token
storage.remove('refreshToken');

export const tokens = {
  get access(): string {
    return accessToken.value;
  },
  get refresh(): string {
    return refreshToken.value;
  },
  /** 更新令牌；refresh 只保留在内存（Cookie 由后端写入），传 null/空串表示清除。 */
  set(access: string, refresh?: string | null): void {
    accessToken.value = access;
    storage.set('token', access);
    if (refresh !== undefined) {
      refreshToken.value = refresh || '';
    }
  },
  clear(): void {
    accessToken.value = '';
    refreshToken.value = '';
    storage.remove('token');
    storage.remove('refreshToken');
  },
};
