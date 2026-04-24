<template>
  <div class="dashboard-page">
    <!-- 费用预警滚动条 -->
    <div v-if="feeWarnings.length > 0" class="warning-strip">
      <div class="marquee">
        <div class="marquee-content">
          {{ warningText }}
        </div>
      </div>
    </div>

    <!-- 5个渐变色统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="4" v-for="item in statsCards" :key="item.title" style="min-width: 180px">
        <div class="stat-card" :style="{ background: item.gradient }">
          <div class="stat-icon">
            <el-icon :size="32"><component :is="item.icon" /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ item.value }}</div>
            <div class="stat-title">{{ item.title }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 日历面板（左栏）+ 待办提醒（右栏）-->
    <el-row :gutter="16" class="main-row">
      <el-col :span="14">
        <el-card class="calendar-card" shadow="hover">
          <template #header>
            <div class="calendar-header">
              <el-button :icon="ArrowLeft" circle size="small" @click="prevMonth" />
              <span class="calendar-title">{{ currentYear }} 年 {{ currentMonth }} 月</span>
              <el-button :icon="ArrowRight" circle size="small" @click="nextMonth" />
              <el-button size="small" @click="goToday" style="margin-left: 12px">今天</el-button>
            </div>
          </template>

          <!-- 星期头 -->
          <div class="calendar-grid">
            <div class="calendar-weekday" v-for="d in ['日','一','二','三','四','五','六']" :key="d">{{ d }}</div>

            <!-- 日期格子 -->
            <div
              v-for="(cell, idx) in calendarCells"
              :key="idx"
              class="calendar-cell"
              :class="{
                'is-today': cell.isToday,
                'is-other-month': !cell.isCurrentMonth,
                'has-events': cell.events.length > 0
              }"
            >
              <el-tooltip
                v-if="cell.events.length > 0"
                placement="top"
                :content="getEventTooltip(cell.events)"
              >
                <div class="cell-content">
                  <span class="cell-day">{{ cell.day }}</span>
                  <div class="cell-dots" v-if="cell.events.length > 0">
                    <span
                      v-for="(color, i) in cell.colors"
                      :key="i"
                      class="event-dot"
                      :style="{ background: color }"
                    />
                  </div>
                </div>
              </el-tooltip>
              <template v-else>
                <span class="cell-day">{{ cell.day }}</span>
              </template>
            </div>
          </div>

          <!-- 图例 -->
          <div class="calendar-legend">
            <span class="legend-item"><span class="legend-dot" style="background:#fa8c16"></span>社会化入住</span>
            <span class="legend-item"><span class="legend-dot" style="background:#fadb14"></span>五保入住</span>
            <span class="legend-item"><span class="legend-dot" style="background:#1890ff"></span>低保入住</span>
            <span class="legend-item"><span class="legend-dot" style="background:#52c41a"></span>请假</span>
            <span class="legend-item"><span class="legend-dot" style="background:#13c2c2"></span>已销假</span>
            <span class="legend-item"><span class="legend-dot" style="background:#ff4d4f"></span>退住</span>
          </div>
        </el-card>
      </el-col>

      <el-col :span="10">
        <el-card class="pending-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>待办提醒</span>
            </div>
          </template>
          <div class="pending-list" v-if="pendingSummary">
            <div class="pending-item warn" @click="$router.push('/finance/payment')">
              <div class="pending-icon"><el-icon :size="24"><Warning /></el-icon></div>
              <div class="pending-info">
                <div class="pending-count">{{ pendingSummary.feeWarningCount ?? 0 }}</div>
                <div class="pending-label">费用预警</div>
              </div>
              <el-icon class="pending-arrow"><ArrowRight /></el-icon>
            </div>
            <div class="pending-item warn" @click="$router.push('/finance/reimbursement')">
              <div class="pending-icon"><el-icon :size="24"><Tickets /></el-icon></div>
              <div class="pending-info">
                <div class="pending-count">{{ pendingSummary.pendingReimbursementCount ?? 0 }}</div>
                <div class="pending-label">待审报账</div>
              </div>
              <el-icon class="pending-arrow"><ArrowRight /></el-icon>
            </div>
            <div class="pending-item info" @click="$router.push('/finance/voucher')">
              <div class="pending-icon"><el-icon :size="24"><Document /></el-icon></div>
              <div class="pending-info">
                <div class="pending-count">{{ pendingSummary.pendingVoucherCount ?? 0 }}</div>
                <div class="pending-label">待审凭证</div>
              </div>
              <el-icon class="pending-arrow"><ArrowRight /></el-icon>
            </div>
            <div class="pending-item danger" @click="$router.push('/pharmacy/drugs')">
              <div class="pending-icon"><el-icon :size="24"><FirstAidKit /></el-icon></div>
              <div class="pending-info">
                <div class="pending-count">{{ pendingSummary.drugExpiryWarningCount ?? 0 }}</div>
                <div class="pending-label">药品效期预警</div>
              </div>
              <el-icon class="pending-arrow"><ArrowRight /></el-icon>
            </div>
          </div>
          <el-empty v-else description="暂无待办" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷操作 -->
    <el-card class="quick-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>快捷操作</span>
        </div>
      </template>
      <div class="quick-actions">
        <el-button type="primary" size="large" @click="$router.push('/finance/payment')">
          <el-icon><Money /></el-icon> 登记收入
        </el-button>
        <el-button type="danger" size="large" @click="$router.push('/finance/payment')">
          <el-icon><Wallet /></el-icon> 登记支出
        </el-button>
        <el-button type="success" size="large" @click="$router.push('/elderly/add')">
          <el-icon><Plus /></el-icon> 新增老人
        </el-button>
        <el-button type="warning" size="large" @click="$router.push('/home-service/orders')">
          <el-icon><Tickets /></el-icon> 服务工单
        </el-button>
      </div>
    </el-card>

    <!-- 底部版权区域 -->
    <div class="dashboard-footer">
      <p>安庆市泰呈健康评估服务有限公司 版权所有</p>
      <p><a href="https://beian.miit.gov.cn/" target="_blank" rel="noopener noreferrer" style="color: inherit; text-decoration: none;">皖ICP备2022009803号-4</a></p>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useAuthStore } from '../store/auth'
