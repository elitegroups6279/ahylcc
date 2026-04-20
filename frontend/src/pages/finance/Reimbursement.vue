<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="header">
          <span>报账管理</span>
          <div class="header-actions">
            <el-input v-model="keyword" placeholder="事由关键字" clearable style="width: 220px" @keyup.enter="reload" />
            <el-button @click="fetchList">刷新</el-button>
            <el-button type="primary" @click="openCreate">新建报账</el-button>
          </div>
        </div>
      </template>

      <el-tabs v-model="activeStatus" @tab-change="reload">
        <el-tab-pane label="全部" name="" />
        <el-tab-pane label="待审批" name="PENDING" />
        <el-tab-pane label="已通过" name="APPROVED" />
        <el-tab-pane label="已驳回" name="REJECTED" />
        <el-tab-pane label="已支付" name="PAID" />
      </el-tabs>

      <el-table :data="list" v-loading="loading" row-key="id">
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="amount" label="金额" width="120">
          <template #default="{ row }">￥{{ formatAmount(row.amount) }}</template>
        </el-table-column>
        <el-table-column prop="reason" label="事由" min-width="260" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING' || row.status === 'APPROVING'" link type="primary" @click="approve(row)">通过</el-button>
            <el-button v-if="row.status === 'PENDING' || row.status === 'APPROVING'" link type="danger" @click="reject(row)">驳回</el-button>
            <el-button v-if="row.status === 'APPROVED'" link type="primary" @click="markPaid(row)">标记已支付</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" title="新建报账" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="金额" prop="amount">
          <el-input-number v-model="form.amount" :min="0" :precision="2" :step="10" style="width: 220px" />
        </el-form-item>
        <el-form-item label="事由" prop="reason">
          <el-input v-model="form.reason" type="textarea" :rows="3" placeholder="请输入报账事由" />
        </el-form-item>
        <el-form-item label="附件URL" prop="attachmentUrls">
          <el-input v-model="form.attachmentUrls" placeholder="可选，多个用逗号分隔" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../../api/client'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const activeStatus = ref('')
const keyword = ref('')

const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({
  amount: 0,
  reason: '',
  attachmentUrls: ''
})

const rules = {
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  reason: [{ required: true, message: '请输入事由', trigger: 'blur' }]
}

function statusText(status) {
  if (status === 'PENDING') return '待审批'
  if (status === 'APPROVING') return '审批中'
  if (status === 'APPROVED') return '已通过'
  if (status === 'REJECTED') return '已驳回'
  if (status === 'PAID') return '已支付'
  return status || '-'
}

function statusTagType(status) {
  if (status === 'PENDING' || status === 'APPROVING') return 'warning'
  if (status === 'APPROVED') return 'success'
  if (status === 'REJECTED') return 'danger'
  if (status === 'PAID') return 'info'
  return 'info'
}

function formatAmount(amount) {
  if (amount === null || amount === undefined) return '0.00'
  const n = Number(amount)
  if (Number.isNaN(n)) return String(amount)
  return n.toFixed(2)
}

async function fetchList() {
  loading.value = true
  try {
    const resp = await api.get('/api/finance/reimbursements', {
      params: {
        page: page.value,
        pageSize: pageSize.value,
        status: activeStatus.value || undefined,
        keyword: keyword.value || undefined
      }
    })
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '加载失败')
    list.value = body.data?.list || []
    total.value = body.data?.total || 0
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function reload() {
  page.value = 1
  fetchList()
}

function openCreate() {
  form.amount = 0
  form.reason = ''
  form.attachmentUrls = ''
  dialogVisible.value = true
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate()
  saving.value = true
  try {
    const resp = await api.post('/api/finance/reimbursements', {
      amount: form.amount,
      reason: form.reason,
      attachmentUrls: form.attachmentUrls || null
    })
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '提交失败')
    ElMessage.success('提交成功')
    dialogVisible.value = false
    await fetchList()
  } catch (e) {
    ElMessage.error(e.message || '提交失败')
  } finally {
    saving.value = false
  }
}

async function approve(row) {
  try {
    await ElMessageBox.confirm(`确定通过报账单 #${row.id} 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const resp = await api.put(`/api/finance/reimbursements/${row.id}/approve`)
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '操作失败')
    ElMessage.success('已通过')
    await fetchList()
  } catch (e) {
    if (e === 'cancel') return
    ElMessage.error(e.message || '操作失败')
  }
}

async function reject(row) {
  try {
    const { value } = await ElMessageBox.prompt('请输入驳回原因', '驳回', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputValidator: (v) => !!(v && String(v).trim()),
      inputErrorMessage: '驳回原因不能为空'
    })
    const resp = await api.put(`/api/finance/reimbursements/${row.id}/reject`, {
      rejectReason: value
    })
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '操作失败')
    ElMessage.success('已驳回')
    await fetchList()
  } catch (e) {
    if (e === 'cancel') return
    ElMessage.error(e.message || '操作失败')
  }
}

async function markPaid(row) {
  try {
    await ElMessageBox.confirm(`确定标记报账单 #${row.id} 已支付吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const resp = await api.put(`/api/finance/reimbursements/${row.id}/mark-paid`)
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '操作失败')
    ElMessage.success('已标记')
    await fetchList()
  } catch (e) {
    if (e === 'cancel') return
    ElMessage.error(e.message || '操作失败')
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定删除报账单 #${row.id} 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const resp = await api.delete(`/api/finance/reimbursements/${row.id}`)
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '删除失败')
    ElMessage.success('删除成功')
    await fetchList()
  } catch (e) {
    if (e === 'cancel') return
    ElMessage.error(e.message || '删除失败')
  }
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
