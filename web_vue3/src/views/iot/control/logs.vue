<script setup lang="ts">
import { onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const route = useRoute();
const router = useRouter();

onMounted(() => {
  router.replace({
    path: '/iot/control',
    query: { tab: 'logs', ...(route.query.areaId ? { areaId: String(route.query.areaId) } : {}) },
  });
});
</script>

<template>
  <div />
</template>
