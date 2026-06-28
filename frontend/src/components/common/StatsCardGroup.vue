<template>
  <el-row :gutter="16" class="stats-card-group">
    <el-col v-for="(item, index) in items" :key="index" :xs="12" :sm="12" :md="Math.floor(24 / cols)" :lg="Math.floor(24 / cols)">
      <div class="stats-card" :style="{ background: item.gradient || defaultGradients[index % 4] }">
        <div class="stats-card-icon">
          <el-icon :size="28"><component :is="item.icon" /></el-icon>
        </div>
        <div class="stats-card-content">
          <div class="stats-card-value">{{ item.value }}</div>
          <div class="stats-card-label">{{ item.label }}</div>
        </div>
      </div>
    </el-col>
  </el-row>
</template>

<script setup>
defineProps({
  items: {
    type: Array,
    required: true,
    // Each item: { label, value, icon (component), gradient? (optional CSS gradient string) }
  },
  cols: {
    type: Number,
    default: 4
  }
})

const defaultGradients = [
  'linear-gradient(135deg, #2B7A78 0%, #3AAFA9 100%)',
  'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
  'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
  'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)'
]
</script>

<style scoped>
.stats-card-group {
  margin-bottom: 16px;
}

.stats-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  border-radius: var(--radius-md, 8px);
  color: #fff;
  box-shadow: var(--shadow-md);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  cursor: default;
}

.stats-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
}

.stats-card-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.2);
}

.stats-card-content {
  display: flex;
  flex-direction: column;
}

.stats-card-value {
  font-size: 24px;
  font-weight: 700;
  line-height: 1.2;
}

.stats-card-label {
  font-size: 13px;
  opacity: 0.85;
  margin-top: 4px;
}
</style>
