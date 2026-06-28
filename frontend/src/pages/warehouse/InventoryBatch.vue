<template>
  <div class="page">
    <PageHeader title="批次追踪">
      <template #actions>
        <el-select
          v-model="materialId"
          clearable
          filterable
          remote
          :remote-method="searchMaterial"
          :loading="materialLoading"
          placeholder="搜索物资"
          style="width: 220px"
          @change="reload"
        >
          <el-option v-for="m in materialOptions" :key="m.id" :label="m.name" :value="m.id" />
        </el-select>
        <el-select v-model="supplyCategory" clearable placeholder="供应类别" style="width: 150px" @change="reload">
          <el-option label="集中供养" value="CENTRALIZED" />
          <el-option label="社会化" value="SOCIAL" />
        </el-select>
        <el-button @click="fetchList">刷新</el-button>
      </template>
    </PageHeader>

    <el-card>
      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <el-tab-pane label="全部批次" name="all" />
        <el-tab-pane label="临期预警" name="near" />
      </el-tabs>

      <el-table
        :data="list"
        v-loading="loading"
        row-key="id"
        :row-class-name="rowClassName"
      >
        <el-table-column label="物资名称" min-width="180">
          <template #default="{ row }">{{ materialName(row.materialId) }}</template>
        </el-table-column>
        <el-table-column prop="batchNo" label="批次号" width="160" />
        <el-table-column label="供应类别" width="140">
          <template #default="{ row }">{{ supplyCategoryLabel(row.supplyCategory) }}</template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="100" />
        <el-table-column prop="remainingQuantity" label="剩余数量" width="100" />
        <el-table-column prop="expiryDate" label="有效期" width="130" />
        <el-table-column prop="inDate" label="入库日期" width="130" />
      </el-table>

      <div v-if="activeTab === 'all'" class="pager">
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
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '../../components/common/PageHeader.vue'
import { api } from '../../api/client'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const materialId = ref('')
const supplyCategory = ref('')
const activeTab = ref('all')

const materialLoading = ref(false)
const materialOptions = ref([])

function supplyCategoryLabel(cat) {
  if (cat === 'CENTRALIZED') return '集中供养'
  if (cat === 'SOCIAL') return '社会化'
  return cat || '-'
}

function materialName(id) {
  const m = materialOptions.value.find((item) => String(item.id) === String(id))
  return m ? m.name : id ? '物资 #' + id : '-'
}

function isNearExpiry(row) {
  if (!row.expiryDate) return false
  const expiry = new Date(row.expiryDate)
  if (Number.isNaN(expiry.getTime())) return false
  const diff = expiry.getTime() - Date.now()
  return diff >= 0 && diff <= 30 * 24 * 60 * 60 * 1000
}

function rowClassName({ row }) {
  if (isNearExpiry(row)) return 'warning-row'
  return ''
}

let materialSearchTimer = null
async function searchMaterial(query) {
  if (!query || query.length < 1) return
  clearTimeout(materialSearchTimer)
  materialSearchTimer = setTimeout(async () => {
    materialLoading.value = true
    try {
      const resp = await api.get('/api/warehouse/materials', {
        params: { page: 1, pageSize: 50, keyword: query }
      })
      const body = resp.data
      if (body.code === 200) {
        materialOptions.value = body.data?.list || []
      }
    } catch (e) {
      console.warn('搜索物资失败', e)
    } finally {
      materialLoading.value = false
    }
  }, 300)
}

async function fetchList() {
  loading.value = true
  try {
    if (activeTab.value === 'near') {
      const resp = await api.get('/api/inventory-batch/near-expiry', { params: { days: 30 } })
      const body = resp.data
      if (body.code !== 200) throw new Error(body.msg || '加载失败')
      list.value = body.data || []
      total.value = list.value.length
    } else {
      const resp = await api.get('/api/inventory-batch', {
        params: {
          page: page.value,
          size: pageSize.value,
          materialId: materialId.value || undefined,
          supplyCategory: supplyCategory.value || undefined
        }
      })
      const body = resp.data
      if (body.code !== 200) throw new Error(body.msg || '加载失败')
      list.value = body.data?.list || []
      total.value = body.data?.total || 0
    }
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

function onTabChange() {
  page.value = 1
  fetchList()
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

:deep(.warning-row) {
  color: #e6a23c;
  background: #fdf6ec;
}

:deep(.warning-row td) {
  color: #e6a23c;
}
</style>
