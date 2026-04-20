<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="header">
          <span>退住管理</span>
          <div class="header-actions">
            <el-button @click="goBack">返回</el-button>
            <el-button type="primary" :loading="saving" @click="submit">确认退住</el-button>
          </div>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="选择老人" prop="elderlyId">
          <el-select
            v-model="form.elderlyId"
            filterable
            remote
            clearable
            :remote-method="searchElderly"
            :loading="elderlyLoading"
            placeholder="在住老人"
            style="width: 100%"
          >
            <el-option v-for="o in elderlyOptions" :key="o.id" :label="elderlyLabel(o)" :value="o.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="退住日期" prop="dischargeDate">
          <el-date-picker v-model="form.dischargeDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="退住原因" prop="dischargeReason">
          <el-input v-model="form.dischargeReason" type="textarea" :rows="3" placeholder="可选" />
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 已退住老人列表 -->
    <el-card style="margin-top: 20px">
      <template #header>
        <div class="header">
          <span>已退住老人列表</span>
          <el-button type="primary" size="small" @click="loadDischarged">刷新</el-button>
        </div>
      </template>
      <el-table :data="dischargedList" v-loading="dischargedLoading" stripe>
        <el-table-column prop="uniqueNo" label="编号" width="130" />
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="category" label="类别" width="100">
          <template #default="{ row }">
            {{ row.category === 'WU_BAO' ? '五保对象' : row.category === 'LOW_BAO' ? '低保对象' : '社会化入住' }}
          </template>
        </el-table-column>
        <el-table-column prop="dischargeDate" label="退住日期" width="120" />
        <el-table-column prop="dischargeReason" label="退住原因" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button type="warning" size="small" @click="undoDischarge(row)">撤销退住</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../../api/client'

const route = useRoute()
const router = useRouter()
const saving = ref(false)
const formRef = ref()

const elderlyLoading = ref(false)
const elderlyOptions = ref([])

const dischargedList = ref([])
const dischargedLoading = ref(false)

const form = reactive({
  elderlyId: null,
  dischargeDate: '',
  dischargeReason: ''
})

const rules = {
  elderlyId: [{ required: true, message: '请选择老人', trigger: 'change' }],
  dischargeDate: [{ required: true, message: '请选择退住日期', trigger: 'change' }]
}

function goBack() {
  router.back()
}

function elderlyLabel(o) {
  const no = o.unique_no || o.uniqueNo || ''
  return `${o.name}${no ? '（' + no + '）' : ''}`
}

async function searchElderly(query) {
  elderlyLoading.value = true
  try {
    const resp = await api.get('/api/elderly/options', { params: { keyword: query || undefined } })
    const body = resp.data
    if (body.code === 200) elderlyOptions.value = body.data || []
    else elderlyOptions.value = []
  } catch (e) {
    elderlyOptions.value = []
  } finally {
    elderlyLoading.value = false
  }
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate()
  saving.value = true
  try {
    const resp = await api.put(`/api/elderly/${form.elderlyId}/discharge`, {
      dischargeDate: form.dischargeDate,
      dischargeReason: form.dischargeReason || null
    })
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '退住失败')
    ElMessage.success('退住成功')
    router.push('/elderly/list')
  } catch (e) {
    ElMessage.error(e.message || '退住失败')
  } finally {
    saving.value = false
  }
}

async function loadDischarged() {
  dischargedLoading.value = true
  try {
    const res = await api.get('/api/elderly', { params: { status: 'DISCHARGED', page: 1, pageSize: 100 } })
    if (res.data?.code === 200) {
      dischargedList.value = res.data.data?.list || []
    }
  } catch (e) {
    console.error(e)
  } finally {
    dischargedLoading.value = false
  }
}

async function undoDischarge(row) {
  try {
    await ElMessageBox.confirm(
      `确定要撤销「${row.name}」的退住吗？撤销后该老人将恢复为在住状态，需要手动重新分配床位。`,
      '撤销退住确认',
      { type: 'warning', confirmButtonText: '确定撤销', cancelButtonText: '取消' }
    )
    await api.put(`/api/elderly/${row.id}/undo-discharge`)
    ElMessage.success('退住已撤销，老人已恢复为在住状态')
    loadDischarged()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.message || '撤销失败')
    }
  }
}

onMounted(() => {
  const today = new Date()
  const yyyy = today.getFullYear()
  const mm = String(today.getMonth() + 1).padStart(2, '0')
  const dd = String(today.getDate()).padStart(2, '0')
  form.dischargeDate = `${yyyy}-${mm}-${dd}`
  searchElderly('')
  const id = route.query.id
  if (id) {
    form.elderlyId = Number(id)
  }
  loadDischarged()
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
