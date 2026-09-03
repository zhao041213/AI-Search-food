export const OPERATION_RESULT_OPTIONS = [
  { value: '', label: '全部结果' },
  { value: 'SUCCESS', label: '成功' },
  { value: 'FAILURE', label: '失败' }
]

const OPERATION_TYPE_LABELS = {
  ADMIN_LOGIN: '管理员登录',
  VIEW_DASHBOARD: '查看运营概览',
  VIEW_AI_CONFIG: '查看 AI 配置',
  UPDATE_AI_CONFIG: '更新 AI 配置',
  ADMIN_API_ACCESS: '访问管理接口'
}

export function normalizeAdminOperationLog(item = {}) {
  return {
    id: item.id ?? `${item.createdAt || 'log'}-${item.requestPath || 'unknown'}`,
    adminUsername: item.adminUsername || '未知账号',
    operationType: item.operationType || 'ADMIN_API_ACCESS',
    httpMethod: item.httpMethod || 'UNKNOWN',
    requestPath: item.requestPath || '未知接口',
    operationResult: item.operationResult === 'SUCCESS' ? 'SUCCESS' : 'FAILURE',
    statusCode: Number.isFinite(Number(item.statusCode)) ? Number(item.statusCode) : null,
    ipAddress: item.ipAddress || '暂无',
    createdAt: item.createdAt || ''
  }
}

export function normalizeAdminOperationLogPage(data = {}) {
  const items = Array.isArray(data.items) ? data.items.map(normalizeAdminOperationLog) : []
  const total = Number(data.total)
  return {
    items,
    total: Number.isFinite(total) && total >= 0 ? total : 0,
    limit: Number(data.limit) > 0 ? Number(data.limit) : 20,
    offset: Number(data.offset) >= 0 ? Number(data.offset) : 0
  }
}

export function operationTypeLabel(type) {
  return OPERATION_TYPE_LABELS[type] || '访问管理接口'
}

export function operationResultLabel(result) {
  return result === 'SUCCESS' ? '成功' : '失败'
}

export function operationResultTagType(result) {
  return result === 'SUCCESS' ? 'success' : 'danger'
}

export function formatOperationLogTime(value) {
  if (!value) return '暂无'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', { hour12: false })
}