import { api } from '../api/client'
import {
  User, UserFilled, Money, Wallet, Bell,
  ArrowLeft, ArrowRight, Warning, Document, Tickets,
  FirstAidKit, Plus, DataBoard
} from '@element-plus/icons-vue'

const authStore = useAuthStore()
const feeWarnings = ref([])
const pendingSummary = ref(null)
let timer = null

// 日历相关数据
const currentYear = ref(new Date().getFullYear())
const currentMonth = ref(new Date().getMonth() + 1)
const calendarEvents = ref([])

// 统计数据
const stats = ref({
  elderlyCount: 0,
  staffCount: 0,
  monthlyIncome: 0,
  monthlyExpense: 0,
  bedUsageRate: 0
})

// 5个渐变色统计卡片配置
const statsCards = computed(() => [
  {
    title: '在住老人',
    value: stats.value.elderlyCount,
    icon: User,
    gradient: 'linear-gradient(135deg, #1890ff, #36cfc9)'
  },
  {
    title: '在职护工',
    value: stats.value.staffCount,
    icon: UserFilled,
    gradient: 'linear-gradient(135deg, #52c41a, #95de64)'
  },
  {
    title: '本月收入',
    value: '¥' + (stats.value.monthlyIncome || 0).toLocaleString(),
    icon: Money,
    gradient: 'linear-gradient(135deg, #fa8c16, #ffc53d)'
  },
  {
    title: '本月支出',
    value: '¥' + (stats.value.monthlyExpense || 0).toLocaleString(),
    icon: Wallet,
    gradient: 'linear-gradient(135deg, #722ed1, #b37feb)'
  },
  {
    title: '床位使用率',
    value: (stats.value.bedUsageRate || 0) + '%',
    icon: DataBoard,
    gradient: 'linear-gradient(135deg, #13c2c2, #5cdbd3)'
  }
])

// 费用预警滚动文本
const warningText = computed(() => {
  return feeWarnings.value
    .map(w => `⚠ ${w.name} 余额剩余 ${w.remainingDays} 天`)
    .join(' · ')
})

// 获取日历事件
const loadCalendarEvents = async () => {
  try {
    const res = await api.get('/api/dashboard/calendar-events', {
      params: { year: currentYear.value, month: currentMonth.value }
    })
    if (res.data?.code === 200) {
      calendarEvents.value = res.data.data || []
    }
  } catch (e) {
    console.warn('加载日历事件失败', e)
  }
}

