/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_APP_ENV: string;
  readonly VITE_APP_API_BASE_URL: string;
  readonly VITE_APP_AUTH_API_BASE_URL: string;
  readonly VITE_APP_SSO_LOGIN_URL: string;
  readonly VITE_APP_TIMEOUT: string;
  readonly VITE_APP_TITLE: string;
  /** 高德地图 JS API 2.0 凭据；为空时区域地图显示配置提示 */
  readonly VITE_AMAP_KEY: string;
  readonly VITE_AMAP_SECURITY_CODE: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
