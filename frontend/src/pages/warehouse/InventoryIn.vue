<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="header">
          <span>入库管理</span>
          <div class="header-actions">
            <el-button @click="fetchList">刷新</el-button>
            <el-button type="primary" @click="openCreate">新增入库</el-button>
          </div>
        </div>
      </template>

      <el-table :data="list" v-loading="loading" row-key="id">
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="materialName" label="物资" width="200" />
        <el-table-column prop="supplier" label="供应商" width="160" />
        <el-table-column prop="purchaseOrderNo" label="采购单号" width="140" />
        <el-table-column prop="quantity" label="数量" width="100" />
        <el-table-column prop="unitPrice" label="单价" width="120">
          <template #default="{ row }">￥{{ formatAmount(row.unitPrice) }}</template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="总金额" width="120">
          <template #default="{ row }">￥{{ formatAmount(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column prop="inDate" label="入库日期" width="140" />
        <el-table-column prop="supplyCategory" label="供应类别" width="140">
          <template #default="{ row }">{{ supplyCategoryLabel(row.supplyCategory) }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="200" />
        <el-table-column v-if="isAdmin" label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openEdit(row)">
              <el-icon><Edit /></el-icon> 编辑
            </el-button>
            <el-popconfirm title="确定删除该入库记录？" @confirm="doDelete(row.id)">
              <template #reference>
                <el-button type="danger" link size="small">
                  <el-icon><Delete /></el-icon> 删除
                </el-button>
              </template>
            </el-popconfirm>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '修改入库' : '新增入库'" width="640px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="物资" prop="materialId">
          <el-select
            v-model="form.materialId"
            filterable
            remote
            clearable
            :remote-method="searchMaterials"
            :loading="materialLoading"
            placeholder="请选择物资"
            style="width: 100%"
          >
            <el-option v-for="m in materialOptions" :key="m.id" :label="m.name" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="数量" prop="quantity">
          <el-input-number v-model="form.quantity" :min="1" :max="999999" />
        </el-form-item>
        <el-form-item label="单价" prop="unitPrice">
          <el-input-number v-model="form.unitPrice" :min="0" :precision="2" :step="1" />
        </el-form-item>
        <el-form-item label="总金额" prop="totalAmount">
          <el-input-number v-model="form.totalAmount" :min="0" :precision="2" :step="1" />
          <el-text type="info" style="margin-left: 8px">不填可按数量×单价自动计算</el-text>
        </el-form-item>
        <el-form-item label="入库日期" prop="inDate">
          <el-date-picker v-model="form.inDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="供应商" prop="supplier">
          <el-input v-model="form.supplier" />
        </el-form-item>
        <el-form-item label="采购单号" prop="purchaseOrderNo">
          <el-input v-model="form.purchaseOrderNo" />
        </el-form-item>
        <el-form-item label="供应类别" prop="supplyCategory">
          <el-select v-model="form.supplyCategory" style="width: 100%">
            <el-option label="社会化物资" value="SOCIAL" />
            <el-option label="集中供养物资" value="CENTRALIZED" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!editingId" label="同步生成支出">
          <el-switch v-model="form.syncExpense" active-text="是" inactive-text="否" />
        </el-form-item>
        <el-form-item v-if="!editingId && form.syncExpense" label="出账账户" prop="bankAccountId">
          <el-select v-model="form.bankAccountId" placeholder="选择出账账户" style="width: 100%">
            <el-option v-for="acc in bankAccounts" :key="acc.id" :label="`${acc.accountName} (${acc.accountType === 'BASIC' ? '基本户' : '一般户'})`" :value="acc.id" />
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
import { ElMessage } from 'element-plus'
import { Edit, Delete } from '@element-plus/icons-vue'
import { api } from '../../api/client'
import { useAuthStore } from '../../store/auth'

const authStore = useAuthStore()
const isAdmin = computed(() => {
  const roles = authStore.user?.roles || authStore.userInfo?.roles || []
  return roles.includes('ADMIN') || roles.includes('SUPER_ADMIN')
})

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const dialogVisible = ref(false)
const formRef = ref()
const editingId = ref(null)
const form = reactive({
  materialId: null,
  supplier: '',
  purchaseOrderNo: '',
  quantity: 1,
  unitPrice: 0,
  totalAmount: null,
  inDate: '',
  supplyCategory: 'SOCIAL',
  remark: '',
  syncExpense: false,
  bankAccountId: null
})

