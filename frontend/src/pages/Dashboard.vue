<template>
  <div class="dashboard-page">
    <!-- 费用预警条 -->
    <div v-if="feeWarnings.length > 0" class="warning-strip">
      <div class="marquee">
        <div class="marquee-content">
          {{ warningText }}
        </div>
      </div>
    </div>

    <!-- 统计卡片区 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6" v-for="item in statsCards" :key="item.title">
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

    <!-- 待处理事项区 -->
    <el-card class="pending-card" shadow="hover" v-if="pendingSummary">
      <template #header>
        <div class="card-header">
          <span>待处理事项</span>
        </div>
      </template>
      <el-row :gutter="16">
        <el-col :span="6" v-for="item in pendingItems" :key="item.label">
          <div class="pending-item" :class="item.cls">
            <div class="pending-icon">
              <el-icon :size="24"><component :is="item.icon" /></el-icon>
            </div>
            <div class="pending-info">
              <div class="pending-count">{{ item.value }}</div>
              <div class="pending-label">{{ item.label }}</div>
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 日历卡片 -->
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

    <!-- 图表区域 -->
    <el-row :gutter="16" class="charts-row">
      <el-col :span="14">
        <el-card shadow="hover">
          <template #header>
            <span style="font-weight:600">近6个月收入趋势</span>
          </template>
          <div ref="revenueChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="hover">
          <template #header>
            <span style="font-weight:600">入住类别分布</span>
          </template>
          <div ref="categoryChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="welcome-card">
      <template #header>
        <div class="card-header">
          <span>欢迎使用养老企业管理系统</span>
        </div>
      </template>
      <div class="welcome-content">
        <p>当前用户：<strong>{{ authStore.user?.username || authStore.username || '未知' }}</strong></p>
        <p>系统功能正在开发中，敬请期待...</p>
      </div>
    </el-card>

    <el-card class="permissions-card">
      <template #header>
        <div class="card-header">
          <span>权限列表</span>
        </div>
      </template>
      <div class="permissions-content" v-if="authStore.permissions?.length">
        <el-tag 
          v-for="p in authStore.permissions" 
          :key="p" 
          class="permission-tag"
          type="info"
        >
          {{ p }}
        </el-tag>
      </div>
      <el-empty v-else description="暂无权限数据" :image-size="80" />
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
import { User, UserFilled, Money, Bell, ArrowLeft, ArrowRight, Warning, Document, Tickets, FirstAidKit } from '@element-plus/icons-vue'

const authStore = useAuthStore()
const feeWarnings = ref([])
const pendingSummary = ref(null)
const revenueChartRef = ref(null)
const categoryChartRef = ref(null)
let revenueChart = null
let categoryChart = null
let timer = null

// ECharts lazy loading
let echartsModule = null

async function getEcharts() {
  if (!echartsModule) {
    echartsModule = await import('echarts')
  }
  return echartsModule
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

// 统计数据
const stats = ref({ 
  elderlyCount: 0, 
  staffCount: 0, 
  monthlyIncome: 0, 
  pendingTasks: 0 
})

// 统计卡片配置
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
    title: '待处理',
    value: stats.value.pendingTasks,
    icon: Bell,
    gradient: 'linear-gradient(135deg, #ff4d4f, #ff7875)'
  }
])

const warningText = computed(() => {
  return feeWarnings.value
    .map(w => `⚠ ${w.name} 余额剩余 ${w.remainingDays} 天`)
    .join(' · ')
})

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

// 加载待处理事项汇总
async function loadPendingSummary() {
  try {
    const res = await api.get('/api/dashboard/pending-summary')
    if (res.data?.code === 200) {
      pendingSummary.value = res.data.data
    }
  } catch (e) {
    console.warn('加载待处理事项失败', e)
  }
}

// 待处理事项配置
const pendingItems = computed(() => {
  if (!pendingSummary.value) return []
  return [
    { label: '费用预警', value: pendingSummary.value.feeWarningCount ?? 0, icon: Warning, cls: 'warn' },
    { label: '待审报账', value: pendingSummary.value.pendingReimbursementCount ?? 0, icon: Tickets, cls: 'warn' },
    { label: '待审凭证', value: pendingSummary.value.pendingVoucherCount ?? 0, icon: Document, cls: 'info' },
    { label: '药品效期预警', value: pendingSummary.value.drugExpiryWarningCount ?? 0, icon: FirstAidKit, cls: 'danger' }
  ]
})

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

async function loadCharts() {
  try {
    const res = await api.get('/api/dashboard/charts')
    if (res.data?.code === 200) {
      const data = res.data.data
      await renderRevenueChart(data.revenueTrend || [])
      await renderCategoryChart(data.categoryDistribution || [])
    }
  } catch (e) {
    console.warn('加载图表数据失败', e)
  }
}

async function renderRevenueChart(trend) {
  if (!revenueChartRef.value) return
  if (revenueChart) revenueChart.dispose()
  const echarts = await getEcharts()
  revenueChart = echarts.init(revenueChartRef.value)
  const months = trend.map(t => t.month)
  const amounts = trend.map(t => Number(t.amount) || 0)
  revenueChart.setOption({
    tooltip: { trigger: 'axis', formatter: '{b}<br/>收入：¥{c}' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: months, boundaryGap: false },
    yAxis: { type: 'value', axisLabel: { formatter: '¥{value}' } },
    series: [{
      name: '月收入',
      type: 'line',
      smooth: true,
      areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
        { offset: 0, color: 'rgba(24,144,255,0.3)' },
        { offset: 1, color: 'rgba(24,144,255,0.02)' }
      ])},
      lineStyle: { color: '#1890ff', width: 2 },
      itemStyle: { color: '#1890ff' },
      data: amounts
    }]
  })
}

async function renderCategoryChart(dist) {
  if (!categoryChartRef.value) return
  if (categoryChart) categoryChart.dispose()
  const echarts = await getEcharts()
  categoryChart = echarts.init(categoryChartRef.value)
  const nameMap = { SOCIAL: '社会化', WU_BAO: '五保', LOW_BAO: '低保' }
  const pieData = dist.map(d => ({ name: nameMap[d.category] || d.category, value: d.count }))
  categoryChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c}人 ({d}%)' },
    legend: { bottom: '5%', left: 'center' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}\n{c}人' },
      color: ['#fa8c16', '#fadb14', '#1890ff'],
      data: pieData
    }]
  })
}

onMounted(async () => {
  if (!authStore.permissions || authStore.permissions.length === 0) {
    await authStore.fetchPermissions()
  }
  await fetchFeeWarnings()
  await loadStats()
  await loadPendingSummary()
  await loadCalendarEvents()
  await loadCharts()
  timer = setInterval(fetchFeeWarnings, 30000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  if (revenueChart) revenueChart.dispose()
  if (categoryChart) categoryChart.dispose()
})
</script>

<style scoped>
.dashboard-page {
  padding: 16px;
}

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

.charts-row {
  margin-bottom: 16px;
}

.welcome-card {
  margin-bottom: 16px;
}

.card-header {
  font-weight: 600;
  font-size: 16px;
}

.welcome-content p {
  margin: 8px 0;
  color: #606266;
}

.welcome-content strong {
  color: #1890ff;
}

.permissions-content {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.permission-tag {
  margin: 0;
}

/* 待处理事项区样式 */
.pending-card {
  margin-bottom: 16px;
}

.pending-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border-radius: 8px;
  background: #f5f7fa;
  transition: transform 0.2s;
}

.pending-item:hover {
  transform: translateY(-2px);
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

/* 日历样式 */
.calendar-card {
  margin-bottom: 16px;
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
