<template>
  <div class="page">
    <PageHeader title="集中供养资金看板" />

    <StatsCardGroup :items="statsItems" />

    <el-row :gutter="16">
      <el-col :xs="24" :lg="12">
        <el-card v-loading="loadingAlerts" class="panel-card">
          <template #header>
            <div class="card-header">
              <el-icon><Warning /></el-icon>
              <span>预警信息</span>
            </div>
          </template>
          <div v-if="alerts.length" class="alert-list">
            <div
              v-for="(alert, index) in alerts"
              :key="index"
              class="alert-card"
              :class="['alert-' + alert.level.toLowerCase()]"
            >
              <div class="alert-title">
                <span class="alert-type">{{ alertTypeLabel(alert.type) }}</span>
                <el-tag :type="levelTagType(alert.level)" size="small">{{ alert.level }}</el-tag>
              </div>
              <div class="alert-message">{{ alert.message }}</div>
            </div>
          </div>
          <el-empty v-else description="暂无预警" />
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card v-loading="loadingTrend" class="panel-card">
          <template #header>
            <div class="card-header">
              <el-icon><DataAnalysis /></el-icon>
              <span>近 6 个月消耗趋势</span>
            </div>
          </template>
          <el-table :data="trend" size="small" max-height="400">
            <el-table-column prop="month" label="月份" width="100" />
            <el-table-column prop="category" label="类别" min-width="120" />
            <el-table-column prop="totalQuantity" label="数量" width="90" />
            <el-table-column prop="totalAmount" label="金额" width="130">
              <template #default="{ row }">¥{{ formatMoney(row.totalAmount) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Money, ShoppingCart, Goods, Wallet, Warning, DataAnalysis } from '@element-plus/icons-vue'
import PageHeader from '../../components/common/PageHeader.vue'
import StatsCardGroup from '../../components/common/StatsCardGroup.vue'
import { api } from '../../api/client'
import { useFormat } from '@/composables/useFormat'

const { formatMoney } = useFormat()

const loadingSummary = ref(false)
const loadingAlerts = ref(false)
const loadingTrend = ref(false)

const summary = ref({
  monthlyAllocation: 0,
  purchasedAmount: 0,
  issuedAmount: 0,
  remainingBalance: 0
})

const alerts = ref([])
const trend = ref([])

const statsItems = computed(() => [
  {
    label: '当月拨款总额',
    value: '¥' + formatMoney(summary.value.monthlyAllocation),
    icon: Money,
    gradient: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
  },
  {
    label: '已采购金额',
    value: '¥' + formatMoney(summary.value.purchasedAmount),
    icon: ShoppingCart,
    gradient: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)'
  },
  {
    label: '已领用金额',
    value: '¥' + formatMoney(summary.value.issuedAmount),
    icon: Goods,
    gradient: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)'
  },
  {
    label: '剩余可用',
    value: '¥' + formatMoney(summary.value.remainingBalance),
    icon: Wallet,
    gradient: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)'
  }
])

function levelTagType(level) {
  const l = String(level).toLowerCase()
  if (l === 'danger') return 'danger'
  if (l === 'warning') return 'warning'
  return 'info'
}

function alertTypeLabel(type) {
  const map = {
    FUND_USAGE_HIGH: '资金使用率高',
    NEAR_EXPIRY: '临期预警',
    BUDGET_OVERRUN: '预算超支'
  }
  return map[type] || type
}

async function loadSummary() {
  loadingSummary.value = true
  try {
    const resp = await api.get('/api/warehouse/dashboard/fund-summary')
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '加载失败')
    summary.value = body.data || summary.value
  } catch (e) {
    ElMessage.error(e.message || '加载资金概览失败')
  } finally {
    loadingSummary.value = false
  }
}

async function loadAlerts() {
  loadingAlerts.value = true
  try {
    const resp = await api.get('/api/warehouse/dashboard/alerts')
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '加载失败')
    alerts.value = body.data || []
  } catch (e) {
    ElMessage.error(e.message || '加载预警失败')
  } finally {
    loadingAlerts.value = false
  }
}

async function loadTrend() {
  loadingTrend.value = true
  try {
    const resp = await api.get('/api/warehouse/dashboard/consumption-trend', { params: { months: 6 } })
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '加载失败')
    trend.value = body.data || []
  } catch (e) {
    ElMessage.error(e.message || '加载消耗趋势失败')
  } finally {
    loadingTrend.value = false
  }
}

onMounted(() => {
  loadSummary()
  loadAlerts()
  loadTrend()
})
</script>

<style scoped>
.page {
  padding: 16px;
}

.panel-card {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.alert-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.alert-card {
  padding: 14px 16px;
  border-radius: 8px;
  background: var(--color-bg-card, #fff);
  border: 1px solid var(--el-border-color-lighter);
  border-left-width: 4px;
}

.alert-warning {
  border-left-color: #e6a23c;
}

.alert-danger {
  border-left-color: #f56c6c;
}

.alert-info {
  border-left-color: #409eff;
}

.alert-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.alert-type {
  font-size: 13px;
  color: var(--color-text-secondary, #6b7280);
}

.alert-message {
  font-size: 14px;
  color: var(--color-text-primary, #1f2937);
  line-height: 1.5;
}
</style>
