import { defineStore } from 'pinia';
import { ref } from 'vue';

/** 区域（地块）选择：替代 dist 中 app.globalData.currentPlot / plotList。 */
export const usePlotStore = defineStore('plot', () => {
  const plotList = ref<string[]>(['1#冷棚-L808', '2#冷棚-申香215']);
  const currentPlot = ref<string>(plotList.value[0]);

  function selectPlot(val: string) {
    if (!plotList.value.includes(val)) return;
    currentPlot.value = val;
    uni.setStorageSync('kb.currentPlot', val);
  }

  function restore() {
    const saved = uni.getStorageSync('kb.currentPlot') as string;
    if (saved && plotList.value.includes(saved)) currentPlot.value = saved;
  }

  restore();

  return { plotList, currentPlot, selectPlot, restore };
});
