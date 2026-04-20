<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="header">
          <span>盘点管理</span>
          <div class="header-actions">
            <el-button @click="fetchList">刷新</el-button>
            <el-button type="primary" @click="openCreate">新增盘点</el-button>
          </div>
        </div>
      </template>

      <el-table :data="list" v-loading="loading" row-key="id">
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="materialName" label="物资" width="220" />
        <el-table-column prop="systemQuantity" label="系统数量" width="120" />
        <el-table-column prop="actualQuantity" label="实际数量" width="120" />
        <el-table-column prop="difference" label="差异" width="100" />
        <el-table-column prop="checkDate" label="盘点日期" width="140" />
        <el-table-column prop="remark" label="备注" min-width="200" />
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

    <el-dialog v-model="dialogVisible" title="新增盘点" width="620px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="物资" prop="materialId">
          <el-select
            v-model="form.materialId"
            filterable
            remote
            clearable
            :remote-method="searchMaterials"
            :loading="materialLoading"
            placeholder="请选择物资"
            style="width: 100%"
          >
            <el-option v-for="m in materialOptions" :key="m.id" :label="m.name" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="实际数量" prop="actualQuantity">
          <el-input-number v-model="form.actualQuantity" :min="0" :max="999999" />
        </el-form-item>
        <el-form-item label="盘点日期" prop="checkDate">
          <el-date-picker v-model="form.checkDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" />
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
import { ElMessage } from 'element-plus'
import { api } from '../../api/client'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({
  materialId: null,
  actualQuantity: 0,
  checkDate: '',
  remark: ''
})

const rules = {
  materialId: [{ required: true, message: '请选择物资', trigger: 'change' }],
  actualQuantity: [{ required: true, message: '请输入数量', trigger: 'blur' }],
  checkDate: [{ required: true, message: '请选择日期', trigger: 'change' }]
}

const materialLoading = ref(false)
const materialOptions = ref([])

async function searchMaterials(query) {
  materialLoading.value = true
  try {
    const resp = await api.get('/api/warehouse/materials', {
      params: { page: 1, pageSize: 50, keyword: query || undefined }
    })
    const body = resp.data
    if (body.code === 200) materialOptions.value = body.data?.list || []
    else materialOptions.value = []
  } catch (e) {
    materialOptions.value = []
  } finally {
    materialLoading.value = false
  }
}

async function fetchList() {
  loading.value = true
  try {
    const resp = await api.get('/api/warehouse/checks', { params: { page: page.value, pageSize: pageSize.value } })
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

function openCreate() {
  const today = new Date()
  const yyyy = today.getFullYear()
  const mm = String(today.getMonth() + 1).padStart(2, '0')
  const dd = String(today.getDate()).padStart(2, '0')
  form.materialId = null
  form.actualQuantity = 0
  form.checkDate = `${yyyy}-${mm}-${dd}`
  form.remark = ''
  dialogVisible.value = true
  searchMaterials('')
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate()
  saving.value = true
  try {
    const resp = await api.post('/api/warehouse/checks', {
      materialId: form.materialId,
      actualQuantity: form.actualQuantity,
      checkDate: form.checkDate,
      remark: form.remark || null
    })
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '保存失败')
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await fetchList()
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
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
}

.pager {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}
</style>
