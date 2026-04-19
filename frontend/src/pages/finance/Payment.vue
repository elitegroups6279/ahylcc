<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="header">
          <span>缴费管理</span>
          <div class="header-actions">
            <el-select
              v-model="elderlyId"
              filterable
              remote
              clearable
              :remote-method="searchElderly"
              :loading="elderlyLoading"
              placeholder="选择老人"
              style="width: 220px"
              @change="reload"
            >
              <el-option v-for="o in elderlyOptions" :key="o.id" :label="o.name" :value="o.id" />
            </el-select>
            <el-button @click="fetchList">刷新</el-button>
            <el-button type="primary" @click="openCreate">登记缴费</el-button>
          </div>
        </div>
      </template>

      <el-table :data="list" v-loading="loading" row-key="id">
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="elderlyName" label="老人" width="160" />
        <el-table-column prop="amount" label="金额" width="120">
          <template #default="{ row }">￥{{ formatAmount(row.amount) }}</template>
        </el-table-column>
        <el-table-column prop="paymentMethod" label="方式" width="120" />
        <el-table-column prop="sourceType" label="来源" width="140" />
        <el-table-column prop="receiptNo" label="收据号" width="140" />
        <el-table-column prop="createTime" label="时间" width="180" />
        <el-table-column prop="remark" label="备注" min-width="220" />
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

    <el-dialog v-model="dialogVisible" title="登记缴费" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="老人" prop="elderlyId">
          <el-select
            v-model="form.elderlyId"
            filterable
            remote
            clearable
            :remote-method="searchElderly"
            :loading="elderlyLoading"
            placeholder="选择老人"
            style="width: 100%"
          >
            <el-option v-for="o in elderlyOptions" :key="o.id" :label="o.name" :value="o.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input-number v-model="form.amount" :min="0" :precision="2" :step="10" style="width: 220px" />
        </el-form-item>
        <el-form-item label="方式" prop="paymentMethod">
          <el-select v-model="form.paymentMethod" placeholder="选择方式" style="width: 220px">
            <el-option label="现金" value="CASH" />
            <el-option label="转账" value="TRANSFER" />
            <el-option label="POS" value="POS" />
          </el-select>
        </el-form-item>
        <el-form-item label="来源" prop="sourceType">
          <el-select v-model="form.sourceType" placeholder="选择来源" style="width: 220px">
            <el-option label="长护险" value="LONG_CARE" />
            <el-option label="消费券" value="COUPON" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="凭证URL" prop="voucherUrl">
          <el-input v-model="form.voucherUrl" placeholder="可选" />
        </el-form-item>
        <el-form-item label="收据号" prop="receiptNo">
          <el-input v-model="form.receiptNo" placeholder="可选" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="可选" />
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../../api/client'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const elderlyId = ref(null)
const elderlyOptions = ref([])
const elderlyLoading = ref(false)

const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({
  elderlyId: null,
  amount: 0,
  paymentMethod: 'TRANSFER',
  sourceType: 'OTHER',
  voucherUrl: '',
  receiptNo: '',
  remark: ''
})

const rules = {
  elderlyId: [{ required: true, message: '请选择老人', trigger: 'change' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }]
}

function formatAmount(amount) {
  if (amount === null || amount === undefined) return '0.00'
  const n = Number(amount)
  if (Number.isNaN(n)) return String(amount)
  return n.toFixed(2)
}

async function searchElderly(query) {
  elderlyLoading.value = true
  try {
    const resp = await api.get('/api/finance/elderly/options', { params: { keyword: query || undefined } })
    const body = resp.data
    if (body.code === 200) {
      elderlyOptions.value = body.data || []
    } else {
      elderlyOptions.value = []
    }
  } catch (e) {
    elderlyOptions.value = []
  } finally {
    elderlyLoading.value = false
  }
}

async function fetchList() {
  loading.value = true
  try {
    const resp = await api.get('/api/finance/payments', {
      params: { page: page.value, pageSize: pageSize.value, elderlyId: elderlyId.value || undefined }
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
  form.elderlyId = elderlyId.value
  form.amount = 0
  form.paymentMethod = 'TRANSFER'
  form.sourceType = 'OTHER'
  form.voucherUrl = ''
  form.receiptNo = ''
  form.remark = ''
  dialogVisible.value = true
  searchElderly('')
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate()
  saving.value = true
  try {
    const resp = await api.post('/api/finance/payments', {
      elderlyId: form.elderlyId,
      amount: form.amount,
      paymentMethod: form.paymentMethod,
      sourceType: form.sourceType,
      voucherUrl: form.voucherUrl || null,
      receiptNo: form.receiptNo || null,
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
  searchElderly('')
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
