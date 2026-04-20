<template>
  <div class="cashflow-page">
    <!-- 顶部统计卡片 -->
    <el-row :gutter="16" style="margin-bottom: 20px">
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card income-card">
          <div class="stat-label">本月收入</div>
          <div class="stat-value" style="color: #67C23A">¥ {{ formatMoney(summary.totalIncome) }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card expense-card">
          <div class="stat-label">本月支出</div>
          <div class="stat-value" style="color: #F56C6C">¥ {{ formatMoney(summary.totalExpense) }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card net-card">
          <div class="stat-label">本月净额</div>
          <div class="stat-value" :style="{ color: summary.netAmount >= 0 ? '#409EFF' : '#F56C6C' }">
            ¥ {{ formatMoney(summary.netAmount) }}
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 按钮组切换 -->
    <div style="margin-bottom: 20px">
      <el-button-group>
        <el-button type="success" :plain="activeView !== 'income'" @click="switchView('income')">
          收入管理
        </el-button>
        <el-button type="danger" :plain="activeView !== 'expense'" @click="switchView('expense')">
          支出管理
        </el-button>
      </el-button-group>
    </div>

    <!-- 收入管理视图 -->
    <div v-if="activeView === 'income'">
      <!-- 筛选栏 -->
      <el-row style="margin-bottom: 16px" :gutter="12">
        <el-col :span="8">
          <el-select v-model="incomeFilter.elderlyId" placeholder="选择老人" clearable filterable>
            <el-option v-for="e in elderlyOptions" :key="e.id" :label="`${e.name} (${e.uniqueNo})`" :value="e.id" />
          </el-select>
        </el-col>
        <el-col :span="16" style="text-align: right">
          <el-button @click="loadIncome">查询</el-button>
          <el-button type="success" @click="showIncomeDialog = true">登记收入</el-button>
        </el-col>
      </el-row>

      <!-- 收入列表表格 -->
      <el-table :data="incomeList" border stripe v-loading="incomeLoading">
        <el-table-column label="收入类型" width="110">
          <template #default="{ row }">
            {{ incomeTypeMap[row.incomeType] || row.incomeType || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="老人姓名" width="120">
          <template #default="{ row }">
            {{ row.elderlyName || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="金额" width="120">
          <template #default="{ row }">
            <span style="color: #67C23A; font-weight: bold">+{{ formatMoney(row.amount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="paymentMethod" label="方式" width="100">
          <template #default="{ row }">
            {{ paymentMethodMap[row.paymentMethod] || row.paymentMethod }}
          </template>
        </el-table-column>
        <el-table-column prop="sourceType" label="来源" width="100">
          <template #default="{ row }">
            {{ sourceTypeMap[row.sourceType] || row.sourceType }}
          </template>
        </el-table-column>
        <el-table-column prop="receiptNo" label="收据号" width="140" />
        <el-table-column prop="paymentDate" label="缴费时间" width="120" />
        <el-table-column prop="createTime" label="记录时间" width="180" />
        <el-table-column prop="remark" label="备注" />
      </el-table>

      <!-- 分页 -->
      <div class="pager">
        <el-pagination
          background
          layout="total, prev, pager, next, sizes"
          :total="incomeTotal"
          :current-page="incomePage"
          :page-size="incomePageSize"
          :page-sizes="[10, 20, 50, 100]"
          @update:current-page="(p) => { incomePage = p; loadIncome() }"
          @update:page-size="(s) => { incomePageSize = s; incomePage = 1; loadIncome() }"
        />
      </div>
    </div>

    <!-- 支出管理视图 -->
    <div v-if="activeView === 'expense'">
      <!-- 筛选栏 -->
      <el-row style="margin-bottom: 16px" :gutter="12">
        <el-col :span="6">
          <el-select v-model="expenseFilter.expenseType" placeholder="支出类型" clearable>
            <el-option v-for="t in expenseTypes" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-col>
        <el-col :span="8">
          <el-date-picker
            v-model="expenseFilter.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-col>
        <el-col :span="10" style="text-align: right">
          <el-button @click="loadExpense">查询</el-button>
          <el-button type="danger" @click="showExpenseDialog = true">登记支出</el-button>
        </el-col>
      </el-row>

      <!-- 支出列表表格 -->
      <el-table :data="expenseList" border stripe v-loading="expenseLoading">
        <el-table-column label="支出类型" width="120">
          <template #default="{ row }">
            {{ expenseTypeMap[row.expenseType] || row.expenseType }}
          </template>
        </el-table-column>
        <el-table-column label="金额" width="120">
          <template #default="{ row }">
            <span style="color: #F56C6C; font-weight: bold">-{{ formatMoney(row.amount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="payee" label="收款方" width="140" />
        <el-table-column prop="expenseDate" label="日期" width="120" />
        <el-table-column prop="description" label="说明" />
        <el-table-column prop="operatorName" label="操作员" width="100" />
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-popconfirm title="确定删除?" @confirm="deleteExpense(row.id)">
              <template #reference>
                <el-button type="danger" link size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pager">
        <el-pagination
          background
          layout="total, prev, pager, next, sizes"
          :total="expenseTotal"
          :current-page="expensePage"
          :page-size="expensePageSize"
          :page-sizes="[10, 20, 50, 100]"
          @update:current-page="(p) => { expensePage = p; loadExpense() }"
          @update:page-size="(s) => { expensePageSize = s; expensePage = 1; loadExpense() }"
        />
      </div>
    </div>

    <!-- 收入弹窗 -->
    <el-dialog v-model="showIncomeDialog" title="登记收入" width="560px">
      <el-form ref="incomeFormRef" :model="incomeForm" :rules="incomeRules" label-width="90px">
        <el-form-item label="收入类型" prop="incomeType">
          <el-select v-model="incomeForm.incomeType" placeholder="选择收入类型" style="width: 220px" @change="onIncomeTypeChange">
            <el-option v-for="t in incomeTypeOptions" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="incomeForm.incomeType === 'ELDERLY_FEE'" label="老人" prop="elderlyId">
          <el-select
            v-model="incomeForm.elderlyId"
            filterable
            clearable
            placeholder="选择老人"
            style="width: 100%"
          >
            <el-option v-for="e in elderlyOptions" :key="e.id" :label="`${e.name} (${e.uniqueNo})`" :value="e.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="selectedElderly" label="身份证号">
          <el-input :model-value="selectedElderly.idCard" disabled />
        </el-form-item>
        <el-form-item v-if="incomeForm.incomeType !== 'ELDERLY_FEE'" label="收入说明" prop="description">
          <el-input v-model="incomeForm.description" placeholder="请输入收入说明" />
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input-number v-model="incomeForm.amount" :min="0.01" :precision="2" :step="10" style="width: 220px" />
        </el-form-item>
        <el-form-item label="缴费时间" prop="paymentDate" :rules="[{ required: true, message: '请选择缴费时间' }]">
          <el-date-picker v-model="incomeForm.paymentDate" type="date" placeholder="选择缴费时间" style="width: 100%" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="方式" prop="paymentMethod">
          <el-select v-model="incomeForm.paymentMethod" placeholder="选择方式" style="width: 220px">
            <el-option label="现金" value="CASH" />
            <el-option label="转账" value="TRANSFER" />
            <el-option label="POS刷卡" value="POS" />
          </el-select>
        </el-form-item>
        <el-form-item label="来源" prop="sourceType">
          <el-select v-model="incomeForm.sourceType" placeholder="选择来源" style="width: 220px">
            <el-option label="长护险" value="LONG_CARE" />
            <el-option label="消费券" value="COUPON" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="收据号" prop="receiptNo">
          <el-input v-model="incomeForm.receiptNo" placeholder="可选" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="incomeForm.remark" type="textarea" :rows="3" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showIncomeDialog = false">取消</el-button>
        <el-button type="primary" :loading="incomeSaving" @click="submitIncome">保存</el-button>
      </template>
    </el-dialog>

    <!-- 支出弹窗 -->
    <el-dialog v-model="showExpenseDialog" title="登记支出" width="560px">
      <el-form ref="expenseFormRef" :model="expenseForm" :rules="expenseRules" label-width="90px">
        <el-form-item label="支出类型" prop="expenseType">
          <el-select v-model="expenseForm.expenseType" placeholder="选择支出类型" style="width: 220px">
            <el-option v-for="t in expenseTypes" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input-number v-model="expenseForm.amount" :min="0.01" :precision="2" :step="10" style="width: 220px" />
        </el-form-item>
        <el-form-item label="收款方" prop="payee">
          <el-input v-model="expenseForm.payee" placeholder="请输入收款方" />
        </el-form-item>
        <el-form-item label="支出日期" prop="expenseDate">
          <el-date-picker v-model="expenseForm.expenseDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width: 220px" />
        </el-form-item>
        <el-form-item label="说明" prop="description">
          <el-input v-model="expenseForm.description" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="expenseForm.remark" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showExpenseDialog = false">取消</el-button>
        <el-button type="primary" :loading="expenseSaving" @click="submitExpense">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { api as client } from '../../api/client'

// 数据映射
const paymentMethodMap = { CASH: '现金', TRANSFER: '转账', POS: 'POS刷卡' }
const sourceTypeMap = { LONG_CARE: '长护险', COUPON: '消费券', OTHER: '其他' }
const expenseTypeMap = { FOOD: '食材采购', MEDICAL: '医疗用品', MAINTENANCE: '设施维护', SALARY: '人员工资', UTILITY: '水电费', PURCHASE: '物资采购', OTHER: '其他支出' }
const incomeTypeMap = { ELDERLY_FEE: '养老费用', SUBSIDY: '政府补贴', DONATION: '社会捐赠', RENTAL: '场地租赁', OTHER: '其他收入' }
const expenseTypes = Object.entries(expenseTypeMap).map(([value, label]) => ({ value, label }))
const incomeTypeOptions = Object.entries(incomeTypeMap).map(([value, label]) => ({ value, label }))

// 视图状态
const activeView = ref('income')

// 汇总数据
const summary = reactive({
  totalIncome: 0,
  totalExpense: 0,
  netAmount: 0
})

const selectedElderly = computed(() => {
  return elderlyOptions.value.find(e => e.id === incomeForm.elderlyId) || null
})

// 老人选项
const elderlyOptions = ref([])

// 收入列表状态
const incomeList = ref([])
const incomeTotal = ref(0)
const incomePage = ref(1)
const incomePageSize = ref(10)
const incomeLoading = ref(false)
const incomeFilter = reactive({
  elderlyId: null
})

// 支出列表状态
const expenseList = ref([])
const expenseTotal = ref(0)
const expensePage = ref(1)
const expensePageSize = ref(10)
const expenseLoading = ref(false)
const expenseFilter = reactive({
  expenseType: null,
  dateRange: null
})

// 收入弹窗状态
const showIncomeDialog = ref(false)
const incomeFormRef = ref()
const incomeSaving = ref(false)
const incomeForm = reactive({
  elderlyId: null,
  amount: 0,
  paymentMethod: 'CASH',
  sourceType: 'OTHER',
  receiptNo: '',
  remark: '',
  incomeType: 'ELDERLY_FEE',
  description: '',
  paymentDate: new Date().toISOString().slice(0, 10)
})
const incomeRules = {
  incomeType: [{ required: true, message: '请选择收入类型', trigger: 'change' }],
  elderlyId: [{ required: true, message: '请选择老人', trigger: 'change' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  paymentMethod: [{ required: true, message: '请选择方式', trigger: 'change' }],
  sourceType: [{ required: true, message: '请选择来源', trigger: 'change' }]
}

// 支出弹窗状态
const showExpenseDialog = ref(false)
const expenseFormRef = ref()
const expenseSaving = ref(false)
const expenseForm = reactive({
  expenseType: '',
  amount: 0,
  payee: '',
  expenseDate: '',
  description: '',
  remark: ''
})
const expenseRules = {
  expenseType: [{ required: true, message: '请选择支出类型', trigger: 'change' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  payee: [{ required: true, message: '请输入收款方', trigger: 'blur' }],
  expenseDate: [{ required: true, message: '请选择支出日期', trigger: 'change' }]
}

// 格式化金额
function formatMoney(val) {
  if (val === null || val === undefined) return '0.00'
  const n = Number(val)
  if (Number.isNaN(n)) return String(val)
  return n.toFixed(2)
}

// 获取当前月份 YYYY-MM
function getCurrentMonth() {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
}

// 加载老人选项
async function loadElderlyOptions() {
  try {
    const resp = await client.get('/api/finance/elderly/options')
    const body = resp.data
    if (body.code === 200) {
      elderlyOptions.value = body.data || []
    }
  } catch (e) {
    console.warn('加载老人选项失败', e)
  }
}

// 加载收支汇总
async function loadSummary() {
  try {
    const resp = await client.get('/api/finance/cashflow/summary', { params: { month: getCurrentMonth() } })
    const body = resp.data
    if (body.code === 200 && body.data) {
      summary.totalIncome = body.data.totalIncome || 0
      summary.totalExpense = body.data.totalExpense || 0
      summary.netAmount = body.data.netAmount || 0
    }
  } catch (e) {
    console.warn('加载汇总数据失败', e)
  }
}

// 加载收入列表
async function loadIncome() {
  incomeLoading.value = true
  try {
    const resp = await client.get('/api/finance/payments', {
      params: {
        page: incomePage.value,
        pageSize: incomePageSize.value,
        elderlyId: incomeFilter.elderlyId || undefined
      }
    })
    const body = resp.data
    if (body.code === 200) {
      incomeList.value = body.data?.list || []
      incomeTotal.value = body.data?.total || 0
    } else {
      throw new Error(body.msg || '加载失败')
    }
  } catch (e) {
    ElMessage.error(e.message || '加载收入列表失败')
  } finally {
    incomeLoading.value = false
  }
}

// 加载支出列表
async function loadExpense() {
  expenseLoading.value = true
  try {
    const params = {
      page: expensePage.value,
      pageSize: expensePageSize.value,
      expenseType: expenseFilter.expenseType || undefined
    }
    if (expenseFilter.dateRange && expenseFilter.dateRange.length === 2) {
      params.startDate = expenseFilter.dateRange[0]
      params.endDate = expenseFilter.dateRange[1]
    }
    const resp = await client.get('/api/finance/expenses', { params })
    const body = resp.data
    if (body.code === 200) {
      expenseList.value = body.data?.list || []
      expenseTotal.value = body.data?.total || 0
    } else {
      throw new Error(body.msg || '加载失败')
    }
  } catch (e) {
    ElMessage.error(e.message || '加载支出列表失败')
  } finally {
    expenseLoading.value = false
  }
}

// 切换视图
function switchView(view) {
  activeView.value = view
  if (view === 'income') {
    loadIncome()
  } else {
    loadExpense()
  }
}

// 收入类型切换时清空相关字段
function onIncomeTypeChange(val) {
  if (val !== 'ELDERLY_FEE') {
    incomeForm.elderlyId = null
  } else {
    incomeForm.description = ''
  }
  // 动态调整 elderlyId 的校验规则
  if (val === 'ELDERLY_FEE') {
    incomeRules.elderlyId = [{ required: true, message: '请选择老人', trigger: 'change' }]
  } else {
    incomeRules.elderlyId = []
  }
  // 清除之前的校验状态
  nextTick(() => {
    incomeFormRef.value?.clearValidate()
  })
}

// 提交收入
async function submitIncome() {
  if (!incomeFormRef.value) return
  await incomeFormRef.value.validate()
  incomeSaving.value = true
  try {
    const resp = await client.post('/api/finance/payments', {
      elderlyId: incomeForm.incomeType === 'ELDERLY_FEE' ? incomeForm.elderlyId : null,
      amount: incomeForm.amount,
      paymentMethod: incomeForm.paymentMethod,
      sourceType: incomeForm.sourceType,
      receiptNo: incomeForm.receiptNo || null,
      remark: incomeForm.remark || null,
      incomeType: incomeForm.incomeType,
      description: incomeForm.incomeType !== 'ELDERLY_FEE' ? (incomeForm.description || null) : null,
      paymentDate: incomeForm.paymentDate
    })
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '保存失败')
    ElMessage.success('保存成功')
    showIncomeDialog.value = false
    // 重置表单
    incomeForm.elderlyId = null
    incomeForm.amount = 0
    incomeForm.paymentMethod = 'CASH'
    incomeForm.sourceType = 'OTHER'
    incomeForm.receiptNo = ''
    incomeForm.remark = ''
    incomeForm.incomeType = 'ELDERLY_FEE'
    incomeForm.description = ''
    incomeForm.paymentDate = new Date().toISOString().slice(0, 10)
    // 恢复校验规则
    incomeRules.elderlyId = [{ required: true, message: '请选择老人', trigger: 'change' }]
    await loadIncome()
    await loadSummary()
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    incomeSaving.value = false
  }
}

// 提交支出
async function submitExpense() {
  if (!expenseFormRef.value) return
  await expenseFormRef.value.validate()
  expenseSaving.value = true
  try {
    const resp = await client.post('/api/finance/expenses', {
      expenseType: expenseForm.expenseType,
      amount: expenseForm.amount,
      payee: expenseForm.payee,
      expenseDate: expenseForm.expenseDate,
      description: expenseForm.description || null,
      remark: expenseForm.remark || null
    })
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '保存失败')
    ElMessage.success('保存成功')
    showExpenseDialog.value = false
    // 重置表单
    expenseForm.expenseType = ''
    expenseForm.amount = 0
    expenseForm.payee = ''
    expenseForm.expenseDate = ''
    expenseForm.description = ''
    expenseForm.remark = ''
    await loadExpense()
    await loadSummary()
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    expenseSaving.value = false
  }
}

// 删除支出
async function deleteExpense(id) {
  try {
    const resp = await client.delete('/api/finance/expenses/' + id)
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '删除失败')
    ElMessage.success('删除成功')
    await loadExpense()
    await loadSummary()
  } catch (e) {
    ElMessage.error(e.message || '删除失败')
  }
}

onMounted(() => {
  loadElderlyOptions()
  loadSummary()
  loadIncome()
})
</script>

<style scoped>
.cashflow-page {
  padding: 16px;
}

.stat-card {
  text-align: center;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
}

.pager {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}
</style>
