<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="header">
          <span>药品档案</span>
          <div class="header-actions">
            <el-input v-model="keyword" placeholder="名称/通用名" clearable style="width: 220px" @keyup.enter="reload" />
            <el-button @click="fetchList">刷新</el-button>
            <el-button type="primary" @click="openCreate">新增药品</el-button>
          </div>
        </div>
      </template>

      <el-table :data="list" v-loading="loading" row-key="id">
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="name" label="名称" width="200" />
        <el-table-column prop="genericName" label="通用名" width="200" />
        <el-table-column prop="specification" label="规格" width="160" />
        <el-table-column prop="dosageForm" label="剂型" width="120" />
        <el-table-column prop="manufacturer" label="厂家" min-width="200" />
        <el-table-column prop="warningDays" label="预警天数" width="110" />
        <el-table-column label="处方药" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.isPrescription === 1 ? 'warning' : 'info'">{{ row.isPrescription === 1 ? '是' : '否' }}</el-tag>
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

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新增药品' : '编辑药品'" width="720px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="名称" prop="name">
              <el-input v-model="form.name" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="通用名" prop="genericName">
              <el-input v-model="form.genericName" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="规格" prop="specification">
              <el-input v-model="form.specification" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="剂型" prop="dosageForm">
              <el-input v-model="form.dosageForm" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="厂家" prop="manufacturer">
              <el-input v-model="form.manufacturer" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="批准文号" prop="approvalNumber">
              <el-input v-model="form.approvalNumber" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="存储条件" prop="storageCondition">
              <el-input v-model="form.storageCondition" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预警天数" prop="warningDays">
              <el-input-number v-model="form.warningDays" :min="1" :max="365" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="处方药" prop="isPrescription">
          <el-radio-group v-model="form.isPrescription">
            <el-radio :value="0">否</el-radio>
            <el-radio :value="1">是</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" />
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
  name: '',
  genericName: '',
  specification: '',
  dosageForm: '',
  manufacturer: '',
  approvalNumber: '',
  storageCondition: '',
  isPrescription: 0,
  warningDays: 30,
  description: ''
})

const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }]
}

async function fetchList() {
  loading.value = true
  try {
    const resp = await api.get('/api/pharmacy/drugs', {
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

function reload() {
  page.value = 1
  fetchList()
}

function resetForm() {
  form.id = null
  form.name = ''
  form.genericName = ''
  form.specification = ''
  form.dosageForm = ''
  form.manufacturer = ''
  form.approvalNumber = ''
  form.storageCondition = ''
  form.isPrescription = 0
  form.warningDays = 30
  form.description = ''
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
  form.genericName = row.genericName || ''
  form.specification = row.specification || ''
  form.dosageForm = row.dosageForm || ''
  form.manufacturer = row.manufacturer || ''
  form.approvalNumber = row.approvalNumber || ''
  form.storageCondition = row.storageCondition || ''
  form.isPrescription = row.isPrescription ?? 0
  form.warningDays = row.warningDays ?? 30
  form.description = row.description || ''
  dialogVisible.value = true
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      name: form.name,
      genericName: form.genericName || null,
      specification: form.specification || null,
      dosageForm: form.dosageForm || null,
      manufacturer: form.manufacturer || null,
      approvalNumber: form.approvalNumber || null,
      storageCondition: form.storageCondition || null,
      isPrescription: form.isPrescription,
      warningDays: form.warningDays,
      description: form.description || null
    }
    if (dialogMode.value === 'create') {
      const resp = await api.post('/api/pharmacy/drugs', payload)
      const body = resp.data
      if (body.code !== 200) throw new Error(body.msg || '创建失败')
      ElMessage.success('创建成功')
    } else {
      const resp = await api.put(`/api/pharmacy/drugs/${form.id}`, payload)
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
    await ElMessageBox.confirm(`确定删除药品【${row.name}】吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const resp = await api.delete(`/api/pharmacy/drugs/${row.id}`)
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
