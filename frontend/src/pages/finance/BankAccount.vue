<template>
  <div class="bank-account-page">
    <PageHeader title="银行账户">
      <template #actions>
        <el-button type="primary" @click="showTransactionDialog = true">手动记账</el-button>
      </template>
    </PageHeader>

    <!-- 顶部双账户概览卡片 -->
    <el-row :gutter="16" style="margin-bottom: 20px">
      <el-col :span="12">
        <el-card shadow="hover" class="account-card basic-card">
          <div class="account-header">
            <span class="account-tag basic-tag">基本户</span>
            <span class="account-name">{{ dashboard.basicAccountName || '-' }}</span>
          </div>
          <div class="account-info">
            <div class="info-row">
              <span class="info-label">账户余额</span>
              <span class="info-value balance">¥ {{ formatMoney(dashboard.basicBalance) }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">本月收入</span>
              <span class="info-value income">¥ {{ formatMoney(dashboard.basicMonthlyIncome) }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">本月支出</span>
              <span class="info-value expense">¥ {{ formatMoney(dashboard.basicMonthlyExpense) }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover" class="account-card general-card">
          <div class="account-header">
            <span class="account-tag general-tag">一般户</span>
            <span class="account-name">{{ dashboard.generalAccountName || '-' }}</span>
          </div>
          <div class="account-info">
            <div class="info-row">
              <span class="info-label">账户余额</span>
              <span class="info-value balance">¥ {{ formatMoney(dashboard.generalBalance) }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">本月收入</span>
              <span class="info-value income">¥ {{ formatMoney(dashboard.generalMonthlyIncome) }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">本月支出</span>
              <span class="info-value expense">¥ {{ formatMoney(dashboard.generalMonthlyExpense) }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 月度收支趋势图 -->
    <el-card shadow="hover" style="margin-bottom: 20px">
      <template #header>
        <div class="card-header">
          <span>月度收支趋势（近6个月）</span>
        </div>
      </template>
      <div ref="chartRef" style="height: 320px; width: 100%"></div>
    </el-card>

    <!-- 流水表格 -->
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>交易流水</span>
          <div class="header-actions">
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              style="width: 280px; margin-right: 12px"
            />
            <el-button @click="loadTransactions">查询</el-button>
          </div>
        </div>
      </template>

      <el-tabs v-model="activeAccount" @tab-change="onAccountTabChange">
        <el-tab-pane label="基本户流水" name="basic" />
        <el-tab-pane label="一般户流水" name="general" />
      </el-tabs>

      <el-table :data="transactionList" border stripe v-loading="transactionLoading">
        <el-table-column prop="transactionDate" label="日期" width="120" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="row.transactionType === 'INCOME' ? 'success' : 'danger'" size="small">
              {{ row.transactionType === 'INCOME' ? '收入' : '支出' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="140">
          <template #default="{ row }">
            <span :style="{ color: row.transactionType === 'INCOME' ? '#67C23A' : '#F56C6C', fontWeight: 'bold' }">
              {{ row.transactionType === 'INCOME' ? '+' : '-' }}{{ formatMoney(row.amount) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="counterparty" label="对方户名" width="160" />
        <el-table-column prop="bizType" label="业务类型" width="140">
          <template #default="{ row }">
            {{ businessTypeMap[row.bizType] || row.bizType || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="description" label="摘要" min-width="200" />
        <el-table-column prop="receiptNo" label="回单号" width="160" />
      </el-table>

      <div class="pager">
        <el-pagination
          background
          layout="total, prev, pager, next, sizes"
          :total="transactionTotal"
          :current-page="transactionPage"
          :page-size="transactionPageSize"
          :page-sizes="[10, 20, 50, 100]"
          @update:current-page="(p) => { transactionPage = p; loadTransactions() }"
          @update:page-size="(s) => { transactionPageSize = s; transactionPage = 1; loadTransactions() }"
        />
      </div>
    </el-card>

    <!-- 手动记账弹窗 -->
    <el-dialog v-model="showTransactionDialog" title="手动记账" width="560px">
      <el-form ref="transactionFormRef" :model="transactionForm" :rules="transactionRules" label-width="90px">
        <el-form-item label="记账账户" prop="accountId">
          <el-select v-model="transactionForm.accountId" placeholder="选择账户" style="width: 100%">
            <el-option v-for="acc in bankAccounts" :key="acc.id" :label="`${acc.accountName} (${acc.accountType === 'BASIC' ? '基本户' : '一般户'})`" :value="acc.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="交易类型" prop="transactionType">
          <el-select v-model="transactionForm.transactionType" placeholder="选择类型" style="width: 100%">
            <el-option label="收入" value="INCOME" />
            <el-option label="支出" value="EXPENSE" />
          </el-select>
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input-number v-model="transactionForm.amount" :min="0.01" :precision="2" :step="10" style="width: 100%" />
        </el-form-item>
        <el-form-item label="交易日期" prop="transactionDate">
          <el-date-picker v-model="transactionForm.transactionDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="对方户名" prop="counterparty">
          <el-input v-model="transactionForm.counterparty" placeholder="可选" />
        </el-form-item>
        <el-form-item label="业务类型" prop="bizType">
          <el-select v-model="transactionForm.bizType" placeholder="选择业务类型" style="width: 100%" clearable>
            <el-option v-for="(label, key) in businessTypeMap" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="摘要" prop="description">
          <el-input v-model="transactionForm.description" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
        <el-form-item label="回单号" prop="receiptNo">
          <el-input v-model="transactionForm.receiptNo" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showTransactionDialog = false">取消</el-button>
        <el-button type="primary" :loading="transactionSaving" @click="submitTransaction">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { api as client } from '../../api/client'
import PageHeader from '../../components/common/PageHeader.vue'
import * as echarts from 'echarts'

// 业务类型映射
const businessTypeMap = {
  ELDERLY_FEE: '养老费用',
  SUBSIDY: '政府补贴',
  WUBAO_ALLOCATE: '五保拨付',
  FOOD: '食材采购',
  MEDICAL: '医疗用品',
  SALARY: '人员工资',
  UTILITY: '水电费',
  PURCHASE: '物资采购',
  OTHER: '其他'
}

// 银行账户列表（用于下拉选择）
const bankAccounts = ref([])

async function loadBankAccounts() {
  try {
    const resp = await client.get('/api/finance/bank-accounts')
    const body = resp.data
    if (body.code === 200) {
      bankAccounts.value = body.data || []
    }
  } catch (e) {
    console.warn('加载银行账户列表失败', e)
  }
}

// Dashboard 数据
const dashboard = ref({
  basicAccountId: null,
  basicAccountName: null,
  basicBalance: 0,
  basicMonthlyIncome: 0,
  basicMonthlyExpense: 0,
  generalAccountId: null,
  generalAccountName: null,
  generalBalance: 0,
  generalMonthlyIncome: 0,
  generalMonthlyExpense: 0,
  monthlyTrend: []
})

// 趋势图
const chartRef = ref(null)
let chartInstance = null

// 流水列表
const activeAccount = ref('basic')
const transactionList = ref([])
const transactionTotal = ref(0)
const transactionPage = ref(1)
const transactionPageSize = ref(10)
const transactionLoading = ref(false)
const dateRange = ref(null)

// 手动记账
const showTransactionDialog = ref(false)
const transactionFormRef = ref()
const transactionSaving = ref(false)
const transactionForm = reactive({
  accountId: null,
  transactionType: 'INCOME',
  amount: 0,
  transactionDate: new Date().toISOString().slice(0, 10),
  counterparty: '',
  bizType: '',
  description: '',
  receiptNo: ''
})
const transactionRules = {
  accountId: [{ required: true, message: '请选择记账账户', trigger: 'change' }],
  transactionType: [{ required: true, message: '请选择交易类型', trigger: 'change' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  transactionDate: [{ required: true, message: '请选择交易日期', trigger: 'change' }]
}

// 当前账户ID
const currentAccountId = computed(() => {
  if (activeAccount.value === 'basic') return dashboard.value.basicAccountId
  return dashboard.value.generalAccountId
})

// 格式化金额
function formatMoney(val) {
  if (val === null || val === undefined) return '0.00'
  const n = Number(val)
  if (Number.isNaN(n)) return String(val)
  return n.toFixed(2)
}

// 加载 Dashboard
async function loadDashboard() {
  try {
    const res = await client.get('/api/finance/bank-accounts/dashboard')
    if (res.data?.code === 200) {
      dashboard.value = res.data.data || {}
    }
  } catch (e) {
    console.warn('加载银行账户概览失败', e)
  }
}

// 加载流水
async function loadTransactions() {
  const accountId = currentAccountId.value
  if (!accountId) return
  transactionLoading.value = true
  try {
    const params = {
      page: transactionPage.value,
      size: transactionPageSize.value
    }
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const res = await client.get(`/api/finance/bank-accounts/${accountId}/transactions`, { params })
    if (res.data?.code === 200) {
      transactionList.value = res.data.data?.list || []
      transactionTotal.value = res.data.data?.total || 0
    } else {
      throw new Error(res.data?.msg || '加载失败')
    }
  } catch (e) {
    ElMessage.error(e.message || '加载流水失败')
  } finally {
    transactionLoading.value = false
  }
}

// 切换账户Tab
function onAccountTabChange() {
  transactionPage.value = 1
  loadTransactions()
}

// 提交手动记账
async function submitTransaction() {
  if (!transactionFormRef.value) return
  await transactionFormRef.value.validate()
  transactionSaving.value = true
  try {
    const accountId = transactionForm.accountId
    const res = await client.post(`/api/finance/bank-accounts/${accountId}/transactions`, {
      transactionType: transactionForm.transactionType,
      amount: transactionForm.amount,
      transactionDate: transactionForm.transactionDate,
      counterparty: transactionForm.counterparty || null,
      bizType: transactionForm.bizType || null,
      description: transactionForm.description || null,
      receiptNo: transactionForm.receiptNo || null
    })
    if (res.data?.code !== 200) throw new Error(res.data?.msg || '保存失败')
    ElMessage.success('记账成功')
    showTransactionDialog.value = false
    // 重置表单
    transactionForm.accountId = null
    transactionForm.transactionType = 'INCOME'
    transactionForm.amount = 0
    transactionForm.transactionDate = new Date().toISOString().slice(0, 10)
    transactionForm.counterparty = ''
    transactionForm.bizType = ''
    transactionForm.description = ''
    transactionForm.receiptNo = ''
    await loadDashboard()
    await loadTransactions()
    await nextTick()
    renderChart()
  } catch (e) {
    ElMessage.error(e.message || '记账失败')
  } finally {
    transactionSaving.value = false
  }
}

// 渲染趋势图
function renderChart() {
  if (!chartRef.value) return
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }
  const trend = dashboard.value.monthlyTrend || []
  const months = trend.map(t => t.month)
  const basicIncome = trend.map(t => t.basicIncome || 0)
  const basicExpense = trend.map(t => t.basicExpense || 0)
  const generalIncome = trend.map(t => t.generalIncome || 0)
  const generalExpense = trend.map(t => t.generalExpense || 0)

  chartInstance.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['基本户收入', '基本户支出', '一般户收入', '一般户支出'], bottom: 0 },
    grid: { left: 60, right: 30, top: 20, bottom: 40 },
    xAxis: { type: 'category', data: months },
    yAxis: { type: 'value', axisLabel: { formatter: val => '¥' + (val / 1000).toFixed(0) + 'k' } },
    series: [
      { name: '基本户收入', type: 'line', data: basicIncome, smooth: true, itemStyle: { color: '#67C23A' } },
      { name: '基本户支出', type: 'line', data: basicExpense, smooth: true, itemStyle: { color: '#F56C6C' } },
      { name: '一般户收入', type: 'line', data: generalIncome, smooth: true, itemStyle: { color: '#409EFF' }, lineStyle: { type: 'dashed' } },
      { name: '一般户支出', type: 'line', data: generalExpense, smooth: true, itemStyle: { color: '#E6A23C' }, lineStyle: { type: 'dashed' } }
    ]
  })
}

function handleResize() {
  chartInstance?.resize()
}

onMounted(async () => {
  await loadBankAccounts()
  await loadDashboard()
  await loadTransactions()
  await nextTick()
  renderChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
  chartInstance = null
})
</script>

<style scoped>
.bank-account-page {
  padding: 16px;
}

.account-card {
  border-radius: 8px;
}

.account-card.basic-card {
  border-top: 3px solid #409EFF;
}

.account-card.general-card {
  border-top: 3px solid #E6A23C;
}

.account-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.account-tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 600;
  color: #fff;
}

.basic-tag {
  background: #409EFF;
}

.general-tag {
  background: #E6A23C;
}

.account-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.account-info {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.info-label {
  font-size: 14px;
  color: #909399;
}

.info-value {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

.info-value.balance {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
}

.info-value.income {
  color: #67C23A;
}

.info-value.expense {
  color: #F56C6C;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
  font-size: 16px;
}

.header-actions {
  display: flex;
  align-items: center;
}

.pager {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}
</style>
