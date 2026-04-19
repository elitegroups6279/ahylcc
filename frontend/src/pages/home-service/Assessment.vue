<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="header">
          <span>服务评估</span>
          <div class="header-actions">
            <el-date-picker
              v-model="filterMonth"
              type="month"
              clearable
              placeholder="选择月份"
              style="width: 160px"
              value-format="YYYY-MM"
              @change="reload"
            />
            <el-select v-model="filterStatus" clearable placeholder="状态" style="width: 120px" @change="reload">
              <el-option label="草稿" value="DRAFT" />
              <el-option label="已提交" value="SUBMITTED" />
              <el-option label="已确认" value="CONFIRMED" />
            </el-select>
            <el-select v-model="filterGrade" clearable placeholder="等级" style="width: 120px" @change="reload">
              <el-option label="优秀" value="优秀" />
              <el-option label="良好" value="良好" />
              <el-option label="合格" value="合格" />
              <el-option label="不合格" value="不合格" />
            </el-select>
            <el-button @click="fetchList">查询</el-button>
            <el-button type="primary" @click="openCreate">新增评估</el-button>
          </div>
        </div>
      </template>

      <div style="overflow-x: auto">
        <el-table :data="list" v-loading="loading" row-key="id">
          <el-table-column prop="assessmentNo" label="评估编号" width="160" />
          <el-table-column prop="elderlyName" label="被评老人" width="120" />
          <el-table-column prop="assessorOrg" label="评估机构" min-width="160" />
          <el-table-column prop="assessmentDate" label="评估日期" width="120" />
          <el-table-column prop="totalScore" label="总分" width="90" align="right" />
          <el-table-column label="等级" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="gradeTagType(row.grade)" size="small">{{ row.grade || '-' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="260" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openView(row)">查看</el-button>
              <el-button v-if="row.status === 'DRAFT'" link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button v-if="row.status === 'DRAFT'" link type="danger" @click="remove(row)">删除</el-button>
              <el-button v-if="row.status === 'DRAFT'" link type="warning" @click="submitAssessment(row)">提交</el-button>
              <el-button v-if="row.status === 'SUBMITTED'" link type="success" @click="confirmAssessment(row)">确认</el-button>
              <el-button link type="primary" @click="exportPdf(row)">导出PDF</el-button>
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
          :page-sizes="[10, 20, 50]"
          @update:current-page="(p) => { page = p; fetchList() }"
          @update:page-size="(s) => { pageSize = s; page = 1; fetchList() }"
        />
      </div>
    </el-card>

    <!-- 评估表单抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      direction="rtl"
      :size="'100%'"
      :close-on-click-modal="false"
      @close="onDrawerClose"
    >
      <template #header>
        <span style="font-size: 18px; font-weight: bold">服务评估</span>
      </template>

      <div class="assessment-form" id="assessment-print-area">
        <div class="form-title">居家养老上门服务质量评估报告</div>
        <div class="form-subtitle">（依据 GB/T 43153-2023）</div>

        <!-- 头部信息区 -->
        <el-card class="section-card" shadow="never">
          <el-row :gutter="16">
            <el-col :xs="24" :sm="12" :md="8">
              <el-form-item label="评估编号">
                <el-input :model-value="form.assessmentNo" readonly placeholder="自动生成" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8">
              <el-form-item label="评估日期" required>
                <el-date-picker v-model="form.assessmentDate" type="date" value-format="YYYY-MM-DD" :disabled="isViewMode" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8">
              <el-form-item label="评估机构" required>
                <el-input v-model="form.assessorOrg" :disabled="isViewMode" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8">
              <el-form-item label="评估人员" required>
                <el-input v-model="form.assessorName" :disabled="isViewMode" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8">
              <el-form-item label="评估周期">
                <el-input v-model="form.assessmentPeriod" :disabled="isViewMode" placeholder="如：2026年第1季度" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8">
              <el-form-item label="被评老人">
                <el-select
                  v-model="form.elderlyId"
                  filterable
                  remote
                  clearable
                  :remote-method="searchElderly"
                  :loading="elderlyLoading"
                  placeholder="搜索老人"
                  :disabled="isViewMode"
                  style="width: 100%"
                  @change="onElderlyChange"
                >
                  <el-option v-for="o in elderlyOptions" :key="o.id" :label="o.name + (o.uniqueNo ? '（' + o.uniqueNo + '）' : '')" :value="o.id" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8">
              <el-form-item label="服务地址">
                <el-input v-model="form.serviceAddress" :disabled="isViewMode" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-card>

        <!-- 一、服务协议与方案评价 -->
        <el-card class="section-card" shadow="never">
          <template #header>
            <span class="card-title">一、服务协议与方案评价（满分25分）</span>
          </template>
          <el-row :gutter="16">
            <el-col :xs="24" :sm="12" :md="8">
              <el-form-item label="服务协议是否签订">
                <el-switch v-model="form.agreementSigned" :active-value="1" :inactive-value="0" :disabled="isViewMode" />
                <span class="print-only">{{ form.agreementSigned ? '是' : '否' }}</span>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8">
              <el-form-item label="协议内容是否完整">
                <el-switch v-model="form.agreementComplete" :active-value="1" :inactive-value="0" :disabled="isViewMode" />
                <span class="print-only">{{ form.agreementComplete ? '是' : '否' }}</span>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8">
              <el-form-item label="服务方案是否制定">
                <el-switch v-model="form.planFormulated" :active-value="1" :inactive-value="0" :disabled="isViewMode" />
                <span class="print-only">{{ form.planFormulated ? '是' : '否' }}</span>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8">
              <el-form-item label="方案是否符合老人需求">
                <el-switch v-model="form.planMatchesNeeds" :active-value="1" :inactive-value="0" :disabled="isViewMode" />
                <span class="print-only">{{ form.planMatchesNeeds ? '是' : '否' }}</span>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8">
              <el-form-item label="本项得分">
                <el-input-number v-model="form.agreementScore" :min="0" :max="25" :disabled="isViewMode" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-card>

        <!-- 二、服务履行情况评价 -->
        <el-card class="section-card" shadow="never">
          <template #header>
            <span class="card-title">二、服务履行情况评价（满分30分）</span>
          </template>
          <el-row :gutter="16">
            <el-col :xs="24" :sm="12" :md="8">
              <el-form-item label="是否按预约时间到达">
                <el-switch v-model="form.serviceOnTime" :active-value="1" :inactive-value="0" :disabled="isViewMode" />
                <span class="print-only">{{ form.serviceOnTime ? '是' : '否' }}</span>
                <span class="ref-tag">参照6.4.3</span>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8">
              <el-form-item label="人员着装整齐/出示证件">
                <el-switch v-model="form.staffIdentified" :active-value="1" :inactive-value="0" :disabled="isViewMode" />
                <span class="print-only">{{ form.staffIdentified ? '是' : '否' }}</span>
                <span class="ref-tag">参照6.4.2</span>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8">
              <el-form-item label="是否告知服务风险与注意事项">
                <el-switch v-model="form.riskInformed" :active-value="1" :inactive-value="0" :disabled="isViewMode" />
                <span class="print-only">{{ form.riskInformed ? '是' : '否' }}</span>
                <span class="ref-tag">参照6.5.1</span>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8">
              <el-form-item label="是否按服务方案提供服务">
                <el-switch v-model="form.servicePerPlan" :active-value="1" :inactive-value="0" :disabled="isViewMode" />
                <span class="print-only">{{ form.servicePerPlan ? '是' : '否' }}</span>
                <span class="ref-tag">参照6.5.2</span>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8">
              <el-form-item label="异常情况是否妥善处置">
                <el-switch v-model="form.emergencyHandled" :active-value="1" :inactive-value="0" :disabled="isViewMode" />
                <span class="print-only">{{ form.emergencyHandled ? '是' : '否' }}</span>
                <span class="ref-tag">参照6.5.3</span>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8">
              <el-form-item label="服务验收是否完成">
                <el-switch v-model="form.acceptanceDone" :active-value="1" :inactive-value="0" :disabled="isViewMode" />
                <span class="print-only">{{ form.acceptanceDone ? '是' : '否' }}</span>
                <span class="ref-tag">参照6.5.4</span>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8">
              <el-form-item label="本项得分">
                <el-input-number v-model="form.fulfillmentScore" :min="0" :max="30" :disabled="isViewMode" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-card>

        <!-- 三、服务记录评价 -->
        <el-card class="section-card" shadow="never">
          <template #header>
            <span class="card-title">三、服务记录评价（满分20分）</span>
          </template>
          <el-row :gutter="16">
            <el-col :xs="24" :sm="12" :md="8">
              <el-form-item label="服务记录是否完整">
                <el-switch v-model="form.recordComplete" :active-value="1" :inactive-value="0" :disabled="isViewMode" />
                <span class="print-only">{{ form.recordComplete ? '是' : '否' }}</span>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8">
              <el-form-item label="记录是否及时填写">
                <el-switch v-model="form.recordTimely" :active-value="1" :inactive-value="0" :disabled="isViewMode" />
                <span class="print-only">{{ form.recordTimely ? '是' : '否' }}</span>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8">
              <el-form-item label="记录内容是否准确">
                <el-switch v-model="form.recordAccurate" :active-value="1" :inactive-value="0" :disabled="isViewMode" />
                <span class="print-only">{{ form.recordAccurate ? '是' : '否' }}</span>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8">
              <el-form-item label="本项得分">
                <el-input-number v-model="form.recordScore" :min="0" :max="20" :disabled="isViewMode" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-card>

        <!-- 四、服务满意度 -->
        <el-card class="section-card" shadow="never">
          <template #header>
            <span class="card-title">四、服务满意度（满分25分）</span>
          </template>
          <el-row :gutter="16">
            <el-col :xs="24" :sm="12" :md="8">
              <el-form-item label="满意度调查方式">
                <el-select v-model="form.satisfactionMethod" :disabled="isViewMode" style="width: 100%">
                  <el-option label="问卷" value="问卷" />
                  <el-option label="访谈" value="访谈" />
                  <el-option label="电话回访" value="电话回访" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8">
              <el-form-item label="老人/家属满意度得分">
                <el-input-number v-model="form.elderlySatisfaction" :min="0" :max="25" :disabled="isViewMode" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-card>

        <!-- 综合评分区 -->
        <div class="total-score-area">
          <div class="score-item">
            <span class="score-label">综合评分:</span>
            <span class="score-value">{{ totalScore }} 分</span>
          </div>
          <div class="score-item">
            <span class="score-label">等级:</span>
            <el-tag :type="gradeTagType(gradeResult)" size="large" class="grade-tag">{{ gradeResult }}</el-tag>
          </div>
        </div>

        <!-- 五、发现问题与整改建议 -->
        <el-card class="section-card" shadow="never">
          <template #header>
            <span class="card-title">五、发现问题与整改建议</span>
          </template>
          <el-form-item label="发现问题">
            <el-input v-model="form.issuesFound" type="textarea" :rows="4" :disabled="isViewMode" />
          </el-form-item>
          <el-form-item label="整改措施">
            <el-input v-model="form.improvementMeasures" type="textarea" :rows="4" :disabled="isViewMode" />
          </el-form-item>
          <el-row :gutter="16">
            <el-col :xs="24" :sm="12" :md="8">
              <el-form-item label="整改期限">
                <el-date-picker v-model="form.improvementDeadline" type="date" value-format="YYYY-MM-DD" :disabled="isViewMode" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-card>

        <!-- 六、现场照片 -->
        <el-card class="section-card" shadow="never">
          <template #header>
            <span class="card-title">六、现场照片</span>
          </template>
          <el-upload
            v-if="!isViewMode"
            action="/api/upload/image"
            :headers="uploadHeaders"
            list-type="picture-card"
            :file-list="photoFileList"
            :on-success="onPhotoSuccess"
            :on-remove="onPhotoRemove"
            accept="image/*"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <div v-else class="photo-preview-list">
            <el-image
              v-for="(url, idx) in photoList"
              :key="idx"
              :src="fullUrl(url)"
              :preview-src-list="photoList.map(u => fullUrl(u))"
              :initial-index="idx"
              fit="cover"
              style="width: 148px; height: 148px; margin: 0 8px 8px 0; border-radius: 6px"
            />
          </div>
        </el-card>

        <!-- 七、签字确认 -->
        <el-card class="section-card" shadow="never">
          <template #header>
            <span class="card-title">七、签字确认</span>
          </template>
          <el-row :gutter="16">
            <el-col :xs="24" :sm="12">
              <el-form-item label="评估人员签字">
                <el-upload
                  v-if="!isViewMode"
                  action="/api/upload/image"
                  :headers="uploadHeaders"
                  :show-file-list="false"
                  :on-success="(resp) => onSignatureSuccess(resp, 'assessor')"
                  accept="image/*"
                >
                  <el-image v-if="form.assessorSignatureUrl" :src="fullUrl(form.assessorSignatureUrl)" fit="contain" style="width: 200px; height: 80px; border: 1px dashed #dcdfe6; border-radius: 4px" />
                  <el-button v-else size="small">上传签字图片</el-button>
                </el-upload>
                <el-image v-else-if="form.assessorSignatureUrl" :src="fullUrl(form.assessorSignatureUrl)" fit="contain" style="width: 200px; height: 80px; border: 1px solid #ebeef5; border-radius: 4px" />
                <span v-else style="color: #999">未上传</span>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="机构负责人签字">
                <el-upload
                  v-if="!isViewMode"
                  action="/api/upload/image"
                  :headers="uploadHeaders"
                  :show-file-list="false"
                  :on-success="(resp) => onSignatureSuccess(resp, 'org')"
                  accept="image/*"
                >
                  <el-image v-if="form.orgSignatureUrl" :src="fullUrl(form.orgSignatureUrl)" fit="contain" style="width: 200px; height: 80px; border: 1px dashed #dcdfe6; border-radius: 4px" />
                  <el-button v-else size="small">上传签字图片</el-button>
                </el-upload>
                <el-image v-else-if="form.orgSignatureUrl" :src="fullUrl(form.orgSignatureUrl)" fit="contain" style="width: 200px; height: 80px; border: 1px solid #ebeef5; border-radius: 4px" />
                <span v-else style="color: #999">未上传</span>
              </el-form-item>
            </el-col>
          </el-row>
        </el-card>
      </div>

      <!-- 底部按钮区 -->
      <div class="drawer-footer">
        <template v-if="isViewMode && currentAssessment?.status === 'SUBMITTED'">
          <el-button @click="drawerVisible = false">关闭</el-button>
          <el-button type="success" :loading="actionLoading" @click="confirmAssessment(currentAssessment)">确认</el-button>
        </template>
        <template v-else-if="isViewMode">
          <el-button @click="drawerVisible = false">关闭</el-button>
          <el-button type="primary" @click="exportPdf(currentAssessment)">导出PDF</el-button>
        </template>
        <template v-else>
          <el-button @click="drawerVisible = false">取消</el-button>
          <el-button :loading="saving" @click="saveAssessment(false)">保存草稿</el-button>
          <el-button type="primary" :loading="saving" @click="saveAssessment(true)">保存并提交</el-button>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { api } from '../../api/client'
import { useAuthStore } from '../../store/auth'

const authStore = useAuthStore()
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

// ===================== 列表相关 =====================
const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const filterMonth = ref(null)
const filterStatus = ref('')
const filterGrade = ref('')

function pad2(n) {
  const s = String(n)
  return s.length >= 2 ? s : `0${s}`
}

function formatMonth(d) {
  if (!d) return null
  if (typeof d === 'string') return d
  const date = d instanceof Date ? d : new Date(d)
  if (Number.isNaN(date.getTime())) return null
  return `${date.getFullYear()}-${pad2(date.getMonth() + 1)}`
}

function statusText(status) {
  const map = { DRAFT: '草稿', SUBMITTED: '已提交', CONFIRMED: '已确认' }
  return map[status] || status || '-'
}

function statusTagType(status) {
  const map = { DRAFT: 'info', SUBMITTED: 'warning', CONFIRMED: 'success' }
  return map[status] || 'info'
}

function gradeTagType(grade) {
  const map = { '优秀': 'success', '良好': '', '合格': 'warning', '不合格': 'danger' }
  return map[grade] || 'info'
}

async function fetchList() {
  loading.value = true
  try {
    const resp = await api.get('/api/home-service/assessments', {
      params: {
        page: page.value,
        pageSize: pageSize.value,
        month: formatMonth(filterMonth.value) || undefined,
        status: filterStatus.value || undefined,
        grade: filterGrade.value || undefined
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

// ===================== 老人搜索 =====================
const elderlyLoading = ref(false)
const elderlyOptions = ref([])

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

function onElderlyChange(id) {
  const found = elderlyOptions.value.find(o => o.id === id)
  if (found) {
    form.elderlyName = found.name
  }
}

// ===================== 图片上传相关 =====================
const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${authStore.accessToken}`
}))

function fullUrl(url) {
  if (!url) return ''
  if (url.startsWith('http')) return url
  return apiBaseUrl + url
}

// photoUrls 解析
const photoList = computed(() => {
  if (!form.photoUrls) return []
  try {
    return JSON.parse(form.photoUrls)
  } catch {
    return form.photoUrls ? [form.photoUrls] : []
  }
})

const photoFileList = computed(() => {
  return photoList.value.map((url, idx) => ({
    name: `photo-${idx}`,
    url: fullUrl(url),
    uid: -(idx + 1)
  }))
})

function onPhotoSuccess(resp) {
  if (resp.code === 200 && resp.data) {
    const current = [...photoList.value, resp.data]
    form.photoUrls = JSON.stringify(current)
  } else {
    ElMessage.error('上传失败')
  }
}

function onPhotoRemove(file) {
  const current = photoList.value.filter(url => fullUrl(url) !== file.url)
  form.photoUrls = current.length > 0 ? JSON.stringify(current) : null
}

function onSignatureSuccess(resp, type) {
  if (resp.code === 200 && resp.data) {
    if (type === 'assessor') {
      form.assessorSignatureUrl = resp.data
    } else {
      form.orgSignatureUrl = resp.data
    }
  } else {
    ElMessage.error('上传失败')
  }
}

// ===================== 综合评分计算 =====================
const totalScore = computed(() => {
  return (Number(form.agreementScore) || 0) + (Number(form.fulfillmentScore) || 0) + (Number(form.recordScore) || 0) + (Number(form.elderlySatisfaction) || 0)
})

const gradeResult = computed(() => {
  const s = totalScore.value
  if (s >= 90) return '优秀'
  if (s >= 75) return '良好'
  if (s >= 60) return '合格'
  return '不合格'
})

// ===================== 抽屉 =====================
const drawerVisible = ref(false)
const drawerMode = ref('view') // 'create' | 'edit' | 'view'
const saving = ref(false)
const actionLoading = ref(false)
const currentAssessment = ref(null)

const form = reactive({
  id: null,
  assessmentNo: '',
  assessmentDate: '',
  assessorName: '',
  assessorOrg: '',
  assessmentPeriod: '',
  elderlyId: null,
  elderlyName: '',
  serviceAddress: '',
  agreementSigned: 0,
  agreementComplete: 0,
  planFormulated: 0,
  planMatchesNeeds: 0,
  agreementScore: 0,
  serviceOnTime: 0,
  staffIdentified: 0,
  riskInformed: 0,
  servicePerPlan: 0,
  emergencyHandled: 0,
  acceptanceDone: 0,
  fulfillmentScore: 0,
  recordComplete: 0,
  recordTimely: 0,
  recordAccurate: 0,
  recordScore: 0,
  elderlySatisfaction: 0,
  satisfactionMethod: '问卷',
  issuesFound: '',
  improvementMeasures: '',
  improvementDeadline: '',
  photoUrls: null,
  assessorSignatureUrl: '',
  orgSignatureUrl: ''
})

const isViewMode = computed(() => drawerMode.value === 'view')

function resetForm() {
  form.id = null
  form.assessmentNo = ''
  form.assessmentDate = ''
  form.assessorName = ''
  form.assessorOrg = ''
  form.assessmentPeriod = ''
  form.elderlyId = null
  form.elderlyName = ''
  form.serviceAddress = ''
  form.agreementSigned = 0
  form.agreementComplete = 0
  form.planFormulated = 0
  form.planMatchesNeeds = 0
  form.agreementScore = 0
  form.serviceOnTime = 0
  form.staffIdentified = 0
  form.riskInformed = 0
  form.servicePerPlan = 0
  form.emergencyHandled = 0
  form.acceptanceDone = 0
  form.fulfillmentScore = 0
  form.recordComplete = 0
  form.recordTimely = 0
  form.recordAccurate = 0
  form.recordScore = 0
  form.elderlySatisfaction = 0
  form.satisfactionMethod = '问卷'
  form.issuesFound = ''
  form.improvementMeasures = ''
  form.improvementDeadline = ''
  form.photoUrls = null
  form.assessorSignatureUrl = ''
  form.orgSignatureUrl = ''
  currentAssessment.value = null
}

function fillForm(data) {
  form.id = data.id
  form.assessmentNo = data.assessmentNo || ''
  form.assessmentDate = data.assessmentDate || ''
  form.assessorName = data.assessorName || ''
  form.assessorOrg = data.assessorOrg || ''
  form.assessmentPeriod = data.assessmentPeriod || ''
  form.elderlyId = data.elderlyId || null
  form.elderlyName = data.elderlyName || ''
  form.serviceAddress = data.serviceAddress || ''
  form.agreementSigned = data.agreementSigned || 0
  form.agreementComplete = data.agreementComplete || 0
  form.planFormulated = data.planFormulated || 0
  form.planMatchesNeeds = data.planMatchesNeeds || 0
  form.agreementScore = data.agreementScore || 0
  form.serviceOnTime = data.serviceOnTime || 0
  form.staffIdentified = data.staffIdentified || 0
  form.riskInformed = data.riskInformed || 0
  form.servicePerPlan = data.servicePerPlan || 0
  form.emergencyHandled = data.emergencyHandled || 0
  form.acceptanceDone = data.acceptanceDone || 0
  form.fulfillmentScore = data.fulfillmentScore || 0
  form.recordComplete = data.recordComplete || 0
  form.recordTimely = data.recordTimely || 0
  form.recordAccurate = data.recordAccurate || 0
  form.recordScore = data.recordScore || 0
  form.elderlySatisfaction = data.elderlySatisfaction || 0
  form.satisfactionMethod = data.satisfactionMethod || '问卷'
  form.issuesFound = data.issuesFound || ''
  form.improvementMeasures = data.improvementMeasures || ''
  form.improvementDeadline = data.improvementDeadline || ''
  form.photoUrls = data.photoUrls || null
  form.assessorSignatureUrl = data.assessorSignatureUrl || ''
  form.orgSignatureUrl = data.orgSignatureUrl || ''
}

function openCreate() {
  resetForm()
  drawerMode.value = 'create'
  drawerVisible.value = true
  searchElderly('')
}

async function openView(row) {
  resetForm()
  drawerMode.value = 'view'
  try {
    const resp = await api.get(`/api/home-service/assessments/${row.id}`)
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '加载失败')
    const data = body.data
    currentAssessment.value = data
    fillForm(data)
    drawerVisible.value = true
  } catch (e) {
    ElMessage.error(e.message || '加载详情失败')
  }
}

async function openEdit(row) {
  resetForm()
  drawerMode.value = 'edit'
  try {
    const resp = await api.get(`/api/home-service/assessments/${row.id}`)
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '加载失败')
    const data = body.data
    currentAssessment.value = data
    fillForm(data)
    drawerVisible.value = true
    searchElderly('')
  } catch (e) {
    ElMessage.error(e.message || '加载详情失败')
  }
}

function onDrawerClose() {
  currentAssessment.value = null
}

// ===================== 保存/提交 =====================
async function saveAssessment(andSubmit) {
  if (!form.assessmentDate) {
    ElMessage.warning('请选择评估日期')
    return
  }
  if (!form.assessorOrg) {
    ElMessage.warning('请填写评估机构')
    return
  }
  if (!form.assessorName) {
    ElMessage.warning('请填写评估人员')
    return
  }

  saving.value = true
  try {
    const payload = {
      assessmentDate: form.assessmentDate,
      assessorName: form.assessorName || null,
      assessorOrg: form.assessorOrg || null,
      assessmentPeriod: form.assessmentPeriod || null,
      elderlyId: form.elderlyId || null,
      elderlyName: form.elderlyName || null,
      serviceAddress: form.serviceAddress || null,
      agreementSigned: form.agreementSigned,
      agreementComplete: form.agreementComplete,
      planFormulated: form.planFormulated,
      planMatchesNeeds: form.planMatchesNeeds,
      agreementScore: form.agreementScore,
      serviceOnTime: form.serviceOnTime,
      staffIdentified: form.staffIdentified,
      riskInformed: form.riskInformed,
      servicePerPlan: form.servicePerPlan,
      emergencyHandled: form.emergencyHandled,
      acceptanceDone: form.acceptanceDone,
      fulfillmentScore: form.fulfillmentScore,
      recordComplete: form.recordComplete,
      recordTimely: form.recordTimely,
      recordAccurate: form.recordAccurate,
      recordScore: form.recordScore,
      elderlySatisfaction: form.elderlySatisfaction,
      satisfactionMethod: form.satisfactionMethod || null,
      issuesFound: form.issuesFound || null,
      improvementMeasures: form.improvementMeasures || null,
      improvementDeadline: form.improvementDeadline || null,
      photoUrls: form.photoUrls || null,
      assessorSignatureUrl: form.assessorSignatureUrl || null,
      orgSignatureUrl: form.orgSignatureUrl || null
    }

    let resp
    if (form.id) {
      resp = await api.put(`/api/home-service/assessments/${form.id}`, payload)
    } else {
      resp = await api.post('/api/home-service/assessments', payload)
    }
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '保存失败')

    const savedId = body.data?.id || form.id

    if (andSubmit && savedId) {
      const submitResp = await api.post(`/api/home-service/assessments/${savedId}/submit`)
      const submitBody = submitResp.data
      if (submitBody.code !== 200) throw new Error(submitBody.msg || '提交失败')
      ElMessage.success('保存并提交成功')
    } else {
      ElMessage.success('保存成功')
    }

    drawerVisible.value = false
    await fetchList()
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// ===================== 列表操作 =====================
async function remove(row) {
  try {
    await ElMessageBox.confirm(`确认删除评估 ${row.assessmentNo || row.id}？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    const resp = await api.delete(`/api/home-service/assessments/${row.id}`)
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '删除失败')
    ElMessage.success('删除成功')
    await fetchList()
  } catch (e) {
    ElMessage.error(e.message || '删除失败')
  }
}

async function submitAssessment(row) {
  try {
    await ElMessageBox.confirm(`确认提交评估 ${row.assessmentNo || row.id}？`, '提示', { type: 'warning' })
    const resp = await api.post(`/api/home-service/assessments/${row.id}/submit`)
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '提交失败')
    ElMessage.success('提交成功')
    await fetchList()
  } catch (e) {
    if (e === 'cancel') return
    ElMessage.error(e.message || '提交失败')
  }
}

async function confirmAssessment(row) {
  const id = row?.id || currentAssessment.value?.id
  if (!id) return
  try {
    await ElMessageBox.confirm('确认此评估？确认后不可修改。', '确认评估', { type: 'warning' })
    actionLoading.value = true
    const resp = await api.post(`/api/home-service/assessments/${id}/confirm`)
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '确认失败')
    ElMessage.success('确认成功')
    drawerVisible.value = false
    await fetchList()
  } catch (e) {
    if (e === 'cancel') return
    ElMessage.error(e.message || '确认失败')
  } finally {
    actionLoading.value = false
  }
}

// ===================== PDF 导出 =====================
function exportPdf(row) {
  window.print()
}

// ===================== 初始化 =====================
onMounted(() => {
  filterMonth.value = new Date()
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
  flex-wrap: wrap;
  gap: 8px;
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

/* 抽屉内表单样式 */
.assessment-form {
  padding: 0 8px 80px;
}

.form-title {
  text-align: center;
  font-size: 22px;
  font-weight: bold;
  letter-spacing: 4px;
  margin-bottom: 4px;
  color: #303133;
}

.form-subtitle {
  text-align: center;
  font-size: 14px;
  color: #909399;
  margin-bottom: 20px;
}

.section-card {
  margin-bottom: 16px;
}

.card-title {
  font-size: 15px;
  font-weight: bold;
  color: #303133;
}

.ref-tag {
  display: inline-block;
  font-size: 12px;
  color: #909399;
  margin-left: 8px;
}

/* 综合评分区 */
.total-score-area {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 40px;
  padding: 20px;
  margin-bottom: 16px;
  background: linear-gradient(135deg, #ecf5ff 0%, #f0f9eb 100%);
  border-radius: 8px;
  border: 1px solid #dcdfe6;
}

.score-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.score-label {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

.score-value {
  font-size: 28px;
  font-weight: bold;
  color: #409eff;
}

.grade-tag {
  font-size: 18px;
  padding: 4px 16px;
}

/* 签字上传样式 */
.photo-preview-list {
  display: flex;
  flex-wrap: wrap;
}

/* 底部按钮 */
.drawer-footer {
  position: fixed;
  bottom: 0;
  right: 0;
  width: 100%;
  padding: 12px 20px;
  background: #fff;
  border-top: 1px solid #ebeef5;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  z-index: 10;
}

/* 打印样式 */
.print-only {
  display: none;
}

@media print {
  .page .el-card {
    box-shadow: none !important;
    border: none !important;
  }

  .header,
  .header-actions,
  .pager,
  .drawer-footer,
  .el-drawer__header,
  .el-pagination,
  .el-card__header {
    display: none !important;
  }

  .el-drawer {
    position: static !important;
    width: 100% !important;
    overflow: visible !important;
  }

  .el-drawer__body {
    overflow: visible !important;
  }

  .el-switch {
    display: none !important;
  }

  .print-only {
    display: inline !important;
    font-weight: bold;
    margin-left: 4px;
  }

  .assessment-form {
    padding: 0;
  }

  .section-card {
    break-inside: avoid;
  }

  .total-score-area {
    background: #f5f5f5 !important;
  }

  .el-image {
    max-width: 200px !important;
  }

  body {
    size: A4;
    margin: 10mm;
  }

  .ref-tag {
    font-size: 10px;
  }
}
</style>
