<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { listAreas, type AreaEntity } from '@/api/iot/area';
import { listControlDevices, listDeviceClass, type DeviceClassDict, type DeviceCtrlEntity } from '@/api/iot/device';
import { listGatewayNodes, type GatewayNodeItem } from '@/api/iot/gatewayManage';
import { listSensors, type SensorEntity } from '@/api/iot/sensor';
import type { GatewayTaskEntity } from '@/api/iot/gatewayManage';

const props = defineProps<{ modelValue: GatewayTaskEntity | null }>();
const emit = defineEmits<{ 'update:modelValue': [GatewayTaskEntity] }>();
const { t } = useI18n();

const MAX_TRIGGERS = 2;
const MAX_ACTIONS = 3;

interface TriggerRow {
  id: number;
  joinType: number;
  sensorKey: string;
  nodeId: string;
  channel: number;
  sensorCode: string;
  compareType: number;
  value: number | null;
}

interface ActionRow {
  id: number;
  deviceKey: string;
  nodeId: string;
  channel: number;
  deviceCode: string;
  action: number;
  value: number | null;
}

interface SelectOption<T extends string | number> { value: T; label: string }
interface SensorOption extends SelectOption<string> { nodeId: string; channel: number; sensorCode: string }
interface DeviceOption extends SelectOption<string> {
  nodeId: string;
  channel: number;
  deviceCode: string;
  deviceClass?: string;
}

const form = reactive<GatewayTaskEntity>({
  gatewayCode: '', taskName: '', taskNo: 1, enabled: true,
  triggerType: 'TIME', triggerTime: '07:00:00', triggers: '[]', actions: '[]',
});

const conditionRows = ref<TriggerRow[]>([]);
const actionRows = ref<ActionRow[]>([]);
const sourceInvalid = ref(false);
const optionsLoading = ref(false);
const nodes = ref<GatewayNodeItem[]>([]);
const sensors = ref<SensorEntity[]>([]);
const devices = ref<DeviceCtrlEntity[]>([]);
const areas = ref<AreaEntity[]>([]);
const deviceClasses = ref<DeviceClassDict[]>([]);

let rowId = 0;
const nextId = () => ++rowId;

const compareOptions = computed<SelectOption<number>[]>(() => [
  { value: 1, label: t('gatewayManage.compareEqual') },
  { value: 2, label: t('gatewayManage.compareNotEqual') },
  { value: 3, label: t('gatewayManage.compareGreater') },
  { value: 4, label: t('gatewayManage.compareLess') },
  { value: 5, label: t('gatewayManage.compareGreaterEqual') },
  { value: 6, label: t('gatewayManage.compareLessEqual') },
]);

const joinOptions = computed<SelectOption<number>[]>(() => [
  { value: 1, label: t('gatewayManage.joinAnd') },
  { value: 2, label: labelOfOr() },
]);

const triggerTypeOptions = computed<SelectOption<string>[]>(() => [
  { value: 'TIME', label: t('gatewayManage.triggerTypeTimed') },
  { value: 'CONDITION', label: t('gatewayManage.triggerTypeCondition') },
]);
const isTimeTrigger = computed(() => form.triggerType === 'TIME');

function labelOfOr() { return t('gatewayManage.joinOr'); }

const areaNameMap = computed(() => new Map(areas.value.map((area) => [area.id, area.areaName])));

const sensorOptions = computed<SensorOption[]>(() => sensors.value.map((sensor) => {
  const nodeId = sensor.nodeCode || '';
  const channel = Number(sensor.channel ?? 0);
  const code = sensor.sensorCode;
  const name = sensor.sensorName ? `${sensor.sensorName} - ${code}` : code;
  return {
    value: makeKey(nodeId, channel),
    nodeId,
    channel,
    sensorCode: code,
    label: buildLabel(sensor.areaId, nodeId, name),
  };
}));

const deviceOptions = computed<DeviceOption[]>(() => devices.value.map((device) => {
  const nodeId = device.nodeCode || '';
  const channel = Number(device.channel ?? 0);
  const code = device.deviceCode;
  const name = device.deviceName ? `${device.deviceName} - ${code}` : code;
  return {
    value: makeKey(nodeId, channel),
    nodeId,
    channel,
    deviceCode: code,
    deviceClass: device.deviceClass,
    label: buildLabel(device.areaId, nodeId, name),
  };
}));

