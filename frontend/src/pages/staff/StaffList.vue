<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="header">
          <span>护工管理</span>
          <div class="header-actions">
            <el-select v-model="status" placeholder="状态" clearable style="width: 140px" @change="reload">
              <el-option label="在职" value="ACTIVE" />
              <el-option label="离职" value="RESIGNED" />
            </el-select>
            <el-input v-model="keyword" placeholder="姓名/电话" clearable style="width: 200px" @keyup.enter="reload" />
            <el-button @click="fetchList">刷新</el-button>
            <el-button type="primary" @click="openCreate">新增护工</el-button>
          </div>
        </div>
      </template>

      <el-table :data="list" v-loading="loading" row-key="id">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="姓名" width="140" />
        <el-table-column prop="phone" label="电话" width="160" />
        <el-table-column label="性别" width="80">
          <template #default="{ row }">{{ row.gender === 1 ? '男' : row.gender === 0 ? '女' : '-' }}</template>
        </el-table-column>
        <el-table-column prop="age" label="年龄" width="80" />
        <el-table-column prop="hireDate" label="入职日期" width="140" />
        <el-table-column label="负责老人" width="100">
          <template #default="{ row }">
            <el-button link type="primary" @click="openAssignedElderly(row)">{{ row.elderlyCount }}</el-button>
          </template>
        </el-table-column>
        <el-table-column label="实习状态" prop="probationStatus" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.probationStatus === 'INTERN'" type="warning" size="small">实习</el-tag>
            <el-tag v-else type="success" size="small">正式</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="护工证" prop="hasCaregiverCert" width="80" align="center">
          <template #default="{ row }">
            <el-icon v-if="row.hasCaregiverCert === 1" color="#52c41a"><Check /></el-icon>
            <el-icon v-else color="#d9d9d9"><Close /></el-icon>
          </template>
        </el-table-column>
        <el-table-column label="健康证" prop="hasHealthCert" width="80" align="center">
          <template #default="{ row }">
            <el-icon v-if="row.hasHealthCert === 1" color="#52c41a"><Check /></el-icon>
            <el-icon v-else color="#d9d9d9"><Close /></el-icon>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">{{ row.status === 'ACTIVE' ? '在职' : '离职' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="primary" @click="markResigned(row)" v-if="row.status === 'ACTIVE'">离职</el-button>
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

    <el-dialog v-model="elderlyDialogVisible" :title="`${currentStaffName} 负责老人列表`" width="720px">
      <el-table :data="assignedElderlyList" v-loading="elderlyDialogLoading" row-key="id">
        <el-table-column prop="uniqueNo" label="编号" width="140" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="bedNumber" label="床位" width="100" />
        <el-table-column prop="disabilityLevel" label="失能等级" width="100" />
        <el-table-column prop="category" label="类别" width="100" />
        <el-table-column label="分配类型" width="100">
          <template #default="{ row }">{{ row.assignType === 'PRIMARY' ? '主责' : row.assignType === 'SECONDARY' ? '备用' : row.assignType }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新增护工' : '编辑护工'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model="form.idCard" placeholder="请输入18位身份证号" maxlength="18" @input="handleIdCardChange" />
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="form.gender">
            <el-radio :value="1">男</el-radio>
            <el-radio :value="0">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="出生日期" prop="birthDate">
          <el-date-picker v-model="form.birthDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="年龄" prop="age">
          <el-input-number v-model="form.age" :min="0" :max="150" />
        </el-form-item>
        <el-form-item label="入职日期" prop="hireDate">
          <el-date-picker v-model="form.hireDate" type="date" value-format="YYYY-MM-DD" @change="handleHireDateChange" />
        </el-form-item>
        <!-- 实习状态 -->
        <el-form-item label="人员状态" prop="probationStatus">
          <el-radio-group v-model="form.probationStatus">
            <el-radio :value="'INTERN'">实习护工</el-radio>
            <el-radio :value="'FORMAL'">正式护工</el-radio>
          </el-radio-group>
        </el-form-item>
        <!-- 实习期选择（仅实习状态显示） -->
        <el-form-item v-if="form.probationStatus === 'INTERN'" label="实习期" prop="probationMonths">
          <el-select v-model="form.probationMonths" placeholder="选择实习期" @change="calcProbationEnd" style="width: 220px">
            <el-option :value="1" label="1个月" />
            <el-option :value="2" label="2个月" />
            <el-option :value="3" label="3个月" />
          </el-select>
        </el-form-item>
        <!-- 实习到期日期（只读展示） -->
        <el-form-item v-if="form.probationStatus === 'INTERN' && form.probationEndDate" label="实习到期">
          <el-input :value="form.probationEndDate" disabled />
        </el-form-item>
        <!-- 证书信息 -->
        <el-form-item label="护工证">
          <el-switch v-model="form.hasCaregiverCert" :active-value="1" :inactive-value="0" active-text="有" inactive-text="无" />
        </el-form-item>
        <el-form-item label="健康证">
          <el-switch v-model="form.hasHealthCert" :active-value="1" :inactive-value="0" active-text="有" inactive-text="无" />
        </el-form-item>
        <el-form-item label="岗位类型" prop="jobType">
          <el-select v-model="form.jobType" placeholder="请选择" style="width: 220px">
            <el-option label="全职" value="FULL" />
            <el-option label="兼职" value="PART" />
          </el-select>
        </el-form-item>
        <el-form-item label="基本工资" prop="baseSalary">
          <el-input-number v-model="form.baseSalary" :min="0" :precision="2" :step="100" />
        </el-form-item>
        <el-form-item label="状态" prop="status" v-if="dialogMode === 'edit'">
          <el-radio-group v-model="form.status">
            <el-radio value="ACTIVE">在职</el-radio>
            <el-radio value="RESIGNED">离职</el-radio>
          </el-radio-group>
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
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Close } from '@element-plus/icons-vue'
import { api } from '../../api/client'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const status = ref('')

