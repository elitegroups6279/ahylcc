<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="header">
          <span>在住老人列表</span>
          <div class="header-actions">
            <el-select v-model="status" placeholder="状态" clearable style="width: 140px" @change="reload">
              <el-option label="在住" value="ACTIVE" />
              <el-option label="请假中" value="ON_LEAVE" />
              <el-option label="退住" value="DISCHARGED" />
            </el-select>
            <el-input v-model="keyword" placeholder="姓名/编号" clearable style="width: 200px" @keyup.enter="reload" />
            <el-button @click="fetchList">刷新</el-button>
            <el-button type="success" :icon="Download" @click="downloadTemplate">下载导入模板</el-button>
            <el-button type="warning" :icon="Upload" @click="importDialogVisible = true">导入数据</el-button>
            <el-button type="primary" @click="goAdd">新增入住</el-button>
          </div>
        </div>
      </template>

      <el-table :data="list" v-loading="loading" row-key="id">
        <el-table-column prop="uniqueNo" label="编号" width="120" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column label="性别" width="80">
          <template #default="{ row }">{{ row.gender === 1 ? '男' : row.gender === 0 ? '女' : '-' }}</template>
        </el-table-column>
        <el-table-column prop="age" label="年龄" width="80" />
        <el-table-column label="床位" width="140">
          <template #default="{ row }">{{ row.bedNumber || '-' }}</template>
        </el-table-column>
        <el-table-column label="类别" width="120">
          <template #default="{ row }">{{ categoryText(row.category) }}</template>
        </el-table-column>
        <el-table-column label="失能等级" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.disabilityLevel === 'SELF_CARE' || row.disabilityLevel === 'INTACT'" type="success">能力完好</el-tag>
            <el-tag v-else-if="row.disabilityLevel === 'MILD'" type="primary">轻度失能</el-tag>
            <el-tag v-else-if="row.disabilityLevel === 'MODERATE'" type="warning">中度失能</el-tag>
            <el-tag v-else-if="row.disabilityLevel === 'SEVERE'" type="danger">重度失能</el-tag>
            <el-tag v-else-if="row.disabilityLevel === 'TOTAL'" type="danger">完全失能</el-tag>
            <el-tag v-else type="info">能力完好</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="余额" width="120">
          <template #default="{ row }">￥{{ formatAmount(row.feeBalance) }}</template>
        </el-table-column>
        <el-table-column prop="admissionDate" label="入住日期" width="140" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'ACTIVE'" type="success">在住</el-tag>
            <el-tag v-else-if="row.status === 'ON_LEAVE'" type="warning">请假中</el-tag>
            <el-tag v-else type="info">已退住</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row)">详情</el-button>
            <el-button link type="primary" @click="goTransfer(row)" v-if="row.status === 'ACTIVE'">转床</el-button>
            <el-button link type="warning" @click="openLeaveDialog(row)" v-if="row.status === 'ACTIVE'">请假</el-button>
            <el-button link type="success" @click="handleReturn(row)" v-if="row.status === 'ON_LEAVE'">销假</el-button>
            <el-button link type="danger" @click="goDischarge(row)" v-if="row.status === 'ACTIVE' || row.status === 'ON_LEAVE'">退住</el-button>
            <el-button link type="warning" size="small" @click="handleUndoDischarge(row)" v-if="row.status === 'DISCHARGED'">撤销退住</el-button>
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

    <!-- 导入对话框 -->
    <el-dialog v-model="importDialogVisible" title="批量导入老人数据" width="560px" :close-on-click-modal="false">
      <div v-if="!importResult">
        <el-upload
          ref="uploadRef"
          drag
          action=""
          :auto-upload="false"
          :limit="1"
          accept=".xlsx,.xls"
          :on-change="handleFileChange"
        >
          <el-icon class="el-icon--upload"><Upload /></el-icon>
          <div class="el-upload__text">将Excel文件拖到此处，或<em>点击选择</em></div>
          <template #tip>
            <div class="el-upload__tip">仅支持 .xlsx / .xls 格式，请先下载模板填写数据</div>
          </template>
        </el-upload>
      </div>

      <!-- 导入结果展示 -->
      <div v-else>
        <el-result
          :icon="importResult.failCount === 0 ? 'success' : 'warning'"
          :title="'导入完成'"
          :sub-title="'成功 ' + importResult.successCount + ' 条，失败 ' + importResult.failCount + ' 条'"
        />
        <el-table v-if="importResult.errors && importResult.errors.length" :data="importResult.errors" border size="small" max-height="300" style="margin-top: 12px">
          <el-table-column prop="row" label="行号" width="80" />
          <el-table-column prop="reason" label="失败原因" />
        </el-table>
      </div>

      <template #footer>
        <el-button @click="closeImportDialog">{{ importResult ? '关闭' : '取消' }}</el-button>
        <el-button v-if="!importResult" type="primary" :loading="importing" @click="submitImport" :disabled="!importFile">
          开始导入
        </el-button>
      </template>
    </el-dialog>

    <!-- 请假对话框 -->
    <el-dialog v-model="leaveDialogVisible" title="老人请假" width="500px" :close-on-click-modal="false">
      <el-form :model="leaveForm" :rules="leaveRules" ref="leaveFormRef" label-width="120px">
        <el-form-item label="老人姓名">
          <el-input :value="leaveForm.elderlyName" disabled />
        </el-form-item>
        <el-form-item label="请假开始日期" prop="startDate">
          <el-date-picker v-model="leaveForm.startDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="预计结束日期" prop="endDate">
          <el-date-picker v-model="leaveForm.endDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期（可不填）" style="width: 100%" />
        </el-form-item>
        <el-form-item label="请假原因" prop="reason">
          <el-input v-model="leaveForm.reason" type="textarea" :rows="3" placeholder="请输入请假原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="leaveDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitLeave" :loading="leaveLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 销假对话框 -->
    <el-dialog v-model="returnDialogVisible" title="老人销假" width="500px" :close-on-click-modal="false">
      <el-form :model="returnForm" ref="returnFormRef" label-width="120px">
        <el-form-item label="老人姓名">
          <el-input :value="returnForm.elderlyName" disabled />
        </el-form-item>
        <el-form-item label="返院日期" prop="returnDate">
          <el-date-picker v-model="returnForm.returnDate" type="date" value-format="YYYY-MM-DD" placeholder="选择返院日期" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="returnDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReturn" :loading="returnLoading">确认销假</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Upload } from '@element-plus/icons-vue'
