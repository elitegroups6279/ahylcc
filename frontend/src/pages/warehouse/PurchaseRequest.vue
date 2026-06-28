<template>
  <div class="page">
    <PageHeader title="采购申请">
      <template #actions>
        <el-select v-model="filterStatus" placeholder="审批状态" clearable style="width: 150px" @change="reload">
          <el-option label="全部" value="" />
          <el-option label="待审批" value="PENDING" />
          <el-option label="已通过" value="APPROVED" />
          <el-option label="已拒绝" value="REJECTED" />
        </el-select>
        <el-select v-model="filterCategory" placeholder="供应类别" clearable style="width: 160px" @change="reload">
          <el-option label="全部" value="" />
          <el-option label="社会化物资" value="SOCIAL" />
          <el-option label="集中供养物资" value="CENTRALIZED" />
        </el-select>
        <el-button @click="fetchList">刷新</el-button>
        <el-button type="primary" @click="openCreate">新增申请</el-button>
      </template>
    </PageHeader>

    <el-card>
      <el-table :data="list" v-loading="loading" row-key="id">
        <el-table-column prop="requestNo" label="申请编号" width="190" />
        <el-table-column prop="applicantName" label="申请人" width="120" />
        <el-table-column label="供应类别" width="140">
          <template #default="{ row }">{{ supplyCategoryLabel(row.supplyCategory) }}</template>
        </el-table-column>
        <el-table-column label="总金额" width="140">
          <template #default="{ row }">￥{{ formatAmount(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column label="审批状态" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.approvalStatus === 'PENDING'" type="warning">待审批</el-tag>
            <el-tag v-else-if="row.approvalStatus === 'APPROVED'" type="success">已通过</el-tag>
            <el-tag v-else-if="row.approvalStatus === 'REJECTED'" type="danger">已拒绝</el-tag>
            <el-tag v-else>{{ row.approvalStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="180" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <template v-if="row.approvalStatus === 'PENDING'">
              <el-button link type="success" @click="openApprove(row)">审批通过</el-button>
              <el-button link type="danger" @click="openReject(row)">拒绝</el-button>
            </template>
            <el-button link type="primary" @click="openDetail(row)">明细</el-button>
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

    <!-- 新增申请 -->
    <el-dialog v-model="createDialogVisible" title="新增采购申请" width="860px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="供应类别" prop="supplyCategory">
          <el-select v-model="form.supplyCategory" style="width: 100%">
            <el-option label="社会化物资" value="SOCIAL" />
            <el-option label="集中供养物资" value="CENTRALIZED" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.supplyCategory === 'CENTRALIZED'" label="拨款批次" prop="allocationId">
          <el-select
            v-model="form.allocationId"
            filterable
            clearable
            :loading="allocationLoading"
            placeholder="请选择拨款批次"
            style="width: 100%"
          >
            <el-option
              v-for="a in allocationOptions"
              :key="a.id"
              :label="`${a.allocateMonth} - ￥${formatAmount(a.totalAmount)}`"
              :value="a.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="供应商" prop="supplierId">
          <el-select
            v-model="form.supplierId"
            filterable
            clearable
            :loading="supplierLoading"
            placeholder="请选择供应商"
            style="width: 100%"
          >
            <el-option v-for="s in supplierOptions" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="采购明细" required>
          <div class="items-wrap">
            <el-button type="primary" plain size="small" @click="addItem">+ 添加明细</el-button>
            <el-table :data="form.items" border style="margin-top: 8px">
              <el-table-column label="物资" min-width="220">
                <template #default="{ row }">
                  <el-select
                    v-model="row.materialId"
                    filterable
                    remote
                    clearable
                    :remote-method="searchMaterials"
                    :loading="materialLoading"
                    placeholder="请选择物资"
                    style="width: 100%"
                    @change="(val) => onMaterialChange(row, val)"
                  >
                    <el-option v-for="m in materialOptions" :key="m.id" :label="m.name" :value="m.id" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="数量" width="140">
                <template #default="{ row }">
                  <el-input-number v-model="row.quantity" :min="1" :max="999999" controls-position="right" style="width: 120px" />
                </template>
              </el-table-column>
              <el-table-column label="单价" width="160">
                <template #default="{ row }">
                  <el-input-number v-model="row.unitPrice" :min="0" :precision="2" :step="1" controls-position="right" style="width: 140px" />
                </template>
              </el-table-column>
              <el-table-column label="小计" width="120">
                <template #default="{ row }">￥{{ formatAmount(row.quantity * row.unitPrice) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="90">
                <template #default="{ $index }">
                  <el-button link type="danger" @click="removeItem($index)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <div class="total-row">合计金额：￥{{ formatAmount(totalAmount) }}</div>
          </div>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="备注信息（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitCreate">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 审批/驳回 -->
    <el-dialog v-model="approveDialogVisible" :title="approveMode === 'approve' ? '审批通过' : '驳回申请'" width="480px">
      <el-form label-width="90px">
        <el-form-item label="申请编号">
          <span>{{ currentRow?.requestNo }}</span>
        </el-form-item>
        <el-form-item label="总金额">
          <span>￥{{ formatAmount(currentRow?.totalAmount) }}</span>
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="approveRemark"
            type="textarea"
            :rows="3"
            :placeholder="approveMode === 'reject' ? '请输入驳回原因（必填）' : '审批备注（可选）'"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveDialogVisible = false">取消</el-button>
        <el-button :type="approveMode === 'approve' ? 'success' : 'danger'" :loading="saving" @click="submitApprove">
          {{ approveMode === 'approve' ? '确认通过' : '确认驳回' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 明细 -->
    <el-dialog v-model="detailDialogVisible" title="采购明细" width="740px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="申请编号">{{ currentRow?.requestNo }}</el-descriptions-item>
        <el-descriptions-item label="申请人">{{ currentRow?.applicantName }}</el-descriptions-item>
        <el-descriptions-item label="供应类别">{{ supplyCategoryLabel(currentRow?.supplyCategory) }}</el-descriptions-item>
        <el-descriptions-item label="总金额">￥{{ formatAmount(currentRow?.totalAmount) }}</el-descriptions-item>
        <el-descriptions-item label="审批状态">
          <el-tag v-if="currentRow?.approvalStatus === 'PENDING'" type="warning">待审批</el-tag>
          <el-tag v-else-if="currentRow?.approvalStatus === 'APPROVED'" type="success">已通过</el-tag>
          <el-tag v-else-if="currentRow?.approvalStatus === 'REJECTED'" type="danger">已拒绝</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ currentRow?.createTime }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ currentRow?.remark || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="currentRow?.approveRemark" label="审批备注" :span="2">{{ currentRow.approveRemark }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="detailItems" border style="margin-top: 12px">
        <el-table-column prop="materialName" label="物资" min-width="180" />
        <el-table-column prop="quantity" label="数量" width="100" />
        <el-table-column label="单价" width="120">
          <template #default="{ row }">￥{{ formatAmount(row.unitPrice) }}</template>
        </el-table-column>
        <el-table-column label="小计" width="120">
          <template #default="{ row }">￥{{ formatAmount(row.subtotal) }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '../../components/common/PageHeader.vue'
import { api } from '../../api/client'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const filterStatus = ref('')
const filterCategory = ref('')

const createDialogVisible = ref(false)
const formRef = ref()
const form = reactive({
  supplyCategory: 'SOCIAL',
  allocationId: null,
  supplierId: null,
  items: [],
  remark: ''
})
const rules = {
  supplyCategory: [{ required: true, message: '请选择供应类别', trigger: 'change' }],
  supplierId: [{ required: true, message: '请选择供应商', trigger: 'change' }]
}

const supplierLoading = ref(false)
const supplierOptions = ref([])
const allocationLoading = ref(false)
const allocationOptions = ref([])
const materialLoading = ref(false)
const materialOptions = ref([])

const approveDialogVisible = ref(false)
const approveMode = ref('approve')
const approveRemark = ref('')
const currentRow = ref(null)

const detailDialogVisible = ref(false)
const detailItems = ref([])

const totalAmount = computed(() => {
  return form.items.reduce((sum, i) => sum + (Number(i.quantity) || 0) * (Number(i.unitPrice) || 0), 0)
})

function formatAmount(amount) {
  if (amount === null || amount === undefined) return '0.00'
  const n = Number(amount)
  if (Number.isNaN(n)) return String(amount)
  return n.toFixed(2)
}

function supplyCategoryLabel(cat) {
  if (cat === 'CENTRALIZED') return '集中供养物资'
  if (cat === 'SOCIAL') return '社会化物资'
  return cat || '-'
}

async function fetchList() {
  loading.value = true
  try {
    const resp = await api.get('/api/purchase-request', {
      params: {
        page: page.value,
        size: pageSize.value,
        status: filterStatus.value || undefined,
        supplyCategory: filterCategory.value || undefined
      }
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

function reload() {
  page.value = 1
  fetchList()
}

async function loadSupplierOptions() {
  supplierLoading.value = true
  try {
    const resp = await api.get('/api/supplier/options')
    const body = resp.data
    if (body.code === 200) supplierOptions.value = body.data || []
    else supplierOptions.value = []
  } catch (e) {
    supplierOptions.value = []
  } finally {
    supplierLoading.value = false
  }
}

async function loadAllocations() {
  allocationLoading.value = true
  try {
    const resp = await api.get('/api/finance/wubao/allocation-records', {
      params: { page: 1, size: 100 }
    })
    const body = resp.data
    if (body.code === 200) allocationOptions.value = body.data?.list || body.data?.records || []
    else allocationOptions.value = []
  } catch (e) {
    allocationOptions.value = []
  } finally {
    allocationLoading.value = false
  }
}

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

function addItem() {
  form.items.push({ materialId: null, materialName: '', quantity: 1, unitPrice: 0 })
}

function removeItem(index) {
  form.items.splice(index, 1)
}

function onMaterialChange(row, val) {
  const m = materialOptions.value.find((x) => x.id === val)
  row.materialName = m ? m.name : ''
}

function resetForm() {
  form.supplyCategory = 'SOCIAL'
  form.allocationId = null
  form.supplierId = null
  form.items = []
  form.remark = ''
}

function openCreate() {
  resetForm()
  addItem()
  createDialogVisible.value = true
  searchMaterials('')
  loadSupplierOptions()
  loadAllocations()
}

async function submitCreate() {
  if (!formRef.value) return
  await formRef.value.validate()
  if (!form.items.length) {
    ElMessage.warning('请至少添加一条采购明细')
    return
  }
  for (const it of form.items) {
    if (!it.materialId) {
      ElMessage.warning('请选择所有明细的物资')
      return
    }
  }
  saving.value = true
  try {
    const payload = {
      itemsJson: JSON.stringify(
        form.items.map((i) => ({
          materialId: i.materialId,
          materialName: i.materialName,
          quantity: i.quantity,
          unitPrice: i.unitPrice,
          subtotal: Number(i.quantity) * Number(i.unitPrice)
        }))
      ),
      totalAmount: totalAmount.value,
      supplyCategory: form.supplyCategory,
      allocationId: form.supplyCategory === 'CENTRALIZED' ? form.allocationId : null,
      supplierId: form.supplierId,
      remark: form.remark || null
    }
    const resp = await api.post('/api/purchase-request', payload)
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '创建失败')
    ElMessage.success('提交成功')
    createDialogVisible.value = false
    await fetchList()
  } catch (e) {
    ElMessage.error(e.message || '创建失败')
  } finally {
    saving.value = false
  }
}

function openApprove(row) {
  currentRow.value = row
  approveMode.value = 'approve'
  approveRemark.value = ''
  approveDialogVisible.value = true
}

function openReject(row) {
  currentRow.value = row
  approveMode.value = 'reject'
  approveRemark.value = ''
  approveDialogVisible.value = true
}

async function submitApprove() {
  if (!currentRow.value) return
  if (approveMode.value === 'reject' && !approveRemark.value.trim()) {
    ElMessage.warning('请输入驳回原因')
    return
  }
  saving.value = true
  try {
    const url =
      approveMode.value === 'approve'
        ? `/api/purchase-request/${currentRow.value.id}/approve`
        : `/api/purchase-request/${currentRow.value.id}/reject`
    const resp = await api.post(url, { remark: approveRemark.value })
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '操作失败')
    ElMessage.success(approveMode.value === 'approve' ? '已通过' : '已驳回')
    approveDialogVisible.value = false
    await fetchList()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    saving.value = false
  }
}

function openDetail(row) {
  currentRow.value = row
  detailItems.value = []
  try {
    if (row.itemsJson) detailItems.value = JSON.parse(row.itemsJson)
  } catch (e) {
    detailItems.value = []
  }
  detailDialogVisible.value = true
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

.items-wrap {
  width: 100%;
}

.total-row {
  margin-top: 10px;
  text-align: right;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary, #1f2937);
}
</style>
