<template>
  <div class="sidebar">
    <!-- Logo 区域 -->
    <div class="logo-container">
      <el-icon :size="collapse ? 28 : 24" class="logo-icon">
        <HomeFilled />
      </el-icon>
      <span v-if="!collapse" class="logo-title">智慧养老</span>
    </div>
    
    <!-- 菜单 -->
    <el-scrollbar class="menu-scrollbar">
      <el-menu
        :default-active="activeMenu"
        :collapse="collapse"
        :collapse-transition="false"
        router
        class="sidebar-menu"
      >
        <MenuItem v-for="menu in menus" :key="menu.id" :item="menu" />
      </el-menu>
    </el-scrollbar>

    <!-- 用户信息 -->
    <div class="sidebar-user-info" :class="{ collapsed: collapse }">
      <div class="user-avatar">
        {{ username?.charAt(0)?.toUpperCase() || 'U' }}
      </div>
      <div v-if="!collapse" class="user-details">
        <span class="user-name">{{ username }}</span>
        <span class="user-role">{{ userRole }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '../../store/auth'
import { HomeFilled } from '@element-plus/icons-vue'
import MenuItem from './MenuItem.vue'

const props = defineProps({
  collapse: {
    type: Boolean,
    default: false
  }
})

const route = useRoute()
const authStore = useAuthStore()

const username = computed(() => authStore.username)

const userRole = computed(() => {
  const roles = authStore.user?.roles || authStore.userInfo?.roles || []
  const role = roles[0]
  if (!role) return '用户'
  const map = {
    SUPER_ADMIN: '超级管理员',
    ORG_MANAGER: '机构管理员',
    FINANCE: '财务人员',
    WAREHOUSE: '仓库管理员',
    NURSE: '护理人员'
  }
  return map[role] || role
})

// 当前激活的菜单
const activeMenu = computed(() => {
  return route.path
})

// 侧边栏菜单
const menus = computed(() => {
  return authStore.sidebarMenus
})
</script>

<style scoped>
.sidebar {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.logo-container {
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 12px;
  background-color: var(--sidebar-hover);
  overflow: hidden;
}

.logo-icon {
  color: var(--color-primary);
  flex-shrink: 0;
}

.logo-title {
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  margin-left: 10px;
  white-space: nowrap;
}

.menu-scrollbar {
  flex: 1;
  overflow: hidden;
}

.sidebar-menu {
  border-right: none;
  --el-menu-bg-color: var(--sidebar-bg);
  --el-menu-text-color: var(--sidebar-text);
  --el-menu-active-color: var(--sidebar-text-active);
  --el-menu-hover-bg-color: var(--sidebar-hover);
}

.sidebar-menu:not(.el-menu--collapse) {
  width: 220px;
}

:deep(.el-menu-item),
:deep(.el-sub-menu__title) {
  height: 50px;
  line-height: 50px;
}

:deep(.el-menu-item) {
  transition: background-color 0.15s ease, padding-left 0.15s ease;
}

:deep(.el-menu-item:hover),
:deep(.el-sub-menu__title:hover) {
  background-color: var(--sidebar-hover) !important;
}

:deep(.el-menu-item.is-active) {
  position: relative;
  background-color: var(--sidebar-hover) !important;
  color: var(--sidebar-text-active) !important;
}

:deep(.el-menu-item.is-active::before) {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background-color: var(--sidebar-active);
  border-radius: 0 2px 2px 0;
}

:deep(.el-sub-menu .el-menu) {
  background-color: var(--sidebar-submenu-bg) !important;
}

:deep(.el-sub-menu .el-menu-item) {
  background-color: var(--sidebar-submenu-bg) !important;
}

:deep(.el-sub-menu .el-menu-item:hover) {
  background-color: var(--sidebar-hover) !important;
}

.sidebar-user-info {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  gap: 10px;
  overflow: hidden;
  flex-shrink: 0;
}

.sidebar-user-info.collapsed {
  justify-content: center;
  padding: 12px 0;
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary), #6366f1);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
}

.user-details {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

.user-name {
  color: #fff;
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-role {
  color: rgba(255, 255, 255, 0.5);
  font-size: 11px;
  margin-top: 2px;
}
</style>
