<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="header">
          <span>物资档案</span>
          <div class="header-actions">
            <el-input v-model="keyword" placeholder="名称/规格关键字" clearable style="width: 220px" @keyup.enter="reload" />
            <el-input v-model="category" placeholder="类别" clearable style="width: 180px" @keyup.enter="reload" />
            <el-button @click="fetchList">刷新</el-button>
            <el-button type="primary" @click="openCreate">新增物资</el-button>
          </div>
        </div>
      </template>

      <el-table :data="list" v-loading="loading" row-key="id">
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="name" label="名称" width="180" />
        <el-table-column prop="category" label="类别" width="160" />
        <el-table-column prop="specification" label="规格" min-width="180" />
        <el-table-column prop="unit" label="单位" width="120" />
        <el-table-column prop="warningThreshold" label="预警阈值" width="110" />
        <el-table-column prop="description" label="备注" min-width="200" />
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

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新增物资' : '编辑物资'" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="类别" prop="category">
          <el-input v-model="form.category" placeholder="例如 生活用品/护理耗材" />
        </el-form-item>
        <el-form-item label="规格" prop="specification">
          <el-input v-model="form.specification" />
        </el-form-item>
        <el-form-item label="单位" prop="unit">
          <el-input v-model="form.unit" style="width: 220px" />
        </el-form-item>
        <el-form-item label="预警阈值" prop="warningThreshold">
          <el-input-number v-model="form.warningThreshold" :min="0" :max="999999" />
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
const category = ref('')

const dialogVisible = ref(false)
const dialogMode = ref('create')
const formRef = ref()
const form = reactive({
  id: null,
  name: '',
  category: '',
  specification: '',
  unit: '',
  warningThreshold: 10,
  description: ''
})

const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }]
}

async function fetchList() {
  loading.value = true
  try {
    const resp = await api.get('/api/warehouse/materials', {
      params: {
        page: page.value,
        pageSize: pageSize.value,
        keyword: keyword.value || undefined,
        category: category.value || undefined
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
  form.category = ''
  form.specification = ''
  form.unit = ''
  form.warningThreshold = 10
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
  form.category = row.category || ''
  form.specification = row.specification || ''
  form.unit = row.unit || ''
  form.warningThreshold = row.warningThreshold ?? 10
  form.description = row.description || ''
  dialogVisible.value = true
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate()
  saving.value = true
  try {
    if (dialogMode.value === 'create') {
      const resp = await api.post('/api/warehouse/materials', {
        name: form.name,
        category: form.category || null,
        specification: form.specification || null,
        unit: form.unit || null,
        warningThreshold: form.warningThreshold,
        description: form.description || null
      })
      const body = resp.data
      if (body.code !== 200) throw new Error(body.msg || '创建失败')
      ElMessage.success('创建成功')
    } else {
      const resp = await api.put(`/api/warehouse/materials/${form.id}`, {
        name: form.name,
        category: form.category || null,
        specification: form.specification || null,
        unit: form.unit || null,
        warningThreshold: form.warningThreshold,
        description: form.description || null
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
    await ElMessageBox.confirm(`确定删除物资【${row.name}】吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const resp = await api.delete(`/api/warehouse/materials/${row.id}`)
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
