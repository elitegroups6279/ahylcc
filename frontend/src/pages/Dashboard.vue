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

    <!-- 统计卡片行（5个卡片） -->
    <el-row :gutter="16" style="margin-bottom: 20px">
      <el-col :xs="12" :sm="8" :md="4">
        <div class="stat-card" style="background: linear-gradient(135deg, #1890ff, #69c0ff)" @click="$router.push('/elderly/list')">
          <div class="stat-value">{{ stats.elderlyCount }} 人</div>
          <div class="stat-label">在住老人</div>
          <el-icon class="stat-icon"><User /></el-icon>
        </div>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <div class="stat-card" style="background: linear-gradient(135deg, #52c41a, #95de64)" @click="$router.push('/staff/list')">
          <div class="stat-value">{{ stats.staffCount }} 人</div>
          <div class="stat-label">在职护工</div>
          <el-icon class="stat-icon"><Avatar /></el-icon>
        </div>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <div class="stat-card" style="background: linear-gradient(135deg, #67C23A, #95de64)" @click="$router.push('/finance/payment')">
          <div class="stat-value">¥{{ formatMoney(stats.monthlyIncome) }}</div>
          <div class="stat-label">本月收入</div>
          <el-icon class="stat-icon"><TrendCharts /></el-icon>
        </div>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <div class="stat-card" style="background: linear-gradient(135deg, #F56C6C, #f89898)" @click="$router.push('/finance/payment')">
          <div class="stat-value">¥{{ formatMoney(stats.monthlyExpense) }}</div>
          <div class="stat-label">本月支出</div>
          <el-icon class="stat-icon"><Goods /></el-icon>
        </div>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <div class="stat-card" style="background: linear-gradient(135deg, #722ed1, #b37feb)">
          <div class="stat-value">{{ stats.bedUsageRate }}%</div>
          <div class="stat-label">床位使用率</div>
          <el-icon class="stat-icon"><House /></el-icon>
        </div>
      </el-col>
    </el-row>

    <!-- 中部双栏 -->
    <el-row :gutter="16">
      <!-- 左栏：日历面板 -->
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
            <span class="legend-item"><span class="legend-dot" style="background:#ff4d4f"></span>退住</span>
          </div>
        </el-card>
      </el-col>

      <!-- 右栏：待办 + 快捷操作 -->
      <el-col :span="10">
        <!-- 待办提醒 -->
        <el-card class="pending-card" style="margin-bottom: 16px">
          <template #header><span style="font-weight:bold">待办提醒</span></template>
          <div class="pending-list">
            <div class="pending-item" v-for="item in pendingItems" :key="item.label" :class="{ 'is-empty': item.count === 0 }">
              <span class="pending-dot" :style="{background: item.color}"></span>
              <span class="pending-text">{{ item.label }}</span>
              <el-badge :value="item.count" :type="item.count > 0 ? item.badgeType : 'info'" />
            </div>
          </div>
        </el-card>

        <!-- 快捷操作 -->
        <el-card>
          <template #header><span style="font-weight:bold">快捷操作</span></template>
          <el-row :gutter="12">
            <el-col :span="12" v-for="action in quickActions" :key="action.label">
              <el-button :type="action.type" plain style="width:100%;margin-bottom:12px" @click="router.push(action.path)">
                <el-icon><component :is="action.icon" /></el-icon>
                {{ action.label }}
              </el-button>
            </el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>

    <!-- 底部版权 -->
    <div style="text-align:center;padding:20px 0;color:#c0c4cc;font-size:12px">
      安庆市泰呈健康评估服务有限公司 · <a href="https://beian.miit.gov.cn/" target="_blank" rel="noopener noreferrer" style="color: inherit; text-decoration: none;">皖ICP备2022009803号-4</a>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api/client'
import { 
  User, 
  Avatar, 
  TrendCharts, 
  Goods, 
  House,
  ArrowLeft, 
  ArrowRight,
  Plus,
  Minus,
  UserFilled,
  Document
} from '@element-plus/icons-vue'

const router = useRouter()

// 费用预警
const feeWarnings = ref([])
let warningTimer = null

const warningText = computed(() => {
  return feeWarnings.value
    .map(w => `⚠ ${w.name} 余额剩余 ${w.remainingDays} 天`)
    .join(' · ')
})

