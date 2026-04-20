<template>
  <div class="page" v-loading="loading">
    <el-card>
      <template #header>
        <div class="header">
          <span>老人档案</span>
          <div class="header-actions">
            <template v-if="isEditing">
              <el-button type="primary" @click="saveChanges" :loading="saving">保存</el-button>
              <el-button @click="cancelEdit">取消</el-button>
            </template>
            <template v-else>
              <el-button v-if="detail.status === 'DISCHARGED'" type="warning" @click="undoDischarge">撤销退住</el-button>
              <el-button v-if="detail.status !== 'DISCHARGED'" type="primary" @click="startEdit">编辑</el-button>
              <el-button @click="goBack">返回</el-button>
            </template>
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
        <el-descriptions-item label="床位">
          <template v-if="isEditing">
            <el-input v-model="editForm.bedNumber" placeholder="输入床位号，如 201-1" clearable />
          </template>
          <template v-else>
            {{ detail.bedNumber || '-' }}
          </template>
        </el-descriptions-item>
        <el-descriptions-item label="类别">
          <template v-if="isEditing">
            <el-select v-model="editForm.category" placeholder="选择类别" style="width: 100%">
              <el-option
                v-for="opt in categoryOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </template>
          <template v-else>
            {{ categoryText(detail.category) }}
          </template>
        </el-descriptions-item>
        <el-descriptions-item label="失能等级">
          <template v-if="isEditing">
            <el-select v-model="editForm.disabilityLevel" placeholder="选择失能等级" style="width: 100%">
              <el-option
                v-for="opt in disabilityOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </template>
          <template v-else>
            <el-tag :type="disabilityTagType(detail.disabilityLevel)">
              {{ disabilityText(detail.disabilityLevel) }}
            </el-tag>
          </template>
        </el-descriptions-item>
        <el-descriptions-item label="余额">￥{{ formatAmount(detail.feeBalance) }}</el-descriptions-item>
        <el-descriptions-item label="付款方式">{{ detail.paymentMethod || '-' }}</el-descriptions-item>
        <el-descriptions-item label="合同月费">
          <template v-if="isEditing">
            <el-input-number v-model="editForm.contractMonthlyFee" :min="0" :precision="2" :step="100" style="width: 100%" />
          </template>
          <template v-else>
            ￥{{ formatAmount(detail.contractMonthlyFee) }}
          </template>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detail.status === 'ACTIVE' ? 'success' : detail.status === 'ON_LEAVE' ? 'warning' : 'danger'">
            {{ detail.status === 'ACTIVE' ? '在住' : detail.status === 'ON_LEAVE' ? '请假中' : '已退住' }}
          </el-tag>
        </el-descriptions-item>
        <!-- 退住状态显示退住信息 -->
        <template v-if="detail.status === 'DISCHARGED'">
          <el-descriptions-item label="退住日期">{{ detail.dischargeDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="退住原因">{{ detail.dischargeReason || '-' }}</el-descriptions-item>
        </template>
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
      <template v-if="isEditing">
        <el-select
          v-model="editForm.staffIds"
          multiple
          filterable
          remote
          clearable
          :remote-method="searchStaff"
          :loading="staffLoading"
          placeholder="搜索并选择护工（可多选）"
          style="width: 100%; margin-bottom: 16px"
        >
          <el-option v-for="s in staffOptions" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
        <div style="color: #999; font-size: 12px">第一个选择的护工为主责护工，其余为辅助护工</div>
      </template>
      <template v-else>
        <div v-if="staffNames.length === 0">
          <el-empty description="暂无关联护工" :image-size="80" />
        </div>
        <div v-else class="tags">
          <el-tag v-for="s in staffNames" :key="s.id" style="margin-right: 8px">{{ s.name }}</el-tag>
        </div>
      </template>

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

    <!-- 变更记录 -->
    <el-card style="margin-top: 20px">
      <template #header><span>变更记录</span></template>
      <el-timeline v-if="changeLogs.length > 0">
        <el-timeline-item
          v-for="log in changeLogs"
          :key="log.id"
          :timestamp="formatTime(log.createTime)"
          placement="top">
          <p>{{ log.operator || '系统' }} 将 <b>{{ log.fieldLabel }}</b> 从「{{ log.oldValue }}」变更为「{{ log.newValue }}」</p>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无变更记录" />
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../../api/client'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const paymentLoading = ref(false)
const saving = ref(false)
const isEditing = ref(false)
const detail = reactive({})
const staffNames = ref([])
const staffOptions = ref([])
const staffLoading = ref(false)
const payments = ref([])
const bedOptions = ref([])
const changeLogs = ref([])

// Original values for cancel
const originalValues = reactive({})

// Edit form
const editForm = reactive({
  disabilityLevel: '',
  bedNumber: '',
  category: '',
  contractMonthlyFee: null,
  staffIds: []
})

// Category options
const categoryOptions = [
  { value: 'WU_BAO', label: '五保对象' },
  { value: 'LOW_BAO', label: '低保对象' },
  { value: 'SOCIAL', label: '社会化入住' }
]

// Disability level options (5 levels)
const disabilityOptions = [
  { value: 'INTACT', label: '能力完好' },
  { value: 'MILD', label: '轻度失能' },
  { value: 'MODERATE', label: '中度失能' },
  { value: 'SEVERE', label: '重度失能' },
  { value: 'TOTAL', label: '完全失能' }
]

// Disability level text mapping (handles both old SELF_CARE and new INTACT)
function disabilityText(level) {
  const mapping = {
    'SELF_CARE': '能力完好',
    'INTACT': '能力完好',
    'MILD': '轻度失能',
    'MODERATE': '中度失能',
    'SEVERE': '重度失能',
    'TOTAL': '完全失能'
  }
  return mapping[level] || level || '未知'
}

// Disability level tag type
function disabilityTagType(level) {
  switch (level) {
    case 'SELF_CARE':
    case 'INTACT':
      return 'success'
    case 'MILD':
      return 'primary'
    case 'MODERATE':
      return 'warning'
    case 'SEVERE':
    case 'TOTAL':
      return 'danger'
    default:
      return 'info'
  }
}

// Format bed label for select
function formatBedLabel(bed) {
  if (!bed) return '-'
  const parts = [bed.building, bed.floor, bed.roomNumber, bed.bedNumber].filter(Boolean)
  return parts.join('-')
}

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

async function searchStaff(keyword) {
  if (!keyword) return
  staffLoading.value = true
  try {
    const res = await api.get('/api/staff/options', { params: { keyword } })
    if (res.data?.code === 200) {
      staffOptions.value = res.data.data || []
    }
  } catch(e) { console.warn(e) }
  finally { staffLoading.value = false }
}

function startEdit() {
  // Initialize edit form with current values
  // Map old SELF_CARE to new INTACT for editing
  editForm.disabilityLevel = detail.disabilityLevel === 'SELF_CARE' ? 'INTACT' : detail.disabilityLevel
  editForm.bedNumber = detail.bedNumber || ''
  editForm.category = detail.category
  editForm.contractMonthlyFee = detail.contractMonthlyFee ?? null
  editForm.staffIds = detail.staffIds ? [...detail.staffIds] : []
  
  // Save original values for cancel
  originalValues.disabilityLevel = detail.disabilityLevel
  originalValues.bedNumber = detail.bedNumber || ''
  originalValues.category = detail.category
  originalValues.contractMonthlyFee = detail.contractMonthlyFee ?? null
  originalValues.staffIds = detail.staffIds ? [...detail.staffIds] : []
  
  isEditing.value = true
  
  // Preload associated staff into options
  if (staffNames.value.length > 0) {
    staffOptions.value = staffNames.value.map(s => ({ id: s.id, name: s.name }))
  }
}

function cancelEdit() {
  isEditing.value = false
  // Restore original values
  editForm.disabilityLevel = originalValues.disabilityLevel
  editForm.bedNumber = originalValues.bedNumber
  editForm.category = originalValues.category
  editForm.contractMonthlyFee = originalValues.contractMonthlyFee
  editForm.staffIds = originalValues.staffIds
}

async function saveChanges() {
  saving.value = true
  try {
    const elderlyId = route.params.id
    const originalDisability = originalValues.disabilityLevel === 'SELF_CARE' ? 'INTACT' : originalValues.disabilityLevel
    
    // Category change confirmation: WU_BAO -> LOW_BAO/SOCIAL
    if (originalValues.category === 'WU_BAO' && editForm.category !== 'WU_BAO') {
      try {
        await ElMessageBox.confirm(
          '类别变更后，该老人将需要自行/家属缴纳入住费用，是否确认？',
          '类别变更确认',
          { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' }
        )
      } catch {
        // User cancelled
        saving.value = false
        return
      }
    }
    
    // Build update payload for fields that changed
    const updatePayload = {}
    
    // Check if disability level changed
    if (editForm.disabilityLevel !== originalDisability) {
      updatePayload.disabilityLevel = editForm.disabilityLevel
    }
    
    // Check if category changed
    if (editForm.category !== originalValues.category) {
      updatePayload.category = editForm.category
    }
    
    // Check if contractMonthlyFee changed
    const oldFee = originalValues.contractMonthlyFee
    const newFee = editForm.contractMonthlyFee
    if (newFee !== null && (oldFee === null || Number(oldFee) !== Number(newFee))) {
      updatePayload.contractMonthlyFee = Number(newFee)
    }
    
    // Check if staffIds changed
    const staffChanged = JSON.stringify(editForm.staffIds.sort()) !== JSON.stringify(originalValues.staffIds.sort())
    if (staffChanged) {
      updatePayload.staffIds = editForm.staffIds
    }
    
    // Update elderly info if any field changed
    if (Object.keys(updatePayload).length > 0) {
      await api.put(`/api/elderly/${elderlyId}`, updatePayload)
    }
    
    // Check if bed changed
    if (editForm.bedNumber !== (detail.bedNumber || '')) {
      await api.put(`/api/elderly/${elderlyId}/transfer`, {
        customBedNumber: editForm.bedNumber,
        transferDate: new Date().toISOString().split('T')[0],
        reason: '编辑变更床位'
      })
    }
    
    ElMessage.success('保存成功')
    isEditing.value = false
    
    // Refresh detail and change logs
    await fetchDetail()
    await fetchStaffNames()
    await fetchChangeLogs()
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || e.message || '保存失败')
  } finally {
    saving.value = false
  }
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

async function fetchAvailableBeds() {
  try {
    const resp = await api.get('/api/beds/available')
    const body = resp.data
    if (body.code === 200) {
      const beds = body.data || []
      
      // Check if current bed is already in the list
      const currentBedId = detail.bedId
      const hasCurrentBed = beds.some(b => b.id === currentBedId)
      
      if (!hasCurrentBed && currentBedId) {
        // Add current bed to the list (marked as current)
        const currentBed = {
          id: currentBedId,
          building: detail.building,
          floor: detail.floor,
          roomNumber: detail.roomNumber,
          bedNumber: detail.bedNumber,
          isCurrent: true
        }
        bedOptions.value = [currentBed, ...beds]
      } else {
        bedOptions.value = beds
      }
    }
  } catch (e) {
    console.error('Failed to load beds:', e)
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

async function fetchChangeLogs() {
  try {
    const resp = await api.get(`/api/elderly/${route.params.id}/changes`)
    const body = resp.data
    if (body.code === 200) changeLogs.value = body.data || []
    else changeLogs.value = []
  } catch (e) {
    changeLogs.value = []
  }
}

async function undoDischarge() {
  try {
    await ElMessageBox.confirm(
      '确认撤销该老人的退住操作？撤销后需要手动重新分配床位。',
      '撤销退住确认',
      { type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消' }
    )
    const elderlyId = route.params.id
    await api.put(`/api/elderly/${elderlyId}/undo-discharge`)
    ElMessage.success('退住已撤销，老人已恢复为在住状态')
    await fetchDetail()
    await fetchStaffNames()
    await fetchChangeLogs()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.message || e.response?.data?.msg || '撤销失败')
    }
  }
}

function formatTime(dt) {
  if (!dt) return ''
  // Handle array format [2024,1,15,10,30,0]
  if (Array.isArray(dt)) {
    const [y, m, d, h = 0, min = 0, s = 0] = dt
    return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')} ${String(h).padStart(2, '0')}:${String(min).padStart(2, '0')}:${String(s).padStart(2, '0')}`
  }
  return String(dt).replace('T', ' ').substring(0, 19)
}

onMounted(async () => {
  await fetchDetail()
  await fetchStaffNames()
  await fetchPayments()
  await fetchChangeLogs()
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
