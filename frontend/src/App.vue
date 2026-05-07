<template>
  <router-view />
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from './store/auth'

const authStore = useAuthStore()
const router = useRouter()

onMounted(async () => {
  // 如果本地有 token，在应用初始化时验证其有效性
  if (authStore.accessToken) {
    const valid = await authStore.validateAndRefreshToken()
    if (!valid) {
      router.push('/login')
    }
  }
})
</script>

<style>
:root {
  --primary-color: #1890ff;
  --primary-dark: #096dd9;
  --primary-light: #40a9ff;
  --sidebar-bg: #001529;
  --sidebar-bg-light: #002140;
  --sidebar-text: rgba(255, 255, 255, 0.65);
  --sidebar-text-active: #fff;
  --sidebar-highlight: #1890ff;
  --navbar-bg: #fff;
  --page-bg: #f0f2f5;
  --accent-color: #fa8c16;
  --success-color: #52c41a;
  --warning-color: #faad14;
  --danger-color: #ff4d4f;
}

/* Element Plus主题覆盖 */
.el-button--primary {
  --el-button-bg-color: var(--primary-color);
  --el-button-border-color: var(--primary-color);
  --el-button-hover-bg-color: var(--primary-light);
  --el-button-hover-border-color: var(--primary-light);
  --el-button-active-bg-color: var(--primary-dark);
  --el-button-active-border-color: var(--primary-dark);
}

/* 全局滚动条美化 */
::-webkit-scrollbar { width: 6px; height: 6px; }
::-webkit-scrollbar-thumb { background: #d9d9d9; border-radius: 3px; }
::-webkit-scrollbar-thumb:hover { background: #bfbfbf; }
::-webkit-scrollbar-track { background: transparent; }
</style>

