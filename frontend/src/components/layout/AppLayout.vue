<template>
  <el-container class="app-layout">
    <el-aside :width="isCollapse ? '64px' : '220px'" class="app-aside">
      <Sidebar :collapse="isCollapse" />
    </el-aside>
    <el-container class="main-container">
      <el-header class="app-header">
        <Navbar :is-collapse="isCollapse" @toggle-sidebar="isCollapse = !isCollapse" />
      </el-header>
      <el-main class="app-main">
        <Breadcrumb />
        <div class="page-content">
          <router-view />
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

const isCollapse = ref(false)
</script>

<style scoped>
.app-layout {
  height: 100vh;
  width: 100%;
}

.app-aside {
  background-color: #001529;
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
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  z-index: 10;
}

.app-main {
  flex: 1;
  background-color: #f0f2f5;
  padding: 16px;
  overflow-y: auto;
}

.page-content {
  background-color: #fff;
  border-radius: 4px;
  min-height: calc(100vh - 150px);
}
</style>
