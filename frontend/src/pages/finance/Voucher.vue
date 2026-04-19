<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="header">
          <span>凭证管理</span>
          <div class="header-actions">
            <el-date-picker
              v-model="voucherMonth"
              type="month"
              clearable
              placeholder="选择月份"
              style="width: 160px"
              @change="reload"
            />
            <el-select v-model="voucherType" clearable placeholder="类型" style="width: 140px" @change="reload">
              <el-option label="收入" value="INCOME" />
              <el-option label="支出" value="EXPENSE" />
            </el-select>
            <el-button @click="fetchList">刷新</el-button>
            <el-button type="primary" @click="openCreate">新增凭证</el-button>
          </div>
        </div>
      </template>

      <el-table :data="list" v-loading="loading" row-key="id">
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="voucherNo" label="凭证号" width="160" />
        <el-table-column prop="voucherType" label="类型" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.voucherType === 'INCOME'" type="success">收入</el-tag>
            <el-tag v-else-if="row.voucherType === 'EXPENSE'" type="danger">支出</el-tag>
            <span v-else>{{ row.voucherType }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="类别" width="160" />
        <el-table-column prop="amount" label="金额" width="140">
          <template #default="{ row }">￥{{ formatAmount(row.amount) }}</template>
        </el-table-column>
        <el-table-column prop="voucherDate" label="凭证日期" width="140" />
        <el-table-column prop="relatedId" label="关联ID" width="120" />
        <el-table-column prop="attachmentUrl" label="附件" min-width="220">
          <template #default="{ row }">
            <el-link v-if="row.attachmentUrl" :href="row.attachmentUrl" target="_blank" type="primary">查看</el-link>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="说明" min-width="220" show-overflow-tooltip />
        <el-table-column prop="operatorId" label="操作员ID" width="110" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
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

    <el-dialog v-model="dialogVisible" title="新增凭证" width="620px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="类型" prop="voucherType">
          <el-select v-model="form.voucherType" placeholder="选择类型" style="width: 220px">
            <el-option label="收入" value="INCOME" />
            <el-option label="支出" value="EXPENSE" />
          </el-select>
        </el-form-item>
        <el-form-item label="类别" prop="category">
          <el-input v-model="form.category" placeholder="可选，如：缴费/采购/薪资/水电" />
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input-number v-model="form.amount" :min="0" :precision="2" :step="10" style="width: 220px" />
        </el-form-item>
        <el-form-item label="关联ID" prop="relatedId">
          <el-input-number v-model="form.relatedId" :min="0" style="width: 220px" />
        </el-form-item>
        <el-form-item label="附件URL" prop="attachmentUrl">
          <el-input v-model="form.attachmentUrl" placeholder="可选" />
        </el-form-item>
        <el-form-item label="说明" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="可选" />
        </el-form-item>
        <el-form-item label="日期" prop="voucherDate">
          <el-date-picker v-model="form.voucherDate" type="date" placeholder="选择日期" style="width: 220px" />
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../../api/client'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const voucherMonth = ref(null)
const voucherType = ref('')

const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({
  voucherType: 'EXPENSE',
  category: '',
  amount: 0,
  relatedId: null,
  attachmentUrl: '',
  description: '',
  voucherDate: null
})

const rules = {
  voucherType: [{ required: true, message: '请选择类型', trigger: 'change' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  voucherDate: [{ required: true, message: '请选择日期', trigger: 'change' }]
}

function pad2(n) {
  const s = String(n)
  return s.length >= 2 ? s : `0${s}`
}

function formatMonth(d) {
  if (!d) return null
  const date = d instanceof Date ? d : new Date(d)
  if (Number.isNaN(date.getTime())) return null
  return `${date.getFullYear()}-${pad2(date.getMonth() + 1)}`
}

function formatDate(d) {
  if (!d) return null
  const date = d instanceof Date ? d : new Date(d)
  if (Number.isNaN(date.getTime())) return null
  return `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())}`
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
    const resp = await api.get('/api/finance/vouchers', {
      params: {
        page: page.value,
        pageSize: pageSize.value,
        voucherMonth: formatMonth(voucherMonth.value) || undefined,
        voucherType: voucherType.value || undefined
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
  form.voucherType = 'EXPENSE'
  form.category = ''
  form.amount = 0
  form.relatedId = null
  form.attachmentUrl = ''
  form.description = ''
  form.voucherDate = new Date()
  dialogVisible.value = true
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate()
  saving.value = true
  try {
    const resp = await api.post('/api/finance/vouchers', {
      voucherType: form.voucherType,
      category: form.category || null,
      amount: form.amount,
      relatedId: form.relatedId || null,
      attachmentUrl: form.attachmentUrl || null,
      description: form.description || null,
      voucherDate: formatDate(form.voucherDate)
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

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确认删除凭证 ${row.voucherNo || row.id}？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  saving.value = true
  try {
    const resp = await api.delete(`/api/finance/vouchers/${row.id}`)
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '删除失败')
    ElMessage.success('删除成功')
    await fetchList()
  } catch (e) {
    ElMessage.error(e.message || '删除失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.page {
  padding: 12px;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
}
</style>
