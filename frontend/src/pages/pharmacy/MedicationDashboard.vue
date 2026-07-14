<template>
  <div class="med-page" v-loading="loading">
    <template v-if="!showPrintReport">
    <!-- 统计卡片 -->
    <div class="med-stat-row med-stat-row-4">
      <div class="med-stat-card primary">
        <div class="med-stat-icon"><el-icon><User /></el-icon></div>
        <div class="med-stat-body">
          <div class="med-stat-label">需服药人数</div>
          <div class="med-stat-value">{{ stats.total }}</div>
        </div>
      </div>
      <div class="med-stat-card success">
        <div class="med-stat-icon"><el-icon><Check /></el-icon></div>
        <div class="med-stat-body">
          <div class="med-stat-label">已确认</div>
          <div class="med-stat-value">{{ stats.allDone }}</div>
        </div>
      </div>
      <div class="med-stat-card blue">
        <div class="med-stat-icon"><el-icon><Clock /></el-icon></div>
        <div class="med-stat-body">
          <div class="med-stat-label">待执行</div>
          <div class="med-stat-value">{{ stats.hasPending }}</div>
        </div>
      </div>
      <div class="med-stat-card danger">
        <div class="med-stat-icon"><el-icon><Warning /></el-icon></div>
        <div class="med-stat-body">
          <div class="med-stat-label">漏服/拒服</div>
          <div class="med-stat-value">{{ stats.hasMissedOrRefused }}</div>
        </div>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="med-filter-bar">
      <div class="med-filter-left">
        <div class="med-switcher">
          <button class="med-switcher-btn" :class="{ active: viewFilter === 'ALL' }" @click="switchView('ALL')">全部</button>
          <button class="med-switcher-btn" :class="{ active: viewFilter === 'PENDING' }" @click="switchView('PENDING')">待执行</button>
          <button class="med-switcher-btn" :class="{ active: viewFilter === 'DONE' }" @click="switchView('DONE')">已完成</button>
          <button class="med-switcher-btn" :class="{ active: viewFilter === 'ABNORMAL' }" @click="switchView('ABNORMAL')">异常</button>
        </div>
        <el-date-picker
          v-model="selectedDate"
          type="date"
          value-format="YYYY-MM-DD"
          :clearable="false"
          style="width: 160px"
          @change="loadBoard"
        />
        <el-select v-model="timeSlotFilter" placeholder="时段筛选" clearable style="width: 120px" @change="applyFilters">
          <el-option label="全部时段" value="" />
          <el-option label="早晨" value="MORNING" />
          <el-option label="中午" value="AFTERNOON" />
          <el-option label="晚上" value="EVENING" />
          <el-option label="睡前" value="BEDTIME" />
        </el-select>
      </div>
      <div class="med-filter-right">
        <el-button @click="loadBoard"><el-icon><Refresh /></el-icon> 刷新</el-button>
        <el-button type="primary" @click="batchConfirm"><el-icon><Check /></el-icon> 批量确认</el-button>
        <el-button type="success" @click="openExport"><el-icon><Printer /></el-icon> 导出日报</el-button>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="filteredBoard.length === 0 && !loading" class="med-empty-state">
      <el-empty description="暂无用药记录" />
    </div>

    <!-- 老人卡片网格 -->
    <div class="med-elderly-grid">
      <div
        v-for="elderly in filteredBoard"
        :key="elderly.elderlyId"
        class="med-elderly-card"
      >
        <!-- 卡片头部 -->
        <div class="med-elderly-card-header">
          <div class="med-elderly-card-name">
            {{ elderly.elderlyName }}
            <span class="med-elderly-card-room">
              {{ supplyCategoryLabel(elderly.supplyCategory) }}{{ elderly.bedNumber ? '·' + elderly.bedNumber : '' }}
            </span>
          </div>
          <div class="med-elderly-card-status">
            <span class="med-pill" :class="elderlyStatusPill(elderly)">{{ elderlyStatusLabel(elderly) }}</span>
          </div>
        </div>

        <!-- 卡片主体：时段列表 -->
        <div class="med-elderly-card-body">
          <div
            v-for="slot in elderly.timeSlots"
            :key="slot.slot"
            class="med-slot-row"
          >
            <div class="med-slot-label-col">
              <span class="med-slot-label" :class="slotLabelClass(slot.slot)">{{ slot.slotLabel }}</span>
              <span class="slot-icon" :class="slotIconClass(slot)">
                <el-icon v-if="slot.overallStatus === 'ALL_DONE'"><Check /></el-icon>
                <el-icon v-else-if="hasStatus(slot, 'MISSED')"><Close /></el-icon>
                <el-icon v-else-if="hasStatus(slot, 'REFUSED')"><CircleClose /></el-icon>
                <el-icon v-else-if="slot.overallStatus === 'PARTIAL'"><Loading /></el-icon>
                <el-icon v-else><Clock /></el-icon>
              </span>
            </div>
            <div class="med-slot-records-col">
              <div
                v-for="record in slot.records"
                :key="record.id"
                class="med-drug-item"
                :class="{
                  'med-drug-clickable': record.status === 'PENDING',
                  'med-drug-done': record.status === 'DONE',
                  'med-drug-missed': record.status === 'MISSED',
                  'med-drug-refused': record.status === 'REFUSED',
                  'med-drug-skipped': record.status === 'SKIPPED'
                }"
                @click="record.status === 'PENDING' ? openConfirmDialog(record) : null"
              >
                <span class="med-drug-name">{{ record.drugName }} {{ record.dosage }}</span>
                <span v-if="record.status === 'DONE'" class="med-drug-status med-drug-meta-done">
                  {{ record.executorName }} · {{ formatTime(record.executedAt) }}
                </span>
                <span v-else-if="record.status === 'SKIPPED'" class="med-drug-status med-drug-meta-skip">
                  跳过{{ record.skipReason ? '·' + record.skipReason : '' }}
                </span>
                <span v-else-if="record.status === 'REFUSED'" class="med-drug-status med-drug-meta-refuse">
                  拒服{{ record.skipReason ? '·' + record.skipReason : '' }}
                </span>
                <span v-else-if="record.status === 'MISSED'" class="med-drug-status med-drug-meta-miss">
                  漏服
                </span>
                <span v-else class="med-drug-status med-drug-meta-pending">点击确认</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 确认/跳过/拒服对话框 -->
    <el-dialog v-model="dialogVisible" title="用药执行确认" width="500px" custom-class="med-dialog" @closed="resetDialog">
      <div v-if="currentRecord">
        <!-- 药品信息 -->
        <div class="med-form-section">
          <div class="med-section-title">药品信息</div>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="药品">{{ currentRecord.drugName }}</el-descriptions-item>
            <el-descriptions-item label="剂量">{{ currentRecord.dosage || '-' }}</el-descriptions-item>
            <el-descriptions-item label="时段">{{ slotLabelMap(currentRecord.timeSlot) }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <span class="med-pill" :class="recordStatusPill(currentRecord.status)">{{ recordStatusLabel(currentRecord.status) }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 执行护工 (confirm) -->
        <div v-if="dialogAction === 'confirm'" class="med-form-section">
          <div class="med-section-title">执行护工</div>
          <el-select
            v-model="executorId"
            filterable
            remote
            clearable
            :remote-method="searchStaff"
            :loading="staffLoading"
            placeholder="选择护工或输入姓名"
            style="width: 100%"
          >
            <el-option v-for="s in staffOptions" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
          <el-input
            v-if="!executorId"
            v-model="executorName"
            placeholder="或手动输入护工姓名"
            style="margin-top: 8px"
          />
        </div>

        <!-- 原因 (skip/refuse) -->
        <div v-if="dialogAction === 'skip' || dialogAction === 'refuse'" class="med-form-section">
          <div class="med-section-title">{{ dialogAction === 'skip' ? '跳过原因' : '拒服原因' }}</div>
          <el-input v-model="reason" type="textarea" :rows="3" placeholder="请输入原因（可选）" />
        </div>
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button v-if="dialogAction !== 'refuse'" @click="setDialogAction('refuse')" type="warning" plain>拒服</el-button>
        <el-button v-if="dialogAction !== 'skip'" @click="setDialogAction('skip')" plain>跳过</el-button>
        <el-button v-if="dialogAction !== 'confirm'" @click="setDialogAction('confirm')" type="primary" plain>确认服药</el-button>
        <el-button v-if="dialogAction === 'confirm'" type="primary" :loading="saving" @click="submitConfirm">确认服药</el-button>
        <el-button v-if="dialogAction === 'skip'" type="primary" :loading="saving" @click="submitSkip">确认跳过</el-button>
        <el-button v-if="dialogAction === 'refuse'" type="warning" :loading="saving" @click="submitRefuse">确认拒服</el-button>
      </template>
    </el-dialog>

    <!-- 批量确认对话框 -->
    <el-dialog v-model="batchDialogVisible" title="批量确认用药" width="500px" custom-class="med-dialog" @closed="resetBatchDialog">
      <div class="med-form-section">
        <div class="med-section-title">确认信息</div>
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="确认日期">{{ selectedDate }}</el-descriptions-item>
          <el-descriptions-item label="待确认记录数">{{ pendingCount }} 条</el-descriptions-item>
        </el-descriptions>
      </div>
      <div class="med-form-section">
        <div class="med-section-title">执行护工</div>
        <el-select
          v-model="batchExecutorId"
          filterable
          remote
          clearable
          :remote-method="searchStaff"
          :loading="staffLoading"
          placeholder="选择护工或输入姓名"
          style="width: 100%"
        >
          <el-option v-for="s in staffOptions" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
        <el-input
          v-if="!batchExecutorId"
          v-model="batchExecutorName"
          placeholder="或手动输入护工姓名"
          style="margin-top: 8px"
        />
      </div>
      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchSaving" @click="submitBatchConfirm">确认批量执行</el-button>
      </template>
    </el-dialog>
    </template>

    <!-- 打印报表视图 -->
    <div v-if="showPrintReport" class="med-print-view">
      <div class="med-print-toolbar no-print">
        <div class="med-print-toolbar-left">
          <h2>{{ selectedDate }} 每日用药执行报表</h2>
          <span class="med-print-toolbar-hint">可点击"导出PDF"一键下载，或"打印"通过打印机输出</span>
        </div>
        <div class="med-print-toolbar-right">
          <el-button @click="showPrintReport = false">返回</el-button>
          <el-button type="success" @click="doPdfExport"><el-icon><Download /></el-icon> 导出PDF</el-button>
          <el-button type="primary" @click="doPrintExport"><el-icon><Printer /></el-icon> 打印</el-button>
        </div>
      </div>
      <div class="med-print-scroll">
        <div class="med-print-area" id="med-daily-export-page">
          <div class="med-print-header">
            <div class="med-print-institution">岳西县鸿福养老服务中心</div>
            <div class="med-print-title">每日用药执行报表</div>
          </div>
          <div class="med-print-divider"></div>
          <div class="med-print-filter-row">
            <span>{{ exportFilterDesc }}</span>
            <span>导出时间: {{ exportTime }}</span>
          </div>
          <div class="med-print-stats">
            <div class="med-print-stat-item">
              <div class="med-print-stat-label">需服药人数</div>
              <div class="med-print-stat-value">{{ exportStats.total }}</div>
            </div>
            <div class="med-print-stat-item">
              <div class="med-print-stat-label">已确认</div>
              <div class="med-print-stat-value">{{ exportStats.allDone }}</div>
            </div>
            <div class="med-print-stat-item">
              <div class="med-print-stat-label">待执行</div>
              <div class="med-print-stat-value">{{ exportStats.hasPending }}</div>
            </div>
            <div class="med-print-stat-item">
              <div class="med-print-stat-label">漏服/拒服</div>
              <div class="med-print-stat-value">{{ exportStats.hasMissedOrRefused }}</div>
            </div>
          </div>
          <div v-if="flatPrintData.length === 0" class="med-print-empty">
            暂无用药记录
          </div>
          <table v-else class="med-print-table">
            <thead>
              <tr>
                <th style="width:40px">序号</th>
                <th style="width:70px">老人</th>
                <th style="width:110px">房间/床位</th>
                <th style="width:55px">时段</th>
                <th style="width:150px">药品名称</th>
                <th style="width:70px">剂量</th>
                <th style="width:70px">用药状态</th>
                <th style="width:70px">执行护工</th>
                <th style="width:70px">执行时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, idx) in flatPrintData" :key="idx">
                <td class="med-print-tc">{{ idx + 1 }}</td>
                <td>{{ row.elderlyName }}</td>
                <td>{{ supplyCategoryLabel(row.supplyCategory) }}{{ row.bedNumber ? ' · ' + row.bedNumber + '床' : '' }}</td>
                <td class="med-print-tc">{{ row.slotLabel }}</td>
                <td>{{ row.drugName }}</td>
                <td class="med-print-tc">{{ row.dosage || '-' }}</td>
                <td class="med-print-tc">
                  <span class="med-print-status" :class="'st-' + row.status.toLowerCase()">{{ exportStatusLabel(row.status) }}</span>
                </td>
                <td class="med-print-tc">{{ row.executorName || '-' }}</td>
                <td class="med-print-tc">{{ formatTime(row.executedAt) }}</td>
              </tr>
            </tbody>
          </table>
          <div class="med-print-footer no-print">
            <span>e-Hfnew 养老管理系统</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Close, CircleClose, Clock, Loading, Refresh } from '@element-plus/icons-vue'
