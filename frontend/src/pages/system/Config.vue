<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="header">
          <span>系统配置</span>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <!-- 基础配置 Tab -->
        <el-tab-pane label="基础配置" name="config">
          <div class="tab-header">
            <el-button @click="fetchConfigList">刷新</el-button>
            <el-button type="primary" :loading="saving" @click="saveAll">保存</el-button>
          </div>
          <el-table :data="configList" v-loading="configLoading" row-key="id">
            <el-table-column prop="configKey" label="键" width="220" />
            <el-table-column label="值" min-width="260">
              <template #default="{ row }">
                <el-input v-model="row.configValue" />
              </template>
            </el-table-column>
            <el-table-column prop="configType" label="类型" width="120" />
            <el-table-column prop="description" label="描述" min-width="240" />
          </el-table>
        </el-tab-pane>

        <!-- 补贴政策 Tab -->
        <el-tab-pane label="补贴政策" name="subsidy">
          <div class="tab-header">
            <el-select v-model="subsidyFilter.category" placeholder="筛选类别" clearable style="width:160px;margin-right:12px" @change="fetchSubsidyList">
              <el-option label="社会化" value="SOCIAL" />
              <el-option label="五保" value="WU_BAO" />
              <el-option label="低保" value="LOW_BAO" />
              <el-option label="全部" value="ALL" />
            </el-select>
            <el-button @click="fetchSubsidyList">刷新</el-button>
            <el-button type="primary" @click="openSubsidyDialog(null)">新增策略</el-button>
          </div>
          <el-table :data="subsidyList" v-loading="subsidyLoading" row-key="id" stripe>
            <el-table-column prop="policyName" label="策略名称" min-width="140" />
            <el-table-column prop="policyCode" label="编码" width="150" />
            <el-table-column label="适用类别" width="100">
              <template #default="{ row }">{{ categoryMap[row.category] || row.category }}</template>
            </el-table-column>
            <el-table-column label="失能等级" width="100">
              <template #default="{ row }">{{ disabilityMap[row.disabilityLevel] || '不限' }}</template>
            </el-table-column>
            <el-table-column label="计算方式" width="110">
              <template #default="{ row }">{{ calcTypeMap[row.calcType] || row.calcType }}</template>
            </el-table-column>
            <el-table-column prop="amount" label="金额" width="100" align="right">
              <template #default="{ row }">{{ row.amount != null ? row.amount.toFixed(2) : '-' }}</template>
            </el-table-column>
            <el-table-column label="满额阈值" width="100" align="right">
              <template #default="{ row }">{{ row.thresholdAmount != null ? row.thresholdAmount.toFixed(2) : '-' }}</template>
            </el-table-column>
            <el-table-column label="抵扣金额" width="100" align="right">
              <template #default="{ row }">{{ row.deductAmount != null ? row.deductAmount.toFixed(2) : '-' }}</template>
            </el-table-column>
            <el-table-column label="拨付对象" width="100">
              <template #default="{ row }">{{ payTargetMap[row.payTarget] || row.payTarget }}</template>
            </el-table-column>
            <el-table-column prop="effectiveDate" label="生效日期" width="120" />
            <el-table-column label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-switch :model-value="row.enabled === 1" @change="toggleSubsidy(row)" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openSubsidyDialog(row)">编辑</el-button>
                <el-popconfirm title="确定删除该策略?" @confirm="deleteSubsidy(row.id)">
                  <template #reference>
                    <el-button link type="danger" size="small">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-wrap" v-if="subsidyTotal > 0">
            <el-pagination
              v-model:current-page="subsidyPage"
              v-model:page-size="subsidyPageSize"
              :total="subsidyTotal"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next"
              @current-change="fetchSubsidyList"
              @size-change="fetchSubsidyList"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 补贴策略编辑对话框 -->
    <el-dialog v-model="subsidyDialogVisible" :title="subsidyDialogTitle" width="600px" destroy-on-close>
      <el-form :model="subsidyForm" label-width="100px">
        <el-form-item label="策略编码" required>
          <el-input v-model="subsidyForm.policyCode" placeholder="如 WU_BAO_LIVING" />
        </el-form-item>
        <el-form-item label="策略名称" required>
          <el-input v-model="subsidyForm.policyName" placeholder="如 五保生活费" />
        </el-form-item>
        <el-form-item label="适用类别" required>
          <el-select v-model="subsidyForm.category" placeholder="选择类别" style="width:100%">
            <el-option label="社会化" value="SOCIAL" />
            <el-option label="五保" value="WU_BAO" />
            <el-option label="低保" value="LOW_BAO" />
            <el-option label="全部" value="ALL" />
          </el-select>
        </el-form-item>
        <el-form-item label="失能等级">
          <el-select v-model="subsidyForm.disabilityLevel" placeholder="不限" clearable style="width:100%">
            <el-option label="中度失能" value="MODERATE" />
            <el-option label="重度失能" value="SEVERE" />
          </el-select>
        </el-form-item>
        <el-form-item label="计算方式" required>
          <el-select v-model="subsidyForm.calcType" placeholder="选择计算方式" style="width:100%">
            <el-option label="固定月额" value="FIXED_MONTHLY" />
            <el-option label="日补贴" value="DAILY_RATE" />
            <el-option label="满额抵扣" value="THRESHOLD_DEDUCT" />
          </el-select>
        </el-form-item>
        <el-form-item label="金额" required>
          <el-input-number v-model="subsidyForm.amount" :precision="2" :min="0" style="width:100%" />
        </el-form-item>
        <el-form-item label="满额阈值" v-if="subsidyForm.calcType === 'THRESHOLD_DEDUCT'">
          <el-input-number v-model="subsidyForm.thresholdAmount" :precision="2" :min="0" style="width:100%" />
        </el-form-item>
        <el-form-item label="抵扣金额" v-if="subsidyForm.calcType === 'THRESHOLD_DEDUCT'">
          <el-input-number v-model="subsidyForm.deductAmount" :precision="2" :min="0" style="width:100%" />
        </el-form-item>
        <el-form-item label="拨付对象">
          <el-select v-model="subsidyForm.payTarget" style="width:100%">
            <el-option label="机构" value="ORG" />
            <el-option label="个人账户" value="PERSONAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="最低入住天数">
          <el-input-number v-model="subsidyForm.minStayDays" :min="0" style="width:100%" />
        </el-form-item>
        <el-form-item label="生效日期" required>
          <el-date-picker v-model="subsidyForm.effectiveDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="失效日期">
          <el-date-picker v-model="subsidyForm.expireDate" type="date" value-format="YYYY-MM-DD" placeholder="留空表示长期有效" style="width:100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="subsidyForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="subsidyDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="subsidySaving" @click="saveSubsidy">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../../api/client'

