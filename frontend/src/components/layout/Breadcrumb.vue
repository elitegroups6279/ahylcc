<template>
  <el-breadcrumb separator="/" class="app-breadcrumb">
    <el-breadcrumb-item :to="{ path: '/dashboard' }">
      <el-icon><HomeFilled /></el-icon>
    </el-breadcrumb-item>
    <el-breadcrumb-item v-for="(item, index) in breadcrumbs" :key="index">
      <span v-if="index === breadcrumbs.length - 1">{{ item.title }}</span>
      <router-link v-else :to="item.path">{{ item.title }}</router-link>
    </el-breadcrumb-item>
  </el-breadcrumb>
</template>

<script setup>
import { computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { HomeFilled } from '@element-plus/icons-vue'

const route = useRoute()

const breadcrumbs = computed(() => {
  const crumbs = []
  
  // 从 route.meta 获取面包屑信息
  const parent = route.meta?.parent
  const title = route.meta?.title
  
  if (parent) {
    crumbs.push({ title: parent, path: '' })
  }
  
  if (title && route.name !== 'Dashboard') {
    crumbs.push({ title, path: route.path })
  }
  
  return crumbs
})
</script>

<style scoped>
.app-breadcrumb {
  padding: 16px;
  margin-bottom: 0;
  background-color: transparent;
}

:deep(.el-breadcrumb__item) {
  display: flex;
  align-items: center;
}

:deep(.el-breadcrumb__inner) {
  display: flex;
  align-items: center;
  color: #97a8be;
}

:deep(.el-breadcrumb__inner a),
:deep(.el-breadcrumb__inner.is-link) {
  color: #97a8be;
  font-weight: normal;
}

:deep(.el-breadcrumb__inner a:hover),
:deep(.el-breadcrumb__inner.is-link:hover) {
  color: #409eff;
}

:deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  color: #606266;
}
</style>
