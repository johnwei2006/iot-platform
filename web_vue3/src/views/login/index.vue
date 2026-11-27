<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { User, Lock } from '@element-plus/icons-vue';
import { useUserStore } from '@/stores/modules/user';
import type { LoginPayload } from '@/types';

const { t } = useI18n();
const router = useRouter();
const route = useRoute();
const userStore = useUserStore();

// 统一登录页地址；留空则不展示 SSO 入口（后端 web 客户端 redirectUri 需与本页一致）
const ssoLoginUrl = import.meta.env.VITE_APP_SSO_LOGIN_URL || '';
const handleSsoLogin = () => {
  if (!ssoLoginUrl) return;
  const redirectUri = `${window.location.origin}${import.meta.env.BASE_URL}sso/callback`;
  const url = new URL(ssoLoginUrl, window.location.origin);
  url.searchParams.set('clientId', 'web');
  url.searchParams.set('redirectUri', redirectUri);
  // 透传原始深层链接，登录完成后回调页可跳回
  const redirect = (route.query.redirect as string) || '';
  if (redirect) url.searchParams.set('redirect', redirect);
  window.location.assign(url.toString());
};

const formRef = ref<FormInstance>();
const loading = ref(false);
const form = reactive<LoginPayload>({
  username: '',
  password: '',
});

const rules: FormRules<LoginPayload> = {
  username: [{ required: true, message: () => t('login.usernamePlaceholder'), trigger: 'blur' }],
  password: [{ required: true, message: () => t('login.passwordPlaceholder'), trigger: 'blur' }],
};

const handleSubmit = async () => {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (!valid) return;
    loading.value = true;
    try {
      await userStore.login(form);
      ElMessage.success(t('login.loginSuccess'));
      const redirect = (route.query.redirect as string) || '/home';
      router.push(redirect);
    } finally {
      loading.value = false;
    }
  });
};
</script>

<template>
  <div class="login">
    <el-card class="login-card" shadow="always">
      <template #header>
        <div class="login-header">
          <span class="login-title">{{ $t('login.title') }}</span>
          <span class="login-subtitle">{{ $t('login.subtitle') }}</span>
        </div>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" size="large">
        <el-form-item :label="$t('login.username')" prop="username">
          <el-input v-model="form.username" :placeholder="$t('login.usernamePlaceholder')" :prefix-icon="User" />
        </el-form-item>
        <el-form-item :label="$t('login.password')" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            :placeholder="$t('login.passwordPlaceholder')"
            :prefix-icon="Lock"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="login-btn" :loading="loading" @click="handleSubmit">
            {{ $t('login.submit') }}
          </el-button>
        </el-form-item>
        <el-form-item v-if="ssoLoginUrl">
          <el-button class="sso-btn" @click="handleSsoLogin">{{ $t('login.sso') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.login {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #eef2f7 0%, #dce8fb 100%);
}
.login-card {
  width: 420px;
  border-radius: 14px;
  border: none;
  box-shadow: 0 12px 40px rgba(31, 45, 61, 0.12);
}
.login-header {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.login-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}
.login-subtitle {
  font-size: 13px;
  font-weight: 400;
  color: #909399;
}
.login-btn {
  width: 100%;
  letter-spacing: 4px;
  border-radius: 8px;
}
.sso-btn {
  width: 100%;
  border-radius: 8px;
}
</style>