// ============ 基础配置 ============
const activeTab = ref('config')
const configLoading = ref(false)
const saving = ref(false)
const configList = ref([])

async function fetchConfigList() {
  configLoading.value = true
  try {
    const resp = await api.get('/api/system/config')
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '加载失败')
    configList.value = body.data || []
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    configLoading.value = false
  }
}

async function saveAll() {
  saving.value = true
  try {
    const payload = (configList.value || []).map((c) => ({
      configKey: c.configKey,
      configValue: c.configValue
    }))
    const resp = await api.put('/api/system/config', payload)
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '保存失败')
    ElMessage.success('保存成功')
    await fetchConfigList()
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// ============ 补贴政策 ============
const categoryMap = { SOCIAL: '社会化', WU_BAO: '五保', LOW_BAO: '低保', ALL: '全部' }
const disabilityMap = { MODERATE: '中度', SEVERE: '重度' }
const calcTypeMap = { FIXED_MONTHLY: '固定月额', DAILY_RATE: '日补贴', THRESHOLD_DEDUCT: '满额抵扣' }
const payTargetMap = { ORG: '机构', PERSONAL: '个人账户' }

const subsidyLoading = ref(false)
const subsidyList = ref([])
const subsidyTotal = ref(0)
const subsidyPage = ref(1)
const subsidyPageSize = ref(10)
const subsidyFilter = ref({ category: '' })

async function fetchSubsidyList() {
  subsidyLoading.value = true
  try {
    const params = { page: subsidyPage.value, pageSize: subsidyPageSize.value }
    if (subsidyFilter.value.category) {
      params.category = subsidyFilter.value.category
    }
    const resp = await api.get('/api/subsidy-policies', { params })
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '加载失败')
    subsidyList.value = body.data?.list || []
    subsidyTotal.value = body.data?.total || 0
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    subsidyLoading.value = false
  }
}