// 颜色映射
const getEventColor = (event) => {
  if (event.type === 'DISCHARGE') return '#ff4d4f'
  if (event.type === 'LEAVE') return '#52c41a'
  if (event.type === 'LEAVE_RETURNED') return '#13c2c2'
  if (event.type === 'ADMISSION') {
    if (event.category === 'SOCIAL') return '#fa8c16'
    if (event.category === 'WU_BAO') return '#fadb14'
    if (event.category === 'LOW_BAO') return '#1890ff'
  }
  return '#d9d9d9'
}

// 获取事件类型显示文本
const getEventTypeText = (event) => {
  if (event.type === 'DISCHARGE') return '退住'
  if (event.type === 'LEAVE') return '请假'
  if (event.type === 'LEAVE_RETURNED') return '已销假'
  if (event.type === 'ADMISSION') {
    if (event.category === 'SOCIAL') return '社会化入住'
    if (event.category === 'WU_BAO') return '五保入住'
    if (event.category === 'LOW_BAO') return '低保入住'
    return '入住'
  }
  return event.type
}

// 生成tooltip内容
const getEventTooltip = (events) => {
  return events.map(e => `${e.elderlyName} - ${getEventTypeText(e)}`).join('\n')
}

// 生成日历格子（6行7列=42格）
const calendarCells = computed(() => {
  const year = currentYear.value
  const month = currentMonth.value
  const firstDay = new Date(year, month - 1, 1)
  const startWeekday = firstDay.getDay()
  const daysInMonth = new Date(year, month, 0).getDate()

  const today = new Date()
  const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`

  const eventsMap = {}
  calendarEvents.value.forEach(item => {
    eventsMap[item.date] = item.events
  })

  const cells = []

  // 上月填充
  const prevMonthDays = new Date(year, month - 1, 0).getDate()
  for (let i = startWeekday - 1; i >= 0; i--) {
    cells.push({ day: prevMonthDays - i, isCurrentMonth: false, isToday: false, events: [], colors: [] })
  }

  // 本月
  for (let d = 1; d <= daysInMonth; d++) {
    const dateStr = `${year}-${String(month).padStart(2, '0')}-${String(d).padStart(2, '0')}`
    const events = eventsMap[dateStr] || []
    const colorsSet = new Set()
    events.forEach(e => colorsSet.add(getEventColor(e)))
    cells.push({
      day: d,
      isCurrentMonth: true,
      isToday: dateStr === todayStr,
      events,
      colors: [...colorsSet]
    })
  }

  // 下月填充至42格
  const remaining = 42 - cells.length
  for (let d = 1; d <= remaining; d++) {
    cells.push({ day: d, isCurrentMonth: false, isToday: false, events: [], colors: [] })
  }

  return cells
})

const prevMonth = () => {
  if (currentMonth.value === 1) { currentYear.value--; currentMonth.value = 12 }
  else { currentMonth.value-- }
  loadCalendarEvents()
}

const nextMonth = () => {
  if (currentMonth.value === 12) { currentYear.value++; currentMonth.value = 1 }
  else { currentMonth.value++ }
  loadCalendarEvents()
}

const goToday = () => {
  currentYear.value = new Date().getFullYear()
  currentMonth.value = new Date().getMonth() + 1
  loadCalendarEvents()
}

// 加载费用预警
async function fetchFeeWarnings() {
  try {
    const resp = await api.get('/api/dashboard/fee-warnings')
    const body = resp.data
    if (body.code === 200) {
      feeWarnings.value = body.data || []
    } else {
      feeWarnings.value = []
    }
  } catch (e) {
    feeWarnings.value = []
  }
}

// 加载待办汇总
async function loadPendingSummary() {
  try {
    const res = await api.get('/api/dashboard/pending-summary')
    if (res.data?.code === 200) {
      pendingSummary.value = res.data.data
    }
  } catch (e) {
    console.warn('加载待办汇总失败', e)
  }
}

// 加载统计数据
async function loadStats() {
  try {
    const res = await api.get('/api/dashboard/stats')
    if (res.data?.code === 200) {
      stats.value = res.data.data
    }
  } catch (e) {
    console.warn('加载统计数据失败', e)
  }
}

onMounted(async () => {
  if (!authStore.permissions || authStore.permissions.length === 0) {
    await authStore.fetchPermissions()
  }
  await fetchFeeWarnings()
  await loadStats()
  await loadPendingSummary()
  await loadCalendarEvents()
  timer = setInterval(fetchFeeWarnings, 30000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.dashboard-page {
  padding: 16px;
}

/* 费用预警滚动条 */
.warning-strip {
  height: 36px;
  background: #f56c6c;
  color: #fff;
  display: flex;
  align-items: center;
  padding: 0 12px;
  border-radius: 4px;
  margin-bottom: 16px;
  overflow: hidden;
}

.marquee {
  position: relative;
  width: 100%;
  overflow: hidden;
  white-space: nowrap;
}

.marquee-content {
  display: inline-block;
  padding-left: 100%;
  animation: marquee 18s linear infinite;
}

@keyframes marquee {
  0% { transform: translateX(0); }
  100% { transform: translateX(-100%); }
}

/* 统计卡片样式 */
.stats-row {
  margin-bottom: 16px;
}

.stat-card {
  border-radius: 12px;
  padding: 20px;
  color: #fff;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s;
  cursor: default;
}

.stat-card:hover {
  transform: translateY(-4px);
}

.stat-icon {
  opacity: 0.9;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
}

.stat-title {
  font-size: 14px;
  opacity: 0.85;
  margin-top: 4px;
}

/* 日历+待办 双栏布局 */
.main-row {
  margin-bottom: 16px;
}

/* 日历样式 */
.calendar-card {
  min-height: 460px;
}

.calendar-header {
  display: flex;
  align-items: center;
  justify-content: center;
}

.calendar-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0 16px;
  min-width: 120px;
  text-align: center;
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 2px;
}

.calendar-weekday {
  text-align: center;
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  padding: 8px 0;
  border-bottom: 1px solid #ebeef5;
}

.calendar-cell {
  text-align: center;
  padding: 8px 4px;
  min-height: 50px;
  border-radius: 4px;
  cursor: default;
  transition: background 0.2s;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
}

.calendar-cell:hover {
  background: #f5f7fa;
}

.calendar-cell.is-today {
  background: rgba(24, 144, 255, 0.08);
}

.calendar-cell.is-today .cell-day {
  background: #1890ff;
  color: #fff;
  border-radius: 50%;
  width: 24px;
  height: 24px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.calendar-cell.is-other-month {
  opacity: 0.3;
}

.cell-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
}

.cell-day {
  font-size: 14px;
  display: inline-block;
}

.cell-dots {
  display: flex;
  justify-content: center;
  gap: 3px;
  margin-top: 4px;
  flex-wrap: wrap;
}

.event-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}

.calendar-legend {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding-top: 12px;
  margin-top: 8px;
  border-top: 1px solid #ebeef5;
  flex-wrap: wrap;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
}

/* 待办提醒样式 */
.pending-card {
  min-height: 460px;
}

.card-header {
  font-weight: 600;
  font-size: 16px;
}

.pending-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.pending-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border-radius: 8px;
  background: #f5f7fa;
  transition: transform 0.2s, box-shadow 0.2s;
  cursor: pointer;
}

.pending-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.pending-item.warn { border-left: 3px solid #fa8c16; }
.pending-item.info { border-left: 3px solid #1890ff; }
.pending-item.danger { border-left: 3px solid #ff4d4f; }

.pending-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
}

.pending-item.warn .pending-icon { background: #fff7e6; color: #fa8c16; }
.pending-item.info .pending-icon { background: #e6f7ff; color: #1890ff; }
.pending-item.danger .pending-icon { background: #fff1f0; color: #ff4d4f; }

.pending-info {
  flex: 1;
}

.pending-count {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
}

.pending-label {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.pending-arrow {
  color: #c0c4cc;
  font-size: 14px;
}

/* 快捷操作样式 */
.quick-card {
  margin-bottom: 16px;
}

.quick-actions {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.quick-actions .el-button {
  min-width: 140px;
  height: 48px;
  font-size: 15px;
  border-radius: 8px;
}

/* 底部版权区域 */
.dashboard-footer {
  margin-top: 24px;
  padding: 20px 0;
  text-align: center;
  border-top: 1px solid #e8e8e8;
}

.dashboard-footer p {
  margin: 4px 0;
  font-size: 13px;
  color: #999;
}
</style>
