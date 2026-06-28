<template>
  <div class="page-header">
    <div class="page-header-left">
      <h2 class="page-title">{{ title }}</h2>
      <p v-if="subtitle" class="page-subtitle">{{ subtitle }}</p>
    </div>
    <div class="page-header-right">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' }
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  background: var(--color-bg-card, #fff);
  border-radius: var(--radius-md, 8px);
  margin-bottom: 16px;
  box-shadow: var(--shadow-sm);
}

.page-header-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text-primary, #1F2937);
}

.page-subtitle {
  margin: 0;
  font-size: 14px;
  color: var(--color-text-secondary, #6B7280);
}

.page-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>
