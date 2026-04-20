<template>
  <div class="demo-secure-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>安全接口演示</span>
          <el-tag type="warning" size="small">需要 demo:secure 权限</el-tag>
        </div>
      </template>
      
      <div class="demo-content">
        <el-button type="primary" @click="callApi" :loading="loading">
          {{ loading ? '请求中...' : '调用接口' }}
        </el-button>
        
        <div v-if="result" class="result-box">
          <pre>{{ result }}</pre>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { api } from '../api/client'

const loading = ref(false)
const result = ref('')

async function callApi() {
  loading.value = true
  result.value = ''
  try {
    const resp = await api.get('/api/demo/secure')
    result.value = JSON.stringify(resp.data, null, 2)
  } catch (e) {
    result.value = JSON.stringify(e?.response?.data || { message: 'request failed' }, null, 2)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.demo-secure-page {
  padding: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.demo-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.result-box {
  background-color: #f5f7fa;
  border-radius: 8px;
  padding: 16px;
  overflow: auto;
}

.result-box pre {
  margin: 0;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 13px;
  color: #303133;
}
</style>
