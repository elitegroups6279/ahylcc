import { defineStore } from 'pinia'
import { ElMessage } from 'element-plus'
import { apiRaw, api } from '../api/client'

const ACCESS_KEY = 'hf_access_token'
const REFRESH_KEY = 'hf_refresh_token'

// 兜底菜单数据（后端 API 未就绪时使用）
const fallbackMenus = [
  {
    id: 1,
    menuName: '首页',
    path: '/dashboard',
    icon: 'HomeFilled',
    menuType: 'M',
    orderNum: 0,
    children: []
  },
  {
    id: 2,
    menuName: '入住管理',
    path: '/elderly',
    icon: 'User',
    menuType: 'C',
    orderNum: 1,
    children: [
      { id: 21, menuName: '在住老人列表', path: '/elderly/list', icon: 'List', menuType: 'M', orderNum: 1 },
      { id: 22, menuName: '新增入住', path: '/elderly/add', icon: 'Plus', menuType: 'M', orderNum: 2 },
      { id: 23, menuName: '退住管理', path: '/elderly/discharge', icon: 'Remove', menuType: 'M', orderNum: 3 },
      { id: 24, menuName: '转床管理', path: '/elderly/transfer', icon: 'Switch', menuType: 'M', orderNum: 4 }
    ]
  },
  {
    id: 3,
    menuName: '人事管理',
    path: '/staff',
    icon: 'Avatar',
    menuType: 'C',
    orderNum: 2,
    children: [
      { id: 31, menuName: '护工管理', path: '/staff/list', icon: 'UserFilled', menuType: 'M', orderNum: 1 },
      { id: 32, menuName: '打卡记录', path: '/staff/attendance', icon: 'Clock', menuType: 'M', orderNum: 2 },
      { id: 33, menuName: '排班管理', path: '/staff/schedule', icon: 'Calendar', menuType: 'M', orderNum: 3 }
    ]
  },
  {
    id: 4,
    menuName: '财务管理',
    path: '/finance',
    icon: 'Money',
    menuType: 'C',
    orderNum: 3,
    children: [
      { id: 41, menuName: '缴费管理', path: '/finance/payment', icon: 'Wallet', menuType: 'M', orderNum: 1 },
      { id: 42, menuName: '凭证管理', path: '/finance/voucher', icon: 'Document', menuType: 'M', orderNum: 2 },
      { id: 43, menuName: '报账管理', path: '/finance/reimbursement', icon: 'Tickets', menuType: 'M', orderNum: 3 },
      { id: 44, menuName: '月度账单', path: '/finance/bills', icon: 'Notebook', menuType: 'M', orderNum: 4 },
      { id: 45, menuName: '补贴对账', path: '/finance/subsidy-summary', icon: 'DataAnalysis', menuType: 'M', orderNum: 5 }
    ]
  },
  {
    id: 5,
    menuName: '仓库管理',
    path: '/warehouse',
    icon: 'House',
    menuType: 'C',
    orderNum: 4,
    children: [
      { id: 50, menuName: '物资档案', path: '/warehouse/materials', icon: 'CollectionTag', menuType: 'M', orderNum: 0 },
      { id: 51, menuName: '入库管理', path: '/warehouse/in', icon: 'Download', menuType: 'M', orderNum: 1 },
      { id: 52, menuName: '出库管理', path: '/warehouse/out', icon: 'Upload', menuType: 'M', orderNum: 2 },
      { id: 53, menuName: '库存看板', path: '/warehouse/stock', icon: 'DataBoard', menuType: 'M', orderNum: 3 },
      { id: 54, menuName: '盘点管理', path: '/warehouse/check', icon: 'Checked', menuType: 'M', orderNum: 4 }
    ]
  },
  {
    id: 6,
    menuName: '药物管理',
    path: '/pharmacy',
    icon: 'FirstAidKit',
    menuType: 'C',
    orderNum: 5,
    children: [
      { id: 61, menuName: '药品档案', path: '/pharmacy/drugs', icon: 'Collection', menuType: 'M', orderNum: 1 },
      { id: 62, menuName: '发药管理', path: '/pharmacy/dispense', icon: 'Sell', menuType: 'M', orderNum: 2 }
    ]
  },
  {
    id: 7,
    menuName: '上门服务',
    path: '/home-service',
    icon: 'Van',
    menuType: 'C',
    orderNum: 6,
    children: [
      { id: 71, menuName: '预约管理', path: '/home-service/orders', icon: 'AlarmClock', menuType: 'M', orderNum: 1 },
      { id: 72, menuName: '服务记录', path: '/home-service/records', icon: 'Memo', menuType: 'M', orderNum: 2 },
      { id: 73, menuName: '服务评估', path: '/home-service/assessment', icon: 'Document', menuType: 'M', orderNum: 3 }
    ]
  },
  {
    id: 8,
    menuName: '系统管理',
    path: '/system',
    icon: 'Setting',
    menuType: 'C',
    orderNum: 7,
    children: [
      { id: 81, menuName: '账户管理', path: '/system/accounts', icon: 'User', menuType: 'M', orderNum: 1 },
      { id: 82, menuName: '角色管理', path: '/system/roles', icon: 'Stamp', menuType: 'M', orderNum: 2 },
      { id: 83, menuName: '菜单管理', path: '/system/menus', icon: 'Menu', menuType: 'M', orderNum: 3 },
      { id: 84, menuName: '系统配置', path: '/system/config', icon: 'Tools', menuType: 'M', orderNum: 4 },
      { id: 85, menuName: '操作日志', path: '/system/logs', icon: 'Document', menuType: 'M', orderNum: 5 }
    ]
  },
  {
    id: 9,
    menuName: '报表管理',
    path: '/reports',
    icon: 'DataAnalysis',
    menuType: 'C',
    orderNum: 8,
    children: [
      { id: 91, menuName: '报表导出', path: '/reports/export', icon: 'Download', menuType: 'M', orderNum: 1 }
    ]
  }
]

