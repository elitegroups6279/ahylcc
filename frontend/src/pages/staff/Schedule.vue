<template>
  <div class="page">
    <!-- 顶部工具栏 -->
    <el-card class="toolbar-card">
      <div class="toolbar">
        <!-- 周导航 -->
        <div class="week-nav">
          <el-button-group>
            <el-button @click="prevWeek">
              <el-icon><Arrow-Left /></el-icon>
            </el-button>
            <el-button @click="currentWeek">本周</el-button>
            <el-button @click="nextWeek">
              <el-icon><Arrow-Right /></el-icon>
            </el-button>
          </el-button-group>
          <span class="week-range">{{ weekRangeText }}</span>
        </div>

        <!-- 视图切换 -->
        <div class="view-toggle">
          <el-radio-group v-model="viewMode" size="default">
            <el-radio-button label="week">周视图</el-radio-button>
            <el-radio-button label="list">列表视图</el-radio-button>
          </el-radio-group>
        </div>

        <!-- 操作按钮 -->
        <div class="actions">
          <el-button type="primary" @click="openBatchDialog">
            <el-icon><Plus /></el-icon>批量排班
          </el-button>
          <el-button @click="refresh">
            <el-icon><Refresh /></el-icon>刷新
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 周视图 -->
    <el-card v-if="viewMode === 'week'" v-loading="weekLoading" class="week-view-card">
      <div v-if="weekData.length === 0" class="empty-tip">
        <el-empty description="暂无在职护工数据" />
      </div>
      <div v-else class="week-table-wrapper">
        <table class="week-table">
          <thead>
            <tr>
              <th class="staff-header">护工</th>
              <th v-for="day in weekDays" :key="day.date" :class="{ today: day.isToday }">
                <div class="day-header">
                  <span class="weekday">{{ day.weekday }}</span>
                  <span class="date">{{ day.dateText }}</span>
                </div>
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in weekData" :key="row.staffId">
              <td class="staff-name">
                {{ row.staffName }}
                <el-tag v-if="row.positionType && positionTagMap[row.positionType]" :color="positionTagMap[row.positionType].color" style="color: #fff; border: none; margin-left: 4px;" size="small">{{ positionTagMap[row.positionType].shortLabel }}</el-tag>
              </td>
              <td v-for="day in weekDays" :key="day.date" :class="{ today: day.isToday }">
                <div class="shift-cell" @click="openQuickEdit(row, day)">
                  <el-tag v-if="getShift(row, day.date)" :type="getShiftType(getShift(row, day.date))">
                    {{ getShiftLabel(getShift(row, day.date)) }}
                  </el-tag>
                  <span v-else class="rest">休</span>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </el-card>

    <!-- 列表视图 -->
    <el-card v-else class="list-view-card">
      <div class="list-filter">
        <el-select
          v-model="listFilter.staffId"
          filterable
          remote
          clearable
          :remote-method="searchStaff"
          :loading="staffLoading"
          placeholder="选择护工"
          style="width: 200px"
          @change="reloadList"
        >
          <el-option v-for="s in staffOptions" :key="s.id" :label="s.name + (s.positionType && positionTagMap[s.positionType] ? ' [' + positionTagMap[s.positionType].shortLabel + ']' : '')" :value="s.id" />
        </el-select>
        <el-select v-model="listFilter.shiftType" clearable placeholder="班次" style="width: 140px" @change="reloadList">
          <el-option label="早班" value="MORNING" />
          <el-option label="中班" value="AFTERNOON" />
          <el-option label="夜班" value="NIGHT" />
        </el-select>
        <el-button @click="resetListFilter">重置</el-button>
      </div>

      <el-table :data="listData" v-loading="listLoading" row-key="id">
        <el-table-column prop="staffName" label="护工" width="160" />
        <el-table-column prop="scheduleDate" label="日期" width="140" />
        <el-table-column prop="shiftType" label="班次" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.shiftType === 'MORNING'">早班</el-tag>
            <el-tag v-else-if="row.shiftType === 'AFTERNOON'" type="success">中班</el-tag>
            <el-tag v-else-if="row.shiftType === 'NIGHT'" type="warning">夜班</el-tag>
            <span v-else>{{ row.shiftType }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="editFromList(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          background
          layout="total, prev, pager, next, sizes"
          :total="listTotal"
          :current-page="listPage"
          :page-size="listPageSize"
          :page-sizes="[10, 20, 50, 100]"
          @update:current-page="(p) => { listPage = p; fetchList() }"
          @update:page-size="(s) => { listPageSize = s; listPage = 1; fetchList() }"
        />
      </div>
    </el-card>

    <!-- 月度统计 -->
    <el-card class="stats-card">
      <template #header>
        <div class="stats-header">
          <span>月度排班统计 ({{ currentMonth }})</span>
        </div>
      </template>
      <el-table :data="monthStats" v-loading="statsLoading" size="small">
        <el-table-column prop="staffName" label="护工姓名" min-width="120" />
        <el-table-column prop="morningCount" label="早班次数" width="100" align="center">
          <template #default="{ row }">
            <el-tag>{{ row.morningCount }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="afternoonCount" label="中班次数" width="100" align="center">
          <template #default="{ row }">
            <el-tag type="success">{{ row.afternoonCount }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="nightCount" label="夜班次数" width="100" align="center">
          <template #default="{ row }">
            <el-tag type="warning">{{ row.nightCount }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalShifts" label="总班次" width="100" align="center">
          <template #default="{ row }">
            <strong>{{ row.totalShifts }}</strong>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 批量排班对话框 -->
    <el-dialog v-model="batchDialogVisible" title="批量排班" width="560px">
      <el-form ref="batchFormRef" :model="batchForm" :rules="batchRules" label-width="100px">
        <el-form-item label="选择护工" prop="staffIds">
          <el-select
            v-model="batchForm.staffIds"
            filterable
            multiple
            :loading="staffLoading"
            placeholder="请选择护工（可多选）"
            style="width: 100%"
          >
            <el-option v-for="s in allStaffOptions" :key="s.id" :label="s.name + (s.positionType && positionTagMap[s.positionType] ? ' [' + positionTagMap[s.positionType].shortLabel + ']' : '')" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期范围" prop="dateRange">
          <el-date-picker
            v-model="batchForm.dateRange"
            type="daterange"
            range-separator="~"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="班次" prop="shiftType">
          <el-select v-model="batchForm.shiftType" placeholder="请选择班次" style="width: 100%">
            <el-option label="早班" value="MORNING" />
            <el-option label="中班" value="AFTERNOON" />
            <el-option label="夜班" value="NIGHT" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchSaving" @click="submitBatch">确认排班</el-button>
      </template>
    </el-dialog>

    <!-- 快捷排班 Popover -->
    <el-dialog v-model="quickEditVisible" title="快捷排班" width="400px">
      <div class="quick-edit-info">
        <p><strong>护工：</strong>{{ quickEditData.staffName }}</p>
        <p><strong>日期：</strong>{{ quickEditData.date }}</p>
      </div>
      <div class="quick-edit-actions">
        <el-button type="primary" @click="quickSetShift('MORNING')">设为早班</el-button>
        <el-button type="success" @click="quickSetShift('AFTERNOON')">设为中班</el-button>
        <el-button type="warning" @click="quickSetShift('NIGHT')">设为夜班</el-button>
        <el-button v-if="quickEditData.currentShift" type="danger" @click="quickDelete">删除排班</el-button>
      </div>
    </el-dialog>

    <!-- 编辑排班对话框 -->
    <el-dialog v-model="editDialogVisible" title="编辑排班" width="560px">
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="90px">
        <el-form-item label="护工" prop="staffId">
          <el-select
            v-model="editForm.staffId"
            filterable
            :loading="staffLoading"
            placeholder="请选择护工"
            style="width: 100%"
          >
            <el-option v-for="s in staffOptions" :key="s.id" :label="s.name + (s.positionType && positionTagMap[s.positionType] ? ' [' + positionTagMap[s.positionType].shortLabel + ']' : '')" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期" prop="scheduleDate">
          <el-date-picker v-model="editForm.scheduleDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="班次" prop="shiftType">
          <el-select v-model="editForm.shiftType" placeholder="请选择班次" style="width: 100%">
            <el-option label="早班" value="MORNING" />
            <el-option label="中班" value="AFTERNOON" />
            <el-option label="夜班" value="NIGHT" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="editForm.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSaving" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, ArrowRight, Plus, Refresh } from '@element-plus/icons-vue'
import { api } from '../../api/client'

// 岗位类型标签映射
const positionTagMap = {
  CAREGIVER: { label: '护工', shortLabel: '护工', color: '#8B5CF6' },
  MANAGER: { label: '管理人员', shortLabel: '管理', color: '#409EFF' },
  FINANCE: { label: '财务人员', shortLabel: '财务', color: '#E6A23C' },
  HR: { label: '人事专员', shortLabel: '人事', color: '#67C23A' },
  LOGISTICS: { label: '后勤人员', shortLabel: '后勤', color: '#909399' },
  OTHER: { label: '其他', shortLabel: '其他', color: '#F56C6C' }
}

// ============ 周视图相关 ============
const weekStart = ref(null)
const weekEnd = ref(null)
const weekData = ref([])
const weekLoading = ref(false)

// 计算当前周的日期范围
function initWeekRange() {
  const today = new Date()
  const dayOfWeek = today.getDay() || 7 // 周日为0，转为7
  const monday = new Date(today)
  monday.setDate(today.getDate() - dayOfWeek + 1)
  monday.setHours(0, 0, 0, 0)
  const sunday = new Date(monday)
  sunday.setDate(monday.getDate() + 6)
  sunday.setHours(23, 59, 59, 999)

  weekStart.value = formatDate(monday)
  weekEnd.value = formatDate(sunday)
}

function formatDate(date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

// 周范围显示文本
const weekRangeText = computed(() => {
  if (!weekStart.value || !weekEnd.value) return ''
  const start = new Date(weekStart.value)
  const end = new Date(weekEnd.value)
  return `${start.getMonth() + 1}/${start.getDate()} - ${end.getMonth() + 1}/${end.getDate()}`
})

// 当前月份（用于统计）
const currentMonth = computed(() => {
  if (!weekStart.value) return ''
  const date = new Date(weekStart.value)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
})

// 生成周视图的7天数据
const weekDays = computed(() => {
  const days = []
  const weekdays = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
  const start = new Date(weekStart.value)
  const today = formatDate(new Date())

  for (let i = 0; i < 7; i++) {
    const date = new Date(start)
    date.setDate(start.getDate() + i)
    const dateStr = formatDate(date)
    days.push({
      date: dateStr,
      dateText: `${date.getMonth() + 1}/${date.getDate()}`,
      weekday: weekdays[i],
      isToday: dateStr === today
    })
  }
  return days
})

// 获取护工在某天的班次
function getShift(row, date) {
  return row.days?.[date] || null
}

function getShiftLabel(shiftType) {
  const map = { MORNING: '早班', AFTERNOON: '中班', NIGHT: '夜班' }
  return map[shiftType] || shiftType
}

function getShiftType(shiftType) {
  const map = { MORNING: '', AFTERNOON: 'success', NIGHT: 'warning' }
  return map[shiftType] || ''
}

// 周导航
function prevWeek() {
  const start = new Date(weekStart.value)
  start.setDate(start.getDate() - 7)
  const end = new Date(weekEnd.value)
  end.setDate(end.getDate() - 7)
  weekStart.value = formatDate(start)
  weekEnd.value = formatDate(end)
  loadWeekView()
  loadMonthStats()
}

function nextWeek() {
  const start = new Date(weekStart.value)
  start.setDate(start.getDate() + 7)
  const end = new Date(weekEnd.value)
  end.setDate(end.getDate() + 7)
  weekStart.value = formatDate(start)
  weekEnd.value = formatDate(end)
  loadWeekView()
  loadMonthStats()
}

function currentWeek() {
  initWeekRange()
  loadWeekView()
  loadMonthStats()
}

async function loadWeekView() {
  weekLoading.value = true
  try {
    const resp = await api.get('/api/staff/schedule/week-view', {
      params: { start: weekStart.value, end: weekEnd.value }
    })
    const body = resp.data
    if (body.code === 200) {
      weekData.value = body.data || []
    } else {
      ElMessage.error(body.msg || '加载周视图失败')
    }
  } catch (e) {
    ElMessage.error(e.message || '加载周视图失败')
  } finally {
    weekLoading.value = false
  }
}

// ============ 视图切换 ============
const viewMode = ref('week')

// ============ 列表视图相关 ============
const listData = ref([])
const listTotal = ref(0)
const listPage = ref(1)
const listPageSize = ref(10)
const listLoading = ref(false)
const listFilter = reactive({
  staffId: null,
  shiftType: ''
})

async function fetchList() {
  listLoading.value = true
  try {
    const resp = await api.get('/api/staff/schedule', {
      params: {
        page: listPage.value,
        pageSize: listPageSize.value,
        staffId: listFilter.staffId || undefined,
        shiftType: listFilter.shiftType || undefined
      }
    })
    const body = resp.data
    if (body.code === 200) {
      listData.value = body.data?.list || []
      listTotal.value = body.data?.total || 0
    } else {
      ElMessage.error(body.msg || '加载列表失败')
    }
  } catch (e) {
    ElMessage.error(e.message || '加载列表失败')
  } finally {
    listLoading.value = false
  }
}

function reloadList() {
  listPage.value = 1
  fetchList()
}

function resetListFilter() {
  listFilter.staffId = null
  listFilter.shiftType = ''
  reloadList()
}

function editFromList(row) {
  editForm.staffId = row.staffId
  editForm.scheduleDate = row.scheduleDate
  editForm.shiftType = row.shiftType
  editForm.remark = row.remark || ''
  editDialogVisible.value = true
}

// ============ 月度统计 ============
const monthStats = ref([])
const statsLoading = ref(false)

async function loadMonthStats() {
  statsLoading.value = true
  try {
    const resp = await api.get('/api/staff/schedule/stats', {
      params: { month: currentMonth.value }
    })
    const body = resp.data
    if (body.code === 200) {
      monthStats.value = body.data || []
    }
  } catch (e) {
    console.error('加载月度统计失败', e)
  } finally {
    statsLoading.value = false
  }
}

// ============ 批量排班 ============
const batchDialogVisible = ref(false)
const batchFormRef = ref()
const batchSaving = ref(false)
const batchForm = reactive({
  staffIds: [],
  dateRange: [],
  shiftType: ''
})
const batchRules = {
  staffIds: [{ required: true, message: '请选择护工', trigger: 'change', type: 'array', min: 1 }],
  dateRange: [{ required: true, message: '请选择日期范围', trigger: 'change', type: 'array', len: 2 }],
  shiftType: [{ required: true, message: '请选择班次', trigger: 'change' }]
}

async function openBatchDialog() {
  batchForm.staffIds = []
  batchForm.dateRange = [weekStart.value, weekEnd.value]
  batchForm.shiftType = ''
  batchDialogVisible.value = true
  await loadAllStaff()
}

async function submitBatch() {
  if (!batchFormRef.value) return
  await batchFormRef.value.validate()

  const [startDate, endDate] = batchForm.dateRange
  batchSaving.value = true

  try {
    let totalCount = 0
    for (const staffId of batchForm.staffIds) {
      const resp = await api.post('/api/staff/schedule/batch', {
        staffId,
        startDate,
        endDate,
        shiftType: batchForm.shiftType
      })
      const body = resp.data
      if (body.code === 200) {
        totalCount += body.data || 0
      }
    }
    ElMessage.success(`批量排班成功，共安排 ${totalCount} 个班次`)
    batchDialogVisible.value = false
    refresh()
  } catch (e) {
    ElMessage.error(e.message || '批量排班失败')
  } finally {
    batchSaving.value = false
  }
}

// ============ 快捷编辑 ============
const quickEditVisible = ref(false)
const quickEditData = reactive({
  staffId: null,
  staffName: '',
  date: '',
  currentShift: null
})

function openQuickEdit(row, day) {
  quickEditData.staffId = row.staffId
  quickEditData.staffName = row.staffName
  quickEditData.date = day.date
  quickEditData.currentShift = getShift(row, day.date)
  quickEditVisible.value = true
}

async function quickSetShift(shiftType) {
  try {
    const resp = await api.post('/api/staff/schedule', {
      staffId: quickEditData.staffId,
      scheduleDate: quickEditData.date,
      shiftType,
      remark: ''
    })
    const body = resp.data
    if (body.code === 200) {
      ElMessage.success('排班成功')
      quickEditVisible.value = false
      loadWeekView()
      loadMonthStats()
    } else {
      ElMessage.error(body.msg || '排班失败')
    }
  } catch (e) {
    ElMessage.error(e.message || '排班失败')
  }
}

async function quickDelete() {
  // 需要找到排班ID
  try {
    const resp = await api.get('/api/staff/schedule', {
      params: {
        staffId: quickEditData.staffId,
        startDate: quickEditData.date,
        endDate: quickEditData.date,
        page: 1,
        pageSize: 1
      }
    })
    const body = resp.data
    if (body.code === 200 && body.data?.list?.length > 0) {
      const scheduleId = body.data.list[0].id
      await api.delete(`/api/staff/schedule/${scheduleId}`)
      ElMessage.success('删除成功')
      quickEditVisible.value = false
      loadWeekView()
      loadMonthStats()
    }
  } catch (e) {
    ElMessage.error(e.message || '删除失败')
  }
}

// ============ 编辑排班 ============
const editDialogVisible = ref(false)
const editFormRef = ref()
const editSaving = ref(false)
const editForm = reactive({
  staffId: null,
  scheduleDate: '',
  shiftType: 'MORNING',
  remark: ''
})
const editRules = {
  staffId: [{ required: true, message: '请选择护工', trigger: 'change' }],
  scheduleDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  shiftType: [{ required: true, message: '请选择班次', trigger: 'change' }]
}

async function submitEdit() {
  if (!editFormRef.value) return
  await editFormRef.value.validate()
  editSaving.value = true
  try {
    const resp = await api.post('/api/staff/schedule', {
      staffId: editForm.staffId,
      scheduleDate: editForm.scheduleDate,
      shiftType: editForm.shiftType,
      remark: editForm.remark || null
    })
    const body = resp.data
    if (body.code === 200) {
      ElMessage.success('保存成功')
      editDialogVisible.value = false
      refresh()
    } else {
      ElMessage.error(body.msg || '保存失败')
    }
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    editSaving.value = false
  }
}

// ============ 删除 ============
async function remove(row) {
  try {
    await ElMessageBox.confirm('确认删除该排班记录？', '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    const resp = await api.delete(`/api/staff/schedule/${row.id}`)
    const body = resp.data
    if (body.code === 200) {
      ElMessage.success('删除成功')
      refresh()
    } else {
      ElMessage.error(body.msg || '删除失败')
    }
  } catch (e) {
    ElMessage.error(e.message || '删除失败')
  }
}

// ============ 护工选项 ============
const staffLoading = ref(false)
const staffOptions = ref([])
const allStaffOptions = ref([])

async function searchStaff(query) {
  staffLoading.value = true
  try {
    const resp = await api.get('/api/staff/options', { params: { keyword: query || undefined } })
    const body = resp.data
    if (body.code === 200) {
      staffOptions.value = body.data || []
    }
  } catch (e) {
    staffOptions.value = []
  } finally {
    staffLoading.value = false
  }
}

async function loadAllStaff() {
  staffLoading.value = true
  try {
    const resp = await api.get('/api/staff/options')
    const body = resp.data
    if (body.code === 200) {
      allStaffOptions.value = body.data || []
    }
  } catch (e) {
    allStaffOptions.value = []
  } finally {
    staffLoading.value = false
  }
}

// ============ 刷新 ============
function refresh() {
  loadWeekView()
  if (viewMode.value === 'list') {
    fetchList()
  }
  loadMonthStats()
}

// ============ 初始化 ============
onMounted(() => {
  initWeekRange()
  loadWeekView()
  loadMonthStats()
  searchStaff('')
  fetchList()
})
</script>

<style scoped>
.page {
  padding: 16px;
}

.toolbar-card {
  margin-bottom: 16px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;
}

.week-nav {
  display: flex;
  align-items: center;
  gap: 12px;
}

.week-range {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}

.view-toggle {
  flex: 1;
  display: flex;
  justify-content: center;
}

.actions {
  display: flex;
  gap: 10px;
}

.week-view-card {
  margin-bottom: 16px;
}

.week-table-wrapper {
  overflow-x: auto;
}

.week-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 800px;
}

.week-table th,
.week-table td {
  border: 1px solid #ebeef5;
  padding: 12px 8px;
  text-align: center;
}

.week-table th {
  background-color: #f5f7fa;
  font-weight: 600;
  color: #606266;
}

.week-table th.today,
.week-table td.today {
  background-color: #ecf5ff;
}

.staff-header {
  width: 120px;
  min-width: 120px;
}

.staff-name {
  font-weight: 500;
  color: #303133;
  background-color: #fafafa;
  white-space: nowrap;
}

.day-header {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.day-header .weekday {
  font-size: 14px;
}

.day-header .date {
  font-size: 12px;
  color: #909399;
}

.shift-cell {
  cursor: pointer;
  min-height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.shift-cell:hover {
  background-color: #f5f7fa;
  border-radius: 4px;
}

.shift-cell .rest {
  color: #c0c4cc;
  font-size: 14px;
}

.empty-tip {
  padding: 40px 0;
}

.list-view-card {
  margin-bottom: 16px;
}

.list-filter {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.pager {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}

.stats-card {
  margin-bottom: 16px;
}

.stats-header {
  font-weight: 600;
  color: #303133;
}

.quick-edit-info {
  margin-bottom: 20px;
  padding: 12px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.quick-edit-info p {
  margin: 8px 0;
}

.quick-edit-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.quick-edit-actions .el-button {
  flex: 1;
  min-width: 100px;
}
</style>
