<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="header">
          <span>员工管理</span>
          <div class="header-actions">
            <el-select v-model="filters.positionType" placeholder="岗位类型" clearable style="width: 140px" @change="reload">
              <el-option label="管理人员" value="MANAGER" />
              <el-option label="财务人员" value="FINANCE" />
              <el-option label="人事专员" value="HR" />
              <el-option label="后勤人员" value="LOGISTICS" />
              <el-option label="其他" value="OTHER" />
            </el-select>
            <el-select v-model="filters.status" placeholder="状态" clearable style="width: 140px" @change="reload">
              <el-option label="在职" value="ACTIVE" />
              <el-option label="离职" value="RESIGNED" />
            </el-select>
            <el-input v-model="filters.keyword" placeholder="姓名/电话" clearable style="width: 200px" @keyup.enter="reload" />
            <el-button @click="fetchList">刷新</el-button>
            <el-button type="primary" @click="openCreate">新增员工</el-button>
          </div>
        </div>
      </template>

      <el-table :data="list" v-loading="loading" row-key="id">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="phone" label="电话" width="140" />
        <el-table-column label="性别" width="80">
          <template #default="{ row }">{{ row.gender === 1 ? '男' : row.gender === 0 ? '女' : '-' }}</template>
        </el-table-column>
        <el-table-column prop="age" label="年龄" width="80" />
        <el-table-column label="岗位类型" width="120">
          <template #default="{ row }">
            <el-tag v-if="positionTypeMap[row.positionType]" :color="positionTypeMap[row.positionType].color" style="color: #fff; border: none;" size="small">
              {{ positionTypeMap[row.positionType].label }}
            </el-tag>
            <span v-else>{{ row.positionType || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="hireDate" label="入职日期" width="120" />
        <el-table-column label="编制" width="80">
          <template #default="{ row }">{{ row.jobType === 'FULL' ? '全职' : row.jobType === 'PART' ? '兼职' : '-' }}</template>
        </el-table-column>
        <el-table-column label="基本工资" width="110">
          <template #default="{ row }">{{ row.baseSalary != null ? row.baseSalary.toLocaleString() : '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">{{ row.status === 'ACTIVE' ? '在职' : '离职' }}</el-tag>
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

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新增员工' : '编辑员工'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-divider content-position="left">基本信息</el-divider>
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

        <el-divider content-position="left">工作信息</el-divider>
        <el-form-item label="岗位类型" prop="positionType">
          <el-select v-model="form.positionType" placeholder="请选择岗位类型" style="width: 100%">
            <el-option label="管理人员" value="MANAGER" />
            <el-option label="财务人员" value="FINANCE" />
            <el-option label="人事专员" value="HR" />
            <el-option label="后勤人员" value="LOGISTICS" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="入职日期" prop="hireDate">
          <el-date-picker v-model="form.hireDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="编制" prop="jobType">
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../../api/client'

// 岗位类型映射
const positionTypeMap = {
  MANAGER: { label: '管理人员', color: '#409EFF' },
  FINANCE: { label: '财务人员', color: '#E6A23C' },
  HR: { label: '人事专员', color: '#67C23A' },
  LOGISTICS: { label: '后勤人员', color: '#909399' },
  OTHER: { label: '其他', color: '#F56C6C' },
  CAREGIVER: { label: '护工', color: '#8B5CF6' }
}

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const filters = reactive({
  positionType: '',
  status: '',
  keyword: ''
})

const dialogVisible = ref(false)
const dialogMode = ref('create')
const formRef = ref()

const form = reactive({
  id: null,
  name: '',
  idCard: '',
  phone: '',
  gender: 1,
  birthDate: '',
  age: null,
  positionType: '',
  hireDate: '',
  jobType: 'FULL',
  baseSalary: 0,
  status: 'ACTIVE'
})

const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  positionType: [{ required: true, message: '请选择岗位类型', trigger: 'change' }]
}

async function fetchList() {
  loading.value = true
  try {
    const params = {
      page: page.value,
      pageSize: pageSize.value
    }
    if (filters.keyword) params.keyword = filters.keyword
    if (filters.status) params.status = filters.status
    if (filters.positionType) params.positionType = filters.positionType

    const resp = await api.get('/api/staff', { params })
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '加载失败')

    let dataList = body.data?.list || []
    // 如果没有选特定岗位，前端过滤掉护工
    if (!filters.positionType) {
      dataList = dataList.filter(item => item.positionType !== 'CAREGIVER')
    }
    list.value = dataList
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
  form.positionType = ''
  form.hireDate = ''
  form.jobType = 'FULL'
  form.baseSalary = 0
  form.status = 'ACTIVE'
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  dialogVisible.value = true
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

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      name: form.name,
      idCard: form.idCard || null,
      phone: form.phone || null,
      gender: form.gender,
      birthDate: form.birthDate || null,
      age: form.age,
      positionType: form.positionType,
      hireDate: form.hireDate || null,
      jobType: form.jobType,
      baseSalary: form.baseSalary,
      status: form.status
    }
    if (dialogMode.value === 'create') {
      const resp = await api.post('/api/staff', payload)
      const body = resp.data
      if (body.code !== 200) throw new Error(body.msg || '创建失败')
      ElMessage.success('创建成功')
    } else {
      const resp = await api.put(`/api/staff/${form.id}`, payload)
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
    await ElMessageBox.confirm(`确定删除员工【${row.name}】吗？`, '提示', {
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
