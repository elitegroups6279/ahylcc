<template>
  <div class="med-page">
    <!-- 统计卡片 -->
    <div class="med-stat-row med-stat-row-4">
      <div class="med-stat-card primary">
        <div class="med-stat-icon"><el-icon><Document /></el-icon></div>
        <div class="med-stat-body">
          <div class="med-stat-label">总记录数</div>
          <div class="med-stat-value">{{ stats.total }}</div>
        </div>
      </div>
      <div class="med-stat-card success">
        <div class="med-stat-icon"><el-icon><Check /></el-icon></div>
        <div class="med-stat-body">
          <div class="med-stat-label">已确认</div>
          <div class="med-stat-value">{{ stats.done }}</div>
        </div>
      </div>
      <div class="med-stat-card warning">
        <div class="med-stat-icon"><el-icon><Warning /></el-icon></div>
        <div class="med-stat-body">
          <div class="med-stat-label">待执行/漏服</div>
          <div class="med-stat-value">{{ stats.pendingOrMissed }}</div>
        </div>
      </div>
      <div class="med-stat-card danger">
        <div class="med-stat-icon"><el-icon><CircleClose /></el-icon></div>
        <div class="med-stat-body">
          <div class="med-stat-label">拒服</div>
          <div class="med-stat-value">{{ stats.refused }}</div>
        </div>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="med-filter-bar">
      <div class="med-filter-left">
        <el-select
          v-model="filterElderlyId"
          filterable
          clearable
          placeholder="选择老人"
          style="width: 160px"
          @change="reload"
        >
          <el-option v-for="e in elderlyOptions" :key="e.id" :label="`${e.name}${e.unique_no || e.uniqueNo ? '（' + (e.unique_no || e.uniqueNo) + '）' : ''}`" :value="e.id" />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 260px"
          @change="reload"
        />
        <el-select v-model="filterTimeSlot" placeholder="时段" clearable style="width: 120px" @change="reload">
          <el-option label="全部时段" value="" />
          <el-option label="早晨" value="MORNING" />
          <el-option label="中午" value="AFTERNOON" />
          <el-option label="晚上" value="EVENING" />
          <el-option label="睡前" value="BEDTIME" />
        </el-select>
        <el-select v-model="filterStatus" placeholder="状态" clearable style="width: 120px" @change="reload">
          <el-option label="全部状态" value="" />
          <el-option label="待执行" value="PENDING" />
          <el-option label="已确认" value="DONE" />
          <el-option label="已跳过" value="SKIPPED" />
          <el-option label="拒服" value="REFUSED" />
          <el-option label="漏服" value="MISSED" />
        </el-select>
        <el-input
          v-model="filterKeyword"
          placeholder="搜索药品名称"
          clearable
          style="width: 160px"
          @keyup.enter="reload"
          @clear="reload"
        />
      </div>
      <div class="med-filter-right">
        <el-button @click="fetchList"><el-icon><Refresh /></el-icon> 刷新</el-button>
        <el-button type="success" @click="exportExcel" :loading="exportingExcel">
          <el-icon><Download /></el-icon> 导出Excel
        </el-button>
        <el-button type="primary" @click="openExportPreview"><el-icon><Printer /></el-icon> 导出A4</el-button>
      </div>
    </div>

    <!-- 表格 -->
    <div class="med-table-card">
      <div class="med-table-card-header">
        <div class="med-table-card-header-left">
          <span class="med-table-card-title">用药记录列表</span>
          <span class="med-table-card-count">共 {{ total }} 条</span>
        </div>
      </div>
      <el-table :data="list" v-loading="loading" row-key="id" class="med-table">
        <el-table-column prop="recordDate" label="日期" width="110" />
        <el-table-column prop="elderlyName" label="老人" width="100" />
        <el-table-column prop="drugName" label="药品" width="140" />
        <el-table-column prop="dosage" label="剂量" width="90" />
        <el-table-column label="时段" width="80">
          <template #default="{ row }">
            <span class="med-pill" :class="slotPillClass(row.timeSlot)">{{ slotLabel(row.timeSlot) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <span class="med-pill" :class="statusPillClass(row.status)">{{ statusLabel(row.status) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="executorName" label="护工" width="90" />
        <el-table-column label="执行时间" width="90">
          <template #default="{ row }">{{ formatTime(row.executedAt) }}</template>
        </el-table-column>
        <el-table-column prop="skipReason" label="原因" width="120" show-overflow-tooltip />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'PENDING' || row.status === 'MISSED'"
              link type="primary"
              @click="openSupplementDialog(row)"
            >补录</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="med-pager">
        <el-pagination
          background
          layout="total, prev, pager, next, sizes"
          :total="total"
          :current-page="page"
          :page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          @update:current-page="(p) => { page = p; fetchList() }"
          @update:page-size="(s) => { pageSize = s; page = 1; fetchList() }"
        />
      </div>
    </div>

    <!-- A4导出预览对话框 -->
    <el-dialog v-model="exportVisible" title="导出用药记录 (A4)" width="800px" custom-class="med-dialog" @closed="exportVisible = false">
      <div class="med-export-preview">
        <div class="med-export-page" id="med-export-page">
          <div class="med-export-header">
            <div class="med-export-institution">{{ institutionName }}</div>
            <div class="med-export-title">用药记录报表</div>
          </div>
          <div class="med-export-divider"></div>
          <div class="med-export-filter-info">
            <span>筛选条件: {{ exportFilterDesc }}</span>
            <span>导出时间: {{ exportTime }}</span>
          </div>
          <div class="med-export-stats">
            <div class="med-export-stats-item">
              <div class="med-export-stats-label">总记录</div>
              <div class="med-export-stats-value">{{ stats.total }}</div>
            </div>
            <div class="med-export-stats-item">
              <div class="med-export-stats-label">已确认</div>
              <div class="med-export-stats-value">{{ stats.done }}</div>
            </div>
            <div class="med-export-stats-item">
              <div class="med-export-stats-label">异常(漏服+拒服)</div>
              <div class="med-export-stats-value">{{ stats.pendingOrMissed + stats.refused }}</div>
            </div>
          </div>
          <table class="med-export-table">
            <thead>
              <tr>
                <th>日期</th>
                <th>老人</th>
                <th>药品</th>
                <th>剂量</th>
                <th>时段</th>
                <th>状态</th>
                <th>护工</th>
                <th>时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in exportData" :key="row.id">
                <td>{{ row.recordDate }}</td>
                <td>{{ row.elderlyName }}</td>
                <td>{{ row.drugName }}</td>
                <td>{{ row.dosage }}</td>
                <td>{{ slotLabel(row.timeSlot) }}</td>
                <td>{{ statusLabel(row.status) }}</td>
                <td>{{ row.executorName || '-' }}</td>
                <td>{{ formatTime(row.executedAt) }}</td>
              </tr>
            </tbody>
          </table>
          <div class="med-export-footer">
            <span>e-Hfnew 养老管理系统</span>
            <span>第 1 页</span>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="exportVisible = false">取消</el-button>
        <el-button type="primary" @click="doPrint"><el-icon><Printer /></el-icon> 打印</el-button>
      </template>
    </el-dialog>

    <!-- 补录对话框 -->
    <el-dialog v-model="supplementVisible" title="用药补录" width="500px" custom-class="med-dialog" @closed="resetSupplement">
      <div v-if="supplementRecord">
        <!-- 药品信息 -->
        <div class="med-form-section">
          <div class="med-section-title">药品信息</div>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="老人">{{ supplementRecord.elderlyName }}</el-descriptions-item>
            <el-descriptions-item label="药品">{{ supplementRecord.drugName }}</el-descriptions-item>
            <el-descriptions-item label="剂量">{{ supplementRecord.dosage || '-' }}</el-descriptions-item>
            <el-descriptions-item label="时段">{{ slotLabel(supplementRecord.timeSlot) }}</el-descriptions-item>
            <el-descriptions-item label="当前状态">
              <span class="med-pill" :class="statusPillClass(supplementRecord.status)">{{ statusLabel(supplementRecord.status) }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 补录操作 -->
        <div class="med-form-section">
          <div class="med-section-title">补录操作</div>
          <el-form label-width="100px">
            <el-form-item label="补录类型">
              <el-radio-group v-model="supplementType">
                <el-radio value="CONFIRM">确认服药</el-radio>
                <el-radio value="SKIP">跳过</el-radio>
                <el-radio value="REFUSE">拒服</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="执行护工">
              <el-select
                v-model="supplementExecutorId"
                filterable
                remote
                clearable
                :remote-method="searchStaff"
                :loading="staffLoading"
                placeholder="选择护工"
                style="width: 100%"
              >
                <el-option v-for="s in staffOptions" :key="s.id" :label="s.name" :value="s.id" />
              </el-select>
              <el-input
                v-if="!supplementExecutorId"
                v-model="supplementExecutorName"
                placeholder="或手动输入护工姓名"
                style="margin-top: 8px"
              />
            </el-form-item>
            <el-form-item v-if="supplementType === 'SKIP' || supplementType === 'REFUSE'" label="原因">
              <el-input v-model="supplementReason" type="textarea" :rows="2" placeholder="请输入原因" />
            </el-form-item>
          </el-form>
        </div>
      </div>
      <template #footer>
        <el-button @click="supplementVisible = false">取消</el-button>
        <el-button type="primary" :loading="supplementSaving" @click="submitSupplement">确认补录</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../../api/client'

// ---------- 列表状态 ----------
const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)

const filterElderlyId = ref('')
const dateRange = ref(null)
const filterTimeSlot = ref('')
const filterStatus = ref('')
const filterKeyword = ref('')

// ---------- 统计卡片 ----------
const stats = computed(() => {
  const totalVal = total.value
  const doneVal = list.value.filter(r => r.status === 'DONE').length
  const pendingOrMissedVal = list.value.filter(r => r.status === 'PENDING' || r.status === 'MISSED').length
  const refusedVal = list.value.filter(r => r.status === 'REFUSED').length
  return { total: totalVal, done: doneVal, pendingOrMissed: pendingOrMissedVal, refused: refusedVal }
})

// ---------- 下拉选项 ----------
const elderlyOptions = ref([])

async function loadElderlyOptions() {
  try {
    const resp = await api.get('/api/elderly/options')
    const body = resp.data
    if (body.code === 200) elderlyOptions.value = body.data || []
  } catch { /* ignore */ }
}

// ---------- 映射工具 ----------
function slotLabel(slot) {
  const map = { MORNING: '早晨', AFTERNOON: '中午', EVENING: '晚上', BEDTIME: '睡前' }
  return map[slot] || slot
}

function slotPillClass(slot) {
  const map = { MORNING: 'orange', AFTERNOON: 'blue', EVENING: '', BEDTIME: 'teal' }
  return map[slot] || 'gray'
}

function statusLabel(status) {
  const map = { PENDING: '待执行', DONE: '已确认', SKIPPED: '已跳过', REFUSED: '拒服', MISSED: '漏服' }
  return map[status] || status
}

function statusPillClass(status) {
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

// ---------- 数据加载 ----------
async function fetchList() {
  loading.value = true
  try {
    const params = {
      page: page.value,
      pageSize: pageSize.value,
      elderlyId: filterElderlyId.value || undefined,
      timeSlot: filterTimeSlot.value || undefined,
      status: filterStatus.value || undefined,
      keyword: filterKeyword.value || undefined
    }
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const resp = await api.get('/api/medication/records', { params })
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '加载失败')
    list.value = body.data?.list || body.data?.records || []
    total.value = body.data?.total || 0
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function reload() {
  page.value = 1
  fetchList()
}

// ---------- A4导出 ----------
const institutionName = '安养惠康养老院'
const exportVisible = ref(false)
const exportData = ref([])
const exportTime = ref('')
const exportingExcel = ref(false)
const exportFilterDesc = computed(() => {
  const parts = []
  if (filterElderlyId.value) {
    const found = elderlyOptions.value.find(e => e.id === filterElderlyId.value)
    parts.push(`老人: ${found?.name || filterElderlyId.value}`)
  }
  if (dateRange.value) parts.push(`日期: ${dateRange.value[0]} ~ ${dateRange.value[1]}`)
  if (filterTimeSlot.value) parts.push(`时段: ${slotLabel(filterTimeSlot.value)}`)
  if (filterStatus.value) parts.push(`状态: ${statusLabel(filterStatus.value)}`)
  return parts.length ? parts.join('，') : '全部记录'
})

async function openExportPreview() {
  // Load all data for export (limit to 50 rows for A4 page fitting)
  try {
    const params = {
      page: 1,
      pageSize: 50,
      elderlyId: filterElderlyId.value || undefined,
      timeSlot: filterTimeSlot.value || undefined,
      status: filterStatus.value || undefined,
      keyword: filterKeyword.value || undefined
    }
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const resp = await api.get('/api/medication/records', { params })
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '加载导出数据失败')
    exportData.value = body.data?.list || body.data?.records || []
    const now = new Date()
    exportTime.value = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')} ${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`
    exportVisible.value = true
  } catch (e) {
    ElMessage.error(e.message || '加载导出数据失败')
  }
}

async function exportExcel() {
  exportingExcel.value = true
  try {
    const params = {
      elderlyId: filterElderlyId.value || undefined,
      timeSlot: filterTimeSlot.value || undefined,
      status: filterStatus.value || undefined,
      keyword: filterKeyword.value || undefined
    }
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const resp = await api.get('/api/medication/records/export', {
      params,
      responseType: 'blob'
    })
    const blob = resp.data
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    const now = new Date()
    link.download = `用药记录_${now.getFullYear()}${String(now.getMonth()+1).padStart(2,'0')}${String(now.getDate()).padStart(2,'0')}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败')
  } finally {
    exportingExcel.value = false
  }
}

function doPrint() {
  window.print()
}

// ---------- 补录 ----------
const supplementVisible = ref(false)
const supplementSaving = ref(false)
const supplementRecord = ref(null)
const supplementType = ref('CONFIRM')
const supplementExecutorId = ref(null)
const supplementExecutorName = ref('')
const supplementReason = ref('')
const staffLoading = ref(false)
const staffOptions = ref([])

function openSupplementDialog(record) {
  supplementRecord.value = record
  supplementType.value = 'CONFIRM'
  supplementExecutorId.value = null
  supplementExecutorName.value = ''
  supplementReason.value = ''
  supplementVisible.value = true
  searchStaff('')
}

function resetSupplement() {
  supplementRecord.value = null
  supplementType.value = 'CONFIRM'
  supplementExecutorId.value = null
  supplementExecutorName.value = ''
  supplementReason.value = ''
}

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

async function submitSupplement() {
  if (!supplementRecord.value) return
  supplementSaving.value = true
  try {
    const execName = supplementExecutorId.value
      ? (staffOptions.value.find(s => s.id === supplementExecutorId.value)?.name || '')
      : supplementExecutorName.value.trim()

    const payload = {
      supplementType: supplementType.value,
      executorId: supplementExecutorId.value || null,
      executorName: supplementType.value === 'CONFIRM' ? (execName || '补录') : null,
      reason: supplementReason.value || null
    }

    const resp = await api.put(`/api/medication/records/${supplementRecord.value.id}/supplement`, payload)
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '补录失败')
    ElMessage.success('补录成功')
    supplementVisible.value = false
    await fetchList()
  } catch (e) {
    ElMessage.error(e.message || '补录失败')
  } finally {
    supplementSaving.value = false
  }
}

// ---------- 初始化 ----------
onMounted(() => {
  loadElderlyOptions()
  fetchList()
})
</script>

<style scoped>
</style>
