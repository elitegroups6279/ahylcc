import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../store/auth'
import NProgress from 'nprogress'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../pages/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/forbidden',
    name: 'Forbidden',
    component: () => import('../pages/Forbidden.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('../components/layout/AppLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../pages/Dashboard.vue'),
        meta: { title: '首页', icon: 'HomeFilled', requiresAuth: true }
      },
      // 入住管理
      {
        path: 'elderly/list',
        name: 'ElderlyList',
        component: () => import('../pages/elderly/ElderlyList.vue'),
        meta: { title: '在住老人列表', permission: 'elderly:list', parent: '入住管理', requiresAuth: true }
      },
      {
        path: 'elderly/add',
        name: 'ElderlyAdd',
        component: () => import('../pages/elderly/ElderlyAdd.vue'),
        meta: { title: '新增入住', permission: 'elderly:add', parent: '入住管理', requiresAuth: true }
      },
      {
        path: 'elderly/detail/:id',
        name: 'ElderlyDetail',
        component: () => import('../pages/elderly/ElderlyDetail.vue'),
        meta: { title: '老人档案', permission: 'elderly:list', parent: '入住管理', requiresAuth: true }
      },
      {
        path: 'elderly/discharge',
        name: 'ElderlyDischarge',
        component: () => import('../pages/elderly/ElderlyDischarge.vue'),
        meta: { title: '退住管理', permission: 'elderly:discharge', parent: '入住管理', requiresAuth: true }
      },
      {
        path: 'elderly/transfer',
        name: 'ElderlyTransfer',
        component: () => import('../pages/elderly/ElderlyTransfer.vue'),
        meta: { title: '转床管理', permission: 'elderly:transfer', parent: '入住管理', requiresAuth: true }
      },
      // 人事管理
      {
        path: 'staff/list',
        name: 'StaffList',
        component: () => import('../pages/staff/StaffList.vue'),
        meta: { title: '护工管理', permission: 'staff:list', parent: '人事管理', requiresAuth: true }
      },
      {
        path: 'staff/attendance',
        name: 'Attendance',
        component: () => import('../pages/staff/Attendance.vue'),
        meta: { title: '打卡记录', permission: 'staff:attendance', parent: '人事管理', requiresAuth: true }
      },
      {
        path: 'staff/schedule',
        name: 'Schedule',
        component: () => import('../pages/staff/Schedule.vue'),
        meta: { title: '排班管理', permission: 'staff:schedule', parent: '人事管理', requiresAuth: true }
      },
      // 财务管理
      {
        path: 'finance/payment',
        name: 'Payment',
        component: () => import('../pages/finance/Payment.vue'),
        meta: { title: '缴费管理', permission: 'finance:payment', parent: '财务管理', requiresAuth: true }
      },
      {
        path: 'finance/voucher',
        name: 'Voucher',
        component: () => import('../pages/finance/Voucher.vue'),
        meta: { title: '凭证管理', permission: 'finance:voucher', parent: '财务管理', requiresAuth: true }
      },
      {
        path: 'finance/reimbursement',
        name: 'Reimbursement',
        component: () => import('../pages/finance/Reimbursement.vue'),
        meta: { title: '报账管理', permission: 'finance:reimbursement', parent: '财务管理', requiresAuth: true }
      },
      {
        path: 'finance/subsidy-summary',
        name: 'SubsidySummary',
        component: () => import('../pages/finance/SubsidySummary.vue'),
        meta: { title: '补贴对账', permission: 'finance:subsidy', parent: '财务管理', requiresAuth: true }
      },
      {
        path: 'finance/bills',
        name: 'FeeBills',
        component: () => import('../pages/finance/FeeBill.vue'),
        meta: { title: '月度账单', permission: 'finance:bill', parent: '财务管理', requiresAuth: true }
      },
      // 报表导出
      {
        path: 'reports/export',
        name: 'ReportsExport',
        component: () => import('../pages/reports/Reports.vue'),
        meta: { title: '报表导出', permission: 'report:export', parent: '报表管理', requiresAuth: true }
      },
      // 仓库管理
      {
        path: 'warehouse/materials',
        name: 'WarehouseMaterials',
        component: () => import('../pages/warehouse/Materials.vue'),
        meta: { title: '物资档案', permission: 'warehouse:materials', parent: '仓库管理', requiresAuth: true }
      },
      {
        path: 'warehouse/in',
        name: 'InventoryIn',
        component: () => import('../pages/warehouse/InventoryIn.vue'),
        meta: { title: '入库管理', permission: 'warehouse:in', parent: '仓库管理', requiresAuth: true }
      },
      {
        path: 'warehouse/out',
        name: 'InventoryOut',
        component: () => import('../pages/warehouse/InventoryOut.vue'),
        meta: { title: '出库管理', permission: 'warehouse:out', parent: '仓库管理', requiresAuth: true }
      },
      {
        path: 'warehouse/stock',
        name: 'Stock',
        component: () => import('../pages/warehouse/Stock.vue'),
        meta: { title: '库存看板', permission: 'warehouse:stock', parent: '仓库管理', requiresAuth: true }
      },
      {
        path: 'warehouse/check',
        name: 'InventoryCheck',
        component: () => import('../pages/warehouse/InventoryCheck.vue'),
        meta: { title: '盘点管理', permission: 'warehouse:check', parent: '仓库管理', requiresAuth: true }
      },
      // 药物管理
      {
        path: 'pharmacy/drugs',
        name: 'DrugList',
        component: () => import('../pages/pharmacy/DrugList.vue'),
        meta: { title: '药品档案', permission: 'pharmacy:drugs', parent: '药物管理', requiresAuth: true }
      },
      {
        path: 'pharmacy/dispense',
        name: 'Dispense',
        component: () => import('../pages/pharmacy/Dispense.vue'),
        meta: { title: '发药管理', permission: 'pharmacy:dispense', parent: '药物管理', requiresAuth: true }
      },
      // 上门服务
      {
        path: 'home-service/orders',
        name: 'HomeServiceOrders',
        component: () => import('../pages/home-service/Orders.vue'),
        meta: { title: '预约管理', permission: 'home-service:orders', parent: '上门服务', requiresAuth: true }
      },
      {
        path: 'home-service/records',
        name: 'HomeServiceRecords',
        component: () => import('../pages/home-service/Records.vue'),
        meta: { title: '服务记录', permission: 'home-service:records', parent: '上门服务', requiresAuth: true }
      },
      // 系统管理
      {
        path: 'system/accounts',
        name: 'Accounts',
        component: () => import('../pages/system/Accounts.vue'),
        meta: { title: '账户管理', permission: 'system:accounts', parent: '系统管理', requiresAuth: true }
      },
      {
        path: 'system/roles',
        name: 'Roles',
        component: () => import('../pages/system/Roles.vue'),
        meta: { title: '角色管理', permission: 'system:roles', parent: '系统管理', requiresAuth: true }
      },
      {
        path: 'system/menus',
        name: 'Menus',
        component: () => import('../pages/system/Menus.vue'),
        meta: { title: '菜单管理', permission: 'system:menus', parent: '系统管理', requiresAuth: true }
      },
      {
        path: 'system/config',
        name: 'Config',
        component: () => import('../pages/system/Config.vue'),
        meta: { title: '系统配置', permission: 'system:config', parent: '系统管理', requiresAuth: true }
      },
      {
        path: 'system/logs',
        name: 'Logs',
        component: () => import('../pages/system/Logs.vue'),
        meta: { title: '操作日志', permission: 'system:logs', parent: '系统管理', requiresAuth: true }
      },
      // 保留原有的 demo 路由
      {
        path: 'demo/secure',
        name: 'DemoSecure',
        component: () => import('../pages/DemoSecure.vue'),
        meta: { title: '安全演示', permission: 'demo:secure', requiresAuth: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  NProgress.start()
  
  const authStore = useAuthStore()

  // 去登录页：若已登录则直接去 dashboard
  if (to.path === '/login') {
    if (authStore.accessToken) {
      NProgress.done()
      return next('/dashboard')
    }
    return next()
  }

  // 不需要认证的页面直接放行
  if (to.meta?.requiresAuth === false) {
    return next()
  }

  // 受保护路由：检查是否已登录
  if (!authStore.accessToken) {
    NProgress.done()
    return next('/login')
  }

  try {
    // 首次进入：拉取权限和菜单
    if (!authStore.permissions || authStore.permissions.length === 0) {
      await authStore.fetchPermissions()
    }
    if (!authStore.menuTree || authStore.menuTree.length === 0) {
      await authStore.fetchMenuTree()
    }
  } catch (e) {
    authStore.clear()
    NProgress.done()
    return next('/login')
  }

  // 权限拦截（基于 permissions 字符串集合）
  // 超级管理员跳过权限检查
  const isSuperAdmin = authStore.permissions.includes('*')
  
  if (!isSuperAdmin && to.meta?.permission && !authStore.permissions.includes(to.meta.permission)) {
    NProgress.done()
    return next('/forbidden')
  }

  return next()
})

router.afterEach(() => {
  NProgress.done()
})

export default router