import { api } from '../../api/client'

const router = useRouter()
const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const status = ref('ACTIVE')

// 导入相关
const importDialogVisible = ref(false)
const importing = ref(false)
const importFile = ref(null)
const importResult = ref(null)
const uploadRef = ref(null)

function formatAmount(amount) {
  if (amount === null || amount === undefined) return '0.00'
  const n = Number(amount)
  if (Number.isNaN(n)) return String(amount)
  return n.toFixed(2)
}

function categoryText(c) {
  if (c === 'SOCIAL') return '社会化'
  if (c === 'LOW_BAO') return '低保对象'
  if (c === 'WU_BAO') return '五保对象'
  return c || '-'
}

async function fetchList() {
  loading.value = true
  try {
    const resp = await api.get('/api/elderly', {
      params: {
        page: page.value,
        pageSize: pageSize.value,
        keyword: keyword.value || undefined,
        status: status.value || undefined
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

function goAdd() {
  router.push('/elderly/add')
}

function goDetail(row) {
  router.push(`/elderly/detail/${row.id}`)
}

function goDischarge(row) {
  router.push({ path: '/elderly/discharge', query: { id: row.id } })
}

function goTransfer(row) {
  router.push({ path: '/elderly/transfer', query: { id: row.id } })
}

// 下载导入模板
const downloadTemplate = async () => {
  try {
    const res = await api.get('/api/elderly/import-template', { responseType: 'blob' })
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', 'elderly_import_template.xlsx')
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error('下载模板失败')
  }
}

// 处理文件选择
const handleFileChange = (file) => {
  importFile.value = file.raw
}

// 提交导入
const submitImport = async () => {
  if (!importFile.value) return
  importing.value = true
  try {
    const formData = new FormData()
    formData.append('file', importFile.value)
    const res = await api.post('/api/elderly/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    if (res.data?.code === 200) {
      importResult.value = res.data.data
      if (res.data.data.successCount > 0) {
        fetchList() // 刷新列表
      }
    } else {
      ElMessage.error(res.data?.msg || '导入失败')
    }
  } catch (e) {
    ElMessage.error('导入失败: ' + (e.response?.data?.msg || e.message))
  } finally {
    importing.value = false
  }
}

// 关闭导入对话框
const closeImportDialog = () => {
  importDialogVisible.value = false
  importFile.value = null
  importResult.value = null
  if (uploadRef.value) uploadRef.value.clearFiles()
}

// 请假相关
const leaveDialogVisible = ref(false)
const leaveLoading = ref(false)
const leaveFormRef = ref(null)
const leaveForm = reactive({
  elderlyId: null,
  elderlyName: '',
  startDate: '',
  endDate: '',
  reason: ''
})
const leaveRules = {
  startDate: [{ required: true, message: '请选择请假开始日期', trigger: 'change' }]
}

// 销假相关
const returnDialogVisible = ref(false)
const returnLoading = ref(false)
const returnFormRef = ref(null)
const returnForm = reactive({
  elderlyId: null,
  elderlyName: '',
  returnDate: ''
})

const openLeaveDialog = (row) => {
  leaveForm.elderlyId = row.id
  leaveForm.elderlyName = row.name
  leaveForm.startDate = new Date().toISOString().slice(0, 10)
  leaveForm.endDate = ''
  leaveForm.reason = ''
  leaveDialogVisible.value = true
}

const submitLeave = async () => {
  const valid = await leaveFormRef.value.validate().catch(() => false)
  if (!valid) return
  leaveLoading.value = true
  try {
    await api.post(`/api/elderly/${leaveForm.elderlyId}/leave`, {
      startDate: leaveForm.startDate,
      endDate: leaveForm.endDate || null,
      reason: leaveForm.reason
    })
    ElMessage.success('请假成功')
    leaveDialogVisible.value = false
    fetchList()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '请假失败')
  } finally {
    leaveLoading.value = false
  }
}

const handleReturn = (row) => {
  returnForm.elderlyId = row.id
  returnForm.elderlyName = row.name
  returnForm.returnDate = new Date().toISOString().slice(0, 10)
  returnDialogVisible.value = true
}

const submitReturn = async () => {
  returnLoading.value = true
  try {
    await api.put(`/api/elderly/${returnForm.elderlyId}/leave/return`, {
      returnDate: returnForm.returnDate || null
    })
    ElMessage.success('销假成功')
    returnDialogVisible.value = false
    fetchList()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '销假失败')
  } finally {
    returnLoading.value = false
  }
}

const handleUndoDischarge = async (row) => {
  try {
    await ElMessageBox.confirm(
      '确认撤销该老人的退住操作？撤销后需要手动重新分配床位。',
      '撤销退住确认',
      { type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消' }
    )
    await api.put(`/api/elderly/${row.id}/undo-discharge`)
    ElMessage.success('撤销退住成功')
    reload()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.message || e.response?.data?.msg || '撤销失败')
    }
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
