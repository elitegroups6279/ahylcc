<template>
  <div class="page">
    <PageHeader title="消耗趋势分析">
      <template #actions>
        <el-select v-model="months" style="width: 140px" @change="reload">
          <el-option label="近 3 个月" :value="3" />
          <el-option label="近 6 个月" :value="6" />
          <el-option label="近 12 个月" :value="12" />
        </el-select>
        <el-select v-model="supplyCategory" clearable placeholder="供应类别" style="width: 150px" @change="reload">
          <el-option label="集中供养" value="CENTRALIZED" />
          <el-option label="社会化" value="SOCIAL" />
        </el-select>
        <el-button @click="fetchList">刷新</el-button>
      </template>
    </PageHeader>

    <el-card>
      <el-table :data="pagedList" v-loading="loading" :row-key="(row, index) => index">
        <el-table-column prop="month" label="月份" width="110" sortable />
        <el-table-column prop="category" label="类别" min-width="140" />
        <el-table-column prop="totalQuantity" label="消耗数量" width="120" />
        <el-table-column prop="totalAmount" label="消耗金额" width="140">
          <template #default="{ row }">¥{{ formatMoney(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column label="相对占比" min-width="220">
          <template #default="{ row }">
            <el-progress :percentage="progress(row)" :color="progressColor" />
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          background
          layout="total, prev, pager, next, sizes"
          :total="list.length"
          :current-page="page"
          :page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          @update:current-page="(p) => { page = p }"
          @update:page-size="(s) => { pageSize = s; page = 1 }"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '../../components/common/PageHeader.vue'
import { api } from '../../api/client'
import { useFormat } from '@/composables/useFormat'

const { formatMoney } = useFormat()

const loading = ref(false)
const list = ref([])
const page = ref(1)
const pageSize = ref(20)
const months = ref(6)
const supplyCategory = ref('')

const progressColor = [
  { color: '#67c23a', percentage: 40 },
  { color: '#e6a23c', percentage: 80 },
  { color: '#f56c6c', percentage: 100 }
]

const maxAmount = computed(() => {
  const amounts = list.value.map((i) => Number(i.totalAmount) || 0)
  return amounts.length ? Math.max(...amounts) : 0
})

const pagedList = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return list.value.slice(start, start + pageSize.value)
})

function progress(row) {
  if (maxAmount.value <= 0) return 0
  const pct = (Number(row.totalAmount) || 0) / maxAmount.value * 100
  return Math.min(Math.round(pct), 100)
}

async function fetchList() {
  loading.value = true
  try {
    const resp = await api.get('/api/warehouse/dashboard/consumption-trend', {
      params: {
        months: months.value,
        supplyCategory: supplyCategory.value || undefined
      }
    })
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '加载失败')
    list.value = body.data || []
    page.value = 1
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
</style>
