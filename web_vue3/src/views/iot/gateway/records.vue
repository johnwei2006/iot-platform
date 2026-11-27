<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { ElMessage } from 'element-plus';
import { fetchReconnectRecords, fetchTokenRequests } from '@/api/iot/gatewayRecords';
import type { GatewayReconnectRecord, GatewayTokenRequestRecord } from '@/types';

const { t } = useI18n();
const props = defineProps<{ deviceId?: string }>();

const activeTab = ref<'reconnect' | 'token'>('reconnect');
const loading = ref(false);

const filter = reactive({
  deviceId: props.deviceId ?? '',
  page: 0,
  size: 20,
});

const reconnectTotal = ref(0);
const reconnectRecords = ref<GatewayReconnectRecord[]>([]);
const tokenTotal = ref(0);
const tokenRecords = ref<GatewayTokenRequestRecord[]>([]);

const resultTagType = (result: string): 'success' | 'danger' | 'warning' | 'info' => {
  if (result === 'SUCCESS') return 'success';
  if (result === 'ERROR') return 'danger';
  if (result === 'INVALID_SECRET' || result === 'UNKNOWN_DEVICE') return 'warning';
  return 'info';
};

const resultText = (result: string): string => {
  const map: Record<string, string> = {
    SUCCESS: t('gatewayRecords.resultSuccess'),
    UNKNOWN_DEVICE: t('gatewayRecords.unknownDevice'),
    INVALID_SECRET: t('gatewayRecords.invalidSecret'),
    ERROR: t('gatewayRecords.platformError'),
  };
  return map[result] ?? result;
};

const formatTime = (value?: string): string => {
  if (!value) return '-';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('zh-CN');
};

async function load() {
  loading.value = true;
  try {
    const params = { deviceId: filter.deviceId || undefined, page: filter.page, size: filter.size };
    if (activeTab.value === 'reconnect') {
      const res = await fetchReconnectRecords(params);
      reconnectRecords.value = res.records ?? [];
      reconnectTotal.value = res.total ?? 0;
    } else {
      const res = await fetchTokenRequests(params);
      tokenRecords.value = res.records ?? [];
      tokenTotal.value = res.total ?? 0;
    }
  } catch (error) {
    console.error('加载网关认证记录失败', error);
    ElMessage.error(t('gatewayRecords.loadFailed'));
  } finally {
    loading.value = false;
  }
}

function switchTab() {
  filter.page = 0;
  load();
}

function search() {
  filter.page = 0;
  load();
}

function reset() {
  filter.deviceId = '';
  filter.page = 0;
  load();
}

function handlePageChange(page: number) {
  filter.page = page - 1;
  load();
}

function handleSizeChange(size: number) {
  filter.size = size;
  filter.page = 0;
  load();
}

watch(() => props.deviceId, (value) => {
  filter.deviceId = value ?? '';
  filter.page = 0;
  load();
});

onMounted(load);
</script>

<template>
  <div class="gateway-records">
    <el-card shadow="never" class="filter-card">
      <div class="filter-bar">
        <el-input
          v-model="filter.deviceId"
          :placeholder="$t('gatewayRecords.deviceIdPlaceholder')"
          clearable
          class="filter-input"
          @keyup.enter="search"
        />
        <el-button type="primary" @click="search">{{ $t('common.query') }}</el-button>
        <el-button @click="reset">{{ $t('common.reset') }}</el-button>
      </div>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-tabs v-model="activeTab" @tab-change="switchTab">
        <el-tab-pane :label="$t('gatewayRecords.reconnect')" name="reconnect">
          <el-table :data="reconnectRecords" v-loading="loading" border stripe>
            <el-table-column prop="deviceId" :label="$t('gatewayRecords.deviceId')" min-width="140" />
            <el-table-column prop="ip" :label="$t('gatewayRecords.ip')" min-width="140" />
            <el-table-column prop="port" :label="$t('gatewayRecords.port')" width="90" />
            <el-table-column :label="$t('gatewayRecords.connectTime')" min-width="170">
              <template #default="{ row }">{{ formatTime(row.connectTime) }}</template>
            </el-table-column>
            <el-table-column :label="$t('common.type')" width="110">
              <template #default="{ row }">
                <el-tag :type="row.type === 'FIRST' ? 'success' : 'primary'">
                  {{ row.type === 'FIRST' ? $t('gatewayRecords.first') : $t('gatewayRecords.reconnectTag') }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane :label="$t('gatewayRecords.tokenRequests')" name="token">
          <el-table :data="tokenRecords" v-loading="loading" border stripe>
            <el-table-column prop="deviceId" :label="$t('gatewayRecords.deviceId')" min-width="140" />
            <el-table-column prop="ip" :label="$t('gatewayRecords.ip')" min-width="140" />
            <el-table-column prop="port" :label="$t('gatewayRecords.port')" width="90" />
            <el-table-column :label="$t('gatewayRecords.requestTime')" min-width="170">
              <template #default="{ row }">{{ formatTime(row.requestTime) }}</template>
            </el-table-column>
            <el-table-column :label="$t('gatewayRecords.result')" width="130">
              <template #default="{ row }">
                <el-tag :type="resultTagType(row.result)">{{ resultText(row.result) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="$t('gatewayRecords.tokenHex')" min-width="180">
              <template #default="{ row }">
                <span class="token-hex">{{ row.tokenHex || '-' }}</span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>

      <div class="pagination-bar">
        <el-pagination
          :current-page="filter.page + 1"
          :page-size="filter.size"
          :total="activeTab === 'reconnect' ? reconnectTotal : tokenTotal"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.gateway-records {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.filter-card {
  background: #fff;
}
.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
}
.filter-input {
  width: 260px;
}
.table-card {
  background: #fff;
}
.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
.token-hex {
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 12px;
  color: #303133;
}
</style>
