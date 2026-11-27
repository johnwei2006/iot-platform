/** 科百云田业务类型（与 web_vue3/src/types/kb.ts 同构） */

export interface DeviceEntry {
  deviceId: string;
  nodeId?: string;
  channel?: number;
  deviceName?: string;
  deviceType?: string;
}

export interface ControlRule {
  id?: string;
  name: string;
  kind: 'GROUP' | 'TIMED' | 'CONDITION';
  trigger: 'MANUAL' | 'TIMED' | 'CONDITION';
  devices: string;
  action?: string;
  time?: string;
  closeTime?: string;
  metric?: string;
  metricDeviceId?: string;
  station?: string;
  operator?: string;
  threshold?: number;
  startTime?: string;
  endTime?: string;
  effectiveFrom?: string;
  effectiveTo?: string;
  useReturn?: boolean;
  returnThreshold?: number;
  plotId?: string;
  enabled?: boolean;
}

export interface ConflictHit {
  ruleId?: string;
  ruleName: string;
  hard: boolean;
  detail: string;
}

export interface ConflictResult {
  error: string;
  hits: ConflictHit[];
}

export interface ControlLog {
  id: string;
  ruleId?: string;
  ruleName?: string;
  deviceId: string;
  deviceName?: string;
  source: 'MANUAL' | 'RULE' | 'GROUP';
  action: string;
  result: 'PENDING' | 'SUCCESS' | 'TIMEOUT' | 'FAILED' | 'PARTIAL';
  detail?: string;
  createdAt?: string;
}

export interface KbMessage {
  id: string;
  userId: string;
  type: 'ALARM' | 'SYSTEM';
  refId?: string;
  title: string;
  content?: string;
  readAt?: string;
  createdAt?: string;
}

export interface MetricThreshold {
  id?: string;
  deviceId: string;
  metric: string;
  station?: string;
  minValue?: number;
  maxValue?: number;
}

export interface RealtimeSensor {
  channel: number;
  sensorCode?: string;
  sensorType?: string;
  unit?: string;
  sensorValue: number;
}

export interface RealtimeStatus {
  deviceId: string;
  status: string;
  lastSeen?: number;
  sensors?: RealtimeSensor[];
}