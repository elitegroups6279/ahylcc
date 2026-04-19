<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="header">
          <span>凭证管理</span>
          <div class="header-actions">
            <el-date-picker
              v-model="filterMonth"
              type="month"
              clearable
              placeholder="选择月份"
              style="width: 160px"
              @change="reload"
            />
            <el-select v-model="filterStatus" clearable placeholder="状态" style="width: 120px" @change="reload">
              <el-option label="草稿" value="DRAFT" />
              <el-option label="待审核" value="SUBMITTED" />
              <el-option label="已审核" value="APPROVED" />
              <el-option label="已驳回" value="REJECTED" />
            </el-select>
            <el-select v-model="filterWord" clearable placeholder="凭证字" style="width: 110px" @change="reload">
              <el-option label="记" value="记" />
              <el-option label="收" value="收" />
              <el-option label="付" value="付" />
              <el-option label="转" value="转" />
            </el-select>
            <el-button @click="fetchList">查询</el-button>
            <el-button type="primary" @click="openCreate">新增凭证</el-button>
          </div>
        </div>
      </template>

      <el-table :data="list" v-loading="loading" row-key="id">
        <el-table-column label="凭证字号" width="160">
          <template #default="{ row }">{{ row.voucherWord }}-{{ row.voucherNo }}</template>
        </el-table-column>
        <el-table-column prop="voucherDate" label="日期" width="120" />
        <el-table-column label="摘要" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.description || getFirstSummary(row) || '-' }}</template>
        </el-table-column>
        <el-table-column label="借方合计" width="130" align="right">
          <template #default="{ row }">{{ formatAmount(row.totalDebit) }}</template>
        </el-table-column>
        <el-table-column label="贷方合计" width="130" align="right">
          <template #default="{ row }">{{ formatAmount(row.totalCredit) }}</template>
        </el-table-column>
        <el-table-column label="附件" width="80" align="center">
          <template #default="{ row }">{{ row.attachmentCount || 0 }}张</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openView(row)">查看</el-button>
            <el-button v-if="row.status === 'DRAFT' || row.status === 'REJECTED'" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 'DRAFT'" link type="danger" @click="remove(row)">删除</el-button>
            <el-button v-if="row.status === 'DRAFT' || row.status === 'REJECTED'" link type="warning" @click="submitVoucher(row)">提交</el-button>
            <el-button v-if="row.status === 'SUBMITTED' && hasPermission('finance:voucher')" link type="success" @click="approveVoucher(row)">审核</el-button>
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
          :page-sizes="[10, 20, 50]"
          @update:current-page="(p) => { page = p; fetchList() }"
          @update:page-size="(s) => { pageSize = s; page = 1; fetchList() }"
        />
      </div>
    </el-card>

    <!-- 凭证编辑/查看对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="900px" :close-on-click-modal="false" @close="onDialogClose">
      <div class="voucher-header">
        <div class="voucher-info-row">
          <div class="info-item">
            <span class="info-label">凭证字:</span>
            <el-select v-model="form.voucherWord" :disabled="isViewMode" style="width: 80px" @change="onVoucherWordChange">
              <el-option label="记" value="记" />
              <el-option label="收" value="收" />
              <el-option label="付" value="付" />
              <el-option label="转" value="转" />
            </el-select>
          </div>
          <div class="info-item">
            <span class="info-label">号:</span>
            <el-input :model-value="previewNo" readonly style="width: 150px" />
          </div>
          <div class="info-item">
            <span class="info-label">日期:</span>
            <el-date-picker
              v-model="form.voucherDate"
              type="date"
              :disabled="isViewMode"
              placeholder="选择日期"
              style="width: 160px"
              @change="onDateChange"
            />
          </div>
          <div class="info-item">
            <span class="info-label">附件:</span>
            <el-input-number v-model="form.attachmentCount" :min="0" :disabled="isViewMode" style="width: 110px" />
            <span class="info-label" style="margin-left: 4px">张</span>
          </div>
        </div>
        <div class="voucher-title">记 账 凭 证</div>
        <div class="voucher-period">{{ periodText }}</div>
      </div>

      <div class="voucher-remark" v-if="!isViewMode || form.description">
        <span class="info-label">备注:</span>
        <el-input v-model="form.description" :disabled="isViewMode" placeholder="可选" style="flex: 1" />
      </div>
      <div class="voucher-remark" v-else>
        <span class="info-label">备注:</span>
        <span style="color: #999">-</span>
      </div>

      <!-- 分录表格 -->
      <el-table :data="form.entries" border size="small" class="entry-table" :class="{ 'entry-readonly': isViewMode }">
        <el-table-column label="序号" width="55" align="center">
          <template #default="{ $index }">{{ $index + 1 }}</template>
        </el-table-column>
        <el-table-column label="摘要" min-width="160">
          <template #default="{ row }">
            <el-input v-if="!isViewMode" v-model="row.summary" placeholder="摘要" size="small" />
            <span v-else>{{ row.summary || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="科目" min-width="200">
          <template #default="{ row }">
            <el-select
              v-if="!isViewMode"
              v-model="row.subjectId"
              filterable
              placeholder="选择科目"
              size="small"
              style="width: 100%"
            >
              <el-option
                v-for="s in subjects"
                :key="s.id"
                :label="s.code + ' ' + s.name"
                :value="s.id"
              />
            </el-select>
            <span v-else>{{ row.subjectCode }} {{ row.subjectName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="借方金额" width="140" align="right">
          <template #default="{ row }">
            <el-input-number
              v-if="!isViewMode"
              v-model="row.debitAmount"
              :min="0"
              :precision="2"
              :controls="false"
              size="small"
              style="width: 100%"
              @change="onAmountChange(row, 'debit')"
            />
            <span v-else class="amount-text">{{ formatAmount(row.debitAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="贷方金额" width="140" align="right">
          <template #default="{ row }">
            <el-input-number
              v-if="!isViewMode"
              v-model="row.creditAmount"
              :min="0"
              :precision="2"
              :controls="false"
              size="small"
              style="width: 100%"
              @change="onAmountChange(row, 'credit')"
            />
            <span v-else class="amount-text">{{ formatAmount(row.creditAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="!isViewMode" label="操作" width="60" align="center">
          <template #default="{ $index }">
            <el-button link type="danger" size="small" @click="removeEntry($index)">删</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="entry-footer" v-if="!isViewMode">
        <el-button size="small" @click="addEntry">+ 添加分录</el-button>
      </div>

      <div class="total-row" :class="{ 'total-unbalanced': !isBalanced }">
        <span class="total-label">合计：</span>
        <span class="total-cell">借方 {{ formatAmount(totalDebit) }}</span>
        <span class="total-cell">贷方 {{ formatAmount(totalCredit) }}</span>
        <span v-if="!isBalanced" class="balance-tip">（差额：{{ formatAmount(Math.abs(totalDebit - totalCredit)) }}）</span>
      </div>

      <!-- 驳回原因展示 -->
      <div v-if="currentVoucher && currentVoucher.rejectReason" class="reject-reason-show">
        <el-tag type="danger" size="small">驳回原因</el-tag>
        <span style="margin-left: 8px; color: #f56c6c">{{ currentVoucher.rejectReason }}</span>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <div class="footer-left">
            <span v-if="currentVoucher" class="creator-text">制单：{{ currentVoucher.creatorName || '-' }}</span>
          </div>
          <div class="footer-right">
            <template v-if="isViewMode && currentVoucher?.status === 'SUBMITTED' && hasPermission('finance:voucher')">
              <el-button @click="dialogVisible = false">取消</el-button>
              <el-button type="danger" @click="openRejectDialog">驳回</el-button>
              <el-button type="success" :loading="actionLoading" @click="approveVoucher(currentVoucher)">审核通过</el-button>
            </template>
            <template v-else-if="isViewMode">
              <el-button @click="dialogVisible = false">关闭</el-button>
            </template>
            <template v-else>
              <el-button @click="dialogVisible = false">取消</el-button>
              <el-button :loading="saving" @click="saveVoucher(false)">保存草稿</el-button>
              <el-button type="primary" :loading="saving" @click="saveVoucher(true)">保存并提交审核</el-button>
            </template>
          </div>
        </div>
      </template>
    </el-dialog>

    <!-- 驳回原因对话框 -->
    <el-dialog v-model="rejectDialogVisible" title="驳回凭证" width="420px" append-to-body>
      <el-form ref="rejectFormRef" :model="rejectForm" :rules="rejectRules">
        <el-form-item label="驳回原因" prop="reason">
          <el-input v-model="rejectForm.reason" type="textarea" :rows="3" placeholder="请输入驳回原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="actionLoading" @click="confirmReject">确定驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../../api/client'
import { useAuthStore } from '../../store/auth'

const authStore = useAuthStore()

// ===================== 列表相关 =====================
const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const filterMonth = ref(null)
const filterStatus = ref('')
const filterWord = ref('')

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
  if (Number.isNaN(n)) return '0.00'
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function statusText(status) {
  const map = { DRAFT: '草稿', SUBMITTED: '待审核', APPROVED: '已审核', REJECTED: '已驳回' }
  return map[status] || status || '-'
}

function statusTagType(status) {
  const map = { DRAFT: 'info', SUBMITTED: 'warning', APPROVED: 'success', REJECTED: 'danger' }
  return map[status] || 'info'
}

function hasPermission(perm) {
  return authStore.permissions && authStore.permissions.includes(perm)
}

function getFirstSummary(row) {
  if (row.entries && row.entries.length > 0 && row.entries[0].summary) {
    return row.entries[0].summary
  }
  return ''
}

async function fetchList() {
  loading.value = true
  try {
    const resp = await api.get('/api/finance/vouchers', {
      params: {
        page: page.value,
        pageSize: pageSize.value,
        month: formatMonth(filterMonth.value) || undefined,
        status: filterStatus.value || undefined,
        voucherWord: filterWord.value || undefined
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

// ===================== 科目缓存 =====================
const subjects = ref([])

async function fetchSubjects() {
  try {
    const resp = await api.get('/api/finance/accounting-subjects')
    const body = resp.data
    if (body.code === 200) {
      subjects.value = body.data || []
    }
  } catch (e) {
    console.warn('加载科目失败', e)
  }
}

// ===================== 凭证编号预览 =====================
const previewNo = ref('')

async function fetchNextNo() {
  if (!form.voucherWord || !form.voucherDate) {
    previewNo.value = ''
    return
  }
  try {
    const resp = await api.get('/api/finance/vouchers/next-no', {
      params: {
        voucherWord: form.voucherWord,
        date: formatDate(form.voucherDate)
      }
    })
    const body = resp.data
    if (body.code === 200 && body.data) {
      previewNo.value = body.data
    } else {
      previewNo.value = ''
    }
  } catch (e) {
    previewNo.value = ''
  }
}

// ===================== 对话框 =====================
const dialogVisible = ref(false)
const dialogMode = ref('view') // 'create' | 'edit' | 'view'
const saving = ref(false)
const actionLoading = ref(false)
const currentVoucher = ref(null)

const form = reactive({
  id: null,
  voucherWord: '记',
  voucherDate: null,
  attachmentCount: 0,
  description: '',
  entries: []
})

const isViewMode = computed(() => dialogMode.value === 'view')

const dialogTitle = computed(() => {
  if (dialogMode.value === 'create') return '新增凭证'
  if (dialogMode.value === 'edit') return '编辑凭证'
  return '查看凭证'
})

const periodText = computed(() => {
  if (!form.voucherDate) return ''
  const d = form.voucherDate instanceof Date ? form.voucherDate : new Date(form.voucherDate)
  if (Number.isNaN(d.getTime())) return ''
  return `${d.getFullYear()}年第${d.getMonth() + 1}期`
})

// 借贷合计计算
const totalDebit = computed(() => {
  return form.entries.reduce((sum, e) => sum + (Number(e.debitAmount) || 0), 0)
})

const totalCredit = computed(() => {
  return form.entries.reduce((sum, e) => sum + (Number(e.creditAmount) || 0), 0)
})

const isBalanced = computed(() => {
  if (form.entries.length === 0) return true
  return Math.abs(totalDebit.value - totalCredit.value) < 0.005
})

function createEmptyEntry() {
  return {
    summary: '',
    subjectId: null,
    subjectCode: '',
    subjectName: '',
    debitAmount: 0,
    creditAmount: 0
  }
}

function addEntry() {
  form.entries.push(createEmptyEntry())
}

function removeEntry(index) {
  form.entries.splice(index, 1)
}

function onAmountChange(row, type) {
  // 允许都填，不做强制清零
}

function onVoucherWordChange() {
  fetchNextNo()
}

function onDateChange() {
  fetchNextNo()
}

function resetForm() {
  form.id = null
  form.voucherWord = '记'
  form.voucherDate = new Date()
  form.attachmentCount = 0
  form.description = ''
  form.entries = [createEmptyEntry(), createEmptyEntry()]
  currentVoucher.value = null
  previewNo.value = ''
}

function openCreate() {
  resetForm()
  dialogMode.value = 'create'
  dialogVisible.value = true
  fetchNextNo()
}

async function openView(row) {
  resetForm()
  dialogMode.value = 'view'
  try {
    const resp = await api.get(`/api/finance/vouchers/${row.id}`)
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '加载失败')
    const data = body.data
    currentVoucher.value = data
    form.id = data.id
    form.voucherWord = data.voucherWord || '记'
    form.voucherDate = data.voucherDate
    form.attachmentCount = data.attachmentCount || 0
    form.description = data.description || ''
    previewNo.value = data.voucherNo || ''
    form.entries = (data.entries || []).map(e => ({
      id: e.id,
      lineNo: e.lineNo,
      summary: e.summary || '',
      subjectId: e.subjectId,
      subjectCode: e.subjectCode || '',
      subjectName: e.subjectName || '',
      debitAmount: e.debitAmount || 0,
      creditAmount: e.creditAmount || 0
    }))
    dialogVisible.value = true
  } catch (e) {
    ElMessage.error(e.message || '加载凭证详情失败')
  }
}

async function openEdit(row) {
  resetForm()
  dialogMode.value = 'edit'
  try {
    const resp = await api.get(`/api/finance/vouchers/${row.id}`)
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '加载失败')
    const data = body.data
    currentVoucher.value = data
    form.id = data.id
    form.voucherWord = data.voucherWord || '记'
    form.voucherDate = data.voucherDate
    form.attachmentCount = data.attachmentCount || 0
    form.description = data.description || ''
    form.entries = (data.entries || []).map(e => ({
      id: e.id,
      lineNo: e.lineNo,
      summary: e.summary || '',
      subjectId: e.subjectId,
      subjectCode: e.subjectCode || '',
      subjectName: e.subjectName || '',
      debitAmount: e.debitAmount || 0,
      creditAmount: e.creditAmount || 0
    }))
    dialogVisible.value = true
    fetchNextNo()
  } catch (e) {
    ElMessage.error(e.message || '加载凭证详情失败')
  }
}

function onDialogClose() {
  currentVoucher.value = null
}

// ===================== 保存/提交 =====================
function validateEntries() {
  if (form.entries.length < 2) {
    ElMessage.warning('至少需要2条分录')
    return false
  }
  for (let i = 0; i < form.entries.length; i++) {
    const e = form.entries[i]
    if (!e.subjectId) {
      ElMessage.warning(`第${i + 1}条分录未选择科目`)
      return false
    }
  }
  if (!isBalanced.value) {
    ElMessage.warning('借贷不平衡，请检查金额')
    return false
  }
  if (!form.voucherWord) {
    ElMessage.warning('请选择凭证字')
    return false
  }
  if (!form.voucherDate) {
    ElMessage.warning('请选择日期')
    return false
  }
  return true
}

async function saveVoucher(andSubmit) {
  if (!validateEntries()) return
  saving.value = true
  try {
    const payload = {
      voucherWord: form.voucherWord,
      voucherDate: formatDate(form.voucherDate),
      attachmentCount: form.attachmentCount,
      description: form.description || null,
      entries: form.entries.map((e, idx) => ({
        summary: e.summary || null,
        subjectId: e.subjectId,
        debitAmount: Number(e.debitAmount) || 0,
        creditAmount: Number(e.creditAmount) || 0
      }))
    }

    let resp
    if (form.id) {
      resp = await api.put(`/api/finance/vouchers/${form.id}`, payload)
    } else {
      resp = await api.post('/api/finance/vouchers', payload)
    }
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '保存失败')

    const savedId = body.data?.id || form.id

    if (andSubmit && savedId) {
      const submitResp = await api.post(`/api/finance/vouchers/${savedId}/submit`)
      const submitBody = submitResp.data
      if (submitBody.code !== 200) throw new Error(submitBody.msg || '提交审核失败')
      ElMessage.success('保存并提交审核成功')
    } else {
      ElMessage.success('保存成功')
    }

    dialogVisible.value = false
    await fetchList()
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// ===================== 列表操作 =====================
async function remove(row) {
  try {
    await ElMessageBox.confirm(`确认删除凭证 ${row.voucherWord}-${row.voucherNo}？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    const resp = await api.delete(`/api/finance/vouchers/${row.id}`)
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '删除失败')
    ElMessage.success('删除成功')
    await fetchList()
  } catch (e) {
    ElMessage.error(e.message || '删除失败')
  }
}

async function submitVoucher(row) {
  try {
    await ElMessageBox.confirm(`确认提交凭证 ${row.voucherWord}-${row.voucherNo} 进行审核？`, '提示', { type: 'warning' })
    const resp = await api.post(`/api/finance/vouchers/${row.id}/submit`)
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '提交失败')
    ElMessage.success('提交成功')
    await fetchList()
  } catch (e) {
    if (e === 'cancel') return
    ElMessage.error(e.message || '提交失败')
  }
}

async function approveVoucher(row) {
  const id = row?.id || currentVoucher.value?.id
  if (!id) return
  try {
    await ElMessageBox.confirm('确认审核通过此凭证？', '审核确认', { type: 'warning' })
    actionLoading.value = true
    const resp = await api.post(`/api/finance/vouchers/${id}/approve`)
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '审核失败')
    ElMessage.success('审核通过')
    dialogVisible.value = false
    await fetchList()
  } catch (e) {
    if (e === 'cancel') return
    ElMessage.error(e.message || '审核失败')
  } finally {
    actionLoading.value = false
  }
}

// ===================== 驳回 =====================
const rejectDialogVisible = ref(false)
const rejectFormRef = ref()
const rejectForm = reactive({ reason: '' })
const rejectRules = {
  reason: [{ required: true, message: '请输入驳回原因', trigger: 'blur' }]
}

function openRejectDialog() {
  rejectForm.reason = ''
  rejectDialogVisible.value = true
}

async function confirmReject() {
  if (!rejectFormRef.value) return
  await rejectFormRef.value.validate()
  const id = currentVoucher.value?.id
  if (!id) return
  actionLoading.value = true
  try {
    const resp = await api.post(`/api/finance/vouchers/${id}/reject`, { reason: rejectForm.reason })
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '驳回失败')
    ElMessage.success('已驳回')
    rejectDialogVisible.value = false
    dialogVisible.value = false
    await fetchList()
  } catch (e) {
    ElMessage.error(e.message || '驳回失败')
  } finally {
    actionLoading.value = false
  }
}

// ===================== 初始化 =====================
onMounted(() => {
  // 默认筛选当月
  filterMonth.value = new Date()
  fetchSubjects()
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

/* 凭证对话框样式 */
.voucher-header {
  text-align: center;
  margin-bottom: 12px;
}

.voucher-info-row {
  display: flex;
  align-items: center;
  gap: 16px;
  justify-content: center;
  margin-bottom: 10px;
  flex-wrap: wrap;
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

.voucher-title {
  font-size: 22px;
  font-weight: bold;
  letter-spacing: 8px;
  margin: 8px 0 4px;
  color: #303133;
}

.voucher-period {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}

.voucher-remark {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.entry-table {
  margin-bottom: 4px;
}

.entry-readonly :deep(.el-input__inner) {
  border-color: transparent;
  background: transparent;
}

.total-row {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 20px;
  padding: 8px 12px;
  font-size: 14px;
  font-weight: bold;
  border: 1px solid #ebeef5;
  border-top: none;
  background: #fafafa;
}

.total-row.total-unbalanced {
  color: #f56c6c;
  background: #fef0f0;
  border-color: #fbc4c4;
}

.total-label {
  margin-right: auto;
}

.total-cell {
  min-width: 140px;
  text-align: right;
}

.balance-tip {
  font-weight: normal;
  font-size: 12px;
  margin-left: 8px;
}

.entry-footer {
  margin: 8px 0;
}

.reject-reason-show {
  margin-top: 12px;
  padding: 8px 12px;
  background: #fef0f0;
  border-radius: 4px;
  display: flex;
  align-items: center;
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

.amount-text {
  font-family: 'Consolas', 'Monaco', monospace;
}
</style>
