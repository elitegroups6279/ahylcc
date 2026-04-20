<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="header">
          <span>新增入住</span>
          <div class="header-actions">
            <el-button @click="goBack">返回</el-button>
            <el-button type="primary" :loading="saving" @click="submit">提交入住</el-button>
          </div>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-divider content-position="left">基本信息</el-divider>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="form.name" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="身份证号" prop="idCard">
              <el-input v-model="form.idCard" @change="handleIdCardChange" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="人员类别" prop="category">
              <el-select v-model="form.category" placeholder="请选择">
                <el-option label="社会化" value="SOCIAL" />
                <el-option label="低保对象" value="LOW_BAO" />
                <el-option label="五保对象" value="WU_BAO" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="失能等级" prop="disabilityLevel">
              <el-select v-model="form.disabilityLevel" placeholder="请选择失能等级">
                <el-option label="能力完好" value="INTACT" />
                <el-option label="轻度失能" value="MILD" />
                <el-option label="中度失能" value="MODERATE" />
                <el-option label="重度失能" value="SEVERE" />
                <el-option label="完全失能" value="TOTAL" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="性别" prop="gender">
              <el-radio-group v-model="form.gender">
                <el-radio :value="1">男</el-radio>
                <el-radio :value="0">女</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="出生日期" prop="birthDate">
              <el-date-picker v-model="form.birthDate" type="date" value-format="YYYY-MM-DD" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="年龄" prop="age">
              <el-input-number v-model="form.age" :min="0" :max="150" />
              <el-text v-if="ageWarn" type="danger" style="margin-left: 8px">年龄提示</el-text>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="入住日期" prop="admissionDate">
              <el-date-picker v-model="form.admissionDate" type="date" value-format="YYYY-MM-DD" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="床位">
              <el-input v-model="form.customBedNumber" placeholder="可选，输入床位号如 201-1" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="关联护工" prop="staffIds">
              <el-select
                v-model="form.staffIds"
                multiple
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
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">家属联系人</el-divider>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="联系人姓名" prop="contactName">
              <el-input v-model="form.contactName" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="关系" prop="contactRelationship">
              <el-select v-model="form.contactRelationship" placeholder="请选择" style="width: 100%">
                <el-option label="子女" value="子女" />
                <el-option label="配偶" value="配偶" />
                <el-option label="其他" value="其他" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="电话" prop="contactPhone">
              <el-input v-model="form.contactPhone" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item>
          <el-checkbox v-model="form.contactIsEmergency" :true-value="1" :false-value="0">设为紧急联系人</el-checkbox>
        </el-form-item>

        <el-divider content-position="left">费用信息</el-divider>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="合同月费用" prop="contractMonthlyFee">
              <el-input-number v-model="form.contractMonthlyFee" :min="0" :precision="2" :step="100" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="押金" prop="deposit">
              <el-input-number v-model="form.deposit" :min="0" :precision="2" :step="100" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="付款方式" prop="paymentMethod">
              <el-select v-model="form.paymentMethod" placeholder="请选择">
                <el-option label="月付" value="MONTHLY" />
                <el-option label="季付" value="QUARTERLY" />
                <el-option label="年付" value="YEARLY" />
                <el-option label="一次性" value="ONCE" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="长护险" prop="enableLongCare">
              <el-radio-group v-model="form.enableLongCare">
                <el-radio :value="1">是</el-radio>
                <el-radio :value="0">否</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="消费券" prop="enableCoupon">
              <el-radio-group v-model="form.enableCoupon">
                <el-radio :value="1">是</el-radio>
                <el-radio :value="0">否</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '../../api/client'

const router = useRouter()
const saving = ref(false)
const formRef = ref()
const bedOptions = ref([])
const staffOptions = ref([])
const staffLoading = ref(false)

