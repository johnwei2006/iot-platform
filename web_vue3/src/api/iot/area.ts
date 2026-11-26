import request from '../request';
import type { ApiResponse } from '@/types';

/** 区域实体（对应 iot_area） */
export interface AreaEntity {
  id: string;
  orgCode?: string;
  areaName: string;
  /** 多边形边界 JSON 字符串：[[lng, lat], ...] */
  boundary?: string;
  /** 绘制面积（亩，由边界自动计算） */
  drawAreaMu?: number;
  /** 标注面积（亩，用户填写） */
  labelAreaMu?: number;
  createBy?: string;
  createTime?: string;
  updateBy?: string;
  updateTime?: string;
  isDeleted?: number;
  ownerUserId?: string;
}

/** 前端坐标点：[经度, 纬度] */
export type AreaPoint = [number, number];

/** 区域在四类实体中的引用数量 */
export interface AreaRefCounts {
  gateway: number;
  node: number;
  sensor: number;
  deviceCtrl: number;
  total: number;
}

export function listAreas(): Promise<ApiResponse<AreaEntity[]>> {
  return request<AreaEntity[]>({ url: '/api/areas', method: 'GET' });
}

export function getArea(id: string): Promise<ApiResponse<AreaEntity>> {
  return request<AreaEntity>({ url: `/api/areas/${id}`, method: 'GET' });
}

export function getAreaRefs(id: string): Promise<ApiResponse<AreaRefCounts>> {
  return request<AreaRefCounts>({ url: `/api/areas/${id}/refs`, method: 'GET' });
}

export function createArea(data: Partial<AreaEntity>): Promise<ApiResponse<AreaEntity>> {
  return request<AreaEntity>({ url: '/api/areas', method: 'POST', data });
}

export function updateArea(id: string, data: Partial<AreaEntity>): Promise<ApiResponse<AreaEntity>> {
  return request<AreaEntity>({ url: `/api/areas/${id}`, method: 'PUT', data });
}

export function deleteArea(id: string): Promise<ApiResponse<null>> {
  return request<null>({ url: `/api/areas/${id}`, method: 'DELETE' });
}

/** 边界 JSON 字符串 -> 坐标点数组；非法数据返回空数组。 */
export function parseBoundary(raw?: string): AreaPoint[] {
  if (!raw) return [];
  try {
    const parsed: unknown = JSON.parse(raw);
    if (Array.isArray(parsed)) {
      return parsed.filter((p): p is number[] =>
        Array.isArray(p) && p.length >= 2 && typeof p[0] === 'number' && typeof p[1] === 'number',
      ).map((p) => [p[0], p[1]] as AreaPoint);
    }
  } catch (error) {
    console.error('[area] 边界数据解析失败:', error);
  }
  return [];
}

/** 坐标点数组 -> 边界 JSON 字符串 */
export function stringifyBoundary(points: AreaPoint[]): string {
  return JSON.stringify(points);
}
