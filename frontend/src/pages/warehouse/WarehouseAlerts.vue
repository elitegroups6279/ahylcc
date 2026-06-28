<template>
  <div class="page">
    <PageHeader title="预警中心">
      <template #actions>
        <el-button @click="fetchList">刷新</el-button>
      </template>
    </PageHeader>

    <el-card v-loading="loading">
      <div v-if="alerts.length" class="alert-grid">
        <el-card
          v-for="(alert, index) in alerts"
          :key="index"
          shadow="hover"
          class="alert-card"
          :class="['alert-' + alert.level.toLowerCase()]"
        >
          <div class="alert-header">
            <div class="alert-icon">
              <el-icon :size="28"><component :is="alertIcon(alert.level)" /></el-icon>
            </div>
            <div class="alert-badges">
              <el-tag :type="levelTagType(alert.level)" size="small">{{ alert.level }}</el-tag>
              <el-tag type="info" size="small" effect="plain">{{ alert.type }}</el-tag>
            </div>
          </div>
          <div class="alert-body">
            <div class="alert-message">{{ alert.message }}</div>
          </div>
        </el-card>
      </div>
      <el-empty v-else description="暂无预警信息" />
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Warning, Bell, InfoFilled, CircleCloseFilled } from '@element-plus/icons-vue'
import PageHeader from '../../components/common/PageHeader.vue'
import { api } from '../../api/client'

const loading = ref(false)
const alerts = ref([])

function levelTagType(level) {
  const l = String(level).toLowerCase()
  if (l === 'danger') return 'danger'
  if (l === 'warning') return 'warning'
  return 'info'
}

function alertIcon(level) {
  const l = String(level).toLowerCase()
  if (l === 'danger') return CircleCloseFilled
  if (l === 'warning') return Warning
  return InfoFilled
}

async function fetchList() {
  loading.value = true
  try {
    const resp = await api.get('/api/warehouse/dashboard/alerts')
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '加载失败')
    alerts.value = body.data || []
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.page {
  padding: 16px;
}

.alert-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.alert-card {
  border-left-width: 4px;
  border-left-style: solid;
}

.alert-card.alert-danger {
  border-left-color: #f56c6c;
}

.alert-card.alert-warning {
  border-left-color: #e6a23c;
}

.alert-card.alert-info {
  border-left-color: #409eff;
}

.alert-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.alert-icon {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-regular);
}

.alert-danger .alert-icon {
  color: #f56c6c;
  background: #fef0f0;
}

.alert-warning .alert-icon {
  color: #e6a23c;
  background: #fdf6ec;
}

.alert-info .alert-icon {
  color: #409eff;
  background: #f0f9ff;
}

.alert-badges {
  display: flex;
  gap: 8px;
}

.alert-body {
  font-size: 14px;
  color: var(--color-text-primary, #1f2937);
  line-height: 1.6;
}
</style>
