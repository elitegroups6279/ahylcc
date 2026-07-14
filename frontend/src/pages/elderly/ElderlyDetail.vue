<template>
  <div class="page" v-loading="loading">
    <PageHeader title="老人档案">
      <template #actions>
        <template v-if="isEditing">
          <el-button type="primary" @click="saveChanges" :loading="saving">保存</el-button>
          <el-button @click="cancelEdit">取消</el-button>
        </template>
        <template v-else>
          <el-button v-if="detail.status === 'DISCHARGED'" type="warning" @click="undoDischarge">撤销退住</el-button>
          <el-button v-if="detail.status !== 'DISCHARGED'" type="primary" @click="startEdit">编辑</el-button>
          <el-button @click="goBack">返回</el-button>
        </template>
      </template>
    </PageHeader>

    <el-card>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="编号">{{ detail.uniqueNo }}</el-descriptions-item>
        <el-descriptions-item label="姓名">
          <template v-if="isEditing">
            <el-input v-model="editForm.name" placeholder="输入姓名" clearable />
          </template>
          <template v-else>
            {{ detail.name }}
          </template>
        </el-descriptions-item>
        <el-descriptions-item label="身份证">
          <template v-if="isEditing">
            <el-input v-model="editForm.idCard" placeholder="输入身份证号" clearable maxlength="18" />
          </template>
          <template v-else>
            {{ detail.idCardMasked }}
          </template>
        </el-descriptions-item>
        <el-descriptions-item label="性别">
          <template v-if="isEditing">
            <el-select v-model="editForm.gender" placeholder="选择性别" style="width: 100%">
              <el-option :value="1" label="男" />
              <el-option :value="0" label="女" />
            </el-select>
          </template>
          <template v-else>
            {{ detail.gender === 1 ? '男' : detail.gender === 0 ? '女' : '-' }}
          </template>
        </el-descriptions-item>
        <el-descriptions-item label="年龄">
          <template v-if="isEditing">
            <el-input-number v-model="editForm.age" :min="0" :max="150" :step="1" style="width: 100%" />
          </template>
          <template v-else>
            {{ detail.age ?? '-' }}
          </template>
        </el-descriptions-item>
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

      <el-divider content-position="left">请假记录</el-divider>
      <el-table :data="leaveHistory" row-key="id" size="small" v-loading="leaveLoading">
        <el-table-column prop="startDate" label="请假日期" width="140" />
        <el-table-column prop="endDate" label="预计返回日期" width="140">
          <template #default="{ row }">{{ row.endDate || '-' }}</template>
        </el-table-column>
        <el-table-column prop="returnDate" label="实际返回日期" width="140">
          <template #default="{ row }">{{ row.returnDate || '-' }}</template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="200">
          <template #default="{ row }">{{ row.reason || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'ON_LEAVE'" type="warning">请假中</el-tag>
            <el-tag v-else-if="row.status === 'RETURNED'" type="success">已返院</el-tag>
            <el-tag v-else-if="row.status === 'CANCELLED'" type="info">已取消</el-tag>
            <el-tag v-else type="info">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
      </el-table>

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

      <!-- 用药信息 (med-tabs) -->
      <el-tabs v-model="medTab" class="med-tabs" style="margin-top: 16px">
        <el-tab-pane label="用药计划" name="plan">
          <div class="med-table-card" style="margin-top: 0">
            <div class="med-table-card-header">
              <div class="med-table-card-header-left">
                <span class="med-table-card-title">用药计划</span>
                <span class="med-table-card-count">{{ medicationPlans.length }} 条</span>
              </div>
            </div>
            <el-table :data="medicationPlans" v-loading="medPlanLoading" row-key="id" class="med-table" size="small">
              <el-table-column label="药品" min-width="180">
                <template #default="{ row }">
                  <div style="display: flex; flex-wrap: wrap; gap: 4px">
                    <el-tag
                      v-for="(item, idx) in (row.items || [])"
                      :key="idx"
                      size="small"
                      type="info"
                      effect="plain"
                    >{{ item.drugName }}<span v-if="item.dosagePerTime" style="color: #6B7280; font-size: 11px"> {{ item.dosagePerTime }}{{ item.dosageUnit || '' }}</span></el-tag>
                    <span v-if="!row.items || row.items.length === 0" style="color: #9CA3AF">-</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="频次" width="80">
                <template #default="{ row }">
                  {{ row.frequencyType === 'MULTI_DAILY' ? '每日' : row.frequencyType === 'N_DAYS' ? '每N日' : row.frequencyType === 'PRN' ? '必要时' : '一次性' }}
                </template>
              </el-table-column>
              <el-table-column label="服药时段" width="160">
                <template #default="{ row }">
                  <span
                    v-for="slot in parseSlots(row.timeSlots)"
                    :key="slot"
                    class="med-pill"
                    :class="slotPillClass(slot)"
                    style="margin-right: 4px"
                  >{{ slotLabelMap(slot) }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="startDate" label="开始" width="100" />
              <el-table-column label="预计耗尽" width="110">
                <template #default="{ row }">{{ earliestDepletion(row.items) || '-' }}</template>
              </el-table-column>
              <el-table-column prop="prescriberName" label="处方医生" width="80" />
              <el-table-column label="状态" width="80">
                <template #default="{ row }">
                  <span class="med-pill" :class="planStatusPill(row.status)">{{ planStatusLabel(row.status) }}</span>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="!medPlanLoading && medicationPlans.length === 0" description="暂无用药计划" :image-size="60" />
          </div>
        </el-tab-pane>

        <el-tab-pane label="历史用药记录" name="records">
          <div class="med-table-card" style="margin-top: 0">
            <div class="med-table-card-header">
              <div class="med-table-card-header-left">
                <span class="med-table-card-title">历史用药记录</span>
                <span class="med-table-card-count">{{ medicationRecords.length }} 条</span>
              </div>
              <div class="med-filter-right" style="gap: 8px">
                <div class="med-switcher">
                  <button class="med-switcher-btn" :class="{ active: medViewMode === 'table' }" @click="medViewMode = 'table'">表格</button>
                  <button class="med-switcher-btn" :class="{ active: medViewMode === 'timeline' }" @click="medViewMode = 'timeline'">时间线</button>
                </div>
              </div>
            </div>

            <!-- 表格模式 -->
            <template v-if="medViewMode === 'table'">
              <el-table :data="medicationRecords" v-loading="medRecordLoading" row-key="recordDate" class="med-table" size="small">
                <el-table-column prop="recordDate" label="日期" width="100" />
                <el-table-column prop="drugName" label="药品" width="120" />
                <el-table-column label="早晨" width="70" align="center">
                  <template #default="{ row }">
                    <span v-if="row.morning" class="med-pill" :class="recordStatusPill(row.morning)">{{ statusIcon(row.morning) }}</span>
                    <span v-else>-</span>
                  </template>
                </el-table-column>
                <el-table-column label="中午" width="70" align="center">
                  <template #default="{ row }">
                    <span v-if="row.afternoon" class="med-pill" :class="recordStatusPill(row.afternoon)">{{ statusIcon(row.afternoon) }}</span>
                    <span v-else>-</span>
                  </template>
                </el-table-column>
                <el-table-column label="晚上" width="70" align="center">
                  <template #default="{ row }">
                    <span v-if="row.evening" class="med-pill" :class="recordStatusPill(row.evening)">{{ statusIcon(row.evening) }}</span>
                    <span v-else>-</span>
                  </template>
                </el-table-column>
                <el-table-column label="睡前" width="70" align="center">
                  <template #default="{ row }">
                    <span v-if="row.bedtime" class="med-pill" :class="recordStatusPill(row.bedtime)">{{ statusIcon(row.bedtime) }}</span>
                    <span v-else>-</span>
                  </template>
                </el-table-column>
                <el-table-column prop="executorName" label="护工" width="80" />
                <el-table-column prop="remark" label="备注" />
              </el-table>
            </template>

            <!-- 时间线模式 -->
            <template v-if="medViewMode === 'timeline'">
              <div v-loading="medRecordLoading" style="padding: 16px 20px">
                <el-timeline v-if="medicationRecords.length > 0">
                  <el-timeline-item
                    v-for="record in medicationRecords"
                    :key="record.recordDate + record.drugName"
                    :timestamp="record.recordDate"
                    placement="top"
                    :color="timelineColor(record)"
                  >
                    <div style="font-weight: 600; color: #1F2937">{{ record.drugName }}</div>
                    <div style="display: flex; gap: 8px; margin-top: 6px; flex-wrap: wrap">
                      <span v-if="record.morning" class="med-pill" :class="recordStatusPill(record.morning)">早晨 {{ statusIcon(record.morning) }}</span>
                      <span v-if="record.afternoon" class="med-pill" :class="recordStatusPill(record.afternoon)">中午 {{ statusIcon(record.afternoon) }}</span>
                      <span v-if="record.evening" class="med-pill" :class="recordStatusPill(record.evening)">晚上 {{ statusIcon(record.evening) }}</span>
                      <span v-if="record.bedtime" class="med-pill" :class="recordStatusPill(record.bedtime)">睡前 {{ statusIcon(record.bedtime) }}</span>
                    </div>
                    <div v-if="record.executorName" style="color: #6B7280; font-size: 12px; margin-top: 4px">护工: {{ record.executorName }}</div>
                  </el-timeline-item>
                </el-timeline>
                <el-empty v-else description="暂无用药记录" :image-size="60" />
              </div>
            </template>
          </div>
        </el-tab-pane>
      </el-tabs>
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
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '../../components/common/PageHeader.vue'
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
const leaveHistory = ref([])
const leaveLoading = ref(false)
const medicationPlans = ref([])
const medicationRecords = ref([])
const medPlanLoading = ref(false)
const medRecordLoading = ref(false)

// Medication tabs & view mode
const medTab = ref('plan')
const medViewMode = ref('table')

// Original values for cancel
const originalValues = reactive({})

// Edit form
const editForm = reactive({
  name: '',
  idCard: '',
  gender: null,
  age: null,
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

// Parse Chinese ID card to extract gender, age, and birth date
function parseIdCard(idCard) {
  if (!idCard || idCard.length !== 18) return null
  const birthStr = idCard.substring(6, 14)
  const year = parseInt(birthStr.substring(0, 4))
  const month = parseInt(birthStr.substring(4, 6))
  const day = parseInt(birthStr.substring(6, 8))
  if (isNaN(year) || isNaN(month) || isNaN(day)) return null
  const birthDate = new Date(year, month - 1, day)
  const today = new Date()
  let age = today.getFullYear() - birthDate.getFullYear()
  if (today.getMonth() < birthDate.getMonth() ||
      (today.getMonth() === birthDate.getMonth() && today.getDate() < birthDate.getDate())) {
    age--
  }
  const genderCode = parseInt(idCard.charAt(16))
  if (isNaN(genderCode)) return null
  const gender = genderCode % 2 === 1 ? 1 : 0  // 1=男, 0=女
  const birthDateStr = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
  return { age, gender, birthDate: birthDateStr }
}

// Watch idCard changes to auto-update gender and age
watch(() => editForm.idCard, (newVal) => {
  if (!isEditing.value) return
  const parsed = parseIdCard(newVal)
  if (parsed) {
    editForm.gender = parsed.gender
    editForm.age = parsed.age
  }
})

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
  editForm.name = detail.name || ''
  editForm.idCard = detail.idCard || ''
  editForm.gender = detail.gender ?? null
  editForm.age = detail.age ?? null
  editForm.disabilityLevel = detail.disabilityLevel === 'SELF_CARE' ? 'INTACT' : detail.disabilityLevel
  editForm.bedNumber = detail.bedNumber || ''
  editForm.category = detail.category
  editForm.contractMonthlyFee = detail.contractMonthlyFee ?? null
  editForm.staffIds = detail.staffIds ? [...detail.staffIds] : []
  
  // Save original values for cancel
  originalValues.name = detail.name || ''
  originalValues.idCard = detail.idCard || ''
  originalValues.gender = detail.gender ?? null
  originalValues.age = detail.age ?? null
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
  editForm.name = originalValues.name
  editForm.idCard = originalValues.idCard
  editForm.gender = originalValues.gender
  editForm.age = originalValues.age
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
    
    // Check if name changed
    if (editForm.name !== originalValues.name) {
      updatePayload.name = editForm.name
    }
    
    // Check if idCard changed
    if (editForm.idCard !== originalValues.idCard) {
      updatePayload.idCard = editForm.idCard
      // Also send gender and age derived from the new idCard
      const parsed = parseIdCard(editForm.idCard)
      if (parsed) {
        updatePayload.gender = parsed.gender
        updatePayload.age = parsed.age
      }
    }
    
    // Check if gender changed (manual edit without idCard change)
    if (editForm.gender !== originalValues.gender && !updatePayload.gender) {
      updatePayload.gender = editForm.gender
    }
    
    // Check if age changed (manual edit without idCard change)
    if (editForm.age !== originalValues.age && !updatePayload.age) {
      updatePayload.age = editForm.age
    }
    
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

// ---- Medication helpers ----
function statusIcon(status) {
  return { DONE: '✓', PENDING: '○', MISSED: '✗', REFUSED: '⊘', SKIPPED: '⊘' }[status] || '-'
}

function slotLabelMap(slot) {
  const map = { MORNING: '早晨', AFTERNOON: '中午', EVENING: '晚上', BEDTIME: '睡前' }
  return map[slot] || slot
}

function slotPillClass(slot) {
  const map = { MORNING: 'orange', AFTERNOON: 'blue', EVENING: 'gray', BEDTIME: 'teal' }
  return map[slot] || 'gray'
}

function parseSlots(slots) {
  if (!slots) return []
  if (Array.isArray(slots)) return slots
  try { return JSON.parse(slots) } catch { return [] }
}

function earliestDepletion(items) {
  if (!items || items.length === 0) return null
  const dates = items.map(i => i.depletionDate).filter(Boolean).sort()
  return dates[0] || null
}

function planStatusLabel(status) {
  const map = { ACTIVE: '生效中', PAUSED: '已暂停', COMPLETED: '已完成', STOPPED: '已停止' }
  return map[status] || status
}

function planStatusPill(status) {
  const map = { ACTIVE: 'green', PAUSED: 'orange', COMPLETED: 'gray', STOPPED: 'red' }
  return map[status] || 'gray'
}

function recordStatusPill(status) {
  const map = { DONE: 'green', PENDING: 'blue', MISSED: 'red', REFUSED: 'orange', SKIPPED: 'gray' }
  return map[status] || 'gray'
}

function timelineColor(record) {
  const statuses = [record.morning, record.afternoon, record.evening, record.bedtime].filter(Boolean)
  if (statuses.some(s => s === 'MISSED')) return '#F56C6C'
  if (statuses.some(s => s === 'REFUSED')) return '#E6A23C'
  if (statuses.every(s => s === 'DONE')) return '#52C41A'
  if (statuses.some(s => s === 'PENDING')) return '#3B82F6'
  return '#6B7280'
}

async function fetchMedicationPlans() {
  medPlanLoading.value = true
  try {
    const resp = await api.get('/api/medication/plans', { params: { elderlyId: route.params.id, page: 1, size: 50 } })
    medicationPlans.value = resp.data?.data?.records || resp.data?.data || []
  } catch (e) { console.error('Failed to load medication plans', e) }
  finally { medPlanLoading.value = false }
}

async function fetchMedicationRecords() {
  medRecordLoading.value = true
  try {
    const resp = await api.get(`/api/medication/records/elderly/${route.params.id}`, { params: { days: 7 } })
    const raw = resp.data?.data || []
    const grouped = {}
    raw.forEach(r => {
      const key = `${r.recordDate}_${r.drugName}`
      if (!grouped[key]) {
        grouped[key] = { recordDate: r.recordDate, drugName: r.drugName, morning: null, afternoon: null, evening: null, bedtime: null, executorName: r.executorName || '', remark: r.remark || '' }
      }
      if (r.timeSlot === 'MORNING') grouped[key].morning = r.status
      else if (r.timeSlot === 'AFTERNOON') grouped[key].afternoon = r.status
      else if (r.timeSlot === 'EVENING') grouped[key].evening = r.status
      else if (r.timeSlot === 'BEDTIME') grouped[key].bedtime = r.status
      if (r.executorName) grouped[key].executorName = r.executorName
      if (r.remark) grouped[key].remark = r.remark
    })
    medicationRecords.value = Object.values(grouped).sort((a, b) => b.recordDate.localeCompare(a.recordDate))
  } catch (e) { console.error('Failed to load medication records', e) }
  finally { medRecordLoading.value = false }
}

async function fetchLeaveHistory() {
  leaveLoading.value = true
  try {
    const resp = await api.get(`/api/elderly/${route.params.id}/leave`)
    const body = resp.data
    if (body.code === 200) leaveHistory.value = body.data || []
    else leaveHistory.value = []
  } catch (e) {
    leaveHistory.value = []
  } finally {
    leaveLoading.value = false
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
  await fetchLeaveHistory()
  await fetchPayments()
  await fetchChangeLogs()
  fetchMedicationPlans()
  fetchMedicationRecords()
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
