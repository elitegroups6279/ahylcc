<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="header">
          <span>发药管理</span>
          <div class="header-actions">
            <el-button @click="reloadAll">刷新</el-button>
          </div>
        </div>
      </template>

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="批次入库" name="batches" />
        <el-tab-pane label="发药单" name="orders" />
        <el-tab-pane label="近效期预警" name="warnings" />
      </el-tabs>

      <div v-show="activeTab === 'batches'">
        <div class="toolbar">
          <el-select
            v-model="batchDrugId"
            filterable
            remote
            clearable
            :remote-method="searchDrugs"
            :loading="drugLoading"
            placeholder="筛选药品"
            style="width: 240px"
            @change="reloadBatches"
          >
            <el-option v-for="d in drugOptions" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
          <el-button type="primary" @click="openBatchCreate">批次入库</el-button>
        </div>

        <el-table :data="batches" v-loading="batchLoading" row-key="id">
          <el-table-column prop="id" label="ID" width="90" />
          <el-table-column prop="drugName" label="药品" width="200" />
          <el-table-column prop="batchNo" label="批号" width="140" />
          <el-table-column prop="quantity" label="入库数量" width="110" />
          <el-table-column prop="remaining" label="剩余" width="90" />
          <el-table-column prop="expiryDate" label="有效期" width="140" />
          <el-table-column prop="inDate" label="入库日期" width="140" />
          <el-table-column prop="supplier" label="供应商" min-width="160" />
        </el-table>

        <div class="pager">
          <el-pagination
            background
            layout="total, prev, pager, next, sizes"
            :total="batchTotal"
            :current-page="batchPage"
            :page-size="batchPageSize"
            :page-sizes="[10, 20, 50, 100]"
            @update:current-page="(p) => { batchPage = p; fetchBatches() }"
            @update:page-size="(s) => { batchPageSize = s; batchPage = 1; fetchBatches() }"
          />
        </div>
      </div>

      <div v-show="activeTab === 'orders'">
        <div class="toolbar">
          <el-select v-model="orderStatus" placeholder="状态" clearable style="width: 160px" @change="reloadOrders">
            <el-option label="待发药" value="PENDING" />
            <el-option label="已发药" value="DISPENSED" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
          <el-button type="primary" @click="openOrderCreate">创建发药单</el-button>
        </div>

        <el-table :data="orders" v-loading="orderLoading" row-key="id">
          <el-table-column prop="id" label="ID" width="90" />
          <el-table-column prop="elderlyName" label="老人" width="160" />
          <el-table-column prop="orderDate" label="日期" width="140" />
          <el-table-column prop="status" label="状态" width="120" />
          <el-table-column prop="remark" label="备注" min-width="200" />
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" v-if="row.status === 'PENDING'" @click="openConfirm(row)">确认发药</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pager">
          <el-pagination
            background
            layout="total, prev, pager, next, sizes"
            :total="orderTotal"
            :current-page="orderPage"
            :page-size="orderPageSize"
            :page-sizes="[10, 20, 50, 100]"
            @update:current-page="(p) => { orderPage = p; fetchOrders() }"
            @update:page-size="(s) => { orderPageSize = s; orderPage = 1; fetchOrders() }"
          />
        </div>
      </div>

      <div v-show="activeTab === 'warnings'">
        <div class="toolbar">
          <el-input-number v-model="warningDays" :min="1" :max="365" />
          <el-button type="primary" @click="fetchWarnings">查询</el-button>
        </div>
        <el-table :data="warnings" v-loading="warningLoading" row-key="batchId">
          <el-table-column prop="drugName" label="药品" width="200" />
          <el-table-column prop="batchNo" label="批号" width="140" />
          <el-table-column prop="remaining" label="剩余" width="90" />
          <el-table-column prop="expiryDate" label="有效期" width="140" />
          <el-table-column prop="daysToExpiry" label="剩余天数" width="110" />
        </el-table>
      </div>
    </el-card>

    <el-dialog v-model="batchDialogVisible" title="批次入库" width="620px">
      <el-form ref="batchFormRef" :model="batchForm" :rules="batchRules" label-width="90px">
        <el-form-item label="药品" prop="drugId">
          <el-select
            v-model="batchForm.drugId"
            filterable
            remote
            clearable
            :remote-method="searchDrugs"
            :loading="drugLoading"
            placeholder="请选择药品"
            style="width: 100%"
          >
            <el-option v-for="d in drugOptions" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="批号" prop="batchNo">
          <el-input v-model="batchForm.batchNo" />
        </el-form-item>
        <el-form-item label="数量" prop="quantity">
          <el-input-number v-model="batchForm.quantity" :min="1" :max="999999" />
        </el-form-item>
        <el-form-item label="有效期" prop="expiryDate">
          <el-date-picker v-model="batchForm.expiryDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="入库日期" prop="inDate">
          <el-date-picker v-model="batchForm.inDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="供应商" prop="supplier">
          <el-input v-model="batchForm.supplier" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitBatch">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="orderDialogVisible" title="创建发药单" width="620px">
      <el-form ref="orderFormRef" :model="orderForm" :rules="orderRules" label-width="90px">
        <el-form-item label="老人" prop="elderlyId">
          <el-select
            v-model="orderForm.elderlyId"
            filterable
            remote
            clearable
            :remote-method="searchElderly"
            :loading="elderlyLoading"
            placeholder="请选择老人"
            style="width: 100%"
          >
            <el-option v-for="o in elderlyOptions" :key="o.id" :label="elderlyLabel(o)" :value="o.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期" prop="orderDate">
          <el-date-picker v-model="orderForm.orderDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="orderForm.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="orderDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitOrder">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="confirmDialogVisible" title="确认发药" width="820px">
      <div class="toolbar">
        <el-button type="primary" @click="addConfirmRow">新增明细</el-button>
      </div>
      <el-table :data="confirmItems" row-key="__k" size="small">
        <el-table-column label="药品" min-width="220">
          <template #default="{ row }">
            <el-select
              v-model="row.drugId"
              filterable
              remote
              clearable
              :remote-method="searchDrugs"
              :loading="drugLoading"
              placeholder="选择药品"
              style="width: 100%"
            >
              <el-option v-for="d in drugOptions" :key="d.id" :label="d.name" :value="d.id" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="数量" width="120">
          <template #default="{ row }">
            <el-input-number v-model="row.quantity" :min="1" :max="999999" />
          </template>
        </el-table-column>
        <el-table-column label="剂量" width="160">
          <template #default="{ row }">
            <el-input v-model="row.dosage" />
          </template>
        </el-table-column>
        <el-table-column label="执行护工" width="220">
          <template #default="{ row }">
            <el-select
              v-model="row.executorId"
              filterable
              remote
              clearable
              :remote-method="searchStaff"
              :loading="staffLoading"
              placeholder="可选"
              style="width: 100%"
            >
              <el-option v-for="s in staffOptions" :key="s.id" :label="s.name" :value="s.id" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="180">
          <template #default="{ row }">
            <el-input v-model="row.remark" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{ $index }">
            <el-button link type="danger" @click="removeConfirmRow($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="confirmDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitConfirm">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../../api/client'

