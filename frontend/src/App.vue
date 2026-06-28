<template>
  <router-view />
</template>

<script setup>
// Token 验证完全由 router guard 处理，App.vue 不再重复验证
</script>

<style>
/* ===================================================================
 * Global brand color system — 养老行业墨绿色系
 * =================================================================== */
:root {
  /* Primary brand */
  --color-primary: #2B7A78;
  --color-primary-light: #3AAFA9;
  --color-primary-dark: #1F5F5B;
  --color-primary-bg: #E8F5F4;

  /* Functional colors */
  --color-success: #52C41A;
  --color-warning: #FA8C16;
  --color-danger: #FF4D4F;
  --color-info: #1890FF;

  /* Neutral palette */
  --color-text-primary: #1F2937;
  --color-text-secondary: #6B7280;
  --color-text-disabled: #9CA3AF;
  --color-bg-page: #F5F6F8;
  --color-bg-card: #FFFFFF;
  --color-border: #E8ECF1;

  /* Sidebar */
  --sidebar-bg: #1B2838;
  --sidebar-hover: #243447;
  --sidebar-active: #2B7A78;
  --sidebar-text: #B8BEC8;
  --sidebar-text-active: #FFFFFF;
  --sidebar-submenu-bg: #131E2B;

  /* Shadows */
  --shadow-sm: 0 1px 2px rgba(0,0,0,0.05);
  --shadow-md: 0 4px 6px rgba(0,0,0,0.07);
  --shadow-lg: 0 10px 15px rgba(0,0,0,0.1);

  /* Spacing */
  --spacing-xs: 4px;
  --spacing-sm: 8px;
  --spacing-md: 16px;
  --spacing-lg: 24px;
  --spacing-xl: 32px;

  /* Border radius */
  --radius-sm: 4px;
  --radius-md: 8px;
  --radius-lg: 12px;
}

/* ===================================================================
 * Element Plus theme override
 * =================================================================== */
:root {
  --el-color-primary: #2B7A78;
  --el-color-primary-light-3: #3AAFA9;
  --el-color-primary-light-5: #7CCAC8;
  --el-color-primary-light-7: #B8E0DF;
  --el-color-primary-light-9: #E8F5F4;
  --el-color-primary-dark-2: #1F5F5B;
}

/* ===================================================================
 * Global table styling
 * =================================================================== */
.el-table {
  --el-table-header-bg-color: #F7F8FA;
  --el-table-row-hover-bg-color: #F0F7F7;
  border-radius: 8px;
  overflow: hidden;
}

.el-table th.el-table__cell {
  font-weight: 600;
  font-size: 13px;
  color: #6B7280;
}

.el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell {
  background: #FAFBFC;
}

.el-table__empty-block {
  min-height: 120px;
}

.el-table__empty-text {
  color: var(--color-text-secondary, #909399);
}

/* ===================================================================
 * Card styling
 * =================================================================== */
.el-card {
  border-radius: var(--radius-md);
  border-color: var(--color-border);
  box-shadow: var(--shadow-sm);
}

/* ===================================================================
 * Button primary override
 * =================================================================== */
.el-button--primary {
  --el-button-bg-color: #2B7A78;
  --el-button-border-color: #2B7A78;
  --el-button-hover-bg-color: #3AAFA9;
  --el-button-hover-border-color: #3AAFA9;
  --el-button-active-bg-color: #1F5F5B;
  --el-button-active-border-color: #1F5F5B;
}

/* ===================================================================
 * Page background
 * =================================================================== */
.app-main {
  background-color: var(--color-bg-page);
}

/* ===================================================================
 * Global scrollbar
 * =================================================================== */
::-webkit-scrollbar { width: 6px; height: 6px; }
::-webkit-scrollbar-thumb { background: #d9d9d9; border-radius: 3px; }
::-webkit-scrollbar-thumb:hover { background: #bfbfbf; }
::-webkit-scrollbar-track { background: transparent; }
</style>

