<template>
  <div class="navbar">
    <!-- 左侧：折叠按钮 -->
    <div class="navbar-left">
      <el-icon class="toggle-btn" @click="$emit('toggle-sidebar')">
        <Fold v-if="!isCollapse" />
        <Expand v-else />
      </el-icon>
    </div>
    
    <!-- 右侧：通知铃铛 + 用户下拉 -->
    <div class="navbar-right">
      <el-dropdown trigger="click" @visible-change="handleNotifyVisible">
        <el-badge :value="notifyCount" :hidden="notifyCount <= 0" class="notification-badge">
          <el-icon :size="20" class="notification-icon">
            <Bell />
          </el-icon>
        </el-badge>
        <template #dropdown>
          <el-dropdown-menu class="notify-menu">
            <el-dropdown-item disabled>待处理汇总：{{ notifyCount }}</el-dropdown-item>
            <el-dropdown-item v-if="summary" disabled>
              报账待审批：{{ summary.pendingReimbursementCount || 0 }}
              ｜库存预警：{{ summary.stockWarningCount || 0 }}
              ｜近效期：{{ summary.drugExpiryWarningCount || 0 }}
              ｜合同到期：{{ summary.contractExpiringCount || 0 }}
            </el-dropdown-item>
            <el-dropdown-item v-if="notifyLoading" disabled>
              加载中...
            </el-dropdown-item>
            <el-dropdown-item v-else-if="notifyItems.length === 0" disabled>
              暂无待处理
            </el-dropdown-item>
            <el-dropdown-item
              v-for="item in notifyItems"
              :key="item.id"
              @click="goReimbursements"
            >
              <div class="notify-item">
                <span class="notify-reason">{{ item.reason }}</span>
                <span class="notify-amount">￥{{ formatAmount(item.amount) }}</span>
              </div>
            </el-dropdown-item>
            <el-dropdown-item divided @click="goStockWarnings">
              查看库存预警
            </el-dropdown-item>
            <el-dropdown-item @click="goDrugWarnings">
              查看近效期预警
            </el-dropdown-item>
            <el-dropdown-item @click="goElderlyList">
              查看合同到期
            </el-dropdown-item>
            <el-dropdown-item divided @click="goReimbursements">
              查看报账
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      
      <!-- 用户下拉菜单 -->
      <el-dropdown trigger="click" @command="handleCommand">
        <span class="user-info">
          <el-avatar :size="32" :icon="UserFilled" />
          <span class="username">{{ authStore.username || '用户' }}</span>
          <el-icon class="arrow-icon"><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="logout">
              <el-icon><SwitchButton /></el-icon>
              退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../store/auth'
import { Fold, Expand, Bell, UserFilled, ArrowDown, SwitchButton } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import { api } from '../../api/client'

defineProps({
  isCollapse: {
    type: Boolean,
    default: false
  }
})

defineEmits(['toggle-sidebar'])

const router = useRouter()
const authStore = useAuthStore()

const notifyCount = ref(0)
const notifyItems = ref([])
const notifyLoading = ref(false)
const summary = ref(null)
let timer = null

function formatAmount(amount) {
  if (amount === null || amount === undefined) return '0.00'
  const n = Number(amount)
  if (Number.isNaN(n)) return String(amount)
  return n.toFixed(2)
}

async function fetchNotifySummary() {
  try {
    const resp = await api.get('/api/notifications/summary')
    const body = resp.data
    if (body.code === 200) {
      summary.value = body.data || null
      const s = summary.value || {}
      notifyCount.value =
        (s.pendingReimbursementCount || 0) +
        (s.stockWarningCount || 0) +
        (s.drugExpiryWarningCount || 0) +
        (s.contractExpiringCount || 0)
    }
  } catch (e) {
  }
}

async function fetchNotifyItems() {
  notifyLoading.value = true
  try {
    const resp = await api.get('/api/notifications/reimbursements', { params: { limit: 8 } })
    const body = resp.data
    if (body.code === 200) {
      notifyItems.value = body.data || []
    } else {
      notifyItems.value = []
    }
  } catch (e) {
    notifyItems.value = []
  } finally {
    notifyLoading.value = false
  }
}

function handleNotifyVisible(visible) {
  if (visible) {
    fetchNotifySummary()
    fetchNotifyItems()
  }
}

function goReimbursements() {
  router.push('/finance/reimbursement')
}

function goStockWarnings() {
  router.push('/warehouse/stock')
}

function goDrugWarnings() {
  router.push('/pharmacy/dispense')
}

function goElderlyList() {
  router.push('/elderly/list')
}

async function handleCommand(command) {
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      await authStore.logout()
      router.push('/login')
    } catch (e) {
      // 用户取消
    }
  }
}

onMounted(() => {
  fetchNotifySummary()
  timer = setInterval(fetchNotifySummary, 30000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.navbar {
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  background-color: #fff;
  border-bottom: 1px solid #f0f0f0;
}

.navbar-left {
  display: flex;
  align-items: center;
}

.toggle-btn {
  font-size: 20px;
  cursor: pointer;
  color: #5a5e66;
  transition: color 0.2s;
}

.toggle-btn:hover {
  color: #409eff;
}

.navbar-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.notification-badge {
  cursor: pointer;
}

.notification-icon {
  color: #5a5e66;
  transition: color 0.2s;
}

.notification-icon:hover {
  color: #409eff;
}

.notify-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-width: 260px;
}

.notify-reason {
  flex: 1;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notify-amount {
  color: #409eff;
  font-weight: 600;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  color: #5a5e66;
}

.username {
  margin: 0 8px;
  font-size: 14px;
}

.arrow-icon {
  font-size: 12px;
}

.user-info:hover {
  color: #1890ff;
}

/* 用户头像圆形背景 */
:deep(.el-avatar) {
  background: linear-gradient(135deg, #1890ff, #36cfc9);
}
</style>
