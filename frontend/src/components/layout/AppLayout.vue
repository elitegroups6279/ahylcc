<template>
  <el-container class="app-layout">
    <el-aside :width="isCollapse ? '64px' : '220px'" class="app-aside">
      <Sidebar :collapse="isCollapse" />
    </el-aside>
    <el-container class="main-container">
      <el-header class="app-header">
        <Navbar :is-collapse="isCollapse" @toggle-sidebar="isCollapse = !isCollapse" />
      </el-header>
      <TabBar />
      <el-main class="app-main">
        <Breadcrumb />
        <div class="page-content">
          <router-view v-slot="{ Component }">
            <transition name="fade-slide" mode="out-in">
              <keep-alive :max="8" :include="tabsStore.include">
                <component :is="Component" :key="$route.fullPath" />
              </keep-alive>
            </transition>
          </router-view>
        </div>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref } from 'vue'
import Sidebar from './Sidebar.vue'
import Navbar from './Navbar.vue'
import Breadcrumb from './Breadcrumb.vue'
import TabBar from '../common/TabBar.vue'
import { useTabsStore } from '../../store/tabs'

const isCollapse = ref(false)
const tabsStore = useTabsStore()

tabsStore.initFromStorage()
</script>

<style scoped>
.app-layout {
  height: 100vh;
  width: 100%;
}

.app-aside {
  background-color: var(--sidebar-bg);
  transition: width 0.28s;
  overflow: hidden;
}

.main-container {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.app-header {
  height: 50px;
  padding: 0;
  background-color: var(--color-bg-card);
  box-shadow: var(--shadow-sm);
  z-index: 10;
}

.app-main {
  flex: 1;
  background-color: var(--color-bg-page);
  padding: 16px;
  overflow-y: auto;
}

.page-content {
  background-color: var(--color-bg-card);
  border-radius: var(--radius-sm);
  min-height: calc(100vh - 150px);
  position: relative;
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
