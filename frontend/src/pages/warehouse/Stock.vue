<template>
  <div class="page">
    <PageHeader title="库存管理">
      <template #actions>
        <el-switch v-model="warningOnly" active-text="仅预警" @change="reload" />
        <el-button @click="fetchList">刷新</el-button>
      </template>
    </PageHeader>

    <StatsCardGroup :items="statsItems" />

    <el-card>
      <div class="filter-bar">
        <el-radio-group v-model="selectedCategory" @change="reload">
          <el-radio-button label="">全部</el-radio-button>
          <el-radio-button label="SOCIAL">社会化库存</el-radio-button>
          <el-radio-button label="CENTRALIZED">集中供养库存</el-radio-button>
        </el-radio-group>
      </div>

      <el-table :data="list" v-loading="loading" row-key="stockId" :row-class-name="rowClass">
        <template #empty>
          <el-empty description="暂无数据" :image-size="80" />
        </template>
        <el-table-column prop="materialName" label="物资" width="220" />
        <el-table-column prop="category" label="类别" width="160" />
        <el-table-column label="供应类别" width="160">
          <template #default="{ row }">
            <el-tag v-if="row.supplyCategory === 'SOCIAL'" type="primary">社会化物资</el-tag>
            <el-tag v-else-if="row.supplyCategory === 'CENTRALIZED'" type="success">集中供养物资</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="specification" label="规格" min-width="180" />
        <el-table-column prop="unit" label="单位" width="120" />
        <el-table-column prop="quantity" label="数量" width="120" />
        <el-table-column prop="warningThreshold" label="预警阈值" width="120" />
        <el-table-column prop="totalValue" label="库存金额" width="140">
          <template #default="{ row }">￥{{ formatMoney(row.totalValue) }}</template>
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
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Box, Warning, Money, CircleCheck } from '@element-plus/icons-vue'
import { api } from '../../api/client'
import PageHeader from '../../components/common/PageHeader.vue'
import StatsCardGroup from '../../components/common/StatsCardGroup.vue'
import { useFormat } from '@/composables/useFormat'

const { formatMoney } = useFormat()

const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const warningOnly = ref(false)
const selectedCategory = ref('')

const statsItems = computed(() => [
  { label: '物资种类', value: total.value, icon: Box, gradient: 'linear-gradient(135deg, #2B7A78 0%, #3AAFA9 100%)' },
  { label: '预警物资', value: list.value.filter(i => i.warning === 1).length, icon: Warning, gradient: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)' },
  { label: '库存总值', value: '¥' + formatMoney(list.value.reduce((s, i) => s + (Number(i.totalValue) || 0), 0)), icon: Money, gradient: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)' },
  { label: '正常物资', value: list.value.filter(i => i.warning !== 1).length, icon: CircleCheck, gradient: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)' }
])

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
        warningOnly: warningOnly.value,
        supplyCategory: selectedCategory.value || undefined
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

.filter-bar {
  margin-bottom: 14px;
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
