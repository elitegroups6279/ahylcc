<template>
  <div class="tab-bar" ref="tabBarRef">
    <div
      v-for="tab in tabsStore.tabs"
      :key="tab.path"
      class="tab-item"
      :class="{ active: isActive(tab.path) }"
      @click="onTabClick(tab)"
      @contextmenu.prevent="onContextMenu($event, tab)"
    >
      <span class="tab-title">{{ tab.title }}</span>
      <span
        v-if="tab.path !== DASHBOARD_PATH"
        class="tab-close"
        @click.stop="onClose(tab)"
      >
        <el-icon><Close /></el-icon>
      </span>
    </div>

    <div
      v-if="contextMenu.visible"
      class="tab-context-menu"
      :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }"
      @click.stop
    >
      <div class="menu-item" @click="handleMenu('close')">关闭</div>
      <div class="menu-item" @click="handleMenu('closeOthers')">关闭其他</div>
      <div class="menu-item" @click="handleMenu('closeAll')">关闭全部</div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Close } from '@element-plus/icons-vue'
import { useTabsStore } from '../../store/tabs'

const DASHBOARD_PATH = '/dashboard'

const route = useRoute()
const router = useRouter()
const tabsStore = useTabsStore()
const tabBarRef = ref(null)

const contextMenu = ref({
  visible: false,
  x: 0,
  y: 0,
  tab: null
})

const isActive = (path) => route.path === path

const onTabClick = (tab) => {
  if (route.path !== tab.path) {
    router.push(tab.path)
  }
}

const onClose = (tab) => {
  tabsStore.removeTab(tab.path)
}

const onContextMenu = (event, tab) => {
  contextMenu.value = {
    visible: true,
    x: event.clientX,
    y: event.clientY,
    tab
  }
}

const handleMenu = (type) => {
  const tab = contextMenu.value.tab
  contextMenu.value.visible = false
  if (!tab) return

  if (type === 'close') {
    tabsStore.removeTab(tab.path)
  } else if (type === 'closeOthers') {
    tabsStore.removeOthers(tab.path)
  } else if (type === 'closeAll') {
    tabsStore.removeAll()
  }
}

const closeMenu = () => {
  contextMenu.value.visible = false
}

// Scroll active tab into view smoothly when route changes
watch(
  () => route.path,
  () => {
    nextTick(() => {
      const activeEl = tabBarRef.value?.querySelector('.tab-item.active')
      if (activeEl) {
        activeEl.scrollIntoView({ behavior: 'smooth', inline: 'center', block: 'nearest' })
      }
    })
  },
  { immediate: true }
)

onMounted(() => {
  document.addEventListener('click', closeMenu)
})

onUnmounted(() => {
  document.removeEventListener('click', closeMenu)
})
</script>

<style scoped>
.tab-bar {
  height: 36px;
  display: flex;
  align-items: center;
  background-color: var(--color-bg-card);
  border-bottom: 1px solid var(--color-border);
  padding: 0 16px;
  overflow-x: auto;
  overflow-y: hidden;
  white-space: nowrap;
  scroll-behavior: smooth;
  scrollbar-width: thin;
  position: relative;
}

.tab-bar::-webkit-scrollbar {
  height: 4px;
}

.tab-bar::-webkit-scrollbar-thumb {
  background: #d9d9d9;
  border-radius: 2px;
}

.tab-bar::-webkit-scrollbar-thumb:hover {
  background: #bfbfbf;
}

.tab-bar::-webkit-scrollbar-track {
  background: transparent;
}

.tab-item {
  display: inline-flex;
  align-items: center;
  flex-shrink: 0;
  height: 28px;
  padding: 0 12px;
  margin-right: 8px;
  border-radius: var(--radius-sm);
  font-size: 13px;
  color: var(--color-text-secondary);
  background-color: var(--color-bg-page);
  border: 1px solid var(--color-border);
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;
}

.tab-item:last-child {
  margin-right: 0;
}

.tab-item:hover {
  color: var(--color-primary);
  border-color: var(--color-primary-light);
}

.tab-item.active {
  color: var(--color-primary);
  background-color: var(--color-primary-bg);
  border-color: var(--color-primary);
  font-weight: 500;
}

.tab-title {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tab-close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 14px;
  height: 14px;
  margin-left: 8px;
  border-radius: 50%;
  font-size: 12px;
  color: var(--color-text-secondary);
  transition: all 0.2s ease;
}

.tab-close:hover {
  background-color: var(--color-danger);
  color: #fff;
}

.tab-context-menu {
  position: fixed;
  z-index: 2000;
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  box-shadow: var(--shadow-md);
  padding: 4px 0;
  min-width: 100px;
}

.menu-item {
  padding: 8px 16px;
  font-size: 13px;
  color: var(--color-text-primary);
  cursor: pointer;
  transition: background-color 0.2s, color 0.2s;
}

.menu-item:hover {
  background-color: var(--color-primary-bg);
  color: var(--color-primary);
}
</style>
