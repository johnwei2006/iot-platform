<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue';
import AMapLoader from '@amap/amap-jsapi-loader';
import { parseBoundary, type AreaEntity, type AreaPoint } from '@/api/iot/area';
import type { AmapLngLat, AmapMap, AmapMouseTool, AmapNamespace, AmapPolygon } from '@/types/amap';

const props = defineProps<{
  areas: AreaEntity[];
  selectedId?: string;
  keyReady: boolean;
}>();

const emit = defineEmits<{
  drawn: [payload: { points: AreaPoint[]; areaMu: number }];
  select: [id: string];
}>();

const host = ref<HTMLDivElement | null>(null);
let map: AmapMap | null = null;
let mouseTool: AmapMouseTool | null = null;
let AMap: AmapNamespace | null = null;
let overlays: unknown[] = [];
let drawOverlays: unknown[] = [];

/** 经纬度投影后用 shoelace 公式计算面积，换算为亩（1 亩 = 666.6667 平方米）。 */
function polygonAreaMu(points: AreaPoint[]): number {
  if (points.length < 3) return 0;
  let sum = 0;
  for (let i = 0; i < points.length; i += 1) {
    const [lng1, lat1] = points[i];
    const [lng2, lat2] = points[(i + 1) % points.length];
    const x1 = lng1 * 111320 * Math.cos((lat1 * Math.PI) / 180);
    const y1 = lat1 * 110540;
    const x2 = lng2 * 111320 * Math.cos((lat2 * Math.PI) / 180);
    const y2 = lat2 * 110540;
    sum += x1 * y2 - x2 * y1;
  }
  return Math.abs(sum) / 2 / 666.6666667;
}

function centerOf(points: AreaPoint[]): AreaPoint {
  if (!points.length) return [116.397428, 39.90923];
  const total = points.reduce(
    (acc, p) => [acc[0] + p[0], acc[1] + p[1]] as AreaPoint,
    [0, 0] as AreaPoint,
  );
  return [total[0] / points.length, total[1] / points.length];
}

function removeOverlays(list: unknown[]) {
  for (const overlay of list) {
    try {
      map?.remove(overlay);
    } catch (error) {
      console.error('[area-map] 移除覆盖物失败:', error);
    }
  }
  list.length = 0;
}

function renderAreas() {
  if (!map || !AMap) return;
  removeOverlays(overlays);
  for (const area of props.areas) {
    const points = parseBoundary(area.boundary);
    if (points.length < 3) continue;
    const active = area.id === props.selectedId;
    const polygon: AmapPolygon = new AMap.Polygon({
      path: points.map((p) => new AMap!.LngLat(p[0], p[1])),
      strokeColor: active ? '#0958D9' : '#1677FF',
      strokeWeight: active ? 3 : 2,
      strokeOpacity: 0.9,
      fillColor: active ? '#69b1ff' : '#1677FF',
      fillOpacity: active ? 0.38 : 0.25,
      cursor: 'pointer',
    });
    polygon.on('click', () => emit('select', area.id));
    map.add(polygon);
    overlays.push(polygon);
    const label = new AMap.Text({
      text: `${area.areaName}\n${(area.labelAreaMu ?? area.drawAreaMu ?? 0).toFixed(2)}亩`,
      position: centerOf(points),
      style: {
        fillColor: '#ffffff',
        fontSize: '12px',
        fontWeight: '600',
        textAlign: 'center',
        strokeColor: 'rgba(0,0,0,0.7)',
        strokeWidth: 3,
      },
    });
    map.add(label);
    overlays.push(label);
  }
}

function handleDraw(event: unknown) {
  if (!map || !AMap) return;
  const obj = (event as { obj?: { getPath(): AmapLngLat[] } } | null)?.obj;
  if (!obj) return;
  const points: AreaPoint[] = obj.getPath().map((p) => [p.getLng(), p.getLat()]);
  const areaMu = polygonAreaMu(points);
  removeOverlays(drawOverlays);
  for (let i = 0; i < points.length; i += 1) {
    const a = points[i];
    const b = points[(i + 1) % points.length];
    const dist = AMap.GeometryUtil.distance(
      new AMap.LngLat(a[0], a[1]),
      new AMap.LngLat(b[0], b[1]),
    );
    const label = new AMap.Text({
      text: `${dist.toFixed(2)}m`,
      position: [(a[0] + b[0]) / 2, (a[1] + b[1]) / 2],
      style: {
        fillColor: '#303133',
        backgroundColor: '#ffffff',
        fontSize: '11px',
        padding: '2px 5px',
      },
    });
    map.add(label);
    drawOverlays.push(label);
  }
  emit('drawn', { points, areaMu });
}

function startDraw() {
  removeOverlays(drawOverlays);
  if (!mouseTool) return;
  mouseTool.polygon({
    strokeColor: '#1677FF',
    strokeWeight: 2,
    strokeOpacity: 0.9,
    fillColor: '#1677FF',
    fillOpacity: 0.25,
    cursor: 'crosshair',
  });
}

function clearDraw() {
  removeOverlays(drawOverlays);
  mouseTool?.close(true);
}

function focusArea(id: string) {
  if (!map) return;
  const area = props.areas.find((a) => a.id === id);
  if (!area) return;
  const points = parseBoundary(area.boundary);
  if (points.length) {
    map.setCenter(centerOf(points));
    map.setZoom(17);
  }
}

defineExpose({ startDraw, clearDraw, focusArea });

onMounted(async () => {
  if (!props.keyReady || !host.value) return;
  const key = import.meta.env.VITE_AMAP_KEY;
  const securityCode = import.meta.env.VITE_AMAP_SECURITY_CODE;
  window._AMapSecurityConfig = { securityJsCode: securityCode };
  try {
    AMap = await AMapLoader.load({
      key,
      version: '2.0',
      plugins: ['AMap.MouseTool', 'AMap.PolyEditor'],
    }) as AmapNamespace;
    window.AMap = AMap;
    map = new AMap.Map(host.value, {
      zoom: 15,
      center: [116.397428, 39.90923],
      viewMode: '2D',
    });
    map.add(new AMap.TileLayer.Satellite());
    mouseTool = new AMap.MouseTool(map);
    mouseTool.on('draw', handleDraw);
    renderAreas();
  } catch (error) {
    console.error('[area-map] 高德地图初始化失败:', error);
  }
});

watch(() => props.areas, renderAreas, { deep: true });
watch(() => props.selectedId, renderAreas);

onBeforeUnmount(() => {
  mouseTool?.close(true);
  removeOverlays(overlays);
  removeOverlays(drawOverlays);
  map?.destroy();
  map = null;
  mouseTool = null;
  AMap = null;
});
</script>

<template>
  <div class="area-map-wrap">
    <div v-if="!keyReady" class="map-placeholder">
      <el-result
        icon="warning"
        title="地图服务未配置"
        sub-title="请在环境变量中配置 VITE_AMAP_KEY 与 VITE_AMAP_SECURITY_CODE 后刷新页面"
      >
        <template #extra>
          <span class="hint">区域列表仍可正常使用，配置完成后即可查看卫星地图与绘制边界。</span>
        </template>
      </el-result>
    </div>
    <div ref="host" class="map-host" :class="{ hidden: !keyReady }" />
  </div>
</template>

<style scoped>
.area-map-wrap { position: absolute; inset: 0; }
.map-host { width: 100%; height: 100%; background: #e8eaed; }
.map-host.hidden { display: none; }
.map-placeholder { display: flex; align-items: center; justify-content: center; width: 100%; height: 100%; }
.hint { color: #909399; font-size: 13px; }
</style>
