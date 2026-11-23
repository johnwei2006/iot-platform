<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { usePlotStore } from '@/store/modules/plot';

interface DeviceItem { id: number; name: string; online: boolean; state: boolean }
interface TimerTask { devName: string; action: string; time: string; cycle: string; enable: boolean }
interface RuleTask { desc: string; enable: boolean }

const plot = usePlotStore();
const areaPopShow = ref(false);

const deviceList = ref<DeviceItem[]>([
  { id: 1, name: '通风风机', online: true, state: false },
  { id: 2, name: '喷淋补水', online: true, state: false },
  { id: 3, name: '保温被', online: true, state: true },
  { id: 4, name: '湿帘', online: false, state: false },
  { id: 5, name: '补光灯', online: true, state: false },
]);

const timerOpen = ref(false);
const timerList = ref<TimerTask[]>([
  { devName: '风机', action: '开启', time: '08:00', cycle: '每天', enable: true },
]);

const ruleOpen = ref(false);
const ruleList = ref<RuleTask[]>([
  { desc: '棚内温度 ＜8℃ → 开启保温被', enable: true },
  { desc: '湿度＞95% → 开启风机', enable: true },
]);

const sensorOpt = ['温度', '湿度', 'CO₂'];
const opOpt = ['大于', '小于'];
const devOpt = ['通风风机', '喷淋补水', '保温被', '湿帘', '补光灯'];
const actOpt = ['开启', '关闭'];
const cycleOpt = ['每天', '单次'];

const rulePopShow = ref(false);
const ruleForm = reactive({ sensorIdx: 0, opIdx: 1, val: '8', devIdx: 2, actIdx: 0 });

const timerPopShow = ref(false);
const timerForm = reactive({ devIdx: 0, actIdx: 0, time: '08:00', cycleIdx: 0 });

const currentPlot = computed(() => plot.currentPlot);

function openAreaPop() { areaPopShow.value = true; }
function closeAreaPop() { areaPopShow.value = false; }
function selectArea(val: string) {
  plot.selectPlot(val);
  areaPopShow.value = false;
  uni.showToast({ title: '已切换：' + val });
}
function onRefresh() { uni.showToast({ title: '刷新', icon: 'loading' }); }

function onSwitch(item: DeviceItem, e: { detail: { value: boolean } }) {
  item.state = e.detail.value;
  uni.showToast({ title: '指令下发中', icon: 'loading', duration: 1200 });
}

function toggleTimer() { timerOpen.value = !timerOpen.value; }
function toggleRule() { ruleOpen.value = !ruleOpen.value; }
function openTimerPop() { timerPopShow.value = true; }
function closeTimerPop() { timerPopShow.value = false; }
function openRulePop() { rulePopShow.value = true; }
function closeRulePop() { rulePopShow.value = false; }
function stop() {}

function toggleTimerTask(idx: number, e: { detail: { value: boolean } }) { timerList.value[idx].enable = e.detail.value; }
function toggleRuleTask(idx: number, e: { detail: { value: boolean } }) { ruleList.value[idx].enable = e.detail.value; }
function editTimer(idx: number) { uni.showToast({ title: '编辑定时 ' + (idx + 1), icon: 'none' }); }
function delTimer(idx: number) { timerList.value.splice(idx, 1); }
function editRule(idx: number) { uni.showToast({ title: '编辑规则 ' + (idx + 1), icon: 'none' }); }
function delRule(idx: number) { ruleList.value.splice(idx, 1); }

function ruleSensorChange(e: { detail: { value: number } }) { ruleForm.sensorIdx = Number(e.detail.value); }
function ruleOpChange(e: { detail: { value: number } }) { ruleForm.opIdx = Number(e.detail.value); }
function ruleValInput(e: { detail: { value: string } }) { ruleForm.val = e.detail.value; }
function ruleDevChange(e: { detail: { value: number } }) { ruleForm.devIdx = Number(e.detail.value); }
function ruleActChange(e: { detail: { value: number } }) { ruleForm.actIdx = Number(e.detail.value); }
function timerDevChange(e: { detail: { value: number } }) { timerForm.devIdx = Number(e.detail.value); }
function timerActChange(e: { detail: { value: number } }) { timerForm.actIdx = Number(e.detail.value); }
function timerTimeChange(e: { detail: { value: string } }) { timerForm.time = e.detail.value; }
function timerCycleChange(e: { detail: { value: number } }) { timerForm.cycleIdx = Number(e.detail.value); }