const subsidyDialogVisible = ref(false)
const subsidyDialogTitle = ref('新增策略')
const subsidySaving = ref(false)
const editingSubsidyId = ref(null)
const subsidyForm = ref({
  policyCode: '',
  policyName: '',
  category: '',
  disabilityLevel: null,
  calcType: 'FIXED_MONTHLY',
  amount: 0,
  thresholdAmount: null,
  deductAmount: null,
  payTarget: 'ORG',
  minStayDays: null,
  effectiveDate: '',
  expireDate: null,
  remark: ''
})

function openSubsidyDialog(row) {
  if (row) {
    subsidyDialogTitle.value = '编辑策略'
    editingSubsidyId.value = row.id
    subsidyForm.value = {
      policyCode: row.policyCode,
      policyName: row.policyName,
      category: row.category,
      disabilityLevel: row.disabilityLevel || null,
      calcType: row.calcType,
      amount: row.amount,
      thresholdAmount: row.thresholdAmount,
      deductAmount: row.deductAmount,
      payTarget: row.payTarget,
      minStayDays: row.minStayDays,
      effectiveDate: row.effectiveDate,
      expireDate: row.expireDate,
      remark: row.remark || ''
    }
  } else {
    subsidyDialogTitle.value = '新增策略'
    editingSubsidyId.value = null
    subsidyForm.value = {
      policyCode: '',
      policyName: '',
      category: '',
      disabilityLevel: null,
      calcType: 'FIXED_MONTHLY',
      amount: 0,
      thresholdAmount: null,
      deductAmount: null,
      payTarget: 'ORG',
      minStayDays: null,
      effectiveDate: '',
      expireDate: null,
      remark: ''
    }
  }
  subsidyDialogVisible.value = true
}

async function saveSubsidy() {
  const form = subsidyForm.value
  if (!form.policyCode || !form.policyName || !form.category || !form.calcType || !form.effectiveDate) {
    ElMessage.warning('请填写必填项')
    return
  }
  subsidySaving.value = true
  try {
    if (editingSubsidyId.value) {
      await api.put(`/api/subsidy-policies/${editingSubsidyId.value}`, form)
    } else {
      await api.post('/api/subsidy-policies', form)
    }
    ElMessage.success('保存成功')
    subsidyDialogVisible.value = false
    await fetchSubsidyList()
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || e.message || '保存失败')
  } finally {
    subsidySaving.value = false
  }
}

async function toggleSubsidy(row) {
  try {
    await api.put(`/api/subsidy-policies/${row.id}/toggle`)
    ElMessage.success('状态已切换')
    await fetchSubsidyList()
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || e.message || '操作失败')
  }
}

async function deleteSubsidy(id) {
  try {
    await api.delete(`/api/subsidy-policies/${id}`)
    ElMessage.success('删除成功')
    await fetchSubsidyList()
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || e.message || '删除失败')
  }
}

onMounted(() => {
  fetchConfigList()
  fetchSubsidyList()
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

.tab-header {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  gap: 10px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