import { api } from '../../api/client'
import { useFormat } from '@/composables/useFormat'

const { supplyCategoryLabel } = useFormat()

const loading = ref(false)
const saving = ref(false)
const board = ref([])
const selectedDate = ref(todayStr())
const viewFilter = ref('ALL')
const timeSlotFilter = ref('')

/* ---- Stats ---- */
const stats = computed(() => {
  const total = board.value.length
  let allDone = 0
  let hasPending = 0
  let hasMissedOrRefused = 0

  for (const elderly of board.value) {
    const allRecords = elderly.timeSlots.flatMap(s => s.records || [])
    if (allRecords.length === 0) continue

    const statuses = allRecords.map(r => r.status)
    if (statuses.every(s => s === 'DONE')) allDone++
    if (statuses.some(s => s === 'PENDING')) hasPending++
    if (statuses.some(s => s === 'MISSED' || s === 'REFUSED')) hasMissedOrRefused++
  }

  return { total, allDone, hasPending, hasMissedOrRefused }
})

/* ---- Filtered Board ---- */
const filteredBoard = computed(() => {
  let result = board.value

  // View filter
  if (viewFilter.value === 'PENDING') {
    result = result.filter(e => {
      const allRecords = e.timeSlots.flatMap(s => s.records || [])
      return allRecords.some(r => r.status === 'PENDING')
    })
  } else if (viewFilter.value === 'DONE') {
    result = result.filter(e => {
      const allRecords = e.timeSlots.flatMap(s => s.records || [])
      return allRecords.length > 0 && allRecords.every(r => r.status === 'DONE')
    })
  } else if (viewFilter.value === 'ABNORMAL') {
    result = result.filter(e => {
      const allRecords = e.timeSlots.flatMap(s => s.records || [])
      return allRecords.some(r => r.status === 'MISSED' || r.status === 'REFUSED')
    })
  }

  // Time slot filter
  if (timeSlotFilter.value) {
    result = result.map(e => ({
      ...e,
      timeSlots: e.timeSlots.filter(s => s.slot === timeSlotFilter.value)
    })).filter(e => e.timeSlots.length > 0)
  }

  return result
})

