export const ERROR_SOURCE_OPTIONS = [
  { value: '', label: '全部来源' },
  { value: 'BACKEND', label: '后端' },
  { value: 'AI', label: 'AI' },
  { value: 'DATABASE', label: '数据库' },
  { value: 'TOOL', label: 'Tool 调用' }
]

const SOURCE_LABELS = Object.fromEntries(
  ERROR_SOURCE_OPTIONS.filter((option) => option.value).map((option) => [option.value, option.label])
)

export function normalizeAdminErrorLog(item = {}) {
  const sourceType = Object.hasOwn(SOURCE_LABELS, item.sourceType) ? item.sourceType : 'BACKEND'
  const statusCode = Number(item.statusCode)
  return {
    id: item.id ?? `${item.createdAt || 'error'}-${item.component || 'unknown'}`,
    sourceType,
    severity: item.severity || 'ERROR',
    component: item.component || '未知组件',
    exceptionClass: item.exceptionClass || '未知异常',
    message: item.message || '未提供错误信息',
    rootCause: item.rootCause || '暂无根因信息',
    requestMethod: item.requestMethod || '—',
    requestPath: item.requestPath || '非 HTTP 请求',
    statusCode: Number.isFinite(statusCode) ? statusCode : null,
    userId: item.userId ?? null,
    adminId: item.adminId ?? null,
    ipAddress: item.ipAddress || '暂无',
    stackTrace: item.stackTrace || '',
    createdAt: item.createdAt || ''
  }
}

export function normalizeAdminErrorLogPage(data = {}) {
  const items = Array.isArray(data.items) ? data.items.map(normalizeAdminErrorLog) : []
  const total = Number(data.total)
  return {
    items,
    total: Number.isFinite(total) && total >= 0 ? total : 0,
    limit: Number(data.limit) > 0 ? Number(data.limit) : 20,
    offset: Number(data.offset) >= 0 ? Number(data.offset) : 0
  }
}

export function errorSourceLabel(sourceType) {
  return SOURCE_LABELS[sourceType] || '后端'
}

export function errorSourceTagType(sourceType) {
  return {
    BACKEND: 'danger',
    AI: 'warning',
    DATABASE: 'danger',
    TOOL: 'info'
  }[sourceType] || 'danger'
}

export function formatErrorLogTime(value) {
  if (!value) return '暂无时间'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', { hour12: false })
}
