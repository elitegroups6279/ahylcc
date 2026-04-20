<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="header">
          <span>月度账单</span>
          <div class="header-actions">
            <el-date-picker
              v-model="billMonth"
              type="month"
              value-format="YYYY-MM"
              placeholder="选择月份"
              style="width: 160px"
              @change="loadData"
            />
            <el-select
              v-model="statusFilter"
              placeholder="状态"
              clearable
              style="width: 120px"
              @change="loadData"
            >
              <el-option label="草稿" value="DRAFT" />
              <el-option label="已确认" value="CONFIRMED" />
              <el-option label="已结清" value="SETTLED" />
            </el-select>
            <el-button type="primary" :loading="generating" @click="generateBills">
              生成{{ billMonth || '本月' }}账单
            </el-button>
            <el-button :disabled="selectedIds.length === 0" @click="batchConfirm">
              批量确认
            </el-button>
          </div>
        </div>
      </template>

      <el-table
        :data="billList"
        v-loading="loading"
        border
        show-summary
        :summary-method="getSummaries"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="40" />
        <el-table-column prop="elderlyName" label="老人姓名" width="100" />
        <el-table-column label="类别" width="80">
          <template #default="{ row }">
            <el-tag
              v-if="categoryMap[row.billingRule]"
              :type="categoryTagType(row.billingRule)"
              size="small"
            >
              {{ categoryMap[row.billingRule] }}
            </el-tag>
            <span v-else>{{ row.billingRule || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="stayDays" label="实住天数" width="80" align="center" />
        <el-table-column prop="leaveDays" label="请假天数" width="80" align="center">
          <template #default="{ row }">
            <span :style="{ color: row.leaveDays > 0 ? '#ff4d4f' : '#999' }">{{ row.leaveDays || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="baseFee" label="基础费用" width="100" align="right">
          <template #default="{ row }">¥{{ formatAmount(row.baseFee) }}</template>
        </el-table-column>
        <el-table-column prop="longCareAmount" label="长护险" width="90" align="right">
          <template #default="{ row }">
            <span :style="{ color: row.longCareAmount > 0 ? '#52c41a' : '#999' }">
              ¥{{ formatAmount(row.longCareAmount) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="couponDeduct" label="消费券" width="90" align="right">
          <template #default="{ row }">
            <span :style="{ color: row.couponDeduct > 0 ? '#1890ff' : '#999' }">
              ¥{{ formatAmount(row.couponDeduct) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="subsidyAmount" label="财政补助" width="90" align="right">
          <template #default="{ row }">¥{{ formatAmount(row.subsidyAmount) }}</template>
        </el-table-column>
        <el-table-column prop="personalSubsidy" label="个人补助" width="90" align="right">
          <template #default="{ row }">¥{{ formatAmount(row.personalSubsidy) }}</template>
        </el-table-column>
        <el-table-column prop="familyPayable" label="家属应缴" width="100" align="right">
          <template #default="{ row }">
            <span style="font-weight: bold; color: #ff4d4f;">
              ¥{{ formatAmount(row.familyPayable) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="govPayable" label="政府应拨" width="100" align="right">
          <template #default="{ row }">
            <span style="font-weight: bold; color: #fa8c16;">
              ¥{{ formatAmount(row.govPayable) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag
              :type="row.status === 'DRAFT' ? 'warning' : row.status === 'CONFIRMED' ? 'success' : 'info'"
              size="small"
            >
              {{ row.status === 'DRAFT' ? '草稿' : row.status === 'CONFIRMED' ? '已确认' : '已结清' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row)">明细</el-button>
            <el-button
              v-if="row.status === 'DRAFT'"
              link
              type="success"
              @click="confirmBill(row)"
            >
              确认
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
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
    </el-card>

    <!-- 补贴明细弹窗 -->
    <el-dialog v-model="detailVisible" title="补贴明细" width="600px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="老人姓名">{{ currentBill.elderlyName }}</el-descriptions-item>
        <el-descriptions-item label="账单月份">{{ currentBill.billMonth }}</el-descriptions-item>
        <el-descriptions-item label="实住天数">{{ currentBill.stayDays }} 天</el-descriptions-item>
        <el-descriptions-item label="请假天数">{{ currentBill.leaveDays || 0 }} 天</el-descriptions-item>
        <el-descriptions-item label="基础费用">¥{{ formatAmount(currentBill.baseFee) }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="currentSubsidyDetails" style="margin-top: 16px" border>
        <el-table-column prop="policyName" label="补贴项目" />
        <el-table-column prop="amount" label="金额" width="120" align="right">
          <template #default="{ row }">¥{{ Number(row.amount).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="calcDesc" label="计算说明" />
        <el-table-column label="拨付对象" width="100">
          <template #default="{ row }">
            <el-tag
              :type="row.payTarget === 'ORG' ? 'primary' : 'warning'"
              size="small"
            >
              {{ row.payTarget === 'ORG' ? '机构' : '个人' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top: 16px; text-align: right;">
        <span style="margin-right: 24px;">
          家属应缴: <b style="color: #ff4d4f;">¥{{ formatAmount(currentBill.familyPayable) }}</b>
        </span>
        <span>
          政府应拨: <b style="color: #fa8c16;">¥{{ formatAmount(currentBill.govPayable) }}</b>
        </span>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../../api/client'

const categoryMap = {
  SOCIAL: '社会化',
  WU_BAO: '五保',
  LOW_BAO: '低保'
}

function categoryTagType(rule) {
  if (rule === 'WU_BAO') return 'danger'
  if (rule === 'LOW_BAO') return 'warning'
  return ''
}

const loading = ref(false)
const generating = ref(false)
const billList = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const billMonth = ref(new Date().toISOString().slice(0, 7))
const statusFilter = ref('')

const selectedIds = ref([])
const detailVisible = ref(false)
const currentBill = ref({})
const currentSubsidyDetails = ref([])

function formatAmount(amount) {
  if (amount === null || amount === undefined) return '0.00'
  const n = Number(amount)
  if (Number.isNaN(n)) return '0.00'
  return n.toFixed(2)
}

async function fetchList() {
  loading.value = true
  try {
    const params = {
      page: page.value,
      pageSize: pageSize.value,
      billMonth: billMonth.value || undefined,
      status: statusFilter.value || undefined
    }
    const resp = await api.get('/api/finance/bills', { params })
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '加载失败')
    billList.value = body.data?.list || []
    total.value = body.data?.total || 0
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function loadData() {
  page.value = 1
  fetchList()
}

async function generateBills() {
  const month = billMonth.value
  if (!month) {
    ElMessage.warning('请先选择月份')
    return
  }
  generating.value = true
  try {
    const resp = await api.post('/api/finance/bills/generate', null, {
      params: { billMonth: month }
    })
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '生成失败')
    const count = body.data || 0
    ElMessage.success(`成功生成 ${count} 条账单`)
    await fetchList()
  } catch (e) {
    ElMessage.error(e.message || '生成失败')
  } finally {
    generating.value = false
  }
}

async function confirmBill(row) {
  try {
    await ElMessageBox.confirm(
      `确认老人【${row.elderlyName}】的 ${row.billMonth} 账单？确认后将无法修改。`,
      '确认账单',
      { type: 'warning' }
    )
  } catch {
    return
  }
  try {
    const resp = await api.put(`/api/finance/bills/${row.id}/confirm`)
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '确认失败')
    ElMessage.success('确认成功')
    await fetchList()
  } catch (e) {
    ElMessage.error(e.message || '确认失败')
  }
}

async function batchConfirm() {
  if (selectedIds.value.length === 0) return
  try {
    await ElMessageBox.confirm(
      `确认批量确认 ${selectedIds.value.length} 条账单？`,
      '批量确认',
      { type: 'warning' }
    )
  } catch {
    return
  }
  let successCount = 0
  let failCount = 0
  for (const id of selectedIds.value) {
    try {
      const resp = await api.put(`/api/finance/bills/${id}/confirm`)
      if (resp.data.code === 200) {
        successCount++
      } else {
        failCount++
      }
    } catch {
      failCount++
    }
  }
  if (successCount > 0) ElMessage.success(`成功确认 ${successCount} 条`)
  if (failCount > 0) ElMessage.warning(`${failCount} 条确认失败`)
  await fetchList()
}

function handleSelectionChange(rows) {
  selectedIds.value = rows.filter(r => r.status === 'DRAFT').map(r => r.id)
}

function showDetail(row) {
  currentBill.value = row
  try {
    currentSubsidyDetails.value = row.subsidyDetail ? JSON.parse(row.subsidyDetail) : []
  } catch {
    currentSubsidyDetails.value = []
  }
  detailVisible.value = true
}

function getSummaries({ columns, data }) {
  const sums = []
  const sumFields = ['baseFee', 'longCareAmount', 'couponDeduct', 'subsidyAmount', 'personalSubsidy', 'familyPayable', 'govPayable']
  columns.forEach((col, index) => {
    if (index === 0) {
      sums[index] = '合计'
      return
    }
    if (index === 1 || index === 2 || index === 3 || index === 4) {
      sums[index] = ''
      return
    }
    const prop = col.property
    if (sumFields.includes(prop)) {
      const total = data.reduce((sum, row) => sum + (Number(row[prop]) || 0), 0)
      sums[index] = `¥${total.toFixed(2)}`
    } else {
      sums[index] = ''
    }
  })
  return sums
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.page {
  padding: 16px;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.pager {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}
</style>