function switchView(val) {
  viewFilter.value = val
}

function applyFilters() {
  // computed already handles it
}

/* ---- Helpers ---- */
function todayStr() {
  const d = new Date()
  const yyyy = d.getFullYear()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd}`
}

function slotLabelMap(slot) {
  const map = { MORNING: '早晨', AFTERNOON: '中午', EVENING: '晚上', BEDTIME: '睡前' }
  return map[slot] || slot
}

function slotLabelClass(slot) {
  const map = { MORNING: 'morning', AFTERNOON: 'afternoon', EVENING: 'evening', BEDTIME: 'bedtime' }
  return map[slot] || ''
}

function hasStatus(slot, status) {
  return (slot.records || []).some(r => r.status === status)
}

function slotIconClass(slot) {
  if (slot.overallStatus === 'ALL_DONE') return 'icon-done'
  if (hasStatus(slot, 'MISSED')) return 'icon-missed'
  if (hasStatus(slot, 'REFUSED')) return 'icon-refused'
  if (slot.overallStatus === 'PARTIAL') return 'icon-partial'
  return 'icon-pending'
}

function elderlyStatusPill(elderly) {
  const allRecords = elderly.timeSlots.flatMap(s => s.records || [])
  if (allRecords.length === 0) return 'gray'
  if (allRecords.every(r => r.status === 'DONE')) return 'green'
  if (allRecords.some(r => r.status === 'MISSED' || r.status === 'REFUSED')) return 'red'
  if (allRecords.some(r => r.status === 'PENDING')) return 'blue'
  return 'gray'
}

function elderlyStatusLabel(elderly) {
  const allRecords = elderly.timeSlots.flatMap(s => s.records || [])
  if (allRecords.length === 0) return '无记录'
  if (allRecords.every(r => r.status === 'DONE')) return '已完成'
  if (allRecords.some(r => r.status === 'MISSED' || r.status === 'REFUSED')) return '有异常'
  if (allRecords.some(r => r.status === 'PENDING')) return '待执行'
  return '部分完成'
}

function recordStatusLabel(status) {
  const map = { PENDING: '待执行', DONE: '已确认', SKIPPED: '已跳过', REFUSED: '拒服', MISSED: '漏服' }
  return map[status] || status
}

function recordStatusPill(status) {
  const map = { PENDING: 'blue', DONE: 'green', SKIPPED: 'gray', REFUSED: 'orange', MISSED: 'red' }
  return map[status] || 'gray'
}

function formatTime(dt) {
  if (!dt) return '-'
  const str = String(dt)
  const timePart = str.includes('T') ? str.split('T')[1] : str.split(' ')[1]
  if (!timePart) return '-'
  return timePart.substring(0, 5)
}

/* ---- Data Loading ---- */
async function loadBoard() {
  loading.value = true
  try {
    const resp = await api.get('/api/medication/records/today', {
      params: { date: selectedDate.value }
    })
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '加载失败')
    board.value = body.data || []
  } catch (e) {
    ElMessage.error(e.message || '加载用药看板失败')
  } finally {
    loading.value = false
  }
}

/* ---- Batch Confirm ---- */
const batchDialogVisible = ref(false)
const batchSaving = ref(false)
const batchExecutorId = ref(null)
const batchExecutorName = ref('')
const pendingCount = ref(0)

async function batchConfirm() {
  const pendingRecords = board.value.flatMap(e =>
    e.timeSlots.flatMap(s => (s.records || []).filter(r => r.status === 'PENDING'))
  )
  if (pendingRecords.length === 0) {
    ElMessage.info('没有待确认的记录')
    return
  }
  pendingCount.value = pendingRecords.length
  batchExecutorId.value = null
  batchExecutorName.value = ''
  searchStaff('')
  batchDialogVisible.value = true
}

function resetBatchDialog() {
  batchExecutorId.value = null
  batchExecutorName.value = ''
  pendingCount.value = 0
}

async function submitBatchConfirm() {
  const execName = batchExecutorId.value
    ? (staffOptions.value.find(s => s.id === batchExecutorId.value)?.name || '')
    : batchExecutorName.value.trim()
  if (!execName) {
    ElMessage.error('请选择或输入执行护工')
    return
  }
  batchSaving.value = true
  try {
    const resp = await api.put('/api/medication/records/batch-confirm', {
      date: selectedDate.value,
      executorId: batchExecutorId.value || null,
      executorName: execName
    })
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '批量确认失败')
    ElMessage.success(`已批量确认 ${pendingCount.value} 条记录`)
    batchDialogVisible.value = false
    await loadBoard()
  } catch (e) {
    ElMessage.error(e.message || '批量确认失败')
  } finally {
    batchSaving.value = false
  }
}

/* ---- Staff Search ---- */
const staffLoading = ref(false)
const staffOptions = ref([])

async function searchStaff(query) {
  staffLoading.value = true
  try {
    const resp = await api.get('/api/staff/options', { params: { keyword: query || undefined } })
    const body = resp.data
    if (body.code === 200) staffOptions.value = body.data || []
    else staffOptions.value = []
  } catch {
    staffOptions.value = []
  } finally {
    staffLoading.value = false
  }
}

/* ---- Dialog ---- */
const dialogVisible = ref(false)
const dialogAction = ref('confirm')
const currentRecord = ref(null)
const executorId = ref(null)
const executorName = ref('')
const reason = ref('')

function openConfirmDialog(record) {
  currentRecord.value = record
  dialogAction.value = 'confirm'
  executorId.value = null
  executorName.value = ''
  reason.value = ''
  dialogVisible.value = true
  searchStaff('')
}

function setDialogAction(action) {
  dialogAction.value = action
}

function resetDialog() {
  currentRecord.value = null
  dialogAction.value = 'confirm'
  executorId.value = null
  executorName.value = ''
  reason.value = ''
}

async function submitConfirm() {
  if (!currentRecord.value) return
  const execName = executorId.value
    ? (staffOptions.value.find(s => s.id === executorId.value)?.name || '')
    : executorName.value.trim()
  if (!execName) {
    ElMessage.error('请选择或输入执行护工')
    return
  }
  saving.value = true
  try {
    const resp = await api.put(`/api/medication/records/${currentRecord.value.id}/confirm`, {
      executorId: executorId.value || null,
      executorName: execName
    })
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '确认失败')
    ElMessage.success('已确认服药')
    dialogVisible.value = false
    await loadBoard()
  } catch (e) {
    ElMessage.error(e.message || '确认失败')
  } finally {
    saving.value = false
  }
}

async function submitSkip() {
  if (!currentRecord.value) return
  saving.value = true
  try {
    const resp = await api.put(`/api/medication/records/${currentRecord.value.id}/skip`, {
      reason: reason.value || null
    })
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '跳过失败')
    ElMessage.success('已跳过')
    dialogVisible.value = false
    await loadBoard()
  } catch (e) {
    ElMessage.error(e.message || '跳过失败')
  } finally {
    saving.value = false
  }
}

async function submitRefuse() {
  if (!currentRecord.value) return
  saving.value = true
  try {
    const resp = await api.put(`/api/medication/records/${currentRecord.value.id}/refuse`, {
      reason: reason.value || null
    })
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '拒服失败')
    ElMessage.success('已记录拒服')
    dialogVisible.value = false
    await loadBoard()
  } catch (e) {
    ElMessage.error(e.message || '拒服失败')
  } finally {
    saving.value = false
  }
}

/* ---- Export ---- */
const exportVisible = ref(false)
const exportData = ref([])
const exportTime = ref('')
const showPrintReport = ref(false)

const exportFilterDesc = computed(() => {
  const parts = [`日期: ${selectedDate.value}`]
  if (viewFilter.value === 'PENDING') parts.push('视图: 待执行')
  else if (viewFilter.value === 'DONE') parts.push('视图: 已完成')
  else if (viewFilter.value === 'ABNORMAL') parts.push('视图: 异常')
  if (timeSlotFilter.value) parts.push(`时段: ${slotLabelMap(timeSlotFilter.value)}`)
  return parts.join('  |  ')
})

const exportStats = computed(() => {
  const data = exportData.value
  const total = data.length
  let allDone = 0, hasPending = 0, hasMissedOrRefused = 0
  for (const elderly of data) {
    const allRecords = (elderly.timeSlots || []).flatMap(s => s.records || [])
    if (allRecords.length === 0) continue
    const statuses = allRecords.map(r => r.status)
    if (statuses.every(s => s === 'DONE')) allDone++
    if (statuses.some(s => s === 'PENDING')) hasPending++
    if (statuses.some(s => s === 'MISSED' || s === 'REFUSED')) hasMissedOrRefused++
  }
  return { total, allDone, hasPending, hasMissedOrRefused }
})

const flatPrintData = computed(() => {
  const rows = []
  for (const elderly of exportData.value) {
    for (const slot of (elderly.timeSlots || [])) {
      for (const record of (slot.records || [])) {
        rows.push({
          elderlyName: elderly.elderlyName,
          supplyCategory: elderly.supplyCategory,
          bedNumber: elderly.bedNumber,
          slotLabel: slot.slotLabel,
          slot: slot.slot,
          ...record
        })
      }
    }
  }
  return rows
})

function openExport() {
  // 使用当前筛选后的看板数据直接导出
  exportData.value = JSON.parse(JSON.stringify(filteredBoard.value))
  const now = new Date()
  exportTime.value = `${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,'0')}-${String(now.getDate()).padStart(2,'0')} ${String(now.getHours()).padStart(2,'0')}:${String(now.getMinutes()).padStart(2,'0')}`
  showPrintReport.value = true
}

function exportStatusLabel(status) {
  const map = { PENDING: '待执行', DONE: '已确认', SKIPPED: '已跳过', REFUSED: '拒服', MISSED: '漏服' }
  return map[status] || status
}

function doPrintExport() {
  window.print()
}

function doPdfExport() {
  // 获取报表内容DOM，在新窗口中以纯净模式打印（浏览器支持"另存为PDF"）
  const printArea = document.getElementById('med-daily-export-page')
  if (!printArea) {
    ElMessage.warning('未找到报表内容')
    return
  }
  const win = window.open('', '_blank', 'width=900,height=700')
  if (!win) {
    ElMessage.warning('请允许弹出窗口以导出PDF')
    return
  }
  // 收集页面中 medication-theme 的样式
  let themeCss = ''
  try {
    themeCss = Array.from(document.styleSheets)
      .filter(s => !s.href || s.href.includes('medication-theme'))
      .map(s => {
        try { return Array.from(s.cssRules).map(r => r.cssText).join('\n') }
        catch { return '' }
      }).join('\n')
  } catch { /* ignore */ }

  win.document.write(`<!DOCTYPE html>
