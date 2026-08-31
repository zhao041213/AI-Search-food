export const EXPIRY_WARNING_DAYS = 7

const DAY_MS = 24 * 60 * 60 * 1000

export function getDaysUntilExpiry(expireDate, today = new Date()) {
  const target = parseDateOnly(expireDate)
  const reference = parseDateOnly(today)
  if (!target || !reference) {
    return null
  }
  return Math.round((target.getTime() - reference.getTime()) / DAY_MS)
}

export function getExpiryStatus(expireDate, today = new Date()) {
  const days = getDaysUntilExpiry(expireDate, today)
  if (days === null) {
    return 'missing'
  }
  if (days < 0) {
    return 'expired'
  }
  if (days <= EXPIRY_WARNING_DAYS) {
    return 'soon'
  }
  return 'normal'
}

export function getExpiryLabel(expireDate, today = new Date()) {
  const status = getExpiryStatus(expireDate, today)
  if (status === 'missing') {
    return '未填写'
  }
  if (status === 'expired') {
    return '已过期'
  }
  if (status === 'soon') {
    const days = getDaysUntilExpiry(expireDate, today)
    if (days === 0) {
      return '今天到期'
    }
    if (days === 1) {
      return '明天到期'
    }
    return `${days}天内到期`
  }
  return '正常'
}

export function getExpiryClass(expireDate, today = new Date()) {
  return getExpiryStatus(expireDate, today)
}

export function summarizePantryExpiry(items, today = new Date()) {
  const statusCounts = {
    expired: 0,
    soon: 0,
    normal: 0,
    missing: 0
  }
  const expiredItems = []
  const expiringSoonItems = []

  for (const item of Array.isArray(items) ? items : []) {
    const status = getExpiryStatus(item?.expireDate, today)
    statusCounts[status] += 1
    if (!hasAvailableQuantity(item)) {
      continue
    }
    if (status === 'expired') {
      expiredItems.push(item)
    } else if (status === 'soon') {
      expiringSoonItems.push(item)
    }
  }

  return {
    total: Array.isArray(items) ? items.length : 0,
    statusCounts,
    expiredItems,
    expiringSoonItems
  }
}

export function hasAvailableQuantity(item) {
  const quantity = Number(item?.quantity)
  return Number.isFinite(quantity) && quantity > 0
}

function parseDateOnly(value) {
  if (value instanceof Date) {
    if (Number.isNaN(value.getTime())) {
      return null
    }
    return new Date(value.getFullYear(), value.getMonth(), value.getDate())
  }

  if (typeof value !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(value)) {
    return null
  }

  const [year, month, day] = value.split('-').map(Number)
  const date = new Date(year, month - 1, day)
  if (date.getFullYear() !== year || date.getMonth() !== month - 1 || date.getDate() !== day) {
    return null
  }
  return date
}