function makeKey(nodeId?: string | number, channel?: unknown) {
  return `${nodeId || ''}|${channel ?? ''}`;
}

function buildLabel(areaId: string | undefined, nodeId: string, name: string) {
  const node = nodes.value.find((item) => item.nodeCode === nodeId);
  return [areaNameMap.value.get(areaId || ''), node?.nodeName || nodeId, name].filter(Boolean).join(' / ');
}

function readJsonArray(raw?: string): unknown[] {
  try {
    const parsed = JSON.parse(raw || '[]');
    return Array.isArray(parsed) ? parsed : [];
  } catch (error) {
    console.error('invalid gateway task json', error);
    sourceInvalid.value = true;
    return [];
  }
}

function toCompareType(value: unknown): number {
  if (typeof value === 'number' && value >= 1 && value <= 6) return value;
  const map: Record<string, number> = {
    '=': 1, '==': 1, equal: 1,
    '!=': 2, '<>': 2, notequal: 2,
    '>': 3, greater: 3,
    '<': 4, less: 4,
    '>=': 5, greaterequal: 5,
    '<=': 6, lessequal: 6,
  };
  return map[String(value ?? '').toLowerCase()] ?? 3;
}

function toJoinType(value: unknown): number {
  if (value === 1 || value === 2) return value;
  const text = String(value ?? '').toUpperCase();
  return text === 'OR' || text === '或' ? 2 : 1;
}

function toActionType(value: unknown): number {
  if (value === 1 || value === 2 || value === 3) return value;
  const map: Record<string, number> = {
    ON: 1, OPEN: 1, START: 1,
    OFF: 2, CLOSE: 2, STOP: 2,
    SET: 3,
  };
  return map[String(value ?? '').toUpperCase()] ?? 1;
}

function normalizeTriggers(raw?: string): TriggerRow[] {
  sourceInvalid.value = false;
  const items = readJsonArray(raw);
  const rows = items.map((entry) => {
    const item = entry && typeof entry === 'object' ? entry as Record<string, unknown> : {};
    const nodeId = String(item.nodeId ?? item.node ?? item.nodeCode ?? '');
    const channel = Number(item.channel ?? 0);
    const sensorCode = String(item.sensorCode ?? '');
    return {
      id: nextId(),
      joinType: toJoinType(item.joinType ?? item.join_type),
      sensorKey: makeKey(nodeId, channel),
      nodeId,
      channel,
      sensorCode,
      compareType: toCompareType(item.compareType ?? item.compare_type),
      value: item.value == null ? null : Number(item.value),
    } satisfies TriggerRow;
  });
  return rows.length ? rows : [createTriggerRow()];
}

function normalizeActions(raw?: string): ActionRow[] {
  const items = readJsonArray(raw);
  const rows = items.map((entry) => {
    const item = entry && typeof entry === 'object' ? entry as Record<string, unknown> : {};
    const nodeId = String(item.nodeId ?? item.node ?? item.nodeCode ?? '');
    const channel = Number(item.channel ?? 0);
    const deviceCode = String(item.deviceCode ?? '');
    return {
      id: nextId(),
      deviceKey: makeKey(nodeId, channel),
      nodeId,
      channel,
      deviceCode,
      action: toActionType(item.action),
      value: item.value == null ? null : Number(item.value),
    } satisfies ActionRow;
  });
  return rows.length ? rows : [createActionRow()];
}

function createTriggerRow(): TriggerRow {
  return {
    id: nextId(), joinType: 1, sensorKey: '', nodeId: '', channel: 0,
    sensorCode: '', compareType: 3, value: null,
  };
}

function createActionRow(): ActionRow {
  return {
    id: nextId(), deviceKey: '', nodeId: '', channel: 0,
    deviceCode: '', action: 1, value: null,
  };
}