<html><head><meta charset="utf-8"><title>每日用药执行报表</title>
<style>
  @page { size: A4 portrait; margin: 12mm; }
  body { margin: 0; font-family: "Microsoft YaHei","PingFang SC",sans-serif; font-size:12px; color:#333; }
  ${themeCss}
  @media print {
    .no-print { display: none !important; }
    body { -webkit-print-color-adjust: exact; print-color-adjust: exact; }
  }
</style></head>
<body>${printArea.outerHTML}</body></html>`)
  win.document.close()
  // 等渲染完成后自动弹出打印对话框
  setTimeout(() => {
    win.print()
    // 打印对话框关闭后自动关闭窗口
    win.addEventListener('afterprint', () => { win.close() })
  }, 600)
}

/* ---- Auto-refresh ---- */
let refreshTimer = null

onMounted(() => {
  loadBoard()
  refreshTimer = setInterval(() => {
    loadBoard()
  }, 60000)
})

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
})
</script>

<style scoped>
/* ---- 空状态 ---- */
.med-empty-state {
  display: flex;
  justify-content: center;
  padding: 60px 0;
}

/* ---- 老人卡片网格 ---- */
.med-elderly-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

@media (max-width: 1200px) {
  .med-elderly-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .med-elderly-grid {
    grid-template-columns: 1fr;
  }
}

/* ---- Slot Icon ---- */
.slot-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  font-size: 16px;
  margin-top: 6px;
}

.icon-done {
  background: rgba(82, 196, 26, 0.12);
  color: #52C41A;
}

.icon-pending {
  background: #f5f5f5;
  color: #c0c4cc;
  border: 1px dashed #c0c4cc;
}

.icon-missed {
  background: rgba(245, 108, 108, 0.12);
  color: #F56C6C;
}

.icon-refused {
  background: rgba(230, 162, 60, 0.12);
  color: #E6A23C;
}

.icon-partial {
  background: rgba(59, 130, 246, 0.12);
  color: #3B82F6;
}

/* ---- Drug Status Variants ---- */
.med-drug-clickable {
  cursor: pointer;
  transition: background 0.2s;
}

.med-drug-clickable:hover {
  background: rgba(43, 122, 120, 0.06);
}

.med-drug-meta-pending {
  color: #2B7A78;
  font-weight: 500;
}

.med-drug-done .med-drug-name {
  color: #52C41A;
}

.med-drug-meta-done {
  color: #52C41A;
  font-size: 12px;
}

.med-drug-missed .med-drug-name {
  color: #F56C6C;
  text-decoration: line-through;
}

.med-drug-meta-miss {
  color: #F56C6C;
  font-weight: 500;
  font-size: 12px;
}

.med-drug-refused .med-drug-name {
  color: #E6A23C;
}

.med-drug-meta-refuse {
  color: #E6A23C;
  font-size: 12px;
}

.med-drug-skipped .med-drug-name {
  color: #6B7280;
}

.med-drug-meta-skip {
  color: #6B7280;
  font-size: 12px;
}
</style>
