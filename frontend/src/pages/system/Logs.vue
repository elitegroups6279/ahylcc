<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="header">
          <span>操作日志</span>
          <div class="header-actions">
            <el-input v-model="filters.username" placeholder="用户名" clearable style="width: 160px" @keyup.enter="reload" />
            <el-input v-model="filters.module" placeholder="模块" clearable style="width: 160px" @keyup.enter="reload" />
            <el-date-picker
              v-model="filters.timeRange"
              type="datetimerange"
              range-separator="~"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              format="YYYY-MM-DD HH:mm:ss"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 360px"
              @change="reload"
            />
            <el-button @click="reset">重置</el-button>
            <el-button type="primary" @click="reload">查询</el-button>
          </div>
        </div>
      </template>

      <el-table :data="list" v-loading="loading" row-key="id">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column prop="module" label="模块" width="140" />
        <el-table-column prop="operation" label="操作" min-width="180" />
        <el-table-column prop="ip" label="IP" width="140" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operationTime" label="时间" width="180" />
        <el-table-column label="参数" min-width="220">
          <template #default="{ row }">
            <el-text truncated style="max-width: 520px">{{ row.params }}</el-text>
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../../api/client'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const filters = reactive({
  username: '',
  module: '',
  timeRange: []
})

function buildParams() {
  const [startTime, endTime] = filters.timeRange || []
  return {
    page: page.value,
    pageSize: pageSize.value,
    username: filters.username || undefined,
    module: filters.module || undefined,
    startTime: startTime || undefined,
    endTime: endTime || undefined
  }
}

async function fetchList() {
  loading.value = true
  try {
    const resp = await api.get('/api/system/logs', { params: buildParams() })
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

function reset() {
  filters.username = ''
  filters.module = ''
  filters.timeRange = []
  reload()
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
  flex-wrap: wrap;
  justify-content: flex-end;
}

.pager {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}
</style>