function onSensorChange(row: TriggerRow, key: string) {
  const option = sensorOptions.value.find((item) => item.value === key);
  if (!option) {
    row.sensorKey = '';
    row.nodeId = '';
    row.channel = 0;
    row.sensorCode = '';
    return;
  }
  row.sensorKey = option.value;
  row.nodeId = option.nodeId;
  row.channel = option.channel;
  row.sensorCode = option.sensorCode;
}

function onDeviceChange(row: ActionRow, key: string) {
  const option = deviceOptions.value.find((item) => item.value === key);
  if (!option) {
    row.deviceKey = '';
    row.nodeId = '';
    row.channel = 0;
    row.deviceCode = '';
    return;
  }
  row.deviceKey = option.value;
  row.nodeId = option.nodeId;
  row.channel = option.channel;
  row.deviceCode = option.deviceCode;
  const options = actionOptionsFor(row);
  if (!options.some((item) => item.value === row.action)) row.action = options[0]?.value ?? 1;
}

function actionOptionsFor(row: ActionRow): SelectOption<number>[] {
  const option = deviceOptions.value.find((item) => item.value === row.deviceKey);
  const dict = deviceClasses.value.find((item) => item.deviceClass === option?.deviceClass);
  const options: SelectOption<number>[] = [];
  if (dict?.onName) options.push({ value: 1, label: dict.onName });
  if (dict?.offName) options.push({ value: 2, label: dict.offName });
  if (dict?.setName) options.push({ value: 3, label: dict.setName });
  if (options.length) return options;
  return [
    { value: 1, label: t('gatewayManage.actionOn') },
    { value: 2, label: t('gatewayManage.actionOff') },
    { value: 3, label: t('gatewayManage.actionSet') },
  ];
}

function reconcileRowKeys() {
  for (const row of conditionRows.value) {
    if (!row.sensorCode || !row.nodeId) {
      const option = sensorOptions.value.find((item) => item.nodeId === row.nodeId && item.channel === row.channel);
      if (option) onSensorChange(row, option.value);
    }
  }
  for (const row of actionRows.value) {
    if (!row.deviceCode || !row.nodeId) {
      const option = deviceOptions.value.find((item) => item.nodeId === row.nodeId && item.channel === row.channel);
      if (option) onDeviceChange(row, option.value);
    }
  }
}

async function loadOptions(gatewayCode: string) {
  if (!gatewayCode) {
    nodes.value = [];
    sensors.value = [];
    devices.value = [];
    areas.value = [];
    deviceClasses.value = [];
    return;
  }
  optionsLoading.value = true;
  try {
    const [nodeRes, areaRes, classRes] = await Promise.all([
      listGatewayNodes(gatewayCode), listAreas(), listDeviceClass(),
    ]);
    if (form.gatewayCode !== gatewayCode) return;
    nodes.value = nodeRes.data ?? [];
    areas.value = areaRes.data ?? [];
    deviceClasses.value = classRes.data ?? [];
    const details = await Promise.all(nodes.value.map((node) => Promise.all([
      listSensors(node.nodeCode), listControlDevices(node.nodeCode),
    ])));
    if (form.gatewayCode !== gatewayCode) return;
    sensors.value = details.flatMap(([sensorRes]) => sensorRes.data ?? []);
    devices.value = details.flatMap(([, deviceRes]) => deviceRes.data ?? []);
    reconcileRowKeys();
  } catch (error) {
    console.error('gateway task options load failed', error);
  } finally {
    if (form.gatewayCode === gatewayCode) optionsLoading.value = false;
  }
}

const triggerRowsInvalid = computed(() => isTimeTrigger.value ? false : conditionRows.value.some((row) =>
  !row.nodeId || !row.sensorKey || row.channel < 0 || row.value == null || !Number.isFinite(Number(row.value))));
const actionRowsInvalid = computed(() => actionRows.value.some((row) =>
  !row.nodeId || !row.deviceKey || row.channel < 0 || !row.action));
const rulesInvalid = computed(() => triggerRowsInvalid.value || actionRowsInvalid.value || sourceInvalid.value);
const canSave = computed(() => !!form.taskName?.trim() && !rulesInvalid.value
  && (isTimeTrigger.value || (conditionRows.value.length > 0 && conditionRows.value.length <= MAX_TRIGGERS))
  && actionRows.value.length > 0 && actionRows.value.length <= MAX_ACTIONS);

