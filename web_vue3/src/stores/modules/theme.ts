import { defineStore } from 'pinia';
import { storage } from '@/utils/storage';

export type ThemeMode = 'light' | 'dark';
export type FontSize = 'standard' | 'large';

interface ThemeState {
  mode: ThemeMode;
  fontSize: FontSize;
}

/** 主题偏好：手动覆盖系统深色模式，持久化到 localStorage。 */
export const useThemeStore = defineStore('theme', {
  state: (): ThemeState => ({
    mode: storage.get<ThemeMode>('themeMode') === 'dark' ? 'dark' : 'light',
    fontSize: storage.get<FontSize>('fontSize') === 'large' ? 'large' : 'standard',
  }),
  actions: {
    apply(): void {
      const root = document.documentElement;
      root.dataset.theme = this.mode;
      root.dataset.fs = this.fontSize;
    },
    init(): void {
      this.apply();
    },
    setMode(mode: ThemeMode): void {
      this.mode = mode;
      storage.set('themeMode', mode);
      this.apply();
    },
    setFontSize(fontSize: FontSize): void {
      this.fontSize = fontSize;
      storage.set('fontSize', fontSize);
      this.apply();
    },
    toggleMode(): void {
      this.setMode(this.mode === 'dark' ? 'light' : 'dark');
    },
  },
});
