<template>
  <div class="page" v-loading="loading">
    <el-card>
      <template #header>
        <div class="header">
          <span>老人档案</span>
          <div class="header-actions">
            <el-button @click="goBack">返回</el-button>
          </div>
        </div>
      </template>

      <el-descriptions :column="3" border>
        <el-descriptions-item label="编号">{{ detail.uniqueNo }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ detail.name }}</el-descriptions-item>
        <el-descriptions-item label="身份证">{{ detail.idCardMasked }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ detail.gender === 1 ? '男' : detail.gender === 0 ? '女' : '-' }}</el-descriptions-item>
        <el-descriptions-item label="年龄">{{ detail.age ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="入住日期">{{ detail.admissionDate }}</el-descriptions-item>
        <el-descriptions-item label="床位">{{ detail.bedNumber || '-' }}</el-descriptions-item>
        <el-descriptions-item label="类别">{{ categoryText(detail.category) }}</el-descriptions-item>
        <el-descriptions-item label="失能等级">
          <el-tag v-if="detail.disabilityLevel === 'SELF_CARE'" type="success">自理</el-tag>
          <el-tag v-else-if="detail.disabilityLevel === 'MILD'" type="primary">轻度失能</el-tag>
          <el-tag v-else-if="detail.disabilityLevel === 'MODERATE'" type="warning">中度失能</el-tag>
          <el-tag v-else-if="detail.disabilityLevel === 'SEVERE'" type="danger">重度失能</el-tag>
          <el-tag v-else type="info">自理</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="余额">￥{{ formatAmount(detail.feeBalance) }}</el-descriptions-item>
        <el-descriptions-item label="付款方式">{{ detail.paymentMethod || '-' }}</el-descriptions-item>
        <el-descriptions-item label="合同月费">￥{{ formatAmount(detail.contractMonthlyFee) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detail.status === 'ACTIVE' ? 'success' : 'info'">
            {{ detail.status === 'ACTIVE' ? '在住' : '退住' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">联系人</el-divider>
      <el-table :data="detail.contacts || []" row-key="id" size="small">
        <el-table-column prop="name" label="姓名" width="140" />
        <el-table-column prop="relationship" label="关系" width="120" />
        <el-table-column prop="phone" label="电话" width="160" />
        <el-table-column label="紧急联系人" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="row.isEmergency === 1 ? 'danger' : 'info'">{{ row.isEmergency === 1 ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
      </el-table>

      <el-divider content-position="left">关联护工</el-divider>
      <div v-if="staffNames.length === 0">
        <el-empty description="暂无关联护工" :image-size="80" />
      </div>
      <div v-else class="tags">
        <el-tag v-for="s in staffNames" :key="s.id" style="margin-right: 8px">{{ s.name }}</el-tag>
      </div>

      <el-divider content-position="left">缴费记录</el-divider>
      <el-table :data="payments" row-key="id" size="small" v-loading="paymentLoading">
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="amount" label="金额" width="120">
          <template #default="{ row }">￥{{ formatAmount(row.amount) }}</template>
        </el-table-column>
        <el-table-column prop="paymentMethod" label="方式" width="120" />
        <el-table-column prop="sourceType" label="来源" width="140" />
        <el-table-column prop="receiptNo" label="收据号" width="140" />
        <el-table-column prop="createTime" label="时间" width="180" />
        <el-table-column prop="remark" label="备注" min-width="200" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '../../api/client'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const paymentLoading = ref(false)
const detail = reactive({})
const staffNames = ref([])
const payments = ref([])

function formatAmount(amount) {
  if (amount === null || amount === undefined) return '0.00'
  const n = Number(amount)
  if (Number.isNaN(n)) return String(amount)
  return n.toFixed(2)
}

function categoryText(c) {
  if (c === 'SOCIAL') return '社会化'
  if (c === 'LOW_BAO') return '低保对象'
  if (c === 'WU_BAO') return '五保对象'
  return c || '-'
}

function goBack() {
  router.back()
}

async function fetchDetail() {
  loading.value = true
  try {
    const resp = await api.get(`/api/elderly/${route.params.id}`)
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '加载失败')
    Object.assign(detail, body.data || {})
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function fetchStaffNames() {
  const ids = detail.staffIds || []
  if (!ids || ids.length === 0) {
    staffNames.value = []
    return
  }
  try {
    const resp = await api.get('/api/staff/by-ids', { params: { ids } })
    const body = resp.data
    if (body.code === 200) staffNames.value = body.data || []
    else staffNames.value = []
  } catch (e) {
    staffNames.value = []
  }
}

async function fetchPayments() {
  paymentLoading.value = true
  try {
    const resp = await api.get('/api/finance/payments', { params: { page: 1, pageSize: 50, elderlyId: detail.id } })
    const body = resp.data
    if (body.code === 200) payments.value = body.data?.list || []
    else payments.value = []
  } catch (e) {
    payments.value = []
  } finally {
    paymentLoading.value = false
  }
}

onMounted(async () => {
  await fetchDetail()
  await fetchStaffNames()
  await fetchPayments()
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
}

.tags {
  padding: 6px 0;
}
</style>
