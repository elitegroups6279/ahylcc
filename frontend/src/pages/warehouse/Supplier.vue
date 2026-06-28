<template>
  <div class="page">
    <PageHeader title="供应商管理">
      <template #actions>
        <el-input v-model="keyword" placeholder="名称/统一信用代码" clearable style="width: 240px" @keyup.enter="reload" />
        <el-button type="primary" @click="reload">搜索</el-button>
        <el-button @click="fetchList">刷新</el-button>
        <el-button type="primary" @click="openCreate">新增供应商</el-button>
      </template>
    </PageHeader>

    <el-card>
      <el-table :data="list" v-loading="loading" row-key="id">
        <el-table-column prop="name" label="供应商名称" min-width="180" />
        <el-table-column prop="creditCode" label="统一信用代码" width="200" />
        <el-table-column prop="contact" label="联系人" width="120" />
        <el-table-column prop="phone" label="电话" width="140" />
        <el-table-column prop="bankName" label="开户行" width="180" />
        <el-table-column prop="bankAccount" label="账号" width="200" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'ACTIVE'" type="success">启用</el-tag>
            <el-tag v-else type="danger">禁用</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="isSuperAdmin" link type="danger" @click="remove(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新增供应商' : '编辑供应商'" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="供应商名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入供应商名称" />
        </el-form-item>
        <el-form-item label="统一信用代码" prop="creditCode">
          <el-input v-model="form.creditCode" />
        </el-form-item>
        <el-form-item label="联系人" prop="contact">
          <el-input v-model="form.contact" />
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="开户行" prop="bankName">
          <el-input v-model="form.bankName" />
        </el-form-item>
        <el-form-item label="银行账号" prop="bankAccount">
          <el-input v-model="form.bankAccount" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" style="width: 200px">
            <el-option label="启用" value="ACTIVE" />
            <el-option label="禁用" value="DISABLED" />
          </el-select>
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '../../components/common/PageHeader.vue'
import { api } from '../../api/client'
import { useAuthStore } from '../../store/auth'

const authStore = useAuthStore()
const isSuperAdmin = computed(() => {
  const roles = authStore.user?.roles || authStore.userInfo?.roles || []
  return roles.includes('SUPER_ADMIN') || authStore.permissions.includes('*')
})

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
const form = reactive({
  id: null,
  name: '',
  creditCode: '',
  contact: '',
  phone: '',
  bankName: '',
  bankAccount: '',
  status: 'ACTIVE'
})

const rules = {
  name: [{ required: true, message: '请输入供应商名称', trigger: 'blur' }]
}

async function fetchList() {
  loading.value = true
  try {
    const resp = await api.get('/api/supplier', {
      params: {
        page: page.value,
        size: pageSize.value,
        keyword: keyword.value || undefined
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
  form.id = null
  form.name = ''
  form.creditCode = ''
  form.contact = ''
  form.phone = ''
  form.bankName = ''
  form.bankAccount = ''
  form.status = 'ACTIVE'
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  dialogVisible.value = true
}

function openEdit(row) {
  dialogMode.value = 'edit'
  resetForm()
  form.id = row.id
  form.name = row.name || ''
  form.creditCode = row.creditCode || ''
  form.contact = row.contact || ''
  form.phone = row.phone || ''
  form.bankName = row.bankName || ''
  form.bankAccount = row.bankAccount || ''
  form.status = row.status || 'ACTIVE'
  dialogVisible.value = true
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      name: form.name,
      creditCode: form.creditCode || null,
      contact: form.contact || null,
      phone: form.phone || null,
      bankName: form.bankName || null,
      bankAccount: form.bankAccount || null,
      status: form.status || 'ACTIVE'
    }
    if (dialogMode.value === 'create') {
      const resp = await api.post('/api/supplier', payload)
      const body = resp.data
      if (body.code !== 200) throw new Error(body.msg || '创建失败')
      ElMessage.success('创建成功')
    } else {
      const resp = await api.put(`/api/supplier/${form.id}`, payload)
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

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定删除供应商【${row.name}】吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const resp = await api.delete(`/api/supplier/${row.id}`)
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

.pager {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}
</style>
