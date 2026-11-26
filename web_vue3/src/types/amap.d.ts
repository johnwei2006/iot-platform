/** 高德地图 JS API 2.0 最小类型声明，仅覆盖区域管理用到的能力。 */

export interface AmapLngLat {
  getLng(): number;
  getLat(): number;
}

export interface AmapMap {
  setZoom(zoom: number): void;
  setCenter(center: [number, number] | AmapLngLat): void;
  add(overlay: unknown): void;
  remove(overlay: unknown): void;
  clearMap(): void;
  destroy(): void;
  on(event: string, handler: (event?: unknown) => void): void;
  off(event: string, handler: (event?: unknown) => void): void;
}

export interface AmapPolygon {
  setPath(path: AmapLngLat[]): void;
  getPath(): AmapLngLat[];
  setMap(map: AmapMap | null): void;
  on(event: string, handler: (event?: unknown) => void): void;
  off(event: string, handler: (event?: unknown) => void): void;
}

export interface AmapMouseTool {
  polygon(options?: Record<string, unknown>): void;
  close(clear?: boolean): void;
  on(event: string, handler: (event?: unknown) => void): void;
  off(event: string, handler: (event?: unknown) => void): void;
}

export interface AmapPolyEditor {
  open(): void;
  close(): void;
  on(event: string, handler: (event?: unknown) => void): void;
  off(event: string, handler: (event?: unknown) => void): void;
}

export interface AmapText {
  setMap(map: AmapMap | null): void;
}

export interface AmapNamespace {
  Map: new (container: HTMLElement | string, options?: Record<string, unknown>) => AmapMap;
  LngLat: new (lng: number, lat: number) => AmapLngLat;
  Text: new (options?: Record<string, unknown>) => AmapText;
  Polygon: new (options?: Record<string, unknown>) => AmapPolygon;
  MouseTool: new (map: AmapMap) => AmapMouseTool;
  PolyEditor: new (map: AmapMap, polygon: AmapPolygon, options?: Record<string, unknown>) => AmapPolyEditor;
  TileLayer: {
    Satellite: new (options?: Record<string, unknown>) => unknown;
  };
  GeometryUtil: {
    distance(start: AmapLngLat, end: AmapLngLat): number;
  };
}

declare global {
  interface Window {
    AMap?: AmapNamespace;
    _AMapSecurityConfig?: { securityJsCode: string };
  }
}
