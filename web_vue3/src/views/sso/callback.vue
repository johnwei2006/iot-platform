<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { ElMessage } from 'element-plus';
import { Loading, CircleCheckFilled, CircleCloseFilled } from '@element-plus/icons-vue';
import { useUserStore } from '@/stores/modules/user';

const { t } = useI18n();
const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const status = ref<'loading' | 'success' | 'error'>('loading');
const message = ref(t('sso.processing'));

onMounted(async () => {
  const ticket = route.query.ticket as string | undefined;
  if (!ticket) {
    status.value = 'error';
    message.value = t('sso.missingTicket');
    ElMessage.error(message.value);
    router.replace({ name: 'Login' });
    return;
  }
  try {
    const redirectUri = `${window.location.origin}${import.meta.env.BASE_URL}sso/callback`;
    await userStore.exchangeSsoTicket(ticket, redirectUri);
    status.value = 'success';
    message.value = t('sso.success');
    ElMessage.success(t('sso.successMsg'));
    const redirect = (route.query.redirect as string) || '/home';
    router.replace(redirect);
  } catch {
    status.value = 'error';
    message.value = t('sso.failed');
    ElMessage.error(message.value);
    router.replace({ name: 'Login' });
  }
});
</script>

<template>
  <div class="sso-callback">
    <el-card class="callback-card" shadow="always">
      <div class="callback-content">
        <el-icon v-if="status === 'loading'" class="is-loading callback-icon" :size="36">
          <Loading />
        </el-icon>
        <el-icon v-else-if="status === 'success'" class="callback-icon success" :size="36">
          <CircleCheckFilled />
        </el-icon>
        <el-icon v-else class="callback-icon error" :size="36">
          <CircleCloseFilled />
        </el-icon>
        <p>{{ message }}</p>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.sso-callback {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #eef2f7 0%, #dce8fb 100%);
}
.callback-card {
  width: 420px;
  border-radius: 14px;
  border: none;
}
.callback-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 32px 0;
  color: #606266;
}
.callback-icon {
  color: #409eff;
}
.callback-icon.success {
  color: #67c23a;
}
.callback-icon.error {
  color: #f56c6c;
}
</style>
