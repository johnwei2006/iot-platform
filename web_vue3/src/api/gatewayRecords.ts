import { request } from './request';
import type { GatewayRecordPage, GatewayReconnectRecord, GatewayTokenRequestRecord } from '@/types';

/**
 * 网关认证记录查询（ProxyTcp 8010 端口，经 /proxy-api 代理）。
 * 注意：ProxyTcp 返回裸 JSON 分页结构，无 ApiResponse 包装。
 */

export function fetchReconnectRecords(
  params: { deviceId?: string; page?: number; size?: number },
): Promise<GatewayRecordPage<GatewayReconnectRecord>> {
  return request<GatewayRecordPage<GatewayReconnectRecord>>({
    url: '/proxy-api/api/gateway/reconnect-records',
    method: 'GET',
    params,
  }) as unknown as Promise<GatewayRecordPage<GatewayReconnectRecord>>;
}

export function fetchTokenRequests(
  params: { deviceId?: string; page?: number; size?: number },
): Promise<GatewayRecordPage<GatewayTokenRequestRecord>> {
  return request<GatewayRecordPage<GatewayTokenRequestRecord>>({
    url: '/proxy-api/api/gateway/token-requests',
    method: 'GET',
    params,
  }) as unknown as Promise<GatewayRecordPage<GatewayTokenRequestRecord>>;
}
