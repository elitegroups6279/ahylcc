<template>
  <!-- 有子菜单的目录 -->
  <el-sub-menu v-if="item.children && item.children.length > 0" :index="item.path">
    <template #title>
      <el-icon>
        <component :is="item.icon || 'Menu'" />
      </el-icon>
      <span>{{ item.menuName }}</span>
    </template>
    <MenuItem v-for="child in item.children" :key="child.id" :item="child" />
  </el-sub-menu>
  
  <!-- 叶子菜单 -->
  <el-menu-item v-else :index="item.path">
    <el-icon>
      <component :is="item.icon || 'Document'" />
    </el-icon>
    <template #title>{{ item.menuName }}</template>
  </el-menu-item>
</template>

<script setup>
defineProps({
  item: {
    type: Object,
    required: true
  }
})
</script>
