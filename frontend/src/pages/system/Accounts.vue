<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="header">
          <span>账户管理</span>
          <div class="header-actions">
            <el-input v-model="keyword" placeholder="用户名/姓名关键字" clearable style="width: 220px" @keyup.enter="reload" />
            <el-button type="primary" @click="openCreate">新增账户</el-button>
          </div>
        </div>
      </template>

      <el-table :data="list" v-loading="loading" row-key="id">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column prop="realName" label="姓名" width="140" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column label="角色" min-width="200">
          <template #default="{ row }">
            <el-tag v-for="r in (row.roles || [])" :key="r.id" size="small" style="margin-right: 6px">
              {{ r.roleName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最后登录" width="180" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="primary" @click="openResetPassword(row)">重置密码</el-button>
            <el-button link type="primary" @click="toggleStatus(row)">{{ row.status === 1 ? '停用' : '启用' }}</el-button>
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
          @update:current-page="(p) => { page = p; reload() }"
          @update:page-size="(s) => { pageSize = s; page = 1; reload() }"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新增账户' : '编辑账户'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="用户名" prop="username" v-if="dialogMode === 'create'">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="dialogMode === 'create'">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="角色" prop="roleIds">
          <el-select v-model="form.roleIds" multiple filterable placeholder="请选择角色" style="width: 100%">
            <el-option v-for="r in roleOptions" :key="r.id" :label="r.roleName" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status" v-if="dialogMode === 'edit'">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resetPwdVisible" title="重置密码" width="460px">
      <el-form ref="resetPwdRef" :model="resetPwdForm" :rules="resetPwdRules" label-width="90px">
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="resetPwdForm.newPassword" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetPwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitResetPassword">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../../api/client'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')

const dialogVisible = ref(false)
const dialogMode = ref('create')
const formRef = ref()
const roleOptions = ref([])
const form = reactive({
  id: null,
  username: '',
  password: '',
  realName: '',
  phone: '',
  email: '',
  status: 1,
  roleIds: []
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  roleIds: [{ type: 'array', required: true, message: '请选择角色', trigger: 'change' }]
}

const resetPwdVisible = ref(false)
const resetPwdRef = ref()
const resetPwdForm = reactive({ userId: null, newPassword: '' })
const resetPwdRules = {
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }]
}

async function fetchRoles() {
  const resp = await api.get('/api/system/roles/all')
  const body = resp.data
  if (body.code !== 200) throw new Error(body.msg || '获取角色失败')
  roleOptions.value = body.data || []
}

async function fetchList() {
  loading.value = true
  try {
    const resp = await api.get('/api/system/accounts', {
      params: { page: page.value, pageSize: pageSize.value, keyword: keyword.value || undefined }
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

function resetForm() {
  form.id = null
  form.username = ''
  form.password = ''
  form.realName = ''
  form.phone = ''
  form.email = ''
  form.status = 1
  form.roleIds = []
}

async function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  await fetchRoles()
  dialogVisible.value = true
}

async function openEdit(row) {
  dialogMode.value = 'edit'
  resetForm()
  await fetchRoles()
  try {
    const resp = await api.get(`/api/system/accounts/${row.id}`)
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '加载详情失败')
    const data = body.data
    form.id = data.id
    form.realName = data.realName || ''
    form.phone = data.phone || ''
    form.email = data.email || ''
    form.status = data.status ?? 1
    form.roleIds = (data.roles || []).map(r => r.id)
    dialogVisible.value = true
  } catch (e) {
    ElMessage.error(e.message || '加载详情失败')
  }
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate()
  saving.value = true
  try {
    if (dialogMode.value === 'create') {
      const payload = {
        username: form.username,
        password: form.password,
        realName: form.realName,
        phone: form.phone,
        email: form.email,
        roleIds: form.roleIds
      }
      const resp = await api.post('/api/system/accounts', payload)
      const body = resp.data
      if (body.code !== 200) throw new Error(body.msg || '创建失败')
      ElMessage.success('创建成功')
    } else {
      const payload = {
        realName: form.realName,
        phone: form.phone,
        email: form.email,
        status: form.status,
        roleIds: form.roleIds
      }
      const resp = await api.put(`/api/system/accounts/${form.id}`, payload)
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

function openResetPassword(row) {
  resetPwdForm.userId = row.id
  resetPwdForm.newPassword = ''
  resetPwdVisible.value = true
}

async function submitResetPassword() {
  if (!resetPwdRef.value) return
  await resetPwdRef.value.validate()
  saving.value = true
  try {
    const resp = await api.put(`/api/system/accounts/${resetPwdForm.userId}/reset-password`, {
      newPassword: resetPwdForm.newPassword
    })
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '重置失败')
    ElMessage.success('重置成功')
    resetPwdVisible.value = false
  } catch (e) {
    ElMessage.error(e.message || '重置失败')
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row) {
  try {
    const resp = await api.put(`/api/system/accounts/${row.id}/toggle-status`)
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '操作失败')
    ElMessage.success('操作成功')
    await fetchList()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定删除账户【${row.username}】吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const resp = await api.delete(`/api/system/accounts/${row.id}`)
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '删除失败')
    ElMessage.success('删除成功')
    await fetchList()
  } catch (e) {
    if (e === 'cancel') return
    ElMessage.error(e.message || '删除失败')
  }
}

function reload() {
  page.value = 1
  fetchList()
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
