import axios from 'axios'
import { useAuthStore } from '../store/auth'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? ''

export const apiRaw = axios.create({
  baseURL: apiBaseUrl
})

export const api = axios.create({
  baseURL: apiBaseUrl
})

function getAuthStore() {
  // 避免在模块加载阶段就触发 pinia（main.js 已创建 pinia 后才会用到）
  return useAuthStore()
}

api.interceptors.request.use((config) => {
  const authStore = getAuthStore()
  if (authStore.accessToken) {
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${authStore.accessToken}`
  }
  return config
})

let isRefreshing = false
let refreshPromise = null

api.interceptors.response.use(
  (resp) => resp,
  async (err) => {
    const authStore = getAuthStore()
    const originalConfig = err?.config
    const status = err?.response?.status

    if (status === 401 && originalConfig && !originalConfig._retry && !originalConfig.skipRefresh) {
      originalConfig._retry = true

      // 防止并发刷新
      if (!isRefreshing) {
        isRefreshing = true
        refreshPromise = authStore
          .refreshAccessToken()
          .catch(() => null)
          .finally(() => {
            isRefreshing = false
          })
      }

      const refreshOk = await refreshPromise
      if (refreshOk) {
        // 刷新成功：更新header后重试原请求
        originalConfig.headers = originalConfig.headers || {}
        originalConfig.headers.Authorization = `Bearer ${authStore.accessToken}`
        return api(originalConfig)
      }

      // refresh 失败：清空 token，交给路由守卫跳 login
      return Promise.reject(err)
    }

    return Promise.reject(err)
  }
)

