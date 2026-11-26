
/* ===== 科百云田业务类型（数据管理/设备控制/消息任务） ===== */

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
  trigger: 'MANUAL' | 'TIMED' | 'CONDITION' | 'GROUP' | string;
  devices: string; // JSON 数组字符串
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
  useReturn?: boolean;
  returnThreshold?: number;
  plotId?: string;
  orgCode?: string;
  ownerUserId?: string;
  enabled?: boolean;
  createdAt?: string;
  updatedAt?: string;
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
  nodeId?: string;
  channel?: number;
  deviceName?: string;
  source: 'MANUAL' | 'RULE' | 'GROUP';
  plotId?: string;
  action: string;
  actionValue?: string;
  result: 'PENDING' | 'SUCCESS' | 'TIMEOUT' | 'FAILED' | 'PARTIAL';
  detail?: string;
  orgCode?: string;
  createdAt?: string;
  finishedAt?: string;
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

export interface NotifyPref {
  id?: string;
  userId: string;
  receiveKinds?: string;
  channels?: string;
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
  nodeId?: string;
  status: 'ONLINE' | 'OFFLINE' | string;
  lastSeen?: number;
  updatedAt?: number;
  ttlSeconds?: number;
  serviceType?: number;
  sensors?: RealtimeSensor[];
  latestBusinessData?: Record<string, unknown>;
}