const rules = {
  materialId: [{ required: true, message: '请选择物资', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入数量', trigger: 'blur' }]
}

const materialLoading = ref(false)
const materialOptions = ref([])
const bankAccounts = ref([])

function formatAmount(amount) {
  if (amount === null || amount === undefined) return '0.00'
  const n = Number(amount)
  if (Number.isNaN(n)) return String(amount)
  return n.toFixed(2)
}

function supplyCategoryLabel(cat) {
  if (cat === 'CENTRALIZED') return '集中供养物资'
  if (cat === 'SOCIAL') return '社会化物资'
  return cat || '社会化物资'
}

async function loadBankAccounts() {
  try {
    const resp = await api.get('/api/finance/bank-accounts')
    const body = resp.data
    if (body.code === 200) bankAccounts.value = body.data || []
  } catch (e) {
    console.warn('加载银行账户失败', e)
  }
}

async function searchMaterials(query) {
  materialLoading.value = true
  try {
    const resp = await api.get('/api/warehouse/materials', {
      params: { page: 1, pageSize: 50, keyword: query || undefined }
    })
    const body = resp.data
    if (body.code === 200) materialOptions.value = body.data?.list || []
    else materialOptions.value = []
  } catch (e) {
    materialOptions.value = []
  } finally {
    materialLoading.value = false
  }
}

async function fetchList() {
  loading.value = true
  try {
    const resp = await api.get('/api/warehouse/in', { params: { page: page.value, pageSize: pageSize.value } })
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

function resetForm() {
  const today = new Date()
  const yyyy = today.getFullYear()
  const mm = String(today.getMonth() + 1).padStart(2, '0')
  const dd = String(today.getDate()).padStart(2, '0')
  form.materialId = null
  form.supplier = ''
  form.purchaseOrderNo = ''
  form.quantity = 1
  form.unitPrice = 0
  form.totalAmount = null
  form.inDate = `${yyyy}-${mm}-${dd}`
  form.supplyCategory = 'SOCIAL'
  form.remark = ''
  form.syncExpense = false
  form.bankAccountId = null
}

function openCreate() {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
  searchMaterials('')
  loadBankAccounts()
}

function openEdit(row) {
  editingId.value = row.id
  form.materialId = row.materialId
  form.supplier = row.supplier || ''
  form.purchaseOrderNo = row.purchaseOrderNo || ''
  form.quantity = row.quantity || 1
  form.unitPrice = row.unitPrice || 0
  form.totalAmount = row.totalAmount
  form.inDate = row.inDate || ''
  form.supplyCategory = row.supplyCategory || 'SOCIAL'
  form.remark = row.remark || ''
  form.syncExpense = false
  form.bankAccountId = null
  dialogVisible.value = true
  searchMaterials('')
}

async function doDelete(id) {
  try {
    const resp = await api.delete(`/api/warehouse/in/${id}`)
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '删除失败')
    ElMessage.success('删除成功')
    await fetchList()
  } catch (e) {
    ElMessage.error(e.message || '删除失败')
  }
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      materialId: form.materialId,
      supplier: form.supplier || null,
      purchaseOrderNo: form.purchaseOrderNo || null,
      quantity: form.quantity,
      unitPrice: form.unitPrice,
      totalAmount: form.totalAmount,
      inDate: form.inDate,
      supplyCategory: form.supplyCategory,
      remark: form.remark || null
    }
    let resp
    if (editingId.value) {
      resp = await api.put(`/api/warehouse/in/${editingId.value}`, payload)
    } else {
      if (form.syncExpense) {
        payload.syncExpense = true
        payload.bankAccountId = form.bankAccountId || null
      }
      resp = await api.post('/api/warehouse/in', payload)
    }
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '保存失败')
    ElMessage.success(editingId.value ? '修改成功' : '保存成功')
    dialogVisible.value = false
    await fetchList()
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
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

.pager {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}
</style>
