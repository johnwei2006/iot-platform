import { defineStore } from 'pinia';
import { storage } from '@/utils/storage';

export type ThemeMode = 'light' | 'dark';
export type FontSize = 'standard' | 'large';
/** 配色风格：企业蓝（默认）/ 翡翠绿（品牌绿）/ 石墨灰 */
export type ThemeSkin = 'blue' | 'green' | 'gray';

const SKINS: ThemeSkin[] = ['blue', 'green', 'gray'];

interface ThemeState {
  mode: ThemeMode;
  fontSize: FontSize;
  skin: ThemeSkin;
}

function loadSkin(): ThemeSkin {
  const stored = storage.get<ThemeSkin>('themeSkin');
  return SKINS.includes(stored as ThemeSkin) ? (stored as ThemeSkin) : 'blue';
}

/** 主题偏好：配色风格、深浅模式与字号，持久化到 localStorage。 */
export const useThemeStore = defineStore('theme', {
  state: (): ThemeState => ({
    mode: storage.get<ThemeMode>('themeMode') === 'dark' ? 'dark' : 'light',
    fontSize: storage.get<FontSize>('fontSize') === 'large' ? 'large' : 'standard',
    skin: loadSkin(),
  }),
  actions: {
    apply(): void {
      const root = document.documentElement;
      root.dataset.theme = this.mode;
      root.dataset.fs = this.fontSize;
      root.dataset.skin = this.skin;
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
    setSkin(skin: ThemeSkin): void {
      this.skin = skin;
      storage.set('themeSkin', skin);
      this.apply();
    },
    toggleMode(): void {
      this.setMode(this.mode === 'dark' ? 'light' : 'dark');
    },
  },
});