function addCondition() {
  if (conditionRows.value.length >= MAX_TRIGGERS) return;
  conditionRows.value.push(createTriggerRow());
}

function addAction() {
  if (actionRows.value.length >= MAX_ACTIONS) return;
  actionRows.value.push(createActionRow());
}

function removeCondition(index: number) {
  if (conditionRows.value.length <= 1) return;
  conditionRows.value.splice(index, 1);
}

function removeAction(index: number) {
  if (actionRows.value.length <= 1) return;
  actionRows.value.splice(index, 1);
}

function submit() {
  if (!canSave.value) return;
  form.triggers = isTimeTrigger.value ? '[]' : JSON.stringify(conditionRows.value.map((row, index) => ({
    joinType: index === 0 ? 1 : row.joinType,
    nodeId: row.nodeId,
    channel: Number(row.channel),
    compareType: Number(row.compareType),
    value: Number(row.value ?? 0),
  })));
  form.actions = JSON.stringify(actionRows.value.map((row) => ({
    nodeId: row.nodeId,
    channel: Number(row.channel),
    action: Number(row.action),
    value: row.action === 3 ? Number(row.value ?? 0) : 0,
  })));
  emit('update:modelValue', { ...form });
}

watch(() => props.modelValue, (task) => {
  Object.assign(form, {
    gatewayCode: '', taskName: '', taskNo: 1, enabled: true,
    triggerType: 'TIME', triggerTime: '07:00:00', triggers: '[]', actions: '[]',
  }, task ?? {});
  form.triggerType = task?.triggerType === 'CONDITION' ? 'CONDITION' : 'TIME';
  form.triggerTime = task?.triggerTime || '07:00:00';
  form.triggers = task?.triggers || '[]';
  form.actions = task?.actions || '[]';
  conditionRows.value = normalizeTriggers(form.triggers);
  actionRows.value = normalizeActions(form.actions);
}, { immediate: true });

watch(() => form.gatewayCode, (gatewayCode) => { void loadOptions(gatewayCode); }, { immediate: true });
watch([sensorOptions, deviceOptions], reconcileRowKeys);
</script>

