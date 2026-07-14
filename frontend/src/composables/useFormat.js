/**
 * 通用格式化工具函数
 */
export function useFormat() {
  /**
   * 格式化金额（千分位 + 两位小数）
   */
  function formatMoney(val) {
    if (val === null || val === undefined || val === '') return '0.00'
    const num = Number(val)
    if (isNaN(num)) return '0.00'
    return num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  }

  /**
   * 供应类别标签转换
   */
  function supplyCategoryLabel(cat) {
    const map = {
      'SOCIAL': '社会化',
      'CENTRALIZED': '集中供养'
    }
    return map[cat] || cat || '-'
  }

  return { formatMoney, supplyCategoryLabel }
}
