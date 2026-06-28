<template>
  <div class="page">
    <PageHeader title="出库管理">
      <template #actions>
        <el-button @click="fetchList">刷新</el-button>
        <el-button type="primary" @click="openCreate">新增出库</el-button>
      </template>
    </PageHeader>

    <el-card>
      <div class="filter-bar">
        <el-select v-model="filterCategory" placeholder="供应类别" clearable @change="loadData" style="width: 140px; margin-right: 10px;">
          <el-option label="全部" value="" />
          <el-option label="社会化" value="SOCIAL" />
          <el-option label="集中供养" value="CENTRALIZED" />
        </el-select>
      </div>
      <el-table :data="list" v-loading="loading" row-key="id">
        <template #empty>
          <el-empty description="暂无数据" :image-size="80" />
        </template>
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="materialName" label="物资" width="200" />
        <el-table-column label="部门/用途" min-width="180">
          <template #default="{ row }">
            <div>{{ row.department || '-' }}</div>
            <div v-if="row.purpose" class="text-secondary" style="font-size: 12px; color: #909399">{{ row.purpose }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="100" />
        <el-table-column prop="specification" label="规格" width="120" />
        <el-table-column prop="supplyCategory" label="供应类别" width="140">
          <template #default="{ row }">{{ supplyCategoryLabel(row.supplyCategory) }}</template>
        </el-table-column>
        <el-table-column prop="recipientName" label="领用人" width="120" />
        <el-table-column prop="outDate" label="出库日期" width="140" />
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column prop="remark" label="备注" min-width="200" />
        <el-table-column v-if="isAdmin" label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openEdit(row)">
              <el-icon><Edit /></el-icon> 编辑
            </el-button>
            <el-button type="primary" link size="small" @click="handlePrint(row)">打印</el-button>
            <el-popconfirm title="确定删除该出库记录？" @confirm="doDelete(row.id)">
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '修改出库' : '新增出库'" width="850px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="0">
        <div class="outbound-header">
          <div class="outbound-info-row">
            <div class="info-item">
              <span class="info-label">单据编号:</span>
              <el-input :model-value="voucherNo" readonly style="width: 180px" />
            </div>
            <div class="info-item">
              <span class="info-label">日期:</span>
              <el-form-item prop="outDate" class="cell-form-item">
                <el-date-picker v-model="form.outDate" type="date" value-format="YYYY-MM-DD" style="width: 160px" />
              </el-form-item>
            </div>
            <div class="info-item">
              <span class="info-label">供应类别:</span>
              <el-form-item prop="supplyCategory" class="cell-form-item">
                <el-select v-model="form.supplyCategory" style="width: 140px">
                  <el-option label="社会化物资" value="SOCIAL" />
                  <el-option label="集中供养物资" value="CENTRALIZED" />
                </el-select>
              </el-form-item>
            </div>
          </div>
          <div class="outbound-title">物 资 出 库 单</div>
          <div class="outbound-period">{{ periodText }}</div>
        </div>

        <el-alert
          v-if="form.supplyCategory === 'CENTRALIZED'"
          title="集中供养物资出库需通过审批流程"
          type="warning"
          :closable="false"
          show-icon
          style="margin-bottom: 12px"
        />

        <table class="entry-table">
          <thead>
            <tr>
              <th style="width: 50px">序号</th>
              <th>物资名称</th>
              <th style="width: 130px">规格</th>
              <th style="width: 110px">数量</th>
              <th style="width: 130px">部门</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td class="center">1</td>
              <td>
                <el-form-item prop="materialId" class="cell-form-item">
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
              </td>
              <td>
                <el-form-item prop="specification" class="cell-form-item">
                  <el-input v-model="form.specification" placeholder="如：50kg/袋" />
                </el-form-item>
              </td>
              <td>
                <el-form-item prop="quantity" class="cell-form-item">
                  <el-input-number v-model="form.quantity" :min="1" :max="999999" :controls="false" style="width: 100%" />
                </el-form-item>
              </td>
              <td>
                <el-form-item prop="department" class="cell-form-item">
                  <el-input v-model="form.department" />
                </el-form-item>
              </td>
            </tr>
          </tbody>
        </table>

        <div class="recipient-row">
          <div class="recipient-item" v-if="form.supplyCategory === 'CENTRALIZED'">
            <span class="info-label">领用人:</span>
            <el-form-item prop="recipientStaffId" class="cell-form-item">
              <el-select
                v-model="form.recipientStaffId"
                filterable
                remote
                clearable
                :remote-method="searchStaff"
                :loading="staffLoading"
                placeholder="请选择护工/领用人"
                style="width: 220px"
                @change="onStaffChange"
              >
                <el-option v-for="s in staffOptions" :key="s.id" :label="s.name" :value="s.id" />
              </el-select>
            </el-form-item>
          </div>
          <div class="recipient-item">
            <span class="info-label">审核人:</span>
            <el-form-item prop="reviewerName" class="cell-form-item">
              <el-select
                v-model="form.reviewerName"
                placeholder="请选择审核人"
                style="width: 220px"
              >
                <el-option v-for="r in reviewerOptions" :key="r" :label="r" :value="r" />
              </el-select>
            </el-form-item>
          </div>
        </div>

        <div class="remark-section">
          <span class="info-label">备注:</span>
          <el-form-item prop="remark" class="cell-form-item" style="flex: 1">
            <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="可选" />
          </el-form-item>
        </div>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <div class="footer-left">
            <span class="creator-text">制单人: {{ currentUser }}</span>
          </div>
          <div class="footer-right">
            <el-button @click="dialogVisible = false">取消</el-button>
            <el-button @click="handleExport">导出</el-button>
            <el-button @click="handlePrintForm">打印</el-button>
            <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
          </div>
        </div>
      </template>
    </el-dialog>

    <!-- A5 Print Template -->
    <div ref="printRef" class="print-area">
      <div class="print-receipt">
        <h2 class="print-title">物资领用单</h2>
        <div class="print-info">
          <div class="info-row">
            <span>单据编号：{{ printData.id }}</span>
            <span>出库日期：{{ printData.outDate }}</span>
          </div>
        </div>
        <table class="print-table">
          <thead>
            <tr>
              <th>物资名称</th>
              <th>规格</th>
              <th>数量</th>
              <th>供应类别</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>{{ printData.materialName }}</td>
              <td>{{ printData.specification || '-' }}</td>
              <td>{{ printData.quantity }}</td>
              <td>{{ printData.supplyCategory === 'CENTRALIZED' ? '集中供养' : '社会化' }}</td>
            </tr>
          </tbody>
        </table>
        <div class="print-details">
          <div class="detail-row"><label>领用部门：</label><span>{{ printData.department || '-' }}</span></div>
          <div class="detail-row"><label>用途说明：</label><span>{{ printData.purpose || '-' }}</span></div>
          <div class="detail-row"><label>领用人：</label><span>{{ printData.recipientName || '-' }}</span></div>
          <div class="detail-row"><label>审核人：</label><span>{{ printData.reviewerName || '-' }}</span></div>
          <div class="detail-row"><label>备注：</label><span>{{ printData.remark || '-' }}</span></div>
        </div>
        <div class="print-signatures">
          <div class="sig-box">
            <label>领用人签字：</label>
            <div class="sig-line"></div>
          </div>
          <div class="sig-box">
            <label>仓管员签字：</label>
            <div class="sig-line"></div>
          </div>
          <div class="sig-box">
            <label>审核人签字：</label>
            <div class="sig-line"></div>
          </div>
        </div>
        <div class="print-footer">
          <span>打印时间：{{ printTime }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Edit, Delete } from '@element-plus/icons-vue'
import { api } from '../../api/client'
import PageHeader from '../../components/common/PageHeader.vue'
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
const filterCategory = ref('')

const dialogVisible = ref(false)
const formRef = ref()
const editingId = ref(null)
const form = reactive({
  materialId: null,
  department: '',
  purpose: '',
  quantity: 1,
  specification: '',
  supplyCategory: 'SOCIAL',
  outDate: '',
  remark: '',
  recipientStaffId: null,
  recipientName: '',
  recipientSignUrl: '',
  reviewerName: ''
})

const rules = computed(() => ({
  materialId: [{ required: true, message: '请选择物资', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入数量', trigger: 'blur' }],
  outDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  recipientStaffId: form.supplyCategory === 'CENTRALIZED'
    ? [{ required: true, message: '请选择领用人', trigger: 'change' }]
    : [],
  reviewerName: [{ required: true, message: '请选择审核人', trigger: 'change' }]
}))

const materialLoading = ref(false)
const materialOptions = ref([])

const staffLoading = ref(false)
const staffOptions = ref([])

const reviewerOptions = ref([])

async function loadReviewers() {
  try {
    const resp = await api.get('/api/system/config/reviewers')
    const body = resp.data
    if (body.code === 200) reviewerOptions.value = body.data || []
    else reviewerOptions.value = []
  } catch (e) {
    reviewerOptions.value = []
  }
}

const voucherNo = computed(() => {
  const d = form.outDate ? new Date(form.outDate) : new Date()
  if (Number.isNaN(d.getTime())) return ''
  const yyyy = d.getFullYear()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'
  let suffix = ''
  for (let i = 0; i < 4; i++) {
    suffix += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  return `OUT-${yyyy}${mm}${dd}-${suffix}`
})

const periodText = computed(() => {
  if (!form.outDate) return ''
  const d = new Date(form.outDate)
  if (Number.isNaN(d.getTime())) return ''
  return `${d.getFullYear()}年${d.getMonth() + 1}月`
})

const currentUser = computed(() => authStore.username || '-')

function supplyCategoryLabel(cat) {
  if (cat === 'CENTRALIZED') return '集中供养物资'
  if (cat === 'SOCIAL') return '社会化物资'
  return cat || '社会化物资'
}

let staffSearchTimer = null
async function searchStaff(query) {
  clearTimeout(staffSearchTimer)
  staffSearchTimer = setTimeout(async () => {
    staffLoading.value = true
    try {
      const resp = await api.get('/api/staff/options', {
        params: { keyword: query || undefined }
      })
      const body = resp.data
      if (body.code === 200) staffOptions.value = body.data || []
      else staffOptions.value = []
    } catch (e) {
      staffOptions.value = []
    } finally {
      staffLoading.value = false
    }
  }, 300)
}

function onStaffChange(val) {
  const found = staffOptions.value.find(s => s.id === val)
  form.recipientName = found ? found.name : ''
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
  loadData()
}

async function loadData() {
  loading.value = true
  try {
    const resp = await api.get('/api/warehouse/out', { params: { page: page.value, pageSize: pageSize.value, supplyCategory: filterCategory.value || undefined } })
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
  form.department = ''
  form.purpose = ''
  form.quantity = 1
  form.specification = ''
  form.supplyCategory = 'SOCIAL'
  form.outDate = `${yyyy}-${mm}-${dd}`
  form.remark = ''
  form.recipientStaffId = null
  form.recipientName = ''
  form.recipientSignUrl = ''
  form.reviewerName = ''
}

function openCreate() {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
  searchMaterials('')
  searchStaff('')
  loadReviewers()
}

function openEdit(row) {
  editingId.value = row.id
  form.materialId = row.materialId
  form.department = row.department || ''
  form.purpose = row.purpose || ''
  form.quantity = row.quantity || 1
  form.specification = row.specification || ''
  form.supplyCategory = row.supplyCategory || 'SOCIAL'
  form.outDate = row.outDate || ''
  form.remark = row.remark || ''
  form.recipientStaffId = row.recipientStaffId || null
  form.recipientName = row.recipientName || ''
  form.recipientSignUrl = row.recipientSignUrl || ''
  form.reviewerName = row.reviewerName || ''
  if (form.supplyCategory === 'CENTRALIZED') {
    searchStaff(form.recipientName || '')
  }
  dialogVisible.value = true
  searchMaterials('')
  loadReviewers()
}

async function doDelete(id) {
  try {
    const resp = await api.delete(`/api/warehouse/out/${id}`)
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
      department: form.department || null,
      purpose: form.purpose || null,
      quantity: form.quantity,
      specification: form.specification || null,
      supplyCategory: form.supplyCategory,
      outDate: form.outDate,
      remark: form.remark || null,
      recipientStaffId: form.supplyCategory === 'CENTRALIZED' ? form.recipientStaffId : null,
      recipientName: form.supplyCategory === 'CENTRALIZED' ? form.recipientName : null,
      recipientSignUrl: form.supplyCategory === 'CENTRALIZED' ? form.recipientSignUrl : null,
      reviewerName: form.reviewerName || null
    }
    let resp
    if (editingId.value) {
      resp = await api.put(`/api/warehouse/out/${editingId.value}`, payload)
    } else {
      resp = await api.post('/api/warehouse/out', payload)
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

const printData = ref({})
const printTime = ref('')

const handlePrint = (row) => {
  printData.value = { ...row }
  printTime.value = new Date().toLocaleString('zh-CN')
  nextTick(() => {
    window.print()
  })
}

function handlePrintForm() {
  const material = materialOptions.value.find(m => m.id === form.materialId)
  printData.value = {
    id: editingId.value || voucherNo.value,
    materialName: material?.name || '',
    specification: form.specification,
    quantity: form.quantity,
    supplyCategory: form.supplyCategory,
    department: form.department,
    purpose: form.purpose,
    recipientName: form.recipientName,
    reviewerName: form.reviewerName,
    remark: form.remark,
    outDate: form.outDate
  }
  printTime.value = new Date().toLocaleString('zh-CN')
  nextTick(() => {
    window.print()
  })
}

function handleExport() {
  const material = materialOptions.value.find(m => m.id === form.materialId)
  const lines = [
    '物资出库单',
    '================================',
    `单据编号: ${voucherNo.value}`,
    `日期: ${form.outDate || '-'}`,
    `供应类别: ${supplyCategoryLabel(form.supplyCategory)}`,
    '',
    `物资名称: ${material?.name || '-'}`,
    `规格: ${form.specification || '-'}`,
    `数量: ${form.quantity}`,
    `部门: ${form.department || '-'}`,
    `用途: ${form.purpose || '-'}`,
    `领用人: ${form.recipientName || '-'}`,
    `审核人: ${form.reviewerName || '-'}`,
    `备注: ${form.remark || '-'}`,
    '================================',
    `制单人: ${currentUser.value}`,
    `导出时间: ${new Date().toLocaleString('zh-CN')}`
  ]
  const text = lines.join('\n')
  const blob = new Blob([text], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `出库单_${form.outDate || 'unknown'}.txt`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
  ElMessage.success('导出成功')
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

.filter-bar {
  margin-bottom: 14px;
  display: flex;
  align-items: center;
}

.form-info-item :deep(.el-form-item__content) {
  margin-left: 0 !important;
}

/* Voucher-style dialog */
.outbound-header {
  text-align: center;
  margin-bottom: 20px;
}

.outbound-info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  font-size: 14px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.info-label {
  font-size: 14px;
  color: #606266;
  white-space: nowrap;
}

.outbound-title {
  font-size: 22px;
  font-weight: bold;
  letter-spacing: 8px;
  margin: 15px 0 5px;
  color: #303133;
}

.outbound-period {
  color: #666;
  font-size: 13px;
}

.entry-table {
  width: 100%;
  border-collapse: collapse;
  margin: 15px 0;
}

.entry-table th,
.entry-table td {
  border: 1px solid #ddd;
  padding: 8px 12px;
  text-align: center;
  font-size: 14px;
  vertical-align: middle;
}

.entry-table th {
  background: #f8f9fa;
  font-weight: 600;
}

.entry-table .center {
  text-align: center;
}

.cell-form-item {
  margin-bottom: 0 !important;
}

.cell-form-item :deep(.el-form-item__content) {
  margin-left: 0 !important;
}

.cell-form-item :deep(.el-form-item__error) {
  position: relative;
  font-size: 11px;
  line-height: 1.2;
  padding-top: 2px;
}

.recipient-row {
  display: flex;
  gap: 20px;
  margin-top: 12px;
  align-items: flex-start;
}

.recipient-item {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.remark-section {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-top: 12px;
}

.remark-section .info-label {
  padding-top: 7px;
  flex-shrink: 0;
}

.dialog-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.footer-left {
  flex: 1;
}

.footer-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.creator-text {
  font-size: 13px;
  color: #909399;
}

.print-area {
  display: none;
}

@media print {
  /* Hide everything except print area */
  body * {
    visibility: hidden;
  }
  .print-area,
  .print-area * {
    visibility: visible;
  }
  .print-area {
    display: block;
    position: fixed;
    left: 0;
    top: 0;
    width: 148mm;
    height: 210mm;
    padding: 10mm;
    box-sizing: border-box;
  }
  .print-receipt {
    font-family: 'SimSun', serif;
    font-size: 12px;
    color: #000;
  }
  .print-title {
    text-align: center;
    font-size: 18px;
    font-weight: bold;
    margin-bottom: 15px;
    letter-spacing: 4px;
  }
  .print-info {
    margin-bottom: 10px;
  }
  .print-info .info-row {
    display: flex;
    justify-content: space-between;
    margin-bottom: 5px;
    font-size: 12px;
  }
  .print-table {
    width: 100%;
    border-collapse: collapse;
    margin: 10px 0;
  }
  .print-table th,
  .print-table td {
    border: 1px solid #000;
    padding: 6px 8px;
    text-align: center;
    font-size: 12px;
  }
  .print-table th {
    background: #f0f0f0;
    font-weight: bold;
  }
  .print-details {
    margin: 12px 0;
  }
  .print-details .detail-row {
    margin-bottom: 8px;
    font-size: 12px;
    display: flex;
  }
  .print-details .detail-row label {
    width: 80px;
    font-weight: bold;
    flex-shrink: 0;
  }
  .print-signatures {
    display: flex;
    justify-content: space-between;
    margin-top: 30px;
  }
  .print-signatures .sig-box {
    text-align: center;
    font-size: 12px;
  }
  .print-signatures .sig-line {
    width: 80px;
    border-bottom: 1px solid #000;
    height: 30px;
    margin-top: 5px;
  }
  .print-footer {
    margin-top: 20px;
    font-size: 10px;
    color: #666;
    text-align: right;
  }
}
</style>
