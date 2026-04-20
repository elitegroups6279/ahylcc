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
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '../../api/client'

const route = useRoute()
const router = useRouter()
const saving = ref(false)
const formRef = ref()

const elderlyLoading = ref(false)
const elderlyOptions = ref([])

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
