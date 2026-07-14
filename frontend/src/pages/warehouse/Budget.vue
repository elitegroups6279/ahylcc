<template>
  <div class="page">
    <PageHeader title="预算管理">
      <template #actions>
        <el-select v-model="filterYear" clearable placeholder="年度" style="width: 120px" @change="reload">
          <el-option v-for="y in yearOptions" :key="y" :label="y + '年'" :value="y" />
        </el-select>
        <el-select v-model="filterCategory" clearable placeholder="资金类别" style="width: 150px" @change="reload">
          <el-option label="集中供养" value="CENTRALIZED" />
          <el-option label="社会化" value="SOCIAL" />
        </el-select>
        <el-button @click="fetchList">刷新</el-button>
        <el-button type="primary" @click="openCreate">新增预算</el-button>
      </template>
    </PageHeader>

    <el-card>
      <el-table :data="list" v-loading="loading" row-key="id">
        <el-table-column prop="year" label="年度" width="100" />
        <el-table-column prop="month" label="月份" width="90" />
        <el-table-column label="资金类别" width="140">
          <template #default="{ row }">{{ supplyCategoryLabel(row.supplyCategory) }}</template>
        </el-table-column>
        <el-table-column prop="budgetAmount" label="预算金额" width="140">
          <template #default="{ row }">¥{{ formatMoney(row.budgetAmount) }}</template>
        </el-table-column>
        <el-table-column prop="usedAmount" label="已使用" width="140">
          <template #default="{ row }">¥{{ formatMoney(row.usedAmount) }}</template>
        </el-table-column>
        <el-table-column label="使用率" width="200">
          <template #default="{ row }">
            <el-progress :percentage="usageRate(row)" :status="usageStatus(row)" />
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
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

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新增预算' : '编辑预算'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="年度" prop="year">
          <el-select v-model="form.year" placeholder="请选择年度" style="width: 100%">
            <el-option v-for="y in yearOptions" :key="y" :label="y + '年'" :value="y" />
          </el-select>
        </el-form-item>
        <el-form-item label="月份" prop="month">
          <el-select v-model="form.month" placeholder="请选择月份" style="width: 100%">
            <el-option v-for="m in 12" :key="m" :label="m + '月'" :value="m" />
          </el-select>
        </el-form-item>
        <el-form-item label="资金类别" prop="supplyCategory">
          <el-select v-model="form.supplyCategory" placeholder="请选择资金类别" style="width: 100%">
            <el-option label="集中供养" value="CENTRALIZED" />
            <el-option label="社会化" value="SOCIAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="预算金额" prop="budgetAmount">
          <el-input-number v-model="form.budgetAmount" :min="0" :precision="2" :step="100" style="width: 220px" />
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '../../components/common/PageHeader.vue'
import { api } from '../../api/client'
import { useFormat } from '@/composables/useFormat'

const { formatMoney, supplyCategoryLabel } = useFormat()

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const filterYear = ref('')
const filterCategory = ref('')

const dialogVisible = ref(false)
const dialogMode = ref('create')
const formRef = ref()
const form = reactive({
  id: null,
  year: new Date().getFullYear(),
  month: new Date().getMonth() + 1,
  supplyCategory: 'CENTRALIZED',
  budgetAmount: 0,
  remark: ''
})

const currentYear = new Date().getFullYear()
const yearOptions = Array.from({ length: 10 }, (_, i) => currentYear - 5 + i)

const rules = {
  year: [{ required: true, message: '请选择年度', trigger: 'change' }],
  month: [{ required: true, message: '请选择月份', trigger: 'change' }],
  supplyCategory: [{ required: true, message: '请选择资金类别', trigger: 'change' }],
  budgetAmount: [{ required: true, message: '请输入预算金额', trigger: 'blur' }]
}

function usageRate(row) {
  const budget = Number(row.budgetAmount) || 0
  const used = Number(row.usedAmount) || 0
  if (budget <= 0) return 0
  const rate = Math.round((used / budget) * 100)
  return Math.min(rate, 100)
}

function usageStatus(row) {
  const rate = usageRate(row)
  if (rate >= 100) return 'exception'
  if (rate >= 80) return 'warning'
  return ''
}

async function fetchList() {
  loading.value = true
  try {
    const resp = await api.get('/api/budget', {
      params: {
        page: page.value,
        size: pageSize.value,
        year: filterYear.value || undefined,
        supplyCategory: filterCategory.value || undefined
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
  form.year = currentYear
  form.month = new Date().getMonth() + 1
  form.supplyCategory = 'CENTRALIZED'
  form.budgetAmount = 0
  form.remark = ''
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
  form.year = row.year
  form.month = row.month
  form.supplyCategory = row.supplyCategory || 'CENTRALIZED'
  form.budgetAmount = row.budgetAmount || 0
  form.remark = row.remark || ''
  dialogVisible.value = true
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      year: form.year,
      month: form.month,
      supplyCategory: form.supplyCategory,
      budgetAmount: form.budgetAmount,
      remark: form.remark || null
    }
    if (dialogMode.value === 'create') {
      const resp = await api.post('/api/budget', payload)
      const body = resp.data
      if (body.code !== 200) throw new Error(body.msg || '创建失败')
      ElMessage.success('创建成功')
    } else {
      const resp = await api.put(`/api/budget/${form.id}`, payload)
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
    await ElMessageBox.confirm(`确定删除该预算记录吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const resp = await api.delete(`/api/budget/${row.id}`)
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
