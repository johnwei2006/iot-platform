/** 动态路由注册状态（独立模块，避免 router 与 store/布局组件循环依赖）。 */
let dynamicRegistered = false;

export function isDynamicRegistered(): boolean {
  return dynamicRegistered;
}

export function setDynamicRegistered(value: boolean): void {
  dynamicRegistered = value;
}

export function resetDynamicRoutes(): void {
  dynamicRegistered = false;
}
