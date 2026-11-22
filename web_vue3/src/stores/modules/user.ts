import { defineStore } from 'pinia';
import {
  login as loginApi,
  logout as logoutApi,
  ssoCentralLogout,
  exchangeSsoTicket as exchangeSsoTicketApi,
} from '@/api/auth';
import { fetchMe } from '@/api/admin';
import type { LoginPayload, MeResponse, MenuNode, TokenResponse, UserInfo } from '@/types';
import { tokens } from '@/utils/token';
import { storage } from '@/utils/storage';

interface UserState {
  userInfo: (UserInfo & { superAdmin?: boolean }) | null;
  /** 动态菜单树（admin-service /me） */
  menus: MenuNode[];
  /** 按钮权限点 */
  perms: string[];
  /** 数据权限映射（实体 -> 范围） */
  dataScopes: Record<string, string>;
  /** /me 是否已加载（含动态路由是否已注册） */
  meLoaded: boolean;
}

/** userInfo 持久化：页面刷新后路由角色守卫仍可工作 */
function loadUserInfo(): (UserInfo & { superAdmin?: boolean }) | null {
  return storage.get<UserInfo & { superAdmin?: boolean }>('userInfo');
}

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    userInfo: loadUserInfo(),
    menus: [],
    perms: [],
    dataScopes: {},
    meLoaded: false,
  }),
  getters: {
    /** 直接派生自响应式令牌源，refresh 刷新后无需手动同步。 */
    token: () => tokens.access,
    isLogin: () => !!tokens.access,
    roles: (state) => state.userInfo?.roles ?? [],
    superAdmin: (state) => !!state.userInfo?.superAdmin,
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
          orgCode: data.user.orgCode,
        };
        storage.set('userInfo', this.userInfo);
      }
      // 重新登录后强制重新拉取菜单/权限
      this.resetMe();
    },
    /**
     * 拉取当前管理员上下文（动态菜单/按钮权限/数据权限）。
     * 已加载或未登录时跳过；失败静默（无系统管理权限的账号 menus 为空）。
     */
    async loadMe(): Promise<void> {
      if (this.meLoaded || !tokens.access) {
        return;
      }
      try {
        const me: MeResponse = await fetchMe();
        this.menus = me.menus || [];
        this.perms = me.perms || [];
        this.dataScopes = me.dataScopes || {};
        if (me.user) {
          this.userInfo = {
            name: me.user.username,
            username: me.user.username,
            roles: me.user.roles ?? [],
            orgCode: me.user.orgCode,
            superAdmin: !!me.user.superAdmin,
          };
          storage.set('userInfo', this.userInfo);
        }
        this.meLoaded = true;
      } catch (error) {
        // 无 admin-service 权限或服务不可用：菜单为空，业务页面不受影响
        console.error('load admin me failed:', error);
        this.meLoaded = true;
      }
    },
    resetMe(): void {
      this.menus = [];
      this.perms = [];
      this.dataScopes = {};
      this.meLoaded = false;
    },
    async logout(): Promise<void> {
      const access = tokens.access;
      const refresh = tokens.refresh;
      await logoutApi(access, refresh);
      // 配置了统一登录页时联动单点登出，销毁中央会话并撤销全部令牌
      if (import.meta.env.VITE_APP_SSO_LOGIN_URL) {
        await ssoCentralLogout();
      }
      tokens.clear();
      storage.remove('userInfo');
      this.userInfo = null;
      this.resetMe();
    },
  },
});
