<template>
  <div class="page">
    <PageHeader title="出库审批">
      <template #actions>
        <el-button @click="fetchList">刷新</el-button>
        <el-button type="primary" @click="openCreate">新增申领</el-button>
      </template>
    </PageHeader>

    <el-card>
      <div class="filter-bar">
        <el-select v-model="filterStatus" placeholder="审批状态" clearable @change="reload" style="width: 150px; margin-right: 10px;">
          <el-option label="全部" value="" />
          <el-option label="待审批" value="PENDING" />
          <el-option label="已通过" value="APPROVED" />
          <el-option label="已拒绝" value="REJECTED" />
        </el-select>
        <el-select v-model="filterCategory" placeholder="供应类别" clearable @change="reload" style="width: 150px;">
          <el-option label="全部" value="" />
          <el-option label="社会化" value="SOCIAL" />
          <el-option label="集中供养" value="CENTRALIZED" />
        </el-select>
      </div>

      <el-table :data="list" v-loading="loading" row-key="id">
        <el-table-column prop="applicantName" label="申请人" width="120" />
        <el-table-column prop="materialName" label="物资名称" width="180" />
        <el-table-column prop="quantity" label="数量" width="90" />
        <el-table-column prop="supplyCategory" label="供应类别" width="120">
          <template #default="{ row }">{{ supplyCategoryLabel(row.supplyCategory) }}</template>
        </el-table-column>
        <el-table-column prop="department" label="部门" width="130" />
        <el-table-column prop="purpose" label="用途" min-width="160" show-overflow-tooltip />
        <el-table-column prop="recipientName" label="领用人" width="120" />
        <el-table-column prop="approvalStatus" label="审批状态" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.approvalStatus === 'PENDING'" type="warning">待审批</el-tag>
            <el-tag v-else-if="row.approvalStatus === 'APPROVED'" type="success">已通过</el-tag>
            <el-tag v-else-if="row.approvalStatus === 'REJECTED'" type="danger">已拒绝</el-tag>
            <span v-else>{{ row.approvalStatus || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <template v-if="row.approvalStatus === 'PENDING' && canApprove">
              <el-button type="success" link size="small" @click="openApprove(row)">审批通过</el-button>
              <el-button type="danger" link size="small" @click="openReject(row)">拒绝</el-button>
            </template>
            <el-button type="primary" link size="small" @click="openDetail(row)">详情</el-button>
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

    <!-- Create dialog -->
    <el-dialog v-model="dialogVisible" title="新增申领" width="620px">
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
        <el-form-item label="供应类别" prop="supplyCategory">
          <el-select v-model="form.supplyCategory" style="width: 100%">
            <el-option label="社会化物资" value="SOCIAL" />
            <el-option label="集中供养物资" value="CENTRALIZED" />
          </el-select>
        </el-form-item>
        <el-form-item label="部门" prop="department">
          <el-input v-model="form.department" />
        </el-form-item>
        <el-form-item label="用途" prop="purpose">
          <el-input v-model="form.purpose" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item v-if="form.supplyCategory === 'CENTRALIZED'" label="领用人" prop="recipientStaffId">
          <el-select
            v-model="form.recipientStaffId"
            filterable
            remote
            clearable
            :remote-method="searchStaff"
            :loading="staffLoading"
            placeholder="请选择护工/领用人"
            style="width: 100%"
            @change="onStaffChange"
          >
            <el-option v-for="s in staffOptions" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">提交</el-button>
      </template>
    </el-dialog>

    <!-- Approve/Reject dialog -->
    <el-dialog v-model="approveDialogVisible" :title="approveAction === 'approve' ? '审批通过确认' : '拒绝确认'" width="480px">
      <el-form label-width="80px">
        <el-form-item label="申领单号">
          <span>{{ currentRow?.id }}</span>
        </el-form-item>
        <el-form-item label="物资">
          <span>{{ currentRow?.materialName }} × {{ currentRow?.quantity }}</span>
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input v-model="approveRemark" type="textarea" :rows="3" :placeholder="approveAction === 'approve' ? '可填写审批意见（选填）' : '请填写拒绝原因'" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveDialogVisible = false">取消</el-button>
        <el-button :type="approveAction === 'approve' ? 'success' : 'danger'" :loading="approving" @click="confirmApprove">
          {{ approveAction === 'approve' ? '确认通过' : '确认拒绝' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- Detail dialog -->
    <el-dialog v-model="detailDialogVisible" title="申领单详情" width="560px">
      <el-descriptions :column="2" border v-if="currentRow">
        <el-descriptions-item label="申领单号">{{ currentRow.id }}</el-descriptions-item>
        <el-descriptions-item label="申请人">{{ currentRow.applicantName }}</el-descriptions-item>
        <el-descriptions-item label="物资名称">{{ currentRow.materialName }}</el-descriptions-item>
        <el-descriptions-item label="数量">{{ currentRow.quantity }}</el-descriptions-item>
        <el-descriptions-item label="供应类别">{{ supplyCategoryLabel(currentRow.supplyCategory) }}</el-descriptions-item>
        <el-descriptions-item label="部门">{{ currentRow.department || '-' }}</el-descriptions-item>
        <el-descriptions-item label="用途" :span="2">{{ currentRow.purpose || '-' }}</el-descriptions-item>
        <el-descriptions-item label="领用人">{{ currentRow.recipientName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审批状态">
          <el-tag v-if="currentRow.approvalStatus === 'PENDING'" type="warning">待审批</el-tag>
          <el-tag v-else-if="currentRow.approvalStatus === 'APPROVED'" type="success">已通过</el-tag>
          <el-tag v-else-if="currentRow.approvalStatus === 'REJECTED'" type="danger">已拒绝</el-tag>
          <span v-else>{{ currentRow.approvalStatus || '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ formatTime(currentRow.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="审批人">{{ currentRow.approverName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审批时间" :span="2">{{ formatTime(currentRow.approveTime) }}</el-descriptions-item>
        <el-descriptions-item label="审批意见" :span="2">{{ currentRow.approveRemark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="关联出库单" :span="2">{{ currentRow.inventoryOutId ? '#' + currentRow.inventoryOutId : '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ currentRow.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../../api/client'
import PageHeader from '../../components/common/PageHeader.vue'
import { useAuthStore } from '../../store/auth'

const authStore = useAuthStore()
const isSuperAdmin = computed(() => authStore.permissions.includes('*'))
const canApprove = computed(() => isSuperAdmin.value || authStore.permissions.includes('warehouse:out:approve'))

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const filterStatus = ref('')
const filterCategory = ref('')

const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({
  materialId: null,
  quantity: 1,
  supplyCategory: 'SOCIAL',
  department: '',
  purpose: '',
  recipientStaffId: null,
  recipientName: '',
  remark: ''
})

const rules = computed(() => ({
  materialId: [{ required: true, message: '请选择物资', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入数量', trigger: 'blur' }],
  supplyCategory: [{ required: true, message: '请选择供应类别', trigger: 'change' }],
  recipientStaffId: form.supplyCategory === 'CENTRALIZED'
    ? [{ required: true, message: '请选择领用人', trigger: 'change' }]
    : []
}))

const materialLoading = ref(false)
const materialOptions = ref([])

const staffLoading = ref(false)
const staffOptions = ref([])

// Approve / Reject dialog
const approveDialogVisible = ref(false)
const approveAction = ref('approve')
const approveRemark = ref('')
const approving = ref(false)
const currentRow = ref(null)

// Detail dialog
const detailDialogVisible = ref(false)

function supplyCategoryLabel(cat) {
  if (cat === 'CENTRALIZED') return '集中供养物资'
  if (cat === 'SOCIAL') return '社会化物资'
  return cat || '-'
}

function formatTime(t) {
  if (!t) return '-'
  const s = String(t).replace('T', ' ')
  return s.length > 19 ? s.substring(0, 19) : s
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

let staffSearchTimer = null
function searchStaff(query) {
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

async function fetchList() {
  loading.value = true
  try {
    const resp = await api.get('/api/outbound-request', {
      params: {
        page: page.value,
        size: pageSize.value,
        status: filterStatus.value || undefined,
        supplyCategory: filterCategory.value || undefined
      }
    })
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '加载失败')
    list.value = body.data?.records || []
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

function resetForm() {
  form.materialId = null
  form.quantity = 1
  form.supplyCategory = 'SOCIAL'
  form.department = ''
  form.purpose = ''
  form.recipientStaffId = null
  form.recipientName = ''
  form.remark = ''
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
  searchMaterials('')
  searchStaff('')
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      materialId: form.materialId,
      quantity: form.quantity,
      supplyCategory: form.supplyCategory,
      department: form.department || null,
      purpose: form.purpose || null,
      recipientStaffId: form.supplyCategory === 'CENTRALIZED' ? form.recipientStaffId : null,
      recipientName: form.supplyCategory === 'CENTRALIZED' ? form.recipientName : null,
      remark: form.remark || null
    }
    const resp = await api.post('/api/outbound-request', payload)
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '提交失败')
    ElMessage.success('申领单提交成功')
    dialogVisible.value = false
    await fetchList()
  } catch (e) {
    ElMessage.error(e.message || '提交失败')
  } finally {
    saving.value = false
  }
}

function openApprove(row) {
  currentRow.value = row
  approveAction.value = 'approve'
  approveRemark.value = ''
  approveDialogVisible.value = true
}

function openReject(row) {
  currentRow.value = row
  approveAction.value = 'reject'
  approveRemark.value = ''
  approveDialogVisible.value = true
}

async function confirmApprove() {
  if (!currentRow.value) return
  approving.value = true
  try {
    const url = `/api/outbound-request/${currentRow.value.id}/${approveAction.value}`
    const resp = await api.post(url, null, { params: { remark: approveRemark.value || undefined } })
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '操作失败')
    ElMessage.success(approveAction.value === 'approve' ? '审批通过成功' : '已拒绝该申领单')
    approveDialogVisible.value = false
    await fetchList()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    approving.value = false
  }
}

function openDetail(row) {
  currentRow.value = row
  detailDialogVisible.value = true
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.page {
  padding: 16px;
}

.filter-bar {
  margin-bottom: 14px;
  display: flex;
  align-items: center;
}

.pager {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}
</style>
