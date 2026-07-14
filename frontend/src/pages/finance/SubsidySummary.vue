<template>
  <div class="fin-page subsidy-page">
    <PageHeader title="补贴核算对账台">
      <template #actions>
        <el-date-picker
          v-model="month"
          type="month"
          value-format="YYYY-MM"
          placeholder="选择月份"
          @change="loadData"
        />
      </template>
    </PageHeader>

    <!-- 概览卡片 -->
    <div class="fin-stat-row fin-stat-row-4">
      <div class="fin-stat-card primary">
        <div class="fin-stat-icon"><HomeFilled /></div>
        <div class="fin-stat-body">
          <div class="fin-stat-label">在住老人数</div>
          <div class="fin-stat-value">{{ overview.elderlyCount || 0 }} 人</div>
        </div>
      </div>
      <div class="fin-stat-card info">
        <div class="fin-stat-icon"><Document /></div>
        <div class="fin-stat-body">
          <div class="fin-stat-label">账单数</div>
          <div class="fin-stat-value">{{ overview.billCount || 0 }} 笔</div>
        </div>
      </div>
      <div class="fin-stat-card warning">
        <div class="fin-stat-icon"><Wallet /></div>
        <div class="fin-stat-body">
          <div class="fin-stat-label">政府应拨合计</div>
          <div class="fin-stat-value">¥{{ (overview.totalGovPayable || 0).toFixed(2) }}</div>
        </div>
      </div>
      <div class="fin-stat-card success">
        <div class="fin-stat-icon"><Money /></div>
        <div class="fin-stat-body">
          <div class="fin-stat-label">财政补助合计</div>
          <div class="fin-stat-value">¥{{ (overview.totalSubsidy || 0).toFixed(2) }}</div>
        </div>
      </div>
    </div>

    <!-- 详细统计卡片 -->
    <div class="fin-stat-row fin-stat-row-4">
      <div class="fin-stat-card info">
        <div class="fin-stat-body">
          <div class="fin-stat-label">基础费用合计</div>
          <div class="fin-stat-value">¥{{ (overview.totalBaseFee || 0).toFixed(2) }}</div>
        </div>
      </div>
      <div class="fin-stat-card expense">
        <div class="fin-stat-body">
          <div class="fin-stat-label">家属应缴合计</div>
          <div class="fin-stat-value">¥{{ (overview.totalFamilyPayable || 0).toFixed(2) }}</div>
        </div>
      </div>
      <div class="fin-stat-card success">
        <div class="fin-stat-body">
          <div class="fin-stat-label">长护险合计</div>
          <div class="fin-stat-value">¥{{ (overview.totalLongCare || 0).toFixed(2) }}</div>
        </div>
      </div>
      <div class="fin-stat-card purple">
        <div class="fin-stat-body">
          <div class="fin-stat-label">消费券抵扣</div>
          <div class="fin-stat-value">¥{{ (overview.totalCouponDeduct || 0).toFixed(2) }}</div>
        </div>
      </div>
    </div>

    <div class="fin-card">
      <el-tabs v-model="activeTab" class="fin-tabs">
        <!-- Tab 1: 机构收入汇总 -->
        <el-tab-pane label="机构收入汇总" name="overview">
          <div class="fin-card-body">
            <el-descriptions :column="3" border>
              <el-descriptions-item label="统计月份">{{ month }}</el-descriptions-item>
              <el-descriptions-item label="在住老人数">{{ overview.elderlyCount || 0 }} 人</el-descriptions-item>
              <el-descriptions-item label="账单数">{{ overview.billCount || 0 }} 笔</el-descriptions-item>
            </el-descriptions>
          </div>
        </el-tab-pane>

        <!-- Tab 2: 政府应拨明细 -->
        <el-tab-pane label="政府应拨明细" name="gov">
          <el-table :data="govDetails" border show-summary :summary-method="govSummary" class="fin-table">
            <el-table-column prop="elderlyName" label="老人姓名" />
            <el-table-column label="类别">
              <template #default="{ row }">
                <el-tag size="small" class="fin-type-tag">{{ categoryMap[row.category] || row.category }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="policyName" label="补贴项目" />
            <el-table-column prop="amount" label="金额" align="right">
              <template #default="{ row }"><span class="fin-amount warning">¥{{ Number(row.amount).toFixed(2) }}</span></template>
            </el-table-column>
            <el-table-column prop="calcDesc" label="计算说明" />
          </el-table>
        </el-tab-pane>

        <!-- Tab 3: 个人账户补助 -->
        <el-tab-pane label="个人账户补助" name="personal">
          <div class="fin-card-body">
            <el-alert type="info" :closable="false" style="margin-bottom: 16px;">
              以下补助金额打到老人个人银行账户，不经过机构。
            </el-alert>
            <el-table :data="personalDetails" border show-summary class="fin-table">
              <el-table-column prop="elderlyName" label="老人姓名" />
              <el-table-column label="类别">
                <template #default="{ row }">
                  <el-tag size="small" type="warning" class="fin-type-tag">{{ categoryMap[row.category] || row.category }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="policyName" label="补贴项目" />
              <el-table-column prop="amount" label="金额" align="right">
                <template #default="{ row }"><span class="fin-amount success">¥{{ Number(row.amount).toFixed(2) }}</span></template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { HomeFilled, Document, Wallet, Money } from '@element-plus/icons-vue'
import PageHeader from '../../components/common/PageHeader.vue'
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
.subsidy-page {
  padding: 16px;
}
</style>
