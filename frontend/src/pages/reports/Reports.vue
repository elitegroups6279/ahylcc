<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="header">
          <span>报表导出</span>
          <el-button @click="downloadAll" :loading="downloading">一键导出本月</el-button>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="收费汇总" name="fee">
          <div class="panel">
            <div class="filters">
              <el-date-picker v-model="feeMonth" type="month" clearable placeholder="选择月份" style="width: 160px" />
              <el-button type="primary" :loading="downloading" @click="exportFeeSummary">导出 Excel</el-button>
            </div>
            <el-alert
              type="info"
              show-icon
              title="按月份导出缴费汇总（来自缴费记录）。"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="花名册" name="roster">
          <div class="panel">
            <div class="filters">
              <el-select v-model="rosterStatus" clearable placeholder="状态" style="width: 180px">
                <el-option label="在住" value="ACTIVE" />
                <el-option label="退住" value="DISCHARGED" />
                <el-option label="转床中" value="TRANSFERRING" />
              </el-select>
              <el-button type="primary" :loading="downloading" @click="exportRoster">导出 Excel</el-button>
            </div>
            <el-alert
              type="info"
              show-icon
              title="导出老人花名册（支持按状态筛选）。"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="考勤" name="attendance">
          <div class="panel">
            <div class="filters">
              <el-date-picker
                v-model="attendanceRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
                style="width: 320px"
              />
              <el-input-number v-model="attendanceStaffId" :min="0" placeholder="护工ID" style="width: 160px" />
              <el-button type="primary" :loading="downloading" @click="exportAttendance">导出 Excel</el-button>
            </div>
            <el-alert
              type="info"
              show-icon
              title="导出护工考勤记录（可按日期范围与护工ID筛选）。"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="物资消耗" name="material">
          <div class="panel">
            <div class="filters">
              <el-date-picker
                v-model="materialRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
                style="width: 320px"
              />
              <el-input-number v-model="materialId" :min="0" placeholder="物资ID" style="width: 160px" />
              <el-button type="primary" :loading="downloading" @click="exportMaterialConsumption">导出 Excel</el-button>
            </div>
            <el-alert
              type="info"
              show-icon
              title="导出已审批出库记录（可按日期范围与物资ID筛选）。"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../../api/client'

const activeTab = ref('fee')
const downloading = ref(false)

const feeMonth = ref(null)
const rosterStatus = ref('')
const attendanceRange = ref([])
const attendanceStaffId = ref(null)
const materialRange = ref([])
const materialId = ref(null)

function pad2(n) {
  const s = String(n)
  return s.length >= 2 ? s : `0${s}`
}

function formatMonth(d) {
  if (!d) return null
  const date = d instanceof Date ? d : new Date(d)
  if (Number.isNaN(date.getTime())) return null
  return `${date.getFullYear()}-${pad2(date.getMonth() + 1)}`
}

function filenameFromDisposition(disposition) {
  if (!disposition) return null
  const m = disposition.match(/filename\*\=UTF-8''([^;]+)/i)
  if (m && m[1]) {
    try {
      return decodeURIComponent(m[1])
    } catch {
      return m[1]
    }
  }
  return null
}

async function downloadExcel(url, params) {
  downloading.value = true
  try {
    const resp = await api.get(url, { params, responseType: 'blob' })
    const disposition = resp.headers?.['content-disposition']
    const name = filenameFromDisposition(disposition) || 'report.xlsx'
    const blob = new Blob([resp.data], { type: resp.headers?.['content-type'] || 'application/octet-stream' })
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = name
    document.body.appendChild(a)
    a.click()
    a.remove()
    URL.revokeObjectURL(a.href)
  } catch (e) {
    ElMessage.error(e?.message || '下载失败')
  } finally {
    downloading.value = false
  }
}

async function exportFeeSummary() {
  await downloadExcel('/api/reports/fee-summary.xlsx', { month: formatMonth(feeMonth.value) || undefined })
}

async function exportRoster() {
  await downloadExcel('/api/reports/roster.xlsx', { status: rosterStatus.value || undefined })
}

async function exportAttendance() {
  const [startDate, endDate] = attendanceRange.value || []
  await downloadExcel('/api/reports/attendance.xlsx', {
    startDate: startDate || undefined,
    endDate: endDate || undefined,
    staffId: attendanceStaffId.value || undefined
  })
}

async function exportMaterialConsumption() {
  const [startDate, endDate] = materialRange.value || []
  await downloadExcel('/api/reports/material-consumption.xlsx', {
    startDate: startDate || undefined,
    endDate: endDate || undefined,
    materialId: materialId.value || undefined
  })
}

async function downloadAll() {
  const now = new Date()
  feeMonth.value = now
  const start = `${now.getFullYear()}-${pad2(now.getMonth() + 1)}-01`
  const end = `${now.getFullYear()}-${pad2(now.getMonth() + 1)}-${pad2(new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate())}`
  attendanceRange.value = [start, end]
  materialRange.value = [start, end]
  await exportFeeSummary()
  await exportRoster()
  await exportAttendance()
  await exportMaterialConsumption()
}
</script>

<style scoped>
.page {
  padding: 12px;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.filters {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
</style>

