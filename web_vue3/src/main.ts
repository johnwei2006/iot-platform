import { createApp } from 'vue';
import * as ElementPlusIconsVue from '@element-plus/icons-vue';
import App from './App.vue';
import router from './router';
import pinia from './stores';
import { useThemeStore } from './stores/modules/theme';
import { useLocaleStore } from './stores/modules/locale';
import { setupElementPlus } from './plugins/element';
import { setupDirectives } from './directives/permission';
import { i18n } from './locales';
import './styles/index.css';
// 苹果风设计系统 token（须在 Element Plus 样式之后引入以覆盖 --el-* 变量）
import './styles/theme.css';

const app = createApp(App);

app.use(pinia);
useThemeStore().init();
useLocaleStore().init();
app.use(i18n);
app.use(router);
setupElementPlus(app);
setupDirectives(app);

// 全局注册 Element Plus 图标组件（模板中可直接使用 <HomeFilled /> 等）
for (const [name, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(name, component);
}

app.mount('#app');