const form = reactive({
  name: '',
  idCard: '',
  gender: 1,
  birthDate: '',
  age: null,
  admissionDate: '',
  customBedNumber: '',
  category: '',
  enableLongCare: 0,
  enableCoupon: 0,
  contractMonthlyFee: 0,
  deposit: 0,
  contractStartDate: '',
  contractMonths: null,
  paymentMethod: 'MONTHLY',
  bankAccount: '',
  careLevel: '',
  disabilityLevel: 'INTACT',
  staffIds: [],
  contactName: '',
  contactRelationship: '子女',
  contactPhone: '',
  contactIsEmergency: 1
})



const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  idCard: [{ required: true, message: '请输入身份证号', trigger: 'blur' }],
  category: [{ required: true, message: '请选择人员类别', trigger: 'change' }],
  admissionDate: [{ required: true, message: '请选择入住日期', trigger: 'change' }],
  contactName: [{ required: true, message: '请输入联系人姓名', trigger: 'blur' }],
  contactPhone: [{ required: true, message: '请输入联系人电话', trigger: 'blur' }],
  contractMonthlyFee: [{ required: true, message: '请输入合同月费用', trigger: 'blur' }]
}

const ageWarn = computed(() => {
  if (form.age == null) return false
  const c = form.category
  if (c !== 'SOCIAL' && c !== 'LOW_BAO') return false
  return Number(form.age) < 60
})

function bedLabel(b) {
  return `${b.building || ''}${b.floor ? ' ' + b.floor : ''} ${b.roomNumber || ''} ${b.bedNumber || ''}`.trim()
}

function goBack() {
  router.back()
}

function parseIdCard(idCard) {
  const s = String(idCard || '').trim()
  if (s.length !== 18) return null
  const birthStr = s.substring(6, 14)
  const y = birthStr.substring(0, 4)
  const m = birthStr.substring(4, 6)
  const d = birthStr.substring(6, 8)
  const birthDate = `${y}-${m}-${d}`
  const genderCode = parseInt(s.substring(16, 17), 10)
  const gender = Number.isNaN(genderCode) ? null : genderCode % 2 === 1 ? 1 : 0
  const now = new Date()
  const by = Number(y)
  const bm = Number(m) - 1
  const bd = Number(d)
  let age = now.getFullYear() - by
  const t = new Date(by, bm, bd)
  const hasBirthday =
    now.getMonth() > t.getMonth() || (now.getMonth() === t.getMonth() && now.getDate() >= t.getDate())
  if (!hasBirthday) age -= 1
  return { birthDate, gender, age }
}

function handleIdCardChange() {
  const info = parseIdCard(form.idCard)
  if (!info) return
  if (!form.birthDate) form.birthDate = info.birthDate
  if (form.gender === null || form.gender === undefined) form.gender = info.gender
  if (form.age === null || form.age === undefined) form.age = info.age
}

async function fetchBeds() {
  const resp = await api.get('/api/beds/available')
  const body = resp.data
  if (body.code !== 200) throw new Error(body.msg || '加载床位失败')
  bedOptions.value = body.data || []
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

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      ...form,
      customBedNumber: form.customBedNumber?.trim() || null,
      contacts: [
        {
          name: form.contactName,
          relationship: form.contactRelationship,
          phone: form.contactPhone,
          isEmergency: form.contactIsEmergency,
          sortOrder: 1
        }
      ]
    }
    // If customBedNumber is provided, clear bedId so backend uses customBedNumber
    if (payload.customBedNumber) {
      payload.bedId = null
    }
    const resp = await api.post('/api/elderly', payload)
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '提交失败')
    ElMessage.success('入住成功')
    router.push(`/elderly/detail/${body.data}`)
  } catch (e) {
    ElMessage.error(e.message || '提交失败')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  const today = new Date()
  const yyyy = today.getFullYear()
  const mm = String(today.getMonth() + 1).padStart(2, '0')
  const dd = String(today.getDate()).padStart(2, '0')
  form.admissionDate = `${yyyy}-${mm}-${dd}`
  searchStaff('')
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
</style>
