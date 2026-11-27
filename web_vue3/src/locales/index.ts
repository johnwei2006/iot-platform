import { createI18n, type I18nOptions } from 'vue-i18n';
import type { LocaleMessage } from '@intlify/core-base';
import common from './domains/common';
import framework from './domains/framework';
import gateway from './domains/gateway';
import node from './domains/node';
import sensor from './domains/sensor';
import control from './domains/control';
import device from './domains/device';
import sensorData from './domains/sensorData';
import area from './domains/area';
import alarm from './domains/alarm';
import messages from './domains/messages';
import system from './domains/system';

export type AppLocale = 'zh-CN' | 'en';

export const LOCALES: { value: AppLocale; label: string }[] = [
  { value: 'zh-CN', label: '简体中文' },
  { value: 'en', label: 'English' },
];

const domainModules: Record<AppLocale, LocaleMessage>[] = [
  common,
  framework,
  gateway,
  node,
  sensor,
  control,
  device,
  sensorData,
  area,
  alarm,
  messages,
  system,
];

function pickMessages(locale: AppLocale): LocaleMessage {
  return Object.assign({}, ...domainModules.map((m) => m[locale] ?? {}));
}

/** vue-i18n 实例：语言选择由 stores/modules/locale.ts 在启动时初始化。 */
const options: I18nOptions = {
  legacy: false,
  globalInjection: true,
  locale: 'zh-CN',
  fallbackLocale: 'zh-CN',
  messages: {
    'zh-CN': pickMessages('zh-CN'),
    en: pickMessages('en'),
  },
};
export const i18n = createI18n(options);
