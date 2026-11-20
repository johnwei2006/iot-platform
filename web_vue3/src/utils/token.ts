import { ref } from 'vue';
import { storage } from './storage';

/**
 * 令牌的单一数据源。access/refresh 的读写都必须经过这里，
 * 避免各处直接用 localStorage.getItem 取到 JSON 序列化后的带引号字符串。
 */
const accessToken = ref<string>(storage.get<string>('token') || '');
const refreshToken = ref<string>(storage.get<string>('refreshToken') || '');

export const tokens = {
  get access(): string {
    return accessToken.value;
  },
  get refresh(): string {
    return refreshToken.value;
  },
  /** 更新令牌并持久化；refresh 传 null 或空串表示清除。 */
  set(access: string, refresh?: string | null): void {
    accessToken.value = access;
    storage.set('token', access);
    if (refresh !== undefined) {
      if (refresh) {
        refreshToken.value = refresh;
        storage.set('refreshToken', refresh);
      } else {
        refreshToken.value = '';
        storage.remove('refreshToken');
      }
    }
  },
  clear(): void {
    accessToken.value = '';
    refreshToken.value = '';
    storage.remove('token');
    storage.remove('refreshToken');
  },
};
