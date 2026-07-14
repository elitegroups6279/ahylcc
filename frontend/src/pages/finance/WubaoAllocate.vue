<template>
  <div class="fin-page wubao-allocate-page">
    <PageHeader title="五保拨付" />

    <!-- 顶部概览卡片 -->
    <div class="fin-stat-row fin-stat-row-4" style="margin-bottom: 20px">
      <div class="fin-stat-card info">
        <div class="fin-stat-icon">
          <el-icon :size="24"><User /></el-icon>
        </div>
        <div class="fin-stat-body">
          <div class="fin-stat-label">当月五保人数</div>
          <div class="fin-stat-value">{{ summary.wubaoCount ?? '-' }}</div>
        </div>
      </div>
      <div class="fin-stat-card success">
        <div class="fin-stat-icon">
          <el-icon :size="24"><Money /></el-icon>
        </div>
        <div class="fin-stat-body">
          <div class="fin-stat-label">生活费标准</div>
          <div class="fin-stat-value">¥800</div>
        </div>
      </div>
      <div class="fin-stat-card warning">
        <div class="fin-stat-icon">
          <el-icon :size="24"><Handbag /></el-icon>
        </div>
        <div class="fin-stat-body">
          <div class="fin-stat-label">照料补助标准</div>
          <div class="fin-stat-value">¥2000</div>
        </div>
      </div>
      <div class="fin-stat-card expense">
        <div class="fin-stat-icon">
          <el-icon :size="24"><Wallet /></el-icon>
        </div>
        <div class="fin-stat-body">
          <div class="fin-stat-label">本月拨付总额</div>
          <div class="fin-stat-value">¥ {{ formatMoney(summary.totalAmount) }}</div>
        </div>
      </div>
    </div>

    <!-- 操作区域 -->
    <div class="fin-filter-bar">
      <div class="fin-filter-left">
        <el-date-picker
          v-model="selectedMonth"
          type="month"
          placeholder="选择拨付月份"
          value-format="YYYY-MM"
          style="width: 180px"
        />
        <el-input v-model="counterparty" placeholder="对方户名（默认民政局）" style="width: 200px" />
        <el-input v-model="receiptNo" placeholder="银行回单号（可选）" style="width: 200px" />
      </div>
      <div class="fin-filter-right">
        <el-button type="primary" :loading="allocating" @click="batchAllocate">
          <el-icon><Plus /></el-icon> 一键生成月度拨付
        </el-button>
      </div>
    </div>

    <!-- 拨付记录表 -->
    <div class="fin-table-card">
      <div class="fin-table-card-header">
        <div class="fin-table-card-header-left">
          <span class="fin-table-card-title">拨付记录</span>
          <span class="fin-table-card-count">{{ recordTotal }} 条</span>
        </div>
        <el-date-picker
          v-model="filterMonth"
          type="month"
          placeholder="筛选月份"
          value-format="YYYY-MM"
          style="width: 160px"
          clearable
          @change="loadRecords"
        />
      </div>

      <el-table :data="recordList" class="fin-table" v-loading="recordLoading">
        <el-table-column prop="allocateMonth" label="拨付月份" width="120" />
        <el-table-column prop="wubaoCount" label="五保人数" width="100" />
        <el-table-column label="生活费合计" width="140">
          <template #default="{ row }">
            <span class="fin-amount income">¥ {{ formatMoney(row.livingFeeTotal) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="照料补助合计" width="140">
          <template #default="{ row }">
            <span class="fin-amount warning">¥ {{ formatMoney(row.careSubsidyTotal) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="总金额" width="140">
          <template #default="{ row }">
            <span class="fin-amount expense bold">¥ {{ formatMoney(row.totalAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="counterparty" label="对方户名" width="160" />
        <el-table-column prop="receiptNo" label="回单号" width="160" />
        <el-table-column prop="createTime" label="操作时间" min-width="180" />
      </el-table>

      <div class="fin-pager">
        <el-pagination
          background
          layout="total, prev, pager, next, sizes"
          :total="recordTotal"
          :current-page="recordPage"
          :page-size="recordPageSize"
          :page-sizes="[10, 20, 50, 100]"
          @update:current-page="(p) => { recordPage = p; loadRecords() }"
          @update:page-size="(s) => { recordPageSize = s; recordPage = 1; loadRecords() }"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { User, Money, Handbag, Wallet, Plus } from '@element-plus/icons-vue'
import PageHeader from '../../components/common/PageHeader.vue'
import { api as client } from '../../api/client'

// 概览数据
const summary = ref({
  wubaoCount: 0,
  totalAmount: 0
})

// 操作表单
const selectedMonth = ref('')
const counterparty = ref('')
const receiptNo = ref('')
const allocating = ref(false)

// 记录列表
const recordList = ref([])
const recordTotal = ref(0)
const recordPage = ref(1)
const recordPageSize = ref(10)
const recordLoading = ref(false)
const filterMonth = ref(null)

// 格式化金额
function formatMoney(val) {
  if (val === null || val === undefined) return '0.00'
  const n = Number(val)
  if (Number.isNaN(n)) return String(val)
  return n.toFixed(2)
}

// 获取当前月份
function getCurrentMonth() {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
}

// 加载概览
async function loadSummary() {
  try {
    const month = selectedMonth.value || getCurrentMonth()
    const res = await client.get('/api/finance/wubao/monthly-summary', {
      params: { month }
    })
    if (res.data?.code === 200) {
      summary.value = res.data.data || {}
    }
  } catch (e) {
    console.warn('加载五保概览失败', e)
  }
}

// 加载记录
async function loadRecords() {
  recordLoading.value = true
  try {
    const params = {
      page: recordPage.value,
      size: recordPageSize.value
    }
    if (filterMonth.value) {
      params.month = filterMonth.value
    }
    const res = await client.get('/api/finance/wubao/allocation-records', { params })
    if (res.data?.code === 200) {
      recordList.value = res.data.data?.list || []
      recordTotal.value = res.data.data?.total || 0
    } else {
      throw new Error(res.data?.msg || '加载失败')
    }
  } catch (e) {
    ElMessage.error(e.message || '加载拨付记录失败')
  } finally {
    recordLoading.value = false
  }
}

// 一键拨付
async function batchAllocate() {
  if (!selectedMonth.value) {
    ElMessage.warning('请选择拨付月份')
    return
  }
  allocating.value = true
  try {
    const res = await client.post('/api/finance/wubao/batch-allocate', {
      allocateMonth: selectedMonth.value,
      counterparty: counterparty.value || '民政局',
      receiptNo: receiptNo.value || null
    })
    if (res.data?.code !== 200) throw new Error(res.data?.msg || '拨付失败')
    ElMessage.success('月度拨付生成成功')
    await loadSummary()
    await loadRecords()
  } catch (e) {
    ElMessage.error(e.message || '拨付失败')
  } finally {
    allocating.value = false
  }
}

onMounted(() => {
  selectedMonth.value = getCurrentMonth()
  loadSummary()
  loadRecords()
})
</script>

<style scoped>
.wubao-allocate-page {
  /* page-specific overrides if needed */
}
</style>
