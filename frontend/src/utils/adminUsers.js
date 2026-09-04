export const ADMIN_USER_PAGE_SIZE = 20

export const ADMIN_USER_ENABLED_OPTIONS = [
  { value: '', label: '全部状态' },
  { value: true, label: '正常' },
  { value: false, label: '已禁用' }
]

export const ADMIN_USER_LOCKED_OPTIONS = [
  { value: '', label: '全部锁定状态' },
  { value: true, label: '已锁定' },
  { value: false, label: '未锁定' }
]

export function buildAdminUserParams({ keyword = '', enabled = '', locked = '', limit = ADMIN_USER_PAGE_SIZE, offset = 0 } = {}) {
  return {
    keyword: keyword.trim() || undefined,
    enabled: enabled === '' ? undefined : enabled,
    locked: locked === '' ? undefined : locked,
    limit,
    offset
  }
}

export function normalizeAdminUser(item = {}) {
  return {
    id: item.id ?? `user-${item.nickname || 'unknown'}`,
    nickname: item.nickname || '未设置昵称',
    phone: maskPhone(item.phone),
    avatarUrl: typeof item.avatarUrl === 'string' && item.avatarUrl ? item.avatarUrl : '',
    enabled: item.enabled === true || item.enabled === 1 || item.enabled === 'true',
    passwordLocked: item.passwordLocked === true || item.passwordLocked === 1 || item.passwordLocked === 'true',
    createdAt: item.createdAt || '',
    lastLoginAt: item.lastLoginAt || ''
  }
}

export function normalizeAdminUserPage(data = {}) {
  const items = Array.isArray(data.items) ? data.items.map(normalizeAdminUser) : []
  const total = Number(data.total)
  return {
    items,
    total: Number.isFinite(total) && total >= 0 ? total : 0,
    limit: Number(data.limit) > 0 ? Number(data.limit) : ADMIN_USER_PAGE_SIZE,
    offset: Number(data.offset) >= 0 ? Number(data.offset) : 0
  }
}

export function maskPhone(phone) {
  if (!phone) return '暂无'
  const normalized = String(phone).trim()
  if (/^\d{11}$/.test(normalized)) {
    return `${normalized.slice(0, 3)}****${normalized.slice(-4)}`
  }
  return normalized.length > 7 ? `${normalized.slice(0, 3)}****${normalized.slice(-4)}` : '***'
}

export function formatAdminUserTime(value) {
  if (!value) return '暂无登录记录'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleString('zh-CN', { hour12: false })
}

export function adminUserStatusLabel(enabled) {
  return enabled ? '正常' : '已禁用'
}

export function adminUserStatusType(enabled) {
  return enabled ? 'success' : 'info'
}

export function passwordLockLabel(locked) {
  return locked ? '已锁定' : '未锁定'
}

export function passwordLockType(locked) {
  return locked ? 'warning' : 'info'
}
