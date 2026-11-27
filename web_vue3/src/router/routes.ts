import type { RouteRecordRaw } from 'vue-router';
import Layout from '@/layouts/index.vue';

/** 路由表：集中维护，支持 meta 权限与懒加载 */
export const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: 'route.login', requiresAuth: false },
  },
  {
    path: '/',
    name: 'Layout',
    component: Layout,
    redirect: '/home',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('@/views/home/index.vue'),
        meta: { title: 'route.home', requiresAuth: true, roles: ['admin', 'user'] },
      },
      {
        path: 'iot/gateways/:id',
        name: 'GatewayDetail',
        component: () => import('@/views/iot/gateway/gateway-detail.vue'),
        meta: { title: 'route.gatewayDetail', requiresAuth: true, roles: ['admin', 'user'] },
      },
      {
        path: 'iot/nodes/:id',
        name: 'NodeDetail',
        component: () => import('@/views/iot/node/node-detail.vue'),
        meta: { title: 'route.nodeDetail', requiresAuth: true, roles: ['admin', 'user'] },
      },
      {
        path: 'iot/sensors/:id',
        name: 'SensorDetail',
        component: () => import('@/views/iot/sensor/sensor-detail.vue'),
        meta: { title: 'route.sensorDetail', requiresAuth: true, roles: ['admin', 'user'] },
      },
      {
        path: 'iot/control-devices/:id',
        name: 'ControlDeviceDetail',
        component: () => import('@/views/iot/device/device-detail.vue'),
        meta: { title: 'route.controlDeviceDetail', requiresAuth: true, roles: ['admin', 'user'] },
      },
      {
        path: 'iot/sensor-data/groups',
        name: 'SensorGroupManage',
        component: () => import('@/views/iot/sensor-data/groups.vue'),
        meta: { title: 'route.sensorGroupManage', requiresAuth: true, roles: ['admin', 'user'] },
      },
    ],
  },
  {
    path: '/sso/callback',
    name: 'SsoCallback',
    component: () => import('@/views/sso/callback.vue'),
    meta: { title: 'route.sso', requiresAuth: false },
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: 'route.notFound', requiresAuth: false },
  },
];
