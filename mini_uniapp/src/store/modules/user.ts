import { defineStore } from 'pinia';
import { login as loginApi, wechatLogin as wechatLoginApi } from '@/api/user';
import type { LoginPayload, TokenResponse, UserInfo } from '@/types';
import { storage } from '@/utils/storage';

interface UserState {
  token: string;
  refreshToken: string;
  userInfo: UserInfo | null;
}

/** 设备标识：首次生成并持久化，用于会话管理。 */
function deviceId(): string {
  let id = storage.get<string>('deviceId');
  if (!id) {
    id = 'uni-' + Date.now().toString(36) + '-' + Math.random().toString(36).slice(2, 10);
    storage.set('deviceId', id);
  }
  return id;
}

function applyTokens(state: UserState, data: TokenResponse): void {
  state.token = data.accessToken;
  state.refreshToken = data.refreshToken || '';
  storage.set('token', data.accessToken);
  storage.set('refreshToken', data.refreshToken || '');
  if (data.user) {
    state.userInfo = {
      nickname: data.user.username,
      username: data.user.username,
      roles: data.user.roles ?? [],
    };
  }
}

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    token: storage.get<string>('token') || '',
    refreshToken: storage.get<string>('refreshToken') || '',
    userInfo: null,
  }),
  getters: {
    isLogin: (state) => !!state.token,
  },
  actions: {
    async login(payload: LoginPayload) {
      const res = await loginApi(payload);
      applyTokens(this.$state, res.data);
      return this.token;
    },
    /** 微信一键登录：uni.login 拿 code 换令牌。 */
    async wechatLogin() {
      const code = await new Promise<string>((resolve, reject) => {
        uni.login({
          provider: 'weixin',
          success: (res) => resolve(res.code),
          fail: (err) => reject(err),
        });
      });
      const res = await wechatLoginApi(code, deviceId());
      applyTokens(this.$state, res.data);
      return this.token;
    },
    async refresh() {
      if (!this.refreshToken) {
        throw new Error('missing refresh token');
      }
      const { refresh: refreshApi } = await import('@/api/user');
      const res = await refreshApi(this.refreshToken);
      applyTokens(this.$state, res.data);
    },
    async logout() {
      const { logout: logoutApi } = await import('@/api/user');
      try {
        await logoutApi(this.token, this.refreshToken);
      } catch {
        // 服务端撤销失败不阻断本地清理
      }
      this.token = '';
      this.refreshToken = '';
      this.userInfo = null;
      storage.remove('token');
      storage.remove('refreshToken');
    },
  },
});