const dialogVisible = ref(false)
const dialogMode = ref('create')
const formRef = ref()
const hireDateSetOnce = ref(false)

const elderlyDialogVisible = ref(false)
const elderlyDialogLoading = ref(false)
const assignedElderlyList = ref([])
const currentStaffName = ref('')

const form = reactive({
  id: null,
  name: '',
  idCard: '',
  phone: '',
  gender: 1,
  birthDate: '',
  age: null,
  hireDate: '',
  jobType: 'FULL',
  baseSalary: 0,
  status: 'ACTIVE',
  probationStatus: 'FORMAL',
  probationMonths: null,
  probationEndDate: '',
  hasCaregiverCert: 0,
  hasHealthCert: 0
})

// 监听实习状态变化，切换为正式时清空实习期字段
watch(() => form.probationStatus, (val) => {
  if (val === 'FORMAL') {
    form.probationMonths = null
    form.probationEndDate = ''
  }
})

const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
}

async function fetchList() {
  loading.value = true
  try {
    const resp = await api.get('/api/staff', {
      params: {
        page: page.value,
        pageSize: pageSize.value,
        keyword: keyword.value || undefined,
        status: status.value || undefined,
        positionType: 'CAREGIVER'
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

function resetForm() {
  form.id = null
  form.name = ''
  form.idCard = ''
  form.phone = ''
  form.gender = 1
  form.birthDate = ''
  form.age = null
  form.hireDate = ''
  form.jobType = 'FULL'
  form.baseSalary = 0
  form.status = 'ACTIVE'
  form.probationStatus = 'FORMAL'
  form.probationMonths = null
  form.probationEndDate = ''
  form.hasCaregiverCert = 0
  form.hasHealthCert = 0
  hireDateSetOnce.value = false
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  hireDateSetOnce.value = false
  dialogVisible.value = true
}

async function openAssignedElderly(row) {
  currentStaffName.value = row.name
  elderlyDialogVisible.value = true
  elderlyDialogLoading.value = true
  try {
    const resp = await api.get(`/api/staff/${row.id}/elderly`)
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '加载失败')
    assignedElderlyList.value = body.data || []
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    elderlyDialogLoading.value = false
  }
}

async function openEdit(row) {
  dialogMode.value = 'edit'
  resetForm()
  try {
    const resp = await api.get(`/api/staff/${row.id}`)
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '加载详情失败')
    Object.assign(form, body.data || {})
    dialogVisible.value = true
  } catch (e) {
    ElMessage.error(e.message || '加载详情失败')
  }
}

const handleIdCardChange = (val) => {
  const id = val?.trim()
  if (!id || id.length !== 18) return

  // 提取性别：第17位奇数=男(1)，偶数=女(0)
  const genderCode = parseInt(id.charAt(16), 10)
  form.gender = genderCode % 2 === 1 ? 1 : 0

  // 提取出生日期
  const year = id.substring(6, 10)
  const month = id.substring(10, 12)
  const day = id.substring(12, 14)
  form.birthDate = `${year}-${month}-${day}`

  // 计算年龄
  const birthDate = new Date(form.birthDate)
  const today = new Date()
  let age = today.getFullYear() - birthDate.getFullYear()
  const monthDiff = today.getMonth() - birthDate.getMonth()
  if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthDate.getDate())) {
    age--
  }
  form.age = age
}

// 当新增模式下首次设置入职日期时，自动设为实习护工
const handleHireDateChange = (val) => {
  // 仅在新增模式（非编辑）时自动设置
  if (dialogMode.value === 'create' && val && !hireDateSetOnce.value) {
    form.probationStatus = 'INTERN'
    form.probationMonths = 3  // 默认3个月
    hireDateSetOnce.value = true
    calcProbationEnd()
  }
}

// 计算实习到期日期
const calcProbationEnd = () => {
  if (form.hireDate && form.probationMonths) {
    const date = new Date(form.hireDate)
    date.setMonth(date.getMonth() + form.probationMonths)
    const y = date.getFullYear()
    const m = String(date.getMonth() + 1).padStart(2, '0')
    const d = String(date.getDate()).padStart(2, '0')
    form.probationEndDate = `${y}-${m}-${d}`
  } else {
    form.probationEndDate = ''
  }
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate()
  saving.value = true
  try {
    if (dialogMode.value === 'create') {
      const resp = await api.post('/api/staff', {
        name: form.name,
        idCard: form.idCard || null,
        phone: form.phone || null,
        gender: form.gender,
        birthDate: form.birthDate || null,
        age: form.age,
        hireDate: form.hireDate || null,
        jobType: form.jobType,
        baseSalary: form.baseSalary,
        status: form.status,
        probationStatus: form.probationStatus,
        probationMonths: form.probationMonths,
        probationEndDate: form.probationEndDate || null,
        hasCaregiverCert: form.hasCaregiverCert,
        hasHealthCert: form.hasHealthCert
      })
      const body = resp.data
      if (body.code !== 200) throw new Error(body.msg || '创建失败')
      ElMessage.success('创建成功')
    } else {
      const resp = await api.put(`/api/staff/${form.id}`, {
        name: form.name,
        idCard: form.idCard || null,
        phone: form.phone || null,
        gender: form.gender,
        birthDate: form.birthDate || null,
        age: form.age,
        hireDate: form.hireDate || null,
        jobType: form.jobType,
        baseSalary: form.baseSalary,
        status: form.status,
        probationStatus: form.probationStatus,
        probationMonths: form.probationMonths,
        probationEndDate: form.probationEndDate || null,
        hasCaregiverCert: form.hasCaregiverCert,
        hasHealthCert: form.hasHealthCert
      })
      const body = resp.data
      if (body.code !== 200) throw new Error(body.msg || '更新失败')
      ElMessage.success('更新成功')
    }
    dialogVisible.value = false
    await fetchList()
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function markResigned(row) {
  try {
    const { value } = await ElMessageBox.prompt('请输入离职原因（可选）', '离职', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputValue: ''
    })
    const today = new Date()
    const yyyy = today.getFullYear()
    const mm = String(today.getMonth() + 1).padStart(2, '0')
    const dd = String(today.getDate()).padStart(2, '0')
    const resp = await api.put(`/api/staff/${row.id}`, {
      status: 'RESIGNED',
      resignDate: `${yyyy}-${mm}-${dd}`,
      resignReason: value || null
    })
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '操作失败')
    ElMessage.success('已离职')
    await fetchList()
  } catch (e) {
    if (e === 'cancel') return
    ElMessage.error(e.message || '操作失败')
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定删除护工【${row.name}】吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const resp = await api.delete(`/api/staff/${row.id}`)
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '删除失败')
    ElMessage.success('删除成功')
    await fetchList()
  } catch (e) {
    if (e === 'cancel') return
    ElMessage.error(e.message || '删除失败')
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
  flex-wrap: wrap;
  justify-content: flex-end;
}

.pager {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}
</style>