const activeTab = ref('batches')
const saving = ref(false)

const drugLoading = ref(false)
const drugOptions = ref([])

const elderlyLoading = ref(false)
const elderlyOptions = ref([])

const staffLoading = ref(false)
const staffOptions = ref([])

const batchLoading = ref(false)
const batches = ref([])
const batchTotal = ref(0)
const batchPage = ref(1)
const batchPageSize = ref(10)
const batchDrugId = ref(null)

const orderLoading = ref(false)
const orders = ref([])
const orderTotal = ref(0)
const orderPage = ref(1)
const orderPageSize = ref(10)
const orderStatus = ref('')

const warningLoading = ref(false)
const warnings = ref([])
const warningDays = ref(30)

const batchDialogVisible = ref(false)
const batchFormRef = ref()
const batchForm = reactive({
  drugId: null,
  batchNo: '',
  quantity: 1,
  expiryDate: '',
  inDate: '',
  supplier: ''
})
const batchRules = {
  drugId: [{ required: true, message: '请选择药品', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入数量', trigger: 'blur' }],
  expiryDate: [{ required: true, message: '请选择有效期', trigger: 'change' }]
}

const orderDialogVisible = ref(false)
const orderFormRef = ref()
const orderForm = reactive({
  elderlyId: null,
  orderDate: '',
  remark: ''
})
const orderRules = {
  elderlyId: [{ required: true, message: '请选择老人', trigger: 'change' }],
  orderDate: [{ required: true, message: '请选择日期', trigger: 'change' }]
}

const confirmDialogVisible = ref(false)
const confirmOrderId = ref(null)
const confirmItems = ref([])
let k = 1

function todayStr() {
  const today = new Date()
  const yyyy = today.getFullYear()
  const mm = String(today.getMonth() + 1).padStart(2, '0')
  const dd = String(today.getDate()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd}`
}

async function searchDrugs(query) {
  drugLoading.value = true
  try {
    const resp = await api.get('/api/pharmacy/drugs', { params: { page: 1, pageSize: 50, keyword: query || undefined } })
    const body = resp.data
    if (body.code === 200) drugOptions.value = body.data?.list || []
    else drugOptions.value = []
  } finally {
    drugLoading.value = false
  }
}

async function searchElderly(query) {
  elderlyLoading.value = true
  try {
    const resp = await api.get('/api/elderly/options', { params: { keyword: query || undefined } })
    const body = resp.data
    if (body.code === 200) elderlyOptions.value = body.data || []
    else elderlyOptions.value = []
  } finally {
    elderlyLoading.value = false
  }
}

function elderlyLabel(o) {
  const no = o.unique_no || o.uniqueNo || ''
  return `${o.name}${no ? '（' + no + '）' : ''}`
}

async function searchStaff(query) {
  staffLoading.value = true
  try {
    const resp = await api.get('/api/staff/options', { params: { keyword: query || undefined } })
    const body = resp.data
    if (body.code === 200) staffOptions.value = body.data || []
    else staffOptions.value = []
  } finally {
    staffLoading.value = false
  }
}

async function fetchBatches() {
  batchLoading.value = true
  try {
    const resp = await api.get('/api/pharmacy/batches', {
      params: { page: batchPage.value, pageSize: batchPageSize.value, drugId: batchDrugId.value || undefined }
    })
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '加载失败')
    batches.value = body.data?.list || []
    batchTotal.value = body.data?.total || 0
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    batchLoading.value = false
  }
}

function reloadBatches() {
  batchPage.value = 1
  fetchBatches()
}

function openBatchCreate() {
  batchForm.drugId = batchDrugId.value
  batchForm.batchNo = ''
  batchForm.quantity = 1
  batchForm.expiryDate = ''
  batchForm.inDate = todayStr()
  batchForm.supplier = ''
  batchDialogVisible.value = true
  searchDrugs('')
}

async function submitBatch() {
  if (!batchFormRef.value) return
  await batchFormRef.value.validate()
  saving.value = true
  try {
    const resp = await api.post('/api/pharmacy/batches', {
      drugId: batchForm.drugId,
      batchNo: batchForm.batchNo || null,
      quantity: batchForm.quantity,
      expiryDate: batchForm.expiryDate,
      inDate: batchForm.inDate,
      supplier: batchForm.supplier || null
    })
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '保存失败')
    ElMessage.success('保存成功')
    batchDialogVisible.value = false
    await fetchBatches()
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function fetchOrders() {
  orderLoading.value = true
  try {
    const resp = await api.get('/api/pharmacy/dispense/orders', {
      params: { page: orderPage.value, pageSize: orderPageSize.value, status: orderStatus.value || undefined }
    })
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '加载失败')
    orders.value = body.data?.list || []
    orderTotal.value = body.data?.total || 0
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    orderLoading.value = false
  }
}

function reloadOrders() {
  orderPage.value = 1
  fetchOrders()
}

function openOrderCreate() {
  orderForm.elderlyId = null
  orderForm.orderDate = todayStr()
  orderForm.remark = ''
  orderDialogVisible.value = true
  searchElderly('')
}

async function submitOrder() {
  if (!orderFormRef.value) return
  await orderFormRef.value.validate()
  saving.value = true
  try {
    const resp = await api.post('/api/pharmacy/dispense/orders', {
      elderlyId: orderForm.elderlyId,
      orderDate: orderForm.orderDate,
      remark: orderForm.remark || null
    })
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '保存失败')
    ElMessage.success('创建成功')
    orderDialogVisible.value = false
    await fetchOrders()
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function openConfirm(row) {
  confirmOrderId.value = row.id
  confirmItems.value = []
  addConfirmRow()
  confirmDialogVisible.value = true
  searchDrugs('')
  searchStaff('')
}

function addConfirmRow() {
  confirmItems.value.push({ __k: k++, drugId: null, quantity: 1, dosage: '', executorId: null, remark: '' })
}

function removeConfirmRow(idx) {
  confirmItems.value.splice(idx, 1)
}

async function submitConfirm() {
  if (!confirmOrderId.value) return
  if (confirmItems.value.length === 0) {
    ElMessage.error('请新增发药明细')
    return
  }
  for (const it of confirmItems.value) {
    if (!it.drugId) {
      ElMessage.error('请为每条明细选择药品')
      return
    }
    if (!it.quantity || it.quantity <= 0) {
      ElMessage.error('数量必须大于0')
      return
    }
  }
  saving.value = true
  try {
    const resp = await api.post(`/api/pharmacy/dispense/orders/${confirmOrderId.value}/confirm`, {
      items: confirmItems.value.map(it => ({
        drugId: it.drugId,
        quantity: it.quantity,
        dosage: it.dosage || null,
        executorId: it.executorId || null,
        remark: it.remark || null
      }))
    })
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '确认失败')
    ElMessage.success('发药成功')
    confirmDialogVisible.value = false
    await fetchOrders()
    await fetchBatches()
    await fetchWarnings()
  } catch (e) {
    ElMessage.error(e.message || '确认失败')
  } finally {
    saving.value = false
  }
}

async function fetchWarnings() {
  warningLoading.value = true
  try {
    const resp = await api.get('/api/pharmacy/expiry-warnings', { params: { days: warningDays.value } })
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '加载失败')
    warnings.value = body.data || []
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    warningLoading.value = false
  }
}

function reloadAll() {
  if (activeTab.value === 'batches') fetchBatches()
  if (activeTab.value === 'orders') fetchOrders()
  if (activeTab.value === 'warnings') fetchWarnings()
}

function handleTabChange() {
  reloadAll()
}

onMounted(() => {
  searchDrugs('')
  fetchBatches()
  fetchOrders()
  fetchWarnings()
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

.toolbar {
  margin: 10px 0;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.pager {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}
</style>