<template>
  <el-form :model="form" label-width="110px" v-loading="optionsLoading">
    <el-form-item :label="$t('gatewayManage.taskNo')" required>
      <el-input-number v-model="form.taskNo" :min="1" :max="255" />
    </el-form-item>
    <el-form-item :label="$t('gatewayManage.taskName')" required>
      <el-input v-model="form.taskName" maxlength="128" />
    </el-form-item>
    <el-form-item :label="$t('gatewayManage.enabled')">
      <el-switch v-model="form.enabled" />
    </el-form-item>
    <el-form-item :label="$t('gatewayManage.triggerType')">
      <el-select v-model="form.triggerType" style="width: 180px">
        <el-option v-for="option in triggerTypeOptions" :key="option.value" :value="option.value" :label="option.label" />
      </el-select>
    </el-form-item>
    <el-form-item :label="$t('gatewayManage.triggerTime')" :class="{ 'is-field-disabled': !isTimeTrigger }">
      <el-time-picker v-model="form.triggerTime" format="HH:mm:ss" value-format="HH:mm:ss" :disabled="!isTimeTrigger" />
    </el-form-item>

    <el-form-item :label="$t('gatewayManage.triggers')" :required="!isTimeTrigger"
      :error="!isTimeTrigger && triggerRowsInvalid ? $t('gatewayManage.incompleteRule') : ''">
      <div class="section-head">
        <span class="limit">{{ conditionRows.length }}/{{ MAX_TRIGGERS }}</span>
      </div>
      <div class="rule-list" :class="{ 'section-disabled': isTimeTrigger }">
        <div v-for="(row, index) in conditionRows" :key="row.id" class="condition-row">
          <el-select v-if="index > 0" v-model="row.joinType" class="logic-select" size="small" :disabled="isTimeTrigger">
            <el-option v-for="option in joinOptions" :key="option.value" :value="option.value" :label="option.label" />
          </el-select>
          <div v-else class="logic-placeholder" />
          <div class="condition-fields">
            <el-select v-model="row.sensorKey" filterable clearable :placeholder="$t('gatewayManage.selectSensor')"
              class="sensor-select" :disabled="isTimeTrigger" @change="(value: string) => onSensorChange(row, value)">
              <el-option v-for="option in sensorOptions" :key="option.value" :value="option.value" :label="option.label" />
            </el-select>
            <el-select v-model="row.compareType" class="compare-select" :disabled="isTimeTrigger">
              <el-option v-for="option in compareOptions" :key="option.value" :value="option.value" :label="option.label" />
            </el-select>
            <el-input-number v-model="row.value" class="value-input" :min="0" :max="4294967295" :controls="false"
              :disabled="isTimeTrigger" />
            <el-button link type="danger" class="remove-button" :disabled="isTimeTrigger || conditionRows.length <= 1"
              @click="removeCondition(index)">×</el-button>
          </div>
        </div>
      </div>
      <el-button link type="primary" class="add-button" :disabled="isTimeTrigger || conditionRows.length >= MAX_TRIGGERS"
        @click="addCondition">
        + {{ $t('gatewayManage.addCondition') }}
      </el-button>
    </el-form-item>

    <el-form-item :label="$t('gatewayManage.actions')" required :error="actionRowsInvalid ? $t('gatewayManage.incompleteRule') : ''">
      <div class="section-head">
        <span class="limit">{{ actionRows.length }}/{{ MAX_ACTIONS }}</span>
      </div>
      <div class="rule-list">
        <div v-for="(row, index) in actionRows" :key="row.id" class="action-row">
          <div class="logic-placeholder" />
          <div class="action-fields">
            <el-select v-model="row.deviceKey" filterable clearable :placeholder="$t('gatewayManage.selectDevice')"
              class="device-select" @change="(value: string) => onDeviceChange(row, value)">
              <el-option v-for="option in deviceOptions" :key="option.value" :value="option.value" :label="option.label" />
            </el-select>
            <el-select v-model="row.action" class="action-select">
              <el-option v-for="option in actionOptionsFor(row)" :key="option.value" :value="option.value" :label="option.label" />
            </el-select>
            <el-input-number v-if="row.action === 3" v-model="row.value" class="value-input" :min="0" :max="4294967295" :controls="false" />
            <el-button link type="danger" class="remove-button" :disabled="actionRows.length <= 1"
              @click="removeAction(index)">×</el-button>
          </div>
        </div>
      </div>
      <el-button link type="primary" class="add-button" :disabled="actionRows.length >= MAX_ACTIONS" @click="addAction">
        + {{ $t('gatewayManage.addControlDevice') }}
      </el-button>
    </el-form-item>

    <el-form-item>
      <el-button type="primary" :disabled="!canSave" @click="submit">{{ $t('common.save') }}</el-button>
    </el-form-item>
  </el-form>
</template>

<style scoped>
.section-head {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  width: 100%;
  margin-bottom: 8px;
  font-weight: 600;
}
.limit { color: var(--el-text-color-secondary); font-size: 12px; font-weight: 400; }
.rule-list { display: flex; flex-direction: column; gap: 8px; width: 100%; }
.section-disabled { opacity: 0.55; }
.is-field-disabled :deep(.el-form-item__label) { color: var(--el-text-color-placeholder); }
.condition-row, .action-row { display: flex; align-items: center; gap: 8px; width: 100%; }
.logic-select, .logic-placeholder { flex: 0 0 58px; width: 58px; }
.condition-fields, .action-fields {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
  padding: 8px;
  background: #f0f7ff;
  border: 1px solid #d9e9ff;
  border-radius: 8px;
}
.sensor-select, .device-select { flex: 1 1 260px; min-width: 200px; }
.compare-select, .action-select { flex: 0 0 120px; width: 120px; }
.value-input { flex: 0 0 120px; width: 120px; }
.remove-button { flex: 0 0 28px; font-size: 18px; line-height: 1; }
.add-button { margin-top: 4px; }
</style>
