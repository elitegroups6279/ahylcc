import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import router from '../router/index'

const DASHBOARD_PATH = '/dashboard'
const STORAGE_KEY = 'hfnew_open_tabs'

function getRouteTitle(route) {
  if (route.meta?.title) return route.meta.title
  if (route.name && typeof route.name === 'string') return route.name
  return route.path || '未命名'
}

function normalizeDashboardFirst(list) {
  const normalized = list.filter(t => t && typeof t === 'object' && t.path)
  const dashIdx = normalized.findIndex(t => t.path === DASHBOARD_PATH)
  if (dashIdx > 0) {
    const [dash] = normalized.splice(dashIdx, 1)
    normalized.unshift(dash)
  } else if (dashIdx === -1) {
    normalized.unshift({ path: DASHBOARD_PATH, title: '首页', name: 'Dashboard' })
  }
  return normalized
}

export const useTabsStore = defineStore('tabs', () => {
  const tabs = ref([
    { path: DASHBOARD_PATH, title: '首页', name: 'Dashboard' }
  ])

  const activeTab = computed(() => router.currentRoute.value.path)

  // Component names for keep-alive :include
  const include = computed(() => tabs.value.map(t => t.name).filter(Boolean))

  function persist() {
    try {
      sessionStorage.setItem(STORAGE_KEY, JSON.stringify(tabs.value))
    } catch (e) {
      console.warn('Failed to persist tabs', e)
    }
  }

  function addTab(route) {
    if (!route || !route.path) return
    // Skip auth and layout routes
    if (route.path === '/login' || route.path === '/forbidden' || route.path === '/') return

    const exists = tabs.value.find(t => t.path === route.path)
    if (!exists) {
      tabs.value.push({
        path: route.path,
        title: getRouteTitle(route),
        name: route.name
      })
      persist()
    }
  }

  function removeTab(path) {
    if (path === DASHBOARD_PATH) return
    const idx = tabs.value.findIndex(t => t.path === path)
    if (idx === -1) return

    const isActive = router.currentRoute.value.path === path
    tabs.value.splice(idx, 1)
    persist()

    if (isActive) {
      const target = tabs.value[idx - 1] || tabs.value[0] || { path: DASHBOARD_PATH }
      router.push(target.path)
    }
  }

  function removeOthers(path) {
    const target = tabs.value.find(t => t.path === path)
    tabs.value = target ? [tabs.value[0], target] : [tabs.value[0]]
    tabs.value = normalizeDashboardFirst(tabs.value)
    persist()

    if (router.currentRoute.value.path !== path) {
      router.push(path)
    }
  }

  function removeAll() {
    tabs.value = [{ path: DASHBOARD_PATH, title: '首页', name: 'Dashboard' }]
    persist()
    router.push(DASHBOARD_PATH)
  }

  function initFromStorage() {
    const stored = sessionStorage.getItem(STORAGE_KEY)
    if (stored) {
      try {
        const parsed = JSON.parse(stored)
        if (Array.isArray(parsed) && parsed.length > 0) {
          tabs.value = normalizeDashboardFirst(parsed)
        }
      } catch (e) {
        console.warn('Failed to restore tabs', e)
      }
    }
    // Ensure the current route is represented
    addTab(router.currentRoute.value)
  }

  // Auto-add tab on every route change
  router.afterEach((to) => {
    addTab(to)
  })

  return {
    tabs,
    activeTab,
    include,
    addTab,
    removeTab,
    removeOthers,
    removeAll,
    initFromStorage
  }
})
