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
        background-color="#001529"
        text-color="rgba(255,255,255,0.65)"
        active-text-color="#fff"
        class="sidebar-menu"
      >
        <MenuItem v-for="menu in menus" :key="menu.id" :item="menu" />
      </el-menu>
    </el-scrollbar>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '../../store/auth'
import { HomeFilled } from '@element-plus/icons-vue'
import MenuItem from './MenuItem.vue'

defineProps({
  collapse: {
    type: Boolean,
    default: false
  }
})

const route = useRoute()
const authStore = useAuthStore()

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
  background-color: #002140;
  overflow: hidden;
}

.logo-icon {
  color: #1890ff;
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
}

.sidebar-menu:not(.el-menu--collapse) {
  width: 220px;
}

:deep(.el-menu-item),
:deep(.el-sub-menu__title) {
  height: 50px;
  line-height: 50px;
}

:deep(.el-menu-item:hover),
:deep(.el-sub-menu__title:hover) {
  background-color: #002140 !important;
}

:deep(.el-menu-item.is-active) {
  background-color: #1890ff !important;
  color: #fff !important;
  border-right: 3px solid #fff;
}

:deep(.el-sub-menu .el-menu) {
  background-color: #000c17 !important;
}

:deep(.el-sub-menu .el-menu-item) {
  background-color: #000c17 !important;
}

:deep(.el-sub-menu .el-menu-item:hover) {
  background-color: #002140 !important;
}
</style>
