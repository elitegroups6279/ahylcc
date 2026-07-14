<template>
  <div class="med-page">
    <!-- 统计卡片 -->
    <div class="med-stat-row med-stat-row-4">
      <div class="med-stat-card primary">
        <div class="med-stat-icon"><el-icon><Document /></el-icon></div>
        <div class="med-stat-body">
          <div class="med-stat-label">生效计划数</div>
          <div class="med-stat-value">{{ stats.activeCount }}</div>
        </div>
      </div>
      <div class="med-stat-card blue">
        <div class="med-stat-icon"><el-icon><User /></el-icon></div>
        <div class="med-stat-body">
          <div class="med-stat-label">用药老人数</div>
          <div class="med-stat-value">{{ stats.elderlyCount }}</div>
        </div>
      </div>
      <div class="med-stat-card warning">
        <div class="med-stat-icon"><el-icon><Warning /></el-icon></div>
        <div class="med-stat-body">
          <div class="med-stat-label">7天内耗尽预警</div>
          <div class="med-stat-value">{{ stats.depletionWarning }}</div>
        </div>
      </div>
      <div class="med-stat-card success">
        <div class="med-stat-icon"><el-icon><Clock /></el-icon></div>
        <div class="med-stat-body">
          <div class="med-stat-label">今日待执行</div>
          <div class="med-stat-value">{{ stats.todayPending }}</div>
        </div>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="med-filter-bar">
      <div class="med-filter-left">
        <div class="med-switcher">
          <button
            class="med-switcher-btn"
            :class="{ active: filterStatus === '' }"
            @click="switchStatus('')"
          >全部</button>
          <button
            class="med-switcher-btn"
            :class="{ active: filterStatus === 'ACTIVE' }"
            @click="switchStatus('ACTIVE')"
          >生效中</button>
          <button
            class="med-switcher-btn"
            :class="{ active: filterStatus === 'PAUSED' }"
            @click="switchStatus('PAUSED')"
          >已暂停</button>
        </div>
        <el-input
          v-model="filterElderly"
          placeholder="搜索老人姓名"
          clearable
          style="width: 160px"
          @keyup.enter="reload"
          @clear="reload"
        />
        <el-select
          v-model="filterDrug"
          filterable
          clearable
          placeholder="药品筛选"
          style="width: 160px"
          @change="reload"
        >
          <el-option v-for="d in drugOptions" :key="d.id" :label="d.name" :value="d.id" />
        </el-select>
      </div>
      <div class="med-filter-right">
        <el-button @click="fetchList"><el-icon><Refresh /></el-icon> 刷新</el-button>
        <el-button type="primary" @click="openCreate"><el-icon><Plus /></el-icon> 新增用药计划</el-button>
      </div>
    </div>

    <!-- 表格 -->
    <div class="med-table-card">
      <div class="med-table-card-header">
        <div class="med-table-card-header-left">
          <span class="med-table-card-title">用药计划列表</span>
          <span class="med-table-card-count">共 {{ total }} 条</span>
        </div>
      </div>
      <el-table :data="list" v-loading="loading" row-key="id" class="med-table">
        <el-table-column prop="elderlyName" label="老人" width="120" />
        <el-table-column label="药品" min-width="200">
          <template #default="{ row }">
            <div class="drug-tag-list">
              <el-tag
                v-for="(item, idx) in (row.items || [])"
                :key="idx"
                size="small"
                type="info"
                effect="plain"
                class="drug-tag"
              >{{ item.drugName }}<span v-if="item.dosagePerTime" class="drug-tag-dose"> {{ item.dosagePerTime }}{{ item.dosageUnit || '' }}</span></el-tag>
              <span v-if="!row.items || row.items.length === 0" style="color: #9CA3AF">-</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="频次" width="110">
          <template #default="{ row }">{{ frequencyLabel(row.frequencyType, row.frequencyParam) }}</template>
        </el-table-column>
        <el-table-column label="服药时段" width="200">
          <template #default="{ row }">
            <span
              v-for="slot in parseTimeSlots(row.timeSlots)"
              :key="slot"
              class="med-pill"
              :class="slotPillClass(slot)"
            >{{ timeSlotLabel(slot) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="startDate" label="开始日期" width="120" />
        <el-table-column label="预计耗尽" width="160">
          <template #default="{ row }">
            <template v-if="earliestDepletion(row.items)">
              {{ earliestDepletion(row.items) }}
              <span
                v-if="remainingDays(earliestDepletion(row.items)) <= 7 && remainingDays(earliestDepletion(row.items)) > 0 && row.status === 'ACTIVE'"
                class="med-pill orange"
                style="margin-left: 4px"
              >剩余{{ remainingDays(earliestDepletion(row.items)) }}天</span>
            </template>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <span class="med-pill" :class="statusPillClass(row.status)">{{ statusLabel(row.status) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 'ACTIVE'" link type="warning" @click="pausePlan(row)">暂停</el-button>
            <el-button v-if="row.status === 'PAUSED'" link type="success" @click="resumePlan(row)">恢复</el-button>
            <el-button v-if="row.status !== 'STOPPED' && row.status !== 'COMPLETED'" link type="danger" @click="stopPlan(row)">停止</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="med-pager">
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
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增用药计划' : '编辑用药计划'"
      width="900px"
      custom-class="med-dialog"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <!-- 基本信息 -->
        <div class="med-form-section">
          <div class="med-section-title">基本信息</div>
          <el-form-item label="老人" prop="elderlyId">
            <el-select
              v-model="form.elderlyId"
              filterable
              clearable
              placeholder="搜索并选择老人"
              style="width: 100%"
              :loading="elderlyLoading"
              @change="onElderlyChange"
            >
              <el-option v-for="e in elderlyOptions" :key="e.id" :label="`${e.name}${e.unique_no || e.uniqueNo ? '（' + (e.unique_no || e.uniqueNo) + '）' : ''}`" :value="e.id" />
            </el-select>
          </el-form-item>
        </div>

        <!-- 药品明细 -->
        <div class="med-form-section">
          <div class="med-section-title">
            <span>药品明细</span>
            <el-button type="primary" link size="small" @click="addItem" style="margin-left: 12px">
              <el-icon><Plus /></el-icon> 添加药品
            </el-button>
          </div>
          <el-table :data="form.items" border class="med-inner-table" style="width: 100%">
            <el-table-column label="序号" width="55" align="center">
              <template #default="{ $index }">{{ $index + 1 }}</template>
            </el-table-column>
            <el-table-column label="药品名称" min-width="180">
              <template #default="{ row, $index }">
                <el-select
                  v-model="row.drugId"
                  filterable
                  clearable
                  placeholder="搜索选择药品"
                  style="width: 100%"
                  :loading="drugLoading"
                  @change="(val) => onItemDrugChange(val, $index)"
                >
                  <el-option
                    v-for="d in drugSelectOptions"
                    :key="d.id"
                    :label="`${d.name}${d.specification ? ' (' + d.specification + ')' : ''}`"
                    :value="d.id"
                  />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="用量说明" width="130">
              <template #default="{ row }">
                <el-input v-model="row.dosage" placeholder="如: 饭后" />
              </template>
            </el-table-column>
            <el-table-column label="单位" width="90">
              <template #default="{ row }">
                <el-select v-model="row.dosageUnit" style="width: 100%">
                  <el-option label="片" value="片" />
                  <el-option label="粒" value="粒" />
                  <el-option label="支" value="支" />
                  <el-option label="瓶" value="瓶" />
                  <el-option label="袋" value="袋" />
                  <el-option label="ml" value="ml" />
                  <el-option label="mg" value="mg" />
                  <el-option label="g" value="g" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="每次用量" width="110">
              <template #default="{ row }">
                <el-input-number
                  v-model="row.dosagePerTime"
                  :min="0"
                  :precision="2"
                  :controls="false"
                  style="width: 100%"
                />
              </template>
            </el-table-column>
            <el-table-column label="发药总量" width="110">
              <template #default="{ row }">
                <el-input-number
                  v-model="row.totalQuantity"
                  :min="0"
                  :precision="1"
                  :controls="false"
                  style="width: 100%"
                />
              </template>
            </el-table-column>
            <el-table-column label="预计耗尽" width="120">
              <template #default="{ row, $index }">
                <span v-if="calcItemDepletionDate($index)" class="depletion-text">
                  {{ calcItemDepletionDate($index) }}
                </span>
                <span v-else style="color: #9CA3AF; font-size: 12px">需填完</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="70" align="center">
              <template #default="{ $index }">
                <el-button
                  link
                  type="danger"
                  :icon="Delete"
                  @click="removeItem($index)"
                  :disabled="form.items.length <= 1"
                />
              </template>
            </el-table-column>
          </el-table>
          <div v-if="form.items.length === 0" class="empty-items-hint">
            请点击"添加药品"按钮添加至少一条药品信息
          </div>
        </div>

        <!-- 服药安排 -->
        <div class="med-form-section">
          <div class="med-section-title">服药安排</div>
          <el-form-item label="服药时段" prop="timeSlots">
            <el-checkbox-group v-model="form.timeSlots">
              <el-checkbox value="MORNING">早晨</el-checkbox>
              <el-checkbox value="AFTERNOON">中午</el-checkbox>
              <el-checkbox value="EVENING">晚上</el-checkbox>
              <el-checkbox value="BEDTIME">睡前</el-checkbox>
            </el-checkbox-group>
          </el-form-item>
          <el-form-item label="每日次数">
            <el-input-number
              :model-value="form.timeSlots.length"
              disabled
              :min="0"
              controls-position="right"
              style="width: 120px"
            />
            <span style="margin-left: 8px; color: #6B7280">次/天（自动计算）</span>
          </el-form-item>
          <el-form-item label="开始日期" prop="startDate">
            <el-date-picker
              v-model="form.startDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择开始日期"
              style="width: 220px"
            />
          </el-form-item>
        </div>

        <!-- 可选信息 -->
        <div class="med-form-section">
          <div class="med-section-title">可选信息</div>
          <el-form-item label="服药说明">
            <el-input v-model="form.instructions" type="textarea" :rows="3" placeholder="服药注意事项" />
          </el-form-item>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="处方医生">
                <el-input v-model="form.prescriberName" placeholder="处方医生姓名" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="备注">
                <el-input v-model="form.remark" placeholder="备注信息" />
              </el-form-item>
            </el-col>
          </el-row>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存并生成用药记录</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import { api } from '../../api/client'

// ---------- 列表状态 ----------
const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)

const filterElderly = ref('')
const filterDrug = ref('')
const filterStatus = ref('')

// ---------- 统计卡片 ----------
const stats = computed(() => {
  const activeItems = list.value.filter(r => r.status === 'ACTIVE')
  const activeCount = activeItems.length
  const elderlySet = new Set(activeItems.map(r => r.elderlyId))
  const elderlyCount = elderlySet.size
  const depletionWarning = activeItems.filter(r => {
    const dep = earliestDepletion(r.items)
    return dep && remainingDays(dep) <= 7 && remainingDays(dep) > 0
  }).length
  const todayPending = activeItems.filter(r => {
    if (!r.startDate) return false
    return new Date(r.startDate + 'T00:00:00') <= new Date()
  }).reduce((sum, r) => {
    return sum + parseTimeSlots(r.timeSlots).length * (r.items ? r.items.length : 1)
  }, 0)
  return { activeCount, elderlyCount, depletionWarning, todayPending }
})

// ---------- 下拉选项 ----------
const elderlyLoading = ref(false)
const elderlyOptions = ref([])
const drugLoading = ref(false)
const drugSelectOptions = ref([])
const drugOptions = ref([])

// ---------- 对话框 ----------
const dialogVisible = ref(false)
const dialogMode = ref('create')
const formRef = ref()

function makeEmptyItem() {
  return {
    id: null,
    drugId: null,
    drugName: '',
    dosage: '',
    dosageUnit: '片',
    dosagePerTime: null,
    totalQuantity: null,
    depletionDate: null
  }
}

const defaultForm = {
  id: null,
  elderlyId: null,
  elderlyName: '',
  items: [makeEmptyItem()],
  timeSlots: [],
  startDate: '',
  instructions: '',
  prescriberName: '',
  remark: ''
}

const form = reactive({ ...defaultForm, items: [makeEmptyItem()] })

const rules = {
  elderlyId: [{ required: true, message: '请选择老人', trigger: 'change' }],
  timeSlots: [{ required: true, type: 'array', min: 1, message: '请至少选择一个服药时段', trigger: 'change' }],
  startDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
  items: [{
    validator: (rule, value, callback) => {
      if (!value || value.length === 0) {
        callback(new Error('请至少添加一条药品信息'))
        return
      }
      for (let i = 0; i < value.length; i++) {
        const item = value[i]
        if (!item.drugId) {
          callback(new Error(`第${i + 1}条药品未选择药品名称`))
          return
        }
        if (!item.dosagePerTime || item.dosagePerTime <= 0) {
          callback(new Error(`第${i + 1}条药品未填写每次用量`))
          return
        }
        if (!item.totalQuantity || item.totalQuantity <= 0) {
          callback(new Error(`第${i + 1}条药品未填写发药总量`))
          return
        }
      }
      callback()
    },
    trigger: 'change'
  }]
}

// ---------- 药品行操作 ----------
function addItem() {
  form.items.push(makeEmptyItem())
}

function removeItem(index) {
  form.items.splice(index, 1)
}

function onItemDrugChange(val, index) {
  const found = drugSelectOptions.value.find(d => d.id === val)
  if (found) {
    form.items[index].drugName = found.name
  } else {
    form.items[index].drugName = ''
  }
}

function calcItemDepletionDate(index) {
  const item = form.items[index]
  if (!item) return null
  const qty = Number(item.totalQuantity)
  const dose = Number(item.dosagePerTime)
  const times = form.timeSlots.length
  if (!qty || !dose || !times) return null
  const daily = dose * times
  if (daily <= 0) return null
  const days = Math.floor(qty / daily)
  if (!form.startDate || days <= 0) return null
  const d = new Date(form.startDate + 'T00:00:00')
  d.setDate(d.getDate() + days - 1)
  return formatDate(d)
}

// ---------- 工具函数 ----------
function formatDate(d) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function earliestDepletion(items) {
  if (!items || items.length === 0) return null
  const dates = items
    .map(i => i.depletionDate)
    .filter(Boolean)
    .sort()
  return dates[0] || null
}

// ---------- 映射工具 ----------
const frequencyMap = {
  MULTI_DAILY: '每日',
  N_DAYS: '每N日',
  PRN: '必要时',
  ONCE: '一次性'
}

const timeSlotMap = {
  MORNING: '早晨',
  AFTERNOON: '中午',
  EVENING: '晚上',
  BEDTIME: '睡前'
}

const statusMap = {
  ACTIVE: { label: '生效中', pill: 'green' },
  PAUSED: { label: '已暂停', pill: 'orange' },
  COMPLETED: { label: '已完成', pill: 'gray' },
  STOPPED: { label: '已停止', pill: 'red' }
}

const slotPillMap = {
  MORNING: 'orange',
  AFTERNOON: 'blue',
  EVENING: '',
  BEDTIME: 'teal'
}

function frequencyLabel(type, param) {
  if (!type) return '-'
  if (type === 'N_DAYS' && param) return `每${param}日`
  return frequencyMap[type] || type
}

function timeSlotLabel(slot) {
  return timeSlotMap[slot] || slot
}

function slotPillClass(slot) {
  const cls = slotPillMap[slot]
  return cls ? cls : 'gray'
}

function parseTimeSlots(slots) {
  if (!slots) return []
  if (Array.isArray(slots)) return slots
  try { return JSON.parse(slots) } catch { return [] }
}

function statusPillClass(status) {
  return statusMap[status]?.pill || 'gray'
}

function statusLabel(status) {
  return statusMap[status]?.label || status || '-'
}

function remainingDays(depletionDate) {
  if (!depletionDate) return Infinity
  const now = new Date()
  now.setHours(0, 0, 0, 0)
  const dep = new Date(depletionDate + 'T00:00:00')
  return Math.ceil((dep - now) / (1000 * 60 * 60 * 24))
}

function drugNamesText(row) {
  if (!row.items || row.items.length === 0) return ''
  return row.items.map(i => i.drugName).filter(Boolean).join('、')
}

// ---------- 状态切换 ----------
function switchStatus(val) {
  filterStatus.value = val
  reload()
}

// ---------- 数据加载 ----------
async function fetchList() {
  loading.value = true
  try {
    const resp = await api.get('/api/medication/plans', {
      params: {
        page: page.value,
        pageSize: pageSize.value,
        elderlyName: filterElderly.value || undefined,
        drugId: filterDrug.value || undefined,
        status: filterStatus.value || undefined
      }
    })
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '加载失败')
    list.value = body.data?.list || body.data?.records || []
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

async function loadElderlyOptions() {
  elderlyLoading.value = true
  try {
    const resp = await api.get('/api/elderly/options')
    const body = resp.data
    if (body.code === 200) elderlyOptions.value = body.data || []
    else elderlyOptions.value = []
  } catch {
    elderlyOptions.value = []
  } finally {
    elderlyLoading.value = false
  }
}

async function loadDrugOptions() {
  drugLoading.value = true
  try {
    const resp = await api.get('/api/pharmacy/drugs', { params: { page: 1, pageSize: 100 } })
    const body = resp.data
    if (body.code === 200) {
      const drugs = body.data?.list || body.data?.records || []
      drugSelectOptions.value = drugs
      if (!drugOptions.value.length) drugOptions.value = drugs
    } else {
      drugSelectOptions.value = []
    }
  } catch {
    drugSelectOptions.value = []
  } finally {
    drugLoading.value = false
  }
}

async function loadFilterDrugs() {
  try {
    const resp = await api.get('/api/pharmacy/drugs', { params: { page: 1, pageSize: 100 } })
    const body = resp.data
    if (body.code === 200) drugOptions.value = body.data?.list || body.data?.records || []
  } catch { /* ignore */ }
}

// ---------- 表单操作 ----------
function resetForm() {
  form.id = null
  form.elderlyId = null
  form.elderlyName = ''
  form.items = [makeEmptyItem()]
  form.timeSlots = []
  form.startDate = ''
  form.instructions = ''
  form.prescriberName = ''
  form.remark = ''
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  dialogVisible.value = true
  loadElderlyOptions()
  loadDrugOptions()
}

function openEdit(row) {
  dialogMode.value = 'edit'
  resetForm()
  form.id = row.id
  form.elderlyId = row.elderlyId
  form.elderlyName = row.elderlyName || ''
  // Map items from row
  if (row.items && row.items.length > 0) {
    form.items = row.items.map(i => ({
      id: i.id || null,
      drugId: i.drugId || null,
      drugName: i.drugName || '',
      dosage: i.dosage || '',
      dosageUnit: i.dosageUnit || '片',
      dosagePerTime: i.dosagePerTime || null,
      totalQuantity: i.totalQuantity || null,
      depletionDate: i.depletionDate || null
    }))
  } else {
    form.items = [makeEmptyItem()]
  }
  form.timeSlots = parseTimeSlots(row.timeSlots)
  form.startDate = row.startDate || ''
  form.instructions = row.instructions || ''
  form.prescriberName = row.prescriberName || ''
  form.remark = row.remark || ''
  dialogVisible.value = true
  loadElderlyOptions()
  loadDrugOptions()
}

function onElderlyChange(val) {
  const found = elderlyOptions.value.find(e => e.id === val)
  form.elderlyName = found ? found.name : ''
}

async function submit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    // Build items payload with computed depletion dates
    const itemsPayload = form.items.map((item, index) => ({
      id: item.id || undefined,
      drugId: item.drugId,
      drugName: item.drugName,
      dosage: item.dosage || null,
      dosageUnit: item.dosageUnit,
      dosagePerTime: item.dosagePerTime,
      totalQuantity: item.totalQuantity,
      depletionDate: calcItemDepletionDate(index) || null
    }))

    const payload = {
      elderlyId: form.elderlyId,
      elderlyName: form.elderlyName,
      items: itemsPayload,
      timeSlots: form.timeSlots,
      startDate: form.startDate,
      instructions: form.instructions || null,
      prescriberName: form.prescriberName || null,
      remark: form.remark || null
    }

    if (dialogMode.value === 'create') {
      const resp = await api.post('/api/medication/plans', payload)
      const body = resp.data
      if (body.code !== 200) throw new Error(body.msg || '创建失败')
      ElMessage.success('创建成功')
    } else {
      const resp = await api.put(`/api/medication/plans/${form.id}`, payload)
      const body = resp.data
      if (body.code !== 200) throw new Error(body.msg || '更新失败')
      ElMessage.success('更新成功')
    }
    dialogVisible.value = false
    reload()
  } catch (e) {
    if (e.message) ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// ---------- 状态操作 ----------
async function pausePlan(row) {
  try {
    await ElMessageBox.confirm(`确定暂停【${row.elderlyName}】的用药计划吗？（含${row.items?.length || 0}种药品）`, '暂停用药', {
      confirmButtonText: '确定暂停',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const resp = await api.put(`/api/medication/plans/${row.id}/pause`)
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '操作失败')
    ElMessage.success('已暂停')
    await fetchList()
  } catch (e) {
    if (e === 'cancel') return
    ElMessage.error(e.message || '操作失败')
  }
}

async function resumePlan(row) {
  try {
    const resp = await api.put(`/api/medication/plans/${row.id}/resume`)
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '操作失败')
    ElMessage.success('已恢复')
    await fetchList()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function stopPlan(row) {
  try {
    await ElMessageBox.confirm(
      `确定停止【${row.elderlyName}】的用药计划吗？停止后不可恢复。（含${row.items?.length || 0}种药品）`,
      '停止用药',
      { confirmButtonText: '确定停止', cancelButtonText: '取消', type: 'error' }
    )
    const resp = await api.put(`/api/medication/plans/${row.id}/stop`)
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '操作失败')
    ElMessage.success('已停止')
    await fetchList()
  } catch (e) {
    if (e === 'cancel') return
    ElMessage.error(e.message || '操作失败')
  }
}

async function remove(row) {
  try {
    // 先获取删除统计信息
    let deleteInfo = null
    try {
      const infoResp = await api.get(`/api/medication/plans/${row.id}/delete-info`)
      if (infoResp.data.code === 200) {
        deleteInfo = infoResp.data.data
      }
    } catch { /* ignore */ }

    let confirmMsg = `确定删除【${row.elderlyName}】的用药计划吗？此操作不可恢复。`
    if (deleteInfo) {
      const parts = [`含${deleteInfo.itemCount || 0}种药品`]
      if (deleteInfo.totalRecords > 0) {
        parts.push(`${deleteInfo.totalRecords}条用药记录`)
        if (deleteInfo.doneRecords > 0) {
          parts.push(`其中${deleteInfo.doneRecords}条已确认记录也将被删除`)
        }
      }
      confirmMsg = `确定删除【${row.elderlyName}】的用药计划吗？\n\n${parts.join('，')}。\n此操作不可恢复。`
    }

    await ElMessageBox.confirm(confirmMsg, '删除确认', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const resp = await api.delete(`/api/medication/plans/${row.id}`)
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '删除失败')
    ElMessage.success('删除成功')
    if (list.value.length <= 1 && page.value > 1) {
      page.value--
    }
    await fetchList()
  } catch (e) {
    if (e === 'cancel') return
    ElMessage.error(e.message || '删除失败')
  }
}

// ---------- 初始化 ----------
onMounted(() => {
  loadFilterDrugs()
  fetchList()
})
</script>

<style scoped>
.med-inner-table {
  font-size: 13px;
}
.med-inner-table :deep(.el-input-number) {
  width: 100%;
}
.med-inner-table :deep(.el-input__inner) {
  text-align: center;
}
.depletion-text {
  font-weight: 600;
  color: #2B7A78;
  font-size: 12px;
}
.drug-tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.drug-tag {
  max-width: 180px;
}
.drug-tag-dose {
  color: #6B7280;
  font-size: 11px;
}
.empty-items-hint {
  text-align: center;
  padding: 16px;
  color: #9CA3AF;
  font-size: 13px;
  border: 1px dashed #E5E7EB;
  border-radius: 6px;
  margin-top: 8px;
}
</style>