async function loadFeeWarnings() {
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

// 统计数据
const stats = ref({
  elderlyCount: 0,
  staffCount: 0,
  monthlyIncome: 0,
  monthlyExpense: 0,
  bedUsageRate: 0,
  totalBeds: 0,
  occupiedBeds: 0
})

async function loadStats() {
  try {
    const res = await api.get('/api/dashboard/stats')
    if (res.data?.code === 200) {
      const data = res.data.data
      stats.value = {
        elderlyCount: data.elderlyCount || 0,
        staffCount: data.staffCount || 0,
        monthlyIncome: data.monthlyIncome || 0,
        monthlyExpense: data.monthlyExpense || 0,
        bedUsageRate: data.bedUsageRate || 0,
        totalBeds: data.totalBeds || 0,
        occupiedBeds: data.occupiedBeds || 0
      }
    }
  } catch (e) {
    console.warn('加载统计数据失败', e)
  }
}

function formatMoney(val) {
  return Number(val || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// 日历相关数据
const currentYear = ref(new Date().getFullYear())
const currentMonth = ref(new Date().getMonth() + 1)
const calendarEvents = ref([])

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
  const firstDay = new Date(year, month - 1, 1) // 本月1号
  const startWeekday = firstDay.getDay() // 1号是周几
  const daysInMonth = new Date(year, month, 0).getDate() // 本月天数

  const today = new Date()
  const todayStr = `${today.getFullYear()}-${String(today.getMonth()+1).padStart(2,'0')}-${String(today.getDate()).padStart(2,'0')}`

  // 事件按日期索引
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
    const dateStr = `${year}-${String(month).padStart(2,'0')}-${String(d).padStart(2,'0')}`
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

// 待办提醒
const pending = ref({
  feeWarningCount: 0,
  pendingReimbursement: 0,
  pendingVoucher: 0,
  pharmacyWarningCount: 0
})

const pendingItems = computed(() => [
  { label: '费用预警', count: pending.value.feeWarningCount || 0, color: '#fa8c16', badgeType: 'warning' },
  { label: '待审报账', count: pending.value.pendingReimbursement || 0, color: '#1890ff', badgeType: 'primary' },
  { label: '待审凭证', count: pending.value.pendingVoucher || 0, color: '#722ed1', badgeType: 'primary' },
  { label: '药品预警', count: pending.value.pharmacyWarningCount || 0, color: '#ff4d4f', badgeType: 'danger' }
])

async function loadPendingSummary() {
  try {
    const res = await api.get('/api/dashboard/pending-summary')
    if (res.data?.code === 200) {
      const data = res.data.data
      pending.value = {
        feeWarningCount: data.feeWarningCount || 0,
        pendingReimbursement: data.pendingReimbursement || 0,
        pendingVoucher: data.pendingVoucher || 0,
        pharmacyWarningCount: data.pharmacyWarningCount || 0
      }
    }
  } catch (e) {
    console.warn('加载待办提醒失败', e)
  }
}

// 快捷操作
const quickActions = [
  { label: '登记收入', type: 'success', icon: 'Plus', path: '/finance/payment' },
  { label: '登记支出', type: 'danger', icon: 'Minus', path: '/finance/payment' },
  { label: '新增老人', type: 'primary', icon: 'UserFilled', path: '/elderly/add' },
  { label: '服务工单', type: 'warning', icon: 'Document', path: '/home-service/orders' }
]

// 页面加载
onMounted(async () => {
  await Promise.all([
    loadFeeWarnings(),
    loadStats(),
    loadCalendarEvents(),
    loadPendingSummary()
  ])
  // 费用预警30秒轮询
  warningTimer = setInterval(loadFeeWarnings, 30000)
})

onUnmounted(() => {
  if (warningTimer) clearInterval(warningTimer)
})
</script>

<style scoped>
.dashboard-page {
  padding: 16px;
}

/* 费用预警条 */
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
  0% {
    transform: translateX(0);
  }
  100% {
    transform: translateX(-100%);
  }
}

/* 统计卡片样式 */
.stat-card {
  border-radius: 12px;
  color: white;
  padding: 20px;
  position: relative;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.3s, box-shadow 0.3s;
  margin-bottom: 16px;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.15);
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  opacity: 0.85;
}

.stat-icon {
  position: absolute;
  right: 16px;
  top: 16px;
  font-size: 48px;
  opacity: 0.2;
}

/* 待办提醒样式 */
.pending-card :deep(.el-card__header) {
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;
}

.pending-list {
  padding: 8px 0;
}

.pending-item {
  display: flex;
  align-items: center;
  height: 40px;
  padding: 0 8px;
  transition: background 0.2s;
}

.pending-item:hover {
  background: #f5f7fa;
}

.pending-item.is-empty {
  opacity: 0.5;
}

.pending-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 12px;
  flex-shrink: 0;
}

.pending-text {
  flex: 1;
  font-size: 14px;
  color: #606266;
}

/* 日历样式 */
.calendar-card {
  margin-bottom: 16px;
}

.calendar-card :deep(.el-card__header) {
  padding: 12px 16px;
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
  min-height: 60px;
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
</style>
