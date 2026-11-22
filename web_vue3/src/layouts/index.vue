<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useUserStore } from '@/stores/modules/user';
import { Fold, User, SwitchButton } from '@element-plus/icons-vue';
import type { MenuNode } from '@/types';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const activeMenu = computed(() => route.path);
const username = computed(() => userStore.userInfo?.name ?? '未登录');

/** 动态菜单（admin-service /me 返回；BUTTON 节点不进导航） */
const dynamicMenus = computed<MenuNode[]>(() =>
  (userStore.menus || []).filter((m) => m.type === 'DIR' || m.type === 'MENU'),
);

const handleLogout = async () => {
  await userStore.logout();
  router.push({ name: 'Login' });
};
</script>

<template>
  <el-container class="layout">
    <el-aside width="220px" class="layout-aside">
      <div class="logo">IoT 平台</div>
      <el-scrollbar class="menu-scroll">
        <el-menu
          :default-active="activeMenu"
          router
          background-color="#001529"
          text-color="#fff"
          active-text-color="#409eff"
        >
          <el-menu-item index="/home">
            <el-icon><HomeFilled /></el-icon>
            <span>首页</span>
          </el-menu-item>
          <el-menu-item index="/devices">
            <el-icon><Monitor /></el-icon>
            <span>设备管理</span>
          </el-menu-item>
          <el-menu-item index="/alarms">
            <el-icon><Warning /></el-icon>
            <span>告警看板</span>
          </el-menu-item>
          <el-menu-item index="/gateway-records">
            <el-icon><Key /></el-icon>
            <span>网关认证记录</span>
          </el-menu-item>
          <el-menu-item index="/data">
            <el-icon><DataLine /></el-icon>
            <span>数据查询</span>
          </el-menu-item>

          <!-- 动态菜单：来自 admin-service 角色授权 -->
          <template v-for="menu in dynamicMenus" :key="menu.id">
            <el-sub-menu v-if="menu.type === 'DIR' && menu.children?.length" :index="menu.id">
              <template #title>
                <el-icon v-if="menu.icon"><component :is="menu.icon" /></el-icon>
                <span>{{ menu.name }}</span>
              </template>
              <template v-for="child in menu.children" :key="child.id">
                <el-menu-item v-if="child.type === 'MENU' && child.path" :index="child.path">
                  <el-icon v-if="child.icon"><component :is="child.icon" /></el-icon>
                  <span>{{ child.name }}</span>
                </el-menu-item>
              </template>
            </el-sub-menu>
            <el-menu-item v-else-if="menu.type === 'MENU' && menu.path" :index="menu.path">
              <el-icon v-if="menu.icon"><component :is="menu.icon" /></el-icon>
              <span>{{ menu.name }}</span>
            </el-menu-item>
          </template>
        </el-menu>
      </el-scrollbar>
    </el-aside>
    <el-container>
      <el-header class="layout-header">
        <div class="header-left">
          <el-icon size="20"><Fold /></el-icon>
        </div>
        <div class="header-right">
          <el-dropdown>
            <span class="user">
              <el-icon><User /></el-icon>
              {{ username }}
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="layout-main">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout {
  height: 100vh;
}
.layout-aside {
  background: #001529;
}
.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #fff;
  font-weight: 700;
}
.menu-scroll {
  height: calc(100vh - 60px);
}
.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #eee;
}
.layout-main {
  background: #f0f2f5;
}
.user {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
}
</style>