export const useAuthStore = defineStore('auth', {
  state: () => ({
    accessToken: localStorage.getItem(ACCESS_KEY) || '',
    refreshToken: localStorage.getItem(REFRESH_KEY) || '',
    user: null,
    permissions: [],
    menuTree: [],
    userInfo: {}
  }),

  getters: {
    username: (state) => state.user?.username || state.userInfo?.username || '',
    // 侧边栏菜单：仅返回菜单(M)和目录(C)类型，不含按钮(B)
    sidebarMenus: (state) => {
      const filterMenus = (menus) => {
        return menus
          .filter(m => m.menuType !== 'B' && m.menuType !== 2)
          .map(m => ({
            ...m,
            children: m.children ? filterMenus(m.children) : []
          }))
          .sort((a, b) => {
            const ao = a.sortOrder ?? a.orderNum ?? 0
            const bo = b.sortOrder ?? b.orderNum ?? 0
            return ao - bo
          })
      }
      const menus = state.menuTree.length > 0 ? state.menuTree : fallbackMenus
      return filterMenus(menus)
    }
  },

  actions: {
    persistTokens(accessToken, refreshToken) {
      this.accessToken = accessToken || ''
      this.refreshToken = refreshToken || ''
      if (accessToken) localStorage.setItem(ACCESS_KEY, accessToken)
      else localStorage.removeItem(ACCESS_KEY)
      if (refreshToken) localStorage.setItem(REFRESH_KEY, refreshToken)
      else localStorage.removeItem(REFRESH_KEY)
    },

    clear() {
      this.persistTokens('', '')
      this.user = null
      this.permissions = []
      this.menuTree = []
      this.userInfo = {}
    },

    async login({ username, password }) {
      const resp = await apiRaw.post('/api/auth/login', { username, password })
      const body = resp.data
      if (body.code !== 200) {
        throw new Error(body.msg || 'login failed')
      }

      const data = body.data
      this.persistTokens(data.accessToken, data.refreshToken)
      this.user = data.user || null
      // 登录后立即拉取权限和菜单
      await this.fetchPermissions()
      await this.fetchMenuTree()
      return this.user
    },

    async logout() {
      try {
        await api.post('/api/auth/logout', { refreshToken: this.refreshToken })
      } catch (e) {
        // 忽略错误，继续清理本地状态
      }
      this.clear()
    },

    async refreshAccessToken() {
      if (!this.refreshToken) return false
      try {
        const resp = await apiRaw.post('/api/auth/refresh', { refreshToken: this.refreshToken }, { skipRefresh: true })
        const body = resp.data
        if (body.code !== 200) return false
        const newAccessToken = body.data.accessToken
        if (!newAccessToken) return false
        this.persistTokens(newAccessToken, this.refreshToken)
        return true
      } catch (e) {
        this.clear()
        return false
      }
    },

    async fetchPermissions() {
      try {
        const resp = await api.get('/api/rbac/permissions')
        const body = resp.data
        if (body.code !== 200) {
          throw new Error(body.msg || 'fetch permissions failed')
        }
        this.permissions = body.data?.permissions || []
        return this.permissions
      } catch (e) {
        console.warn('获取权限失败', e)
        ElMessage.warning('权限信息获取失败，部分功能可能受限')
        this.permissions = []
        return []
      }
    },

    async fetchMenuTree() {
      try {
        const resp = await api.get('/api/rbac/menus')
        const body = resp.data
        if (body.code === 200 && body.data) {
          this.menuTree = body.data || []
        } else {
          // API 返回异常，使用兜底菜单
          this.menuTree = []
        }
      } catch (e) {
        // 后端 API 未就绪，使用兜底菜单
        console.warn('获取菜单失败', e)
        ElMessage.warning('菜单信息获取失败，使用默认菜单')
        this.menuTree = []
      }
      return this.menuTree
    },

    async fetchUserInfo() {
      try {
        const resp = await api.get('/api/auth/me')
        const body = resp.data
        if (body.code === 200 && body.data) {
          this.userInfo = body.data
        }
      } catch (e) {
        console.warn('获取用户信息失败', e)
      }
      return this.userInfo
    }
  }
})
