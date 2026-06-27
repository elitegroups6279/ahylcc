<template>
  <div class="wubao-allocate-page">
    <!-- 顶部概览卡片 -->
    <el-row :gutter="16" style="margin-bottom: 20px">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">当月五保人数</div>
          <div class="stat-value" style="color: #409EFF">{{ summary.wubaoCount ?? '-' }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">生活费标准</div>
          <div class="stat-value" style="color: #67C23A">¥800</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">照料补助标准</div>
          <div class="stat-value" style="color: #E6A23C">¥2000</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">本月拨付总额</div>
          <div class="stat-value" style="color: #F56C6C">¥ {{ formatMoney(summary.totalAmount) }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 操作区域 -->
    <el-card shadow="hover" style="margin-bottom: 20px">
      <el-row :gutter="16" align="middle">
        <el-col :span="6">
          <el-date-picker
            v-model="selectedMonth"
            type="month"
            placeholder="选择拨付月份"
            value-format="YYYY-MM"
            style="width: 100%"
          />
        </el-col>
        <el-col :span="5">
          <el-input v-model="counterparty" placeholder="对方户名（默认民政局）" />
        </el-col>
        <el-col :span="5">
          <el-input v-model="receiptNo" placeholder="银行回单号（可选）" />
        </el-col>
        <el-col :span="8">
          <el-button type="primary" :loading="allocating" @click="batchAllocate">
            一键生成月度拨付
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 拨付记录表 -->
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>拨付记录</span>
          <el-date-picker
            v-model="filterMonth"
            type="month"
            placeholder="筛选月份"
            value-format="YYYY-MM"
            style="width: 180px"
            clearable
            @change="loadRecords"
          />
        </div>
      </template>

      <el-table :data="recordList" border stripe v-loading="recordLoading">
        <el-table-column prop="allocateMonth" label="拨付月份" width="120" />
        <el-table-column prop="wubaoCount" label="五保人数" width="100" />
        <el-table-column label="生活费合计" width="140">
          <template #default="{ row }">
            <span style="color: #67C23A; font-weight: bold">¥ {{ formatMoney(row.livingFeeTotal) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="照料补助合计" width="140">
          <template #default="{ row }">
            <span style="color: #E6A23C; font-weight: bold">¥ {{ formatMoney(row.careSubsidyTotal) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="总金额" width="140">
          <template #default="{ row }">
            <span style="color: #F56C6C; font-weight: bold">¥ {{ formatMoney(row.totalAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="counterparty" label="对方户名" width="160" />
        <el-table-column prop="receiptNo" label="回单号" width="160" />
        <el-table-column prop="createTime" label="操作时间" min-width="180" />
      </el-table>

      <div class="pager">
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
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
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

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
  font-size: 16px;
}

.pager {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}
</style>
