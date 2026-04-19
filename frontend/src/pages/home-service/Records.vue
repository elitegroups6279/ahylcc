<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="header">
          <span>服务记录</span>
          <div class="header-actions">
            <el-input v-model="orderIdInput" placeholder="订单ID" clearable style="width: 160px" @keyup.enter="applyOrderId" />
            <el-button @click="applyOrderId">筛选</el-button>
            <el-button @click="fetchList">刷新</el-button>
            <el-button type="primary" @click="openCreate">新增记录</el-button>
          </div>
        </div>
      </template>

      <div style="overflow-x: auto">
      <el-table :data="list" v-loading="loading" row-key="id">
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="orderId" label="订单ID" width="100" />
        <el-table-column prop="actualStartTime" label="开始时间" width="180" />
        <el-table-column prop="actualEndTime" label="结束时间" width="180" />
        <el-table-column prop="amount" label="金额" width="120">
          <template #default="{ row }">￥{{ formatAmount(row.amount) }}</template>
        </el-table-column>
        <el-table-column prop="paymentStatus" label="结算状态" width="120" />
        <el-table-column prop="rating" label="评分" width="90" />
        <el-table-column prop="serviceContent" label="服务内容" min-width="260" />
      </el-table>
      </div>

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

    <el-dialog v-model="dialogVisible" title="新增服务记录" width="760px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12">
            <el-form-item label="订单ID" prop="orderId">
              <el-input v-model="form.orderId" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="金额" prop="amount">
              <el-input-number v-model="form.amount" :min="0" :precision="2" :step="10" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12">
            <el-form-item label="开始时间" prop="actualStartTime">
              <el-date-picker v-model="form.actualStartTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="结束时间" prop="actualEndTime">
              <el-date-picker v-model="form.actualEndTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="内容" prop="serviceContent">
          <el-input v-model="form.serviceContent" type="textarea" :rows="4" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12">
            <el-form-item label="签字" prop="signatureUrl">
              <el-upload
                action="/api/upload/image"
                :headers="uploadHeaders"
                :show-file-list="false"
                :on-success="onSignatureUploadSuccess"
                accept="image/*"
              >
                <el-image v-if="form.signatureUrl" :src="fullUrl(form.signatureUrl)" fit="contain" style="width: 200px; height: 80px; border: 1px dashed #dcdfe6; border-radius: 4px" />
                <el-button v-else size="small">上传签字图片</el-button>
              </el-upload>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="评分" prop="rating">
              <el-input-number v-model="form.rating" :min="1" :max="5" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="结算状态" prop="paymentStatus">
          <el-select v-model="form.paymentStatus" style="width: 220px">
            <el-option label="未结算" value="UNPAID" />
            <el-option label="已结算" value="PAID" />
            <el-option label="合并结算" value="MERGED" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '../../api/client'
import { useAuthStore } from '../../store/auth'

const route = useRoute()
const authStore = useAuthStore()
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const orderId = ref(null)
const orderIdInput = ref('')

const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({
  orderId: '',
  actualStartTime: '',
  actualEndTime: '',
  serviceContent: '',
  signatureUrl: '',
  rating: 5,
  amount: 0,
  paymentStatus: 'UNPAID',
  remark: ''
})

const rules = {
  orderId: [{ required: true, message: '请输入订单ID', trigger: 'blur' }]
}

const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${authStore.accessToken}`
}))

function fullUrl(url) {
  if (!url) return ''
  if (url.startsWith('http')) return url
  return apiBaseUrl + url
}

function onSignatureUploadSuccess(resp) {
  if (resp.code === 200 && resp.data) {
    form.signatureUrl = resp.data
  } else {
    ElMessage.error('上传失败')
  }
}

function formatAmount(amount) {
  if (amount === null || amount === undefined) return '0.00'
  const n = Number(amount)
  if (Number.isNaN(n)) return String(amount)
  return n.toFixed(2)
}

function applyOrderId() {
  const v = orderIdInput.value ? String(orderIdInput.value).trim() : ''
  orderId.value = v ? Number(v) : null
  page.value = 1
  fetchList()
}

async function fetchList() {
  loading.value = true
  try {
    const resp = await api.get('/api/home-service/records', {
      params: { page: page.value, pageSize: pageSize.value, orderId: orderId.value || undefined }
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

function openCreate() {
  form.orderId = orderId.value ? String(orderId.value) : ''
  form.actualStartTime = ''
  form.actualEndTime = ''
  form.serviceContent = ''
  form.signatureUrl = ''
  form.rating = 5
  form.amount = 0
  form.paymentStatus = 'UNPAID'
  form.remark = ''
  dialogVisible.value = true
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate()
  saving.value = true
  try {
    const resp = await api.post('/api/home-service/records', {
      orderId: Number(form.orderId),
      actualStartTime: form.actualStartTime || null,
      actualEndTime: form.actualEndTime || null,
      serviceContent: form.serviceContent || null,
      signatureUrl: form.signatureUrl || null,
      rating: form.rating || null,
      amount: form.amount,
      paymentStatus: form.paymentStatus || 'UNPAID',
      remark: form.remark || null
    })
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '保存失败')
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await fetchList()
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  const q = route.query.orderId
  if (q) {
    orderId.value = Number(q)
    orderIdInput.value = String(q)
  }
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
