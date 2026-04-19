<template>
  <div class="page">
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span>补贴核算对账台</span>
          <el-date-picker
            v-model="month"
            type="month"
            value-format="YYYY-MM"
            placeholder="选择月份"
            @change="loadData"
          />
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <!-- Tab 1: 机构收入汇总 -->
        <el-tab-pane label="机构收入汇总" name="overview">
          <el-row :gutter="16" style="margin-bottom: 20px;">
            <el-col :span="4" v-for="item in summaryCards" :key="item.label">
              <div class="summary-card" :style="{ borderLeftColor: item.color }">
                <div class="summary-label">{{ item.label }}</div>
                <div class="summary-value" :style="{ color: item.color }">
                  ¥{{ item.value.toFixed(2) }}
                </div>
              </div>
            </el-col>
          </el-row>
          <el-descriptions :column="3" border>
            <el-descriptions-item label="统计月份">{{ month }}</el-descriptions-item>
            <el-descriptions-item label="在住老人数">{{ overview.elderlyCount || 0 }} 人</el-descriptions-item>
            <el-descriptions-item label="账单数">{{ overview.billCount || 0 }} 笔</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>

        <!-- Tab 2: 政府应拨明细 -->
        <el-tab-pane label="政府应拨明细" name="gov">
          <el-table :data="govDetails" border show-summary :summary-method="govSummary">
            <el-table-column prop="elderlyName" label="老人姓名" />
            <el-table-column label="类别">
              <template #default="{ row }">
                <el-tag size="small">{{ categoryMap[row.category] || row.category }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="policyName" label="补贴项目" />
            <el-table-column prop="amount" label="金额" align="right">
              <template #default="{ row }">¥{{ Number(row.amount).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column prop="calcDesc" label="计算说明" />
          </el-table>
        </el-tab-pane>

        <!-- Tab 3: 个人账户补助 -->
        <el-tab-pane label="个人账户补助" name="personal">
          <el-alert type="info" :closable="false" style="margin-bottom: 16px;">
            以下补助金额打到老人个人银行账户，不经过机构。
          </el-alert>
          <el-table :data="personalDetails" border show-summary>
            <el-table-column prop="elderlyName" label="老人姓名" />
            <el-table-column label="类别">
              <template #default="{ row }">
                <el-tag size="small" type="warning">{{ categoryMap[row.category] || row.category }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="policyName" label="补贴项目" />
            <el-table-column prop="amount" label="金额" align="right">
              <template #default="{ row }">¥{{ Number(row.amount).toFixed(2) }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../../api/client'

const month = ref(new Date().toISOString().slice(0, 7))
const activeTab = ref('overview')
const overview = ref({})
const govDetails = ref([])
const personalDetails = ref([])

const categoryMap = {
  WU_BAO: '五保',
  LOW_BAO: '低保',
  LOW_EDGE: '低保边缘',
  TE_KUN: '特困',
  ORDINARY: '普通',
  DISABILITY: '残疾',
  VETERAN: '优抚'
}

const summaryCards = computed(() => [
  { label: '基础费用合计', value: overview.value.totalBaseFee || 0, color: '#1890ff' },
  { label: '家属应缴合计', value: overview.value.totalFamilyPayable || 0, color: '#ff4d4f' },
  { label: '政府应拨合计', value: overview.value.totalGovPayable || 0, color: '#fa8c16' },
  { label: '长护险合计', value: overview.value.totalLongCare || 0, color: '#52c41a' },
  { label: '消费券抵扣', value: overview.value.totalCouponDeduct || 0, color: '#722ed1' },
  { label: '财政补助合计', value: overview.value.totalSubsidy || 0, color: '#13c2c2' }
])

function govSummary({ columns, data }) {
  const sums = []
  columns.forEach((col, index) => {
    if (index === 0) {
      sums[index] = '合计'
      return
    }
    if (col.property === 'amount') {
      const val = data.reduce((prev, curr) => prev + Number(curr.amount || 0), 0)
      sums[index] = '¥' + val.toFixed(2)
    } else {
      sums[index] = ''
    }
  })
  return sums
}

async function loadData() {
  try {
    const resp = await api.get('/api/finance/bills/subsidy-summary', {
      params: { month: month.value }
    })
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '加载失败')
    overview.value = body.data?.overview || {}
    govDetails.value = body.data?.govDetails || []
    personalDetails.value = body.data?.personalDetails || []
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.page {
  padding: 16px;
}

.summary-card {
  background: #fafafa;
  border-left: 4px solid;
  padding: 16px;
  border-radius: 4px;
  margin-bottom: 12px;
}

.summary-label {
  font-size: 13px;
  color: #909399;
}

.summary-value {
  font-size: 22px;
  font-weight: bold;
  margin-top: 8px;
}
</style>
