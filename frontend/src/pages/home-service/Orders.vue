<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="header">
          <span>预约管理</span>
          <div class="header-actions">
            <el-select v-model="status" placeholder="状态" clearable style="width: 160px" @change="reload">
              <el-option label="待确认" value="PENDING" />
              <el-option label="已确认" value="CONFIRMED" />
              <el-option label="服务中" value="IN_PROGRESS" />
              <el-option label="已完成" value="COMPLETED" />
              <el-option label="已取消" value="CANCELLED" />
            </el-select>
            <el-button @click="fetchList">刷新</el-button>
            <el-button type="primary" @click="openCreate">创建预约</el-button>
          </div>
        </div>
      </template>

      <div style="overflow-x: auto">
      <el-table :data="list" v-loading="loading" row-key="id">
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="elderlyName" label="老人" width="160" />
        <el-table-column prop="serviceItemName" label="服务项目" width="180" />
        <el-table-column prop="expectedTime" label="期望时间" width="180" />
        <el-table-column prop="address" label="地址" min-width="220" />
        <el-table-column prop="assignedStaffName" label="服务人员" width="160" />
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" v-if="row.status === 'PENDING'" @click="setStatus(row, 'CONFIRMED')">确认</el-button>
            <el-button link type="primary" v-if="row.status === 'CONFIRMED'" @click="setStatus(row, 'IN_PROGRESS')">开始</el-button>
            <el-button link type="primary" v-if="row.status === 'IN_PROGRESS'" @click="setStatus(row, 'COMPLETED')">完成</el-button>
            <el-button link type="danger" v-if="row.status === 'PENDING' || row.status === 'CONFIRMED'" @click="setStatus(row, 'CANCELLED')">取消</el-button>
            <el-button link type="primary" @click="goRecords(row)">记录</el-button>
          </template>
        </el-table-column>
      </el-table>
      </div>

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

    <el-dialog v-model="dialogVisible" title="创建预约" width="720px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12">
            <el-form-item label="老人" prop="elderlyId">
              <el-select
                v-model="form.elderlyId"
                filterable
                remote
                clearable
                :remote-method="searchElderly"
                :loading="elderlyLoading"
                placeholder="可选"
                style="width: 100%"
              >
                <el-option v-for="o in elderlyOptions" :key="o.id" :label="elderlyLabel(o)" :value="o.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="服务项目" prop="serviceItemId">
              <el-select v-model="form.serviceItemId" placeholder="请选择" style="width: 100%">
                <el-option-group v-for="group in groupedItems" :key="group.label" :label="group.label">
                  <el-option v-for="item in group.options" :key="item.id" :label="item.name" :value="item.id" />
                </el-option-group>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12">
            <el-form-item label="期望时间" prop="expectedTime">
              <el-date-picker v-model="form.expectedTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="服务人员" prop="assignedStaffId">
              <el-select
                v-model="form.assignedStaffId"
                filterable
                remote
                clearable
                :remote-method="searchStaff"
                :loading="staffLoading"
                placeholder="可选"
                style="width: 100%"
              >
                <el-option v-for="s in staffOptions" :key="s.id" :label="s.name" :value="s.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="地址" prop="address">
          <el-input v-model="form.address" />
        </el-form-item>
        <el-form-item label="特殊说明" prop="specialNote">
          <el-input v-model="form.specialNote" type="textarea" :rows="3" />
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
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../../api/client'

const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const status = ref('')

const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({
  elderlyId: null,
  serviceItemId: null,
  expectedTime: '',
  address: '',
  specialNote: '',
  assignedStaffId: null
})

const rules = {
  serviceItemId: [{ required: true, message: '请选择服务项目', trigger: 'change' }]
}

const elderlyLoading = ref(false)
const elderlyOptions = ref([])
const staffLoading = ref(false)
const staffOptions = ref([])
const itemOptions = ref([])

const groupedItems = computed(() => {
  const map = new Map()
  for (const item of itemOptions.value) {
    const cat = item.category || '未分类'
    if (!map.has(cat)) map.set(cat, [])
    map.get(cat).push(item)
  }
  return Array.from(map.entries()).map(([label, options]) => ({ label, options }))
})

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
  } finally {
    elderlyLoading.value = false
  }
}

async function searchStaff(query) {
  staffLoading.value = true
  try {
    const resp = await api.get('/api/staff/options', { params: { keyword: query || undefined } })
    const body = resp.data
    if (body.code === 200) staffOptions.value = body.data || []
    else staffOptions.value = []
  } finally {
    staffLoading.value = false
  }
}

async function fetchItems() {
  try {
    const resp = await api.get('/api/home-service/items/options')
    const body = resp.data
    if (body.code === 200) itemOptions.value = body.data || []
    else itemOptions.value = []
  } catch (e) {
    itemOptions.value = []
  }
}

async function fetchList() {
  loading.value = true
  try {
    const resp = await api.get('/api/home-service/orders', {
      params: { page: page.value, pageSize: pageSize.value, status: status.value || undefined }
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

function openCreate() {
  form.elderlyId = null
  form.serviceItemId = null
  form.expectedTime = ''
  form.address = ''
  form.specialNote = ''
  form.assignedStaffId = null
  dialogVisible.value = true
  fetchItems()
  searchElderly('')
  searchStaff('')
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate()
  saving.value = true
  try {
    const resp = await api.post('/api/home-service/orders', {
      elderlyId: form.elderlyId || null,
      serviceItemId: form.serviceItemId,
      expectedTime: form.expectedTime || null,
      address: form.address || null,
      specialNote: form.specialNote || null,
      assignedStaffId: form.assignedStaffId || null
    })
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '保存失败')
    ElMessage.success('创建成功')
    dialogVisible.value = false
    await fetchList()
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function setStatus(row, next) {
  try {
    await ElMessageBox.confirm(`确定将订单 #${row.id} 状态修改为 ${next} 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const resp = await api.put(`/api/home-service/orders/${row.id}`, { status: next })
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '操作失败')
    ElMessage.success('操作成功')
    await fetchList()
  } catch (e) {
    if (e === 'cancel') return
    ElMessage.error(e.message || '操作失败')
  }
}

function goRecords(row) {
  router.push({ path: '/home-service/records', query: { orderId: row.id } })
}

onMounted(() => {
  fetchItems()
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
</style>
