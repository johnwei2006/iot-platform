/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_APP_ENV: string;
  readonly VITE_APP_API_BASE_URL: string;
  readonly VITE_APP_AUTH_API_BASE_URL: string;
  readonly VITE_APP_SSO_LOGIN_URL: string;
  readonly VITE_APP_TIMEOUT: string;
  readonly VITE_APP_TITLE: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
