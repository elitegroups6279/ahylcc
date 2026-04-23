<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="header">
          <span>机构管理</span>
          <div class="header-actions">
            <el-input v-model="keyword" placeholder="机构名称/编码/联系人" clearable style="width: 260px" @keyup.enter="reload" />
            <el-button type="primary" @click="openCreate">新增机构</el-button>
          </div>
        </div>
      </template>

      <el-table :data="list" v-loading="loading" row-key="id">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="orgCode" label="机构编码" width="140" />
        <el-table-column prop="orgName" label="机构名称" width="180" />
        <el-table-column prop="address" label="地址" min-width="200" show-overflow-tooltip />
        <el-table-column prop="phone" label="联系电话" width="140" />
        <el-table-column prop="contactPerson" label="联系人" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新增机构' : '编辑机构'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="机构编码" prop="orgCode">
          <el-input v-model="form.orgCode" placeholder="请输入机构编码" :disabled="dialogMode === 'edit'" />
        </el-form-item>
        <el-form-item label="机构名称" prop="orgName">
          <el-input v-model="form.orgName" placeholder="请输入机构名称" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="form.address" placeholder="请输入地址" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="联系人" prop="contactPerson">
          <el-input v-model="form.contactPerson" placeholder="请输入联系人" />
        </el-form-item>
        <el-form-item v-if="dialogMode === 'edit'" label="状态" prop="status">
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

const form = reactive({
  id: null,
  orgCode: '',
  orgName: '',
  address: '',
  phone: '',
  contactPerson: '',
  status: 1
})

const rules = {
  orgCode: [{ required: true, message: '请输入机构编码', trigger: 'blur' }],
  orgName: [{ required: true, message: '请输入机构名称', trigger: 'blur' }]
}

async function fetchList() {
  loading.value = true
  try {
    const resp = await api.get('/api/system/organizations', {
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
  form.orgCode = ''
  form.orgName = ''
  form.address = ''
  form.phone = ''
  form.contactPerson = ''
  form.status = 1
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
    const resp = await api.get(`/api/system/organizations/${row.id}`)
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '加载详情失败')
    const data = body.data
    form.id = data.id
    form.orgCode = data.orgCode || ''
    form.orgName = data.orgName || ''
    form.address = data.address || ''
    form.phone = data.phone || ''
    form.contactPerson = data.contactPerson || ''
    form.status = data.status ?? 1
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
      const resp = await api.post('/api/system/organizations', {
        orgCode: form.orgCode,
        orgName: form.orgName,
        address: form.address,
        phone: form.phone,
        contactPerson: form.contactPerson
      })
      const body = resp.data
      if (body.code !== 200) throw new Error(body.msg || '创建失败')
      ElMessage.success('创建成功')
    } else {
      const resp = await api.put(`/api/system/organizations/${form.id}`, {
        orgName: form.orgName,
        address: form.address,
        phone: form.phone,
        contactPerson: form.contactPerson,
        status: form.status
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

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定删除机构【${row.orgName}】吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const resp = await api.delete(`/api/system/organizations/${row.id}`)
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
