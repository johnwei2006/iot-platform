<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { listAlarms } from '@/api/iot/alarms';
import { listControlLogs } from '@/api/iot/control';
import type { ControlLog } from '@/types/kb';

interface AlarmRow {
  id: string;
  deviceId?: string;
  rule?: string;
  level?: string;
  message?: string;
  status?: string;
  createdAt?: string;
  resolvedAt?: string;
}

const loading = ref(false);
const alarms = ref<AlarmRow[]>([]);
const kind = ref('全部');
const status = ref('全部');
const detailVisible = ref(false);
const current = ref<AlarmRow | null>(null);
const relatedLogs = ref<ControlLog[]>([]);

const KIND_MAP: Record<string, string> = { 'threshold:': '指标超限', 'collect:': '采集异常', 'control:': '设备异常' };

function kindOf(a: AlarmRow) {
  const rule = a.rule || '';
  for (const [prefix, label] of Object.entries(KIND_MAP)) {
    if (rule.startsWith(prefix)) return label;
  }
  return '设备异常';
}

const filtered = computed(() =>
  alarms.value.filter((a) => (kind.value === '全部' || kindOf(a) === kind.value) && (status.value === '全部' || a.status === (status.value === '持续中' ? 'OPEN' : 'RESOLVED')))
);

async function refresh() {
  loading.value = true;
  try {
    const res = await listAlarms();
    alarms.value = (res.data || []) as unknown as AlarmRow[];
  } catch (error) {
    ElMessage.error((error as Error).message || '告警加载失败');
  } finally {
    loading.value = false;
  }
}

async function openDetail(a: AlarmRow) {
  current.value = a;
  detailVisible.value = true;
  relatedLogs.value = [];
  if (a.deviceId) {
    try {
      const res = await listControlLogs({ deviceId: a.deviceId });
      relatedLogs.value = (res.data || []).slice(0, 5);
    } catch { /* 关联记录可选 */ }
  }
}

onMounted(refresh);
</script>

<template>
  <div v-loading="loading">
    <el-card shadow="never">
      <template #header>
        <div class="filter-bar">
          <el-segmented v-model="kind" :options="['全部', '指标超限', '采集异常', '设备异常']" />
          <el-segmented v-model="status" :options="['全部', '持续中', '已恢复']" />
        </div>
      </template>
      <el-empty v-if="!filtered.length" description="当前筛选下没有告警" />
      <div v-else class="alarm-list">
        <button v-for="a in filtered" :key="a.id" class="kb-tile alarm-item kb-press" :class="a.status === 'RESOLVED' ? 'is-ok' : kindOf(a) === '指标超限' ? 'kb-tile--hot' : 'kb-tile--warn'" @click="openDetail(a)">
          <div class="alarm-item__main">
            <span class="alarm-item__title">{{ a.deviceId }} · {{ a.message }}</span>
            <span class="alarm-item__sub">{{ kindOf(a) }} · {{ a.createdAt }}</span>
          </div>
          <el-tag :type="a.status === 'RESOLVED' ? 'success' : 'danger'" size="small">{{ a.status === 'RESOLVED' ? '已恢复' : '持续中' }}</el-tag>
        </button>
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="告警详情" width="640px">
      <template v-if="current">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="状态">{{ current.status === 'RESOLVED' ? '已恢复' : '持续中' }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ kindOf(current) }}</el-descriptions-item>
          <el-descriptions-item label="设备">{{ current.deviceId }}</el-descriptions-item>
          <el-descriptions-item label="内容">{{ current.message }}</el-descriptions-item>
          <el-descriptions-item label="触发时间">{{ current.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="恢复时间">{{ current.resolvedAt || '—' }}</el-descriptions-item>
        </el-descriptions>
        <h4 class="rel-title">关联控制记录</h4>
        <el-empty v-if="!relatedLogs.length" description="无关联记录" :image-size="56" />
        <div v-for="l in relatedLogs" :key="l.id" class="rel-row">
          <span>{{ l.createdAt }} · {{ l.action }} · {{ l.deviceName || l.deviceId }}</span>
          <el-tag size="small" :type="l.result === 'SUCCESS' ? 'success' : l.result === 'PENDING' ? 'info' : 'danger'">{{ l.result }}</el-tag>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.filter-bar { display: flex; gap: 12px; }
.alarm-list { display: flex; flex-direction: column; gap: 10px; }
.alarm-item { display: flex; align-items: center; justify-content: space-between; padding: 14px 16px; cursor: pointer; font: inherit; color: inherit; text-align: left; width: 100%; }
.alarm-item.is-ok { background: var(--kb-success-bg); border-color: #cfe6d5; color: var(--kb-success); }
.alarm-item__main { display: flex; flex-direction: column; gap: 4px; }
.alarm-item__title { font-weight: 600; }
.alarm-item__sub { font-size: var(--kb-fs-caption); opacity: 0.75; }
.rel-title { margin: 16px 0 8px; color: var(--kb-text-muted); }
.rel-row { display: flex; justify-content: space-between; align-items: center; padding: 8px 12px; border-bottom: 1px solid var(--kb-line); font-size: var(--kb-fs-body); }
</style>