<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="header">
          <span>打卡记录</span>
          <div class="header-actions">
            <el-select
              v-model="staffId"
              filterable
              remote
              clearable
              :remote-method="searchStaff"
              :loading="staffLoading"
              placeholder="选择护工"
              style="width: 220px"
              @change="reload"
            >
              <el-option v-for="s in staffOptions" :key="s.id" :label="s.name" :value="s.id" />
            </el-select>
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="~"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              style="width: 280px"
              @change="reload"
            />
            <el-button @click="reset">重置</el-button>
            <el-button @click="fetchList">刷新</el-button>
            <el-button type="primary" @click="openUpsert">补录打卡</el-button>
          </div>
        </div>
      </template>

      <el-table :data="list" v-loading="loading" row-key="id">
        <el-table-column prop="staffName" label="护工" width="160" />
        <el-table-column prop="attendanceDate" label="日期" width="140" />
        <el-table-column prop="clockInTime" label="上班打卡" width="180" />
        <el-table-column prop="clockOutTime" label="下班打卡" width="180" />
        <el-table-column prop="status" label="状态" width="120" />
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

    <el-dialog v-model="dialogVisible" title="补录打卡" width="620px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="护工" prop="staffId">
          <el-select
            v-model="form.staffId"
            filterable
            remote
            clearable
            :remote-method="searchStaff"
            :loading="staffLoading"
            placeholder="请选择护工"
            style="width: 100%"
          >
            <el-option v-for="s in staffOptions" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期" prop="attendanceDate">
          <el-date-picker v-model="form.attendanceDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="上班时间" prop="clockInTime">
          <el-date-picker v-model="form.clockInTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="下班时间" prop="clockOutTime">
          <el-date-picker v-model="form.clockOutTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择" style="width: 220px">
            <el-option label="正常" value="NORMAL" />
            <el-option label="迟到" value="LATE" />
            <el-option label="早退" value="EARLY" />
            <el-option label="缺勤" value="ABSENT" />
          </el-select>
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

const staffId = ref(null)
const dateRange = ref([])

const staffLoading = ref(false)
const staffOptions = ref([])

const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({
  staffId: null,
  attendanceDate: '',
  clockInTime: '',
  clockOutTime: '',
  status: 'NORMAL',
  remark: ''
})

const rules = {
  staffId: [{ required: true, message: '请选择护工', trigger: 'change' }],
  attendanceDate: [{ required: true, message: '请选择日期', trigger: 'change' }]
}

function buildParams() {
  const [startDate, endDate] = dateRange.value || []
  return {
    page: page.value,
    pageSize: pageSize.value,
    staffId: staffId.value || undefined,
    startDate: startDate || undefined,
    endDate: endDate || undefined
  }
}

async function searchStaff(query) {
  staffLoading.value = true
  try {
    const resp = await api.get('/api/staff/options', { params: { keyword: query || undefined } })
    const body = resp.data
    if (body.code === 200) staffOptions.value = body.data || []
    else staffOptions.value = []
  } catch (e) {
    staffOptions.value = []
  } finally {
    staffLoading.value = false
  }
}

async function fetchList() {
  loading.value = true
  try {
    const resp = await api.get('/api/staff/attendance', { params: buildParams() })
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

function reset() {
  staffId.value = null
  dateRange.value = []
  reload()
}

function openUpsert() {
  const today = new Date()
  const yyyy = today.getFullYear()
  const mm = String(today.getMonth() + 1).padStart(2, '0')
  const dd = String(today.getDate()).padStart(2, '0')
  form.staffId = staffId.value
  form.attendanceDate = `${yyyy}-${mm}-${dd}`
  form.clockInTime = ''
  form.clockOutTime = ''
  form.status = 'NORMAL'
  form.remark = ''
  dialogVisible.value = true
  searchStaff('')
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate()
  saving.value = true
  try {
    const resp = await api.post('/api/staff/attendance', {
      staffId: form.staffId,
      attendanceDate: form.attendanceDate,
      clockInTime: form.clockInTime || null,
      clockOutTime: form.clockOutTime || null,
      status: form.status || 'NORMAL',
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
  searchStaff('')
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
