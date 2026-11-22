<script setup lang="ts">
import { computed } from 'vue';

/**
 * 轻量 SVG 折线图（零依赖）：历史曲线按测点绘制，支持点击取值。
 * props.points: [{ label, value }]
 */
const props = defineProps<{
  points: { label: string; value: number }[];
  unit?: string;
  height?: number;
}>();

const W = 640;
const H = computed(() => props.height ?? 220);
const PAD_L = 42;
const PAD_R = 16;
const PAD_T = 16;
const PAD_B = 26;

const values = computed(() => props.points.map((p) => p.value));
const lo = computed(() => Math.floor((Math.min(...values.value) - 1) / 5) * 5);
const hi = computed(() => Math.ceil((Math.max(...values.value) + 1) / 5) * 5);

const y = (v: number) => {
  const top = Number(hi.value);
  const bottom = Number(lo.value);
  const span = top - bottom || 1;
  return PAD_T + ((top - v) * (H.value - PAD_T - PAD_B)) / span;
};
const x = (i: number) => PAD_L + (i * (W - PAD_L - PAD_R)) / Math.max(props.points.length - 1, 1);

const path = computed(() =>
  props.points.map((p, i) => `${i === 0 ? 'M' : 'L'}${x(i).toFixed(1)},${y(p.value).toFixed(1)}`).join(' ')
);

const gridLines = computed(() => {
  const lines: { v: number; yy: number }[] = [];
  const top = Number(hi.value);
  const bottom = Number(lo.value);
  for (let i = 0; i < 4; i++) {
    const v = top - ((top - bottom) / 3) * i;
    lines.push({ v, yy: y(v) });
  }
  return lines;
});

const emit = defineEmits<{ (e: 'pick', point: { label: string; value: number }): void }>();

function onClick(evt: MouseEvent) {
  const rect = (evt.currentTarget as SVGElement).getBoundingClientRect();
  const rel = evt.clientX - rect.left - PAD_L;
  const span = W - PAD_L - PAD_R;
  const idx = Math.round((rel / span) * (props.points.length - 1));
  const clamped = Math.min(props.points.length - 1, Math.max(0, idx));
  emit('pick', props.points[clamped]);
}
</script>

<template>
  <svg :viewBox="`0 0 ${W} ${H}`" class="kb-chart" @click="onClick">
    <g v-for="g in gridLines" :key="g.v">
      <text :x="4" :y="g.yy + 4" class="kb-chart__tick">{{ Number(g.v.toFixed(1)) }}</text>
      <line :x1="PAD_L" :x2="W - PAD_R" :y1="g.yy" :y2="g.yy" class="kb-chart__grid" />
    </g>
    <path :d="path" class="kb-chart__line" />
    <circle
      v-for="(p, i) in points"
      :key="i"
      :cx="x(i)"
      :cy="y(p.value)"
      r="4"
      class="kb-chart__dot"
    />
    <text :x="PAD_L" :y="H - 6" class="kb-chart__tick">{{ points[0]?.label }}</text>
    <text :x="W - PAD_R" :y="H - 6" text-anchor="end" class="kb-chart__tick">
      {{ points[points.length - 1]?.label }}
    </text>
    <text v-if="unit" :x="W - PAD_R" :y="14" text-anchor="end" class="kb-chart__tick">{{ unit }}</text>
  </svg>
</template>

<style scoped>
.kb-chart {
  width: 100%;
  display: block;
  cursor: crosshair;
}
.kb-chart__grid {
  stroke: var(--kb-line, #d8e2d9);
  stroke-width: 1;
}
.kb-chart__line {
  fill: none;
  stroke: var(--kb-brand-bright, #35a476);
  stroke-width: 2.4;
  stroke-linejoin: round;
  stroke-linecap: round;
}
.kb-chart__dot {
  fill: var(--kb-brand, #17573f);
}
.kb-chart__tick {
  fill: var(--kb-text-muted, #52665d);
  font-size: 11px;
}
</style>