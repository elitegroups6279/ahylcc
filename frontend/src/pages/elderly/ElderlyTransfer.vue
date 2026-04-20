<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="header">
          <span>转床管理</span>
          <div class="header-actions">
            <el-button @click="goBack">返回</el-button>
            <el-button type="primary" :loading="saving" @click="submit">确认转床</el-button>
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
        <el-form-item label="目标床位" prop="toBedId">
          <el-select v-model="form.toBedId" filterable placeholder="请选择空闲床位" style="width: 100%">
            <el-option v-for="b in bedOptions" :key="b.id" :label="bedLabel(b)" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="转床日期" prop="transferDate">
          <el-date-picker v-model="form.transferDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="原因" prop="reason">
          <el-input v-model="form.reason" type="textarea" :rows="3" placeholder="可选" />
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
const bedOptions = ref([])

const form = reactive({
  elderlyId: null,
  toBedId: null,
  transferDate: '',
  reason: ''
})

const rules = {
  elderlyId: [{ required: true, message: '请选择老人', trigger: 'change' }],
  toBedId: [{ required: true, message: '请选择目标床位', trigger: 'change' }],
  transferDate: [{ required: true, message: '请选择转床日期', trigger: 'change' }]
}

function goBack() {
  router.back()
}

function elderlyLabel(o) {
  const no = o.unique_no || o.uniqueNo || ''
  return `${o.name}${no ? '（' + no + '）' : ''}`
}

function bedLabel(b) {
  return `${b.building || ''}${b.floor ? ' ' + b.floor : ''} ${b.roomNumber || ''} ${b.bedNumber || ''}`.trim()
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

async function fetchBeds() {
  const resp = await api.get('/api/beds/available')
  const body = resp.data
  if (body.code === 200) bedOptions.value = body.data || []
  else bedOptions.value = []
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate()
  saving.value = true
  try {
    const resp = await api.put(`/api/elderly/${form.elderlyId}/transfer`, {
      toBedId: form.toBedId,
      transferDate: form.transferDate,
      reason: form.reason || null
    })
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '转床失败')
    ElMessage.success('转床成功')
    router.push('/elderly/list')
  } catch (e) {
    ElMessage.error(e.message || '转床失败')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  const today = new Date()
  const yyyy = today.getFullYear()
  const mm = String(today.getMonth() + 1).padStart(2, '0')
  const dd = String(today.getDate()).padStart(2, '0')
  form.transferDate = `${yyyy}-${mm}-${dd}`
  searchElderly('')
  await fetchBeds()
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
