import { defineStore } from 'pinia';
import type { Composer } from 'vue-i18n';
import { storage } from '@/utils/storage';
import { i18n, type AppLocale } from '@/locales';

const composer = i18n.global as unknown as Composer;

/** 语言偏好：手动切换持久化到 localStorage；未设置时按浏览器语言自动选择。 */
export const useLocaleStore = defineStore('locale', {
  state: (): { locale: AppLocale } => ({
    locale: detectLocale(),
  }),
  actions: {
    init(): void {
      this.apply();
    },
    apply(): void {
      composer.locale.value = this.locale;
      document.documentElement.lang = this.locale;
    },
    setLocale(locale: AppLocale): void {
      this.locale = locale;
      storage.set('appLocale', locale);
      this.apply();
    },
  },
});

function detectLocale(): AppLocale {
  const stored = storage.get<AppLocale>('appLocale');
  if (stored === 'zh-CN' || stored === 'en') return stored;
  const lang = (typeof navigator !== 'undefined' && navigator.language) || '';
  return lang.toLowerCase().startsWith('zh') ? 'zh-CN' : 'en';
}