function saveRule() {
  ruleList.value.push({
    desc: `${sensorOpt[ruleForm.sensorIdx]} ${opOpt[ruleForm.opIdx]} ${ruleForm.val} → ${actOpt[ruleForm.actIdx]}${devOpt[ruleForm.devIdx]}`,
    enable: true,
  });
  rulePopShow.value = false;
  uni.showToast({ title: '规则已保存' });
}
function saveTimer() {
  timerList.value.push({
    devName: devOpt[timerForm.devIdx],
    action: actOpt[timerForm.actIdx],
    time: timerForm.time,
    cycle: cycleOpt[timerForm.cycleIdx],
    enable: true,
  });
  timerPopShow.value = false;
  uni.showToast({ title: '定时已保存' });
}

onShow(() => plot.restore());
</script>

<template>
  <view class="container">
    <view class="nav-bar">
      <view class="nav-area-text">{{ currentPlot }}</view>
      <view class="nav-right">
        <view class="area-btn" @tap="openAreaPop">切换</view>
        <view class="refresh" @tap="onRefresh">↻</view>
      </view>
    </view>

    <view v-if="areaPopShow" class="pop-mask" @tap="closeAreaPop">
      <view class="pop area-pop" @tap.stop="stop">
        <view class="pop-title">选择区域</view>
        <view
          v-for="(item, idx) in plot.plotList"
          :key="idx"
          class="area-item"
          :class="{ active: currentPlot === item }"
          @tap="selectArea(item)"
        >{{ item }}</view>
        <view class="pop-cancel" @tap="closeAreaPop">取消</view>
      </view>
    </view>

    <view class="block">
      <view class="block-title">设备状态</view>
      <view v-for="item in deviceList" :key="item.id" class="device-item">
        <view class="dev-left">
          <view class="dev-name">{{ item.name }}</view>
          <view class="dev-status" :class="item.online ? 'online' : 'offline'">
            {{ item.online ? '在线' : '离线' }}｜{{ item.state ? '开启' : '关闭' }}
          </view>
        </view>
        <switch :checked="item.state" :disabled="!item.online" @change="onSwitch(item, $event)" />
      </view>
    </view>

    <view class="block">
      <view class="fold-head" @tap="toggleTimer">
        <view class="block-title">定时控制</view>
        <view>{{ timerOpen ? '▼' : '▶' }}</view>
      </view>
      <view v-if="timerOpen">
        <view class="add-btn" @tap="openTimerPop">+新增定时任务</view>
        <view v-for="(item, idx) in timerList" :key="idx" class="task-card">
          <view>{{ item.devName }}｜{{ item.action }}｜{{ item.time }}｜{{ item.cycle }}</view>
          <view class="task-op">
            <switch :checked="item.enable" @change="toggleTimerTask(idx, $event)" />
            <button size="mini" @tap="editTimer(idx)">编辑</button>
            <button size="mini" @tap="delTimer(idx)">删除</button>
          </view>
        </view>
      </view>
    </view>

    <view class="block">
      <view class="fold-head" @tap="toggleRule">
        <view class="block-title">条件联动控制</view>
        <view>{{ ruleOpen ? '▼' : '▶' }}</view>
      </view>
      <view v-if="ruleOpen">
        <view class="add-btn" @tap="openRulePop">+新增联动规则</view>
        <view v-for="(item, idx) in ruleList" :key="idx" class="task-card">
          <view>{{ item.desc }}</view>
          <view class="task-op">
            <switch :checked="item.enable" @change="toggleRuleTask(idx, $event)" />
            <button size="mini" @tap="editRule(idx)">编辑</button>
            <button size="mini" @tap="delRule(idx)">删除</button>
          </view>
        </view>
      </view>
    </view>

    <view v-if="rulePopShow" class="pop-mask" @tap="closeRulePop">
      <view class="pop" @tap.stop="stop">
        <view class="pop-title">新增联动策略</view>
        <picker :value="ruleForm.sensorIdx" :range="sensorOpt" @change="ruleSensorChange">
          <view class="pop-row">传感器：{{ sensorOpt[ruleForm.sensorIdx] }}</view>
        </picker>
        <picker :value="ruleForm.opIdx" :range="opOpt" @change="ruleOpChange">
          <view class="pop-row">条件：{{ opOpt[ruleForm.opIdx] }}</view>
        </picker>
        <input class="pop-row" placeholder="阈值" :value="ruleForm.val" @input="ruleValInput" />
        <picker :value="ruleForm.devIdx" :range="devOpt" @change="ruleDevChange">
          <view class="pop-row">受控设备：{{ devOpt[ruleForm.devIdx] }}</view>
        </picker>
        <picker :value="ruleForm.actIdx" :range="actOpt" @change="ruleActChange">
          <view class="pop-row">动作：{{ actOpt[ruleForm.actIdx] }}</view>
        </picker>
        <view class="pop-btn-row">
          <button @tap="closeRulePop">取消</button>
          <button @tap="saveRule">保存</button>
        </view>
      </view>
    </view>

    <view v-if="timerPopShow" class="pop-mask" @tap="closeTimerPop">
      <view class="pop" @tap.stop="stop">
        <view class="pop-title">新增定时任务</view>
        <picker :value="timerForm.devIdx" :range="devOpt" @change="timerDevChange">
          <view class="pop-row">设备：{{ devOpt[timerForm.devIdx] }}</view>
        </picker>
        <picker :value="timerForm.actIdx" :range="actOpt" @change="timerActChange">
          <view class="pop-row">动作：{{ actOpt[timerForm.actIdx] }}</view>
        </picker>
        <picker mode="time" :value="timerForm.time" @change="timerTimeChange">
          <view class="pop-row">时间：{{ timerForm.time }}</view>
        </picker>
        <picker :value="timerForm.cycleIdx" :range="cycleOpt" @change="timerCycleChange">
          <view class="pop-row">周期：{{ cycleOpt[timerForm.cycleIdx] }}</view>
        </picker>
        <view class="pop-btn-row">
          <button @tap="closeTimerPop">取消</button>
          <button @tap="saveTimer">保存</button>
        </view>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.container { padding: 12rpx; background: #f7f8fa; min-height: 100vh; }
.nav-bar { display: flex; align-items: center; justify-content: space-between; padding: 16rpx 12rpx; background: #fff; border-radius: 12rpx; margin-bottom: 12rpx; }
.nav-area-text { font-size: 32rpx; font-weight: bold; }
.nav-right { display: flex; align-items: center; gap: 16rpx; }
.area-btn { font-size: 28rpx; color: #07c160; padding: 4rpx 8rpx; border: 1rpx solid #07c160; border-radius: 6rpx; }
.refresh { font-size: 32rpx; padding: 0 4rpx; }
.block { background: #fff; border-radius: 12rpx; padding: 20rpx; margin-bottom: 12rpx; }
.block-title { font-size: 30rpx; font-weight: bold; margin-bottom: 16rpx; }
.fold-head { display: flex; justify-content: space-between; align-items: center; }
.device-item { display: flex; justify-content: space-between; align-items: center; padding: 16rpx 0; border-bottom: 1rpx solid #eee; }
.dev-name { font-size: 28rpx; }
.dev-status { font-size: 24rpx; margin-top: 4rpx; }
.online { color: #07c160; }
.offline { color: #999; }
.add-btn { background: #07c160; color: #fff; text-align: center; padding: 12rpx; border-radius: 8rpx; margin: 12rpx 0; }
.task-card { border: 1rpx solid #eee; border-radius: 8rpx; padding: 12rpx; margin: 8rpx 0; }
.task-op { display: flex; gap: 8rpx; align-items: center; margin-top: 8rpx; }
.pop-mask { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0, 0, 0, 0.5); display: flex; align-items: center; justify-content: center; z-index: 999; }
.pop { width: 80%; background: #fff; border-radius: 12rpx; padding: 24rpx; }
.area-pop { width: 85%; }
.area-item { padding: 24rpx 12rpx; font-size: 28rpx; border-bottom: 1rpx solid #eee; }
.area-item.active { color: #07c160; font-weight: bold; }
.pop-cancel { text-align: center; padding: 20rpx; font-size: 28rpx; color: #666; margin-top: 12rpx; }
.pop-title { text-align: center; font-size: 32rpx; font-weight: bold; margin-bottom: 20rpx; }
.pop-row { padding: 12rpx 0; border-bottom: 1rpx solid #eee; font-size: 28rpx; }
.pop-btn-row { display: flex; gap: 16rpx; margin-top: 20rpx; }
</style>
