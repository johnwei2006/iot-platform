/** 兼容旧引用：鉴权接口已统一迁移到 auth.ts（直连 auth-service）。 */
export { login, refresh, logout, exchangeSsoTicket } from './auth';
