<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="header">
          <span>库存看板</span>
          <div class="header-actions">
            <el-switch v-model="warningOnly" active-text="仅预警" @change="reload" />
            <el-button @click="fetchList">刷新</el-button>
          </div>
        </div>
      </template>

      <el-table :data="list" v-loading="loading" row-key="stockId" :row-class-name="rowClass">
        <el-table-column prop="materialName" label="物资" width="220" />
        <el-table-column prop="category" label="类别" width="160" />
        <el-table-column prop="specification" label="规格" min-width="180" />
        <el-table-column prop="unit" label="单位" width="120" />
        <el-table-column prop="quantity" label="数量" width="120" />
        <el-table-column prop="warningThreshold" label="预警阈值" width="120" />
        <el-table-column prop="totalValue" label="库存金额" width="140">
          <template #default="{ row }">￥{{ formatAmount(row.totalValue) }}</template>
        </el-table-column>
        <el-table-column label="预警" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.warning === 1" type="danger">预警</el-tag>
            <el-tag v-else type="success">正常</el-tag>
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
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../../api/client'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const warningOnly = ref(false)

function formatAmount(amount) {
  if (amount === null || amount === undefined) return '0.00'
  const n = Number(amount)
  if (Number.isNaN(n)) return String(amount)
  return n.toFixed(2)
}

function rowClass({ row }) {
  return row.warning === 1 ? 'warning-row' : ''
}

async function fetchList() {
  loading.value = true
  try {
    const resp = await api.get('/api/warehouse/stocks', {
      params: {
        page: page.value,
        pageSize: pageSize.value,
        warningOnly: warningOnly.value
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
}

.pager {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}

:deep(.warning-row) {
  background: #fef0f0;
}
</style>
