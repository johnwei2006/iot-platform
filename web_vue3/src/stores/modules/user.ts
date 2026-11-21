import { defineStore } from 'pinia';
import {
  login as loginApi,
  logout as logoutApi,
  exchangeSsoTicket as exchangeSsoTicketApi,
} from '@/api/auth';
import type { LoginPayload, TokenResponse, UserInfo } from '@/types';
import { tokens } from '@/utils/token';

interface UserState {
  userInfo: UserInfo | null;
}

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    userInfo: null,
  }),
  getters: {
    /** 直接派生自响应式令牌源，refresh 刷新后无需手动同步。 */
    token: () => tokens.access,
    isLogin: () => !!tokens.access,
    roles: (state) => state.userInfo?.roles ?? [],
  },
  actions: {
    async login(payload: LoginPayload): Promise<void> {
      this.applyTokens(await loginApi(payload));
    },
    async exchangeSsoTicket(ticket: string, redirectUri: string): Promise<void> {
      this.applyTokens(await exchangeSsoTicketApi(ticket, redirectUri));
    },
    applyTokens(data: TokenResponse): void {
      tokens.set(data.accessToken, data.refreshToken);
      if (data.user) {
        this.userInfo = {
          name: data.user.username,
          username: data.user.username,
          roles: data.user.roles ?? [],
        };
      }
    },
    async logout(): Promise<void> {
      const access = tokens.access;
      const refresh = tokens.refresh;
      await logoutApi(access, refresh);
      tokens.clear();
      this.userInfo = null;
    },
  },
});
