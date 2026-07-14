<template>
  <div class="page">
    <PageHeader title="资金使用明细">
      <template #actions>
        <el-select v-model="supplyCategory" placeholder="供应类别" clearable style="width: 150px" @change="reload">
          <el-option label="集中供养" value="CENTRALIZED" />
          <el-option label="社会化" value="SOCIAL" />
          <el-option label="全部" value="" />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 280px"
          @change="reload"
        />
        <el-button @click="fetchList">刷新</el-button>
        <el-button type="primary" @click="exportReport">导出 Excel</el-button>
      </template>
    </PageHeader>

    <el-card>
      <el-table :data="list" v-loading="loading" row-key="outId">
        <el-table-column prop="materialName" label="物资名称" min-width="180" />
        <el-table-column prop="quantity" label="数量" width="100" />
        <el-table-column label="供应类别" width="140">
          <template #default="{ row }">{{ supplyCategoryLabel(row.supplyCategory) }}</template>
        </el-table-column>
        <el-table-column prop="recipientName" label="领用人" width="140" />
        <el-table-column prop="outDate" label="出库日期" width="130" />
        <el-table-column prop="amount" label="金额" width="140">
          <template #default="{ row }">¥{{ formatMoney(row.amount) }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '-' }}</template>
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
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '../../components/common/PageHeader.vue'
import { api } from '../../api/client'
import { useFormat } from '@/composables/useFormat'

const { formatMoney, supplyCategoryLabel } = useFormat()

const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const dateRange = ref([])
const supplyCategory = ref('CENTRALIZED')

async function fetchList() {
  loading.value = true
  try {
    const [startDate, endDate] = dateRange.value || []
    const resp = await api.get('/api/warehouse-report/fund-usage', {
      params: {
        page: page.value,
        size: pageSize.value,
        startDate: startDate || undefined,
        endDate: endDate || undefined,
        supplyCategory: supplyCategory.value || undefined
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

function exportReport() {
  if (!list.value || list.value.length === 0) {
    ElMessage.warning('暂无数据可导出')
    return
  }
  const headers = ['出库日期', '物资名称', '数量', '供应类别', '领用人', '金额', '备注']
  const rows = list.value.map(item => [
    item.outDate || '',
    item.materialName || '',
    item.quantity || 0,
    supplyCategoryLabel(item.supplyCategory),
    item.recipientName || '',
    formatMoney(item.amount),
    item.remark || ''
  ])
  const csvContent = [headers.join(','), ...rows.map(r => r.join(','))].join('\n')
  const BOM = '\uFEFF'
  const blob = new Blob([BOM + csvContent], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `资金使用明细_${new Date().toISOString().slice(0, 10)}.csv`
  link.click()
  URL.revokeObjectURL(url)
  ElMessage.success('导出成功')
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
