<script setup lang="ts">
import { useRouter } from 'vue-router';

const router = useRouter();
</script>

<template>
  <div class="not-found">
    <h1>404</h1>
    <p>{{ $t('error.notFound') }}</p>
    <el-button type="primary" @click="router.push('/')">{{ $t('error.backHome') }}</el-button>
  </div>
</template>

<style scoped>
.not-found {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
  gap: 12px;
}
.not-found h1 {
  font-size: 72px;
  margin: 0;
  color: #409eff;
}
</style>
