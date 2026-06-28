<template>
  <div class="page">
    <PageHeader title="到货验收">
      <template #actions>
        <el-button @click="fetchList">刷新</el-button>
        <el-button type="primary" @click="openCreate">新增验收</el-button>
      </template>
    </PageHeader>

    <el-card>
      <el-table :data="list" v-loading="loading" row-key="id">
        <el-table-column label="关联采购单号" width="190">
          <template #default="{ row }">
            {{ requestNoMap[row.purchaseRequestId] || ('#' + row.purchaseRequestId) }}
          </template>
        </el-table-column>
        <el-table-column prop="inspectorName" label="验收人" width="120" />
        <el-table-column label="验收结果" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.inspectResult === 'PASS'" type="success">合格</el-tag>
            <el-tag v-else-if="row.inspectResult === 'FAIL'" type="danger">不合格</el-tag>
            <el-tag v-else>{{ row.inspectResult }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="actualQuantity" label="实收数量" width="120" />
        <el-table-column prop="receiptDate" label="验收日期" width="140" />
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

    <!-- 新增验收 -->
    <el-dialog v-model="dialogVisible" title="新增验收" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="采购申请" prop="purchaseRequestId">
          <el-select
            v-model="form.purchaseRequestId"
            filterable
            :loading="prLoading"
            placeholder="请选择已通过的采购申请"
            style="width: 100%"
          >
            <el-option
              v-for="pr in approvedRequests"
              :key="pr.id"
              :label="`${pr.requestNo}（￥${formatAmount(pr.totalAmount)}）`"
              :value="pr.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="验收结果" prop="inspectResult">
          <el-radio-group v-model="form.inspectResult">
            <el-radio value="PASS">合格</el-radio>
            <el-radio value="FAIL">不合格</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="实收数量" prop="actualQuantity">
          <el-input-number v-model="form.actualQuantity" :min="0" :max="999999" />
        </el-form-item>
        <el-form-item label="验收日期" prop="receiptDate">
          <el-date-picker v-model="form.receiptDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="备注信息（可选）" />
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
import PageHeader from '../../components/common/PageHeader.vue'
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
  purchaseRequestId: null,
  inspectResult: 'PASS',
  actualQuantity: 0,
  receiptDate: '',
  remark: ''
})
const rules = {
  purchaseRequestId: [{ required: true, message: '请选择采购申请', trigger: 'change' }],
  inspectResult: [{ required: true, message: '请选择验收结果', trigger: 'change' }],
  actualQuantity: [{ required: true, message: '请输入实收数量', trigger: 'blur' }]
}

const prLoading = ref(false)
const approvedRequests = ref([])
const requestNoMap = ref({})

function formatAmount(amount) {
  if (amount === null || amount === undefined) return '0.00'
  const n = Number(amount)
  if (Number.isNaN(n)) return String(amount)
  return n.toFixed(2)
}

async function fetchList() {
  loading.value = true
  try {
    const resp = await api.get('/api/purchase-receipt', {
      params: { page: page.value, size: pageSize.value }
    })
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '加载失败')
    list.value = body.data?.records || []
    total.value = body.data?.total || 0
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function loadApprovedRequests() {
  prLoading.value = true
  try {
    const resp = await api.get('/api/purchase-request', {
      params: { page: 1, size: 100, status: 'APPROVED' }
    })
    const body = resp.data
    if (body.code === 200) {
      approvedRequests.value = body.data?.records || []
      const map = {}
      approvedRequests.value.forEach((pr) => {
        map[pr.id] = pr.requestNo
      })
      requestNoMap.value = map
    } else {
      approvedRequests.value = []
    }
  } catch (e) {
    approvedRequests.value = []
  } finally {
    prLoading.value = false
  }
}

function resetForm() {
  const today = new Date()
  const yyyy = today.getFullYear()
  const mm = String(today.getMonth() + 1).padStart(2, '0')
  const dd = String(today.getDate()).padStart(2, '0')
  form.purchaseRequestId = null
  form.inspectResult = 'PASS'
  form.actualQuantity = 0
  form.receiptDate = `${yyyy}-${mm}-${dd}`
  form.remark = ''
}

function openCreate() {
  resetForm()
  loadApprovedRequests()
  dialogVisible.value = true
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      purchaseRequestId: form.purchaseRequestId,
      inspectResult: form.inspectResult,
      actualQuantity: form.actualQuantity,
      receiptDate: form.receiptDate,
      remark: form.remark || null
    }
    const resp = await api.post('/api/purchase-receipt', payload)
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
  loadApprovedRequests()
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
