import { mealLabel } from './weeklyMenu.js'

const MAX_WEEKLY_MEALS = 21
const COMPLETE_SHOPPING_STATUSES = new Set(['READY', 'PURCHASED', 'COMPLETED', 'DONE'])
const MEAL_ORDER = { BREAKFAST: 0, LUNCH: 1, DINNER: 2 }

export function buildKitchenOverview(raw = {}, referenceDate = new Date()) {
  const today = toDateKey(referenceDate) || toDateKey(new Date())
  const pantryItems = normalizeItems(raw.pantryItems)
  const expiryAlerts = raw.expiryAlerts && typeof raw.expiryAlerts === 'object' ? raw.expiryAlerts : {}
  const derivedExpiry = deriveExpiryAlerts(pantryItems, today)
  const expiredItems = uniqueItems(
    Array.isArray(expiryAlerts.expiredItems) ? expiryAlerts.expiredItems : derivedExpiry.expiredItems
  )
  const expiringSoonItems = uniqueItems(
    Array.isArray(expiryAlerts.expiringSoonItems) ? expiryAlerts.expiringSoonItems : derivedExpiry.expiringSoonItems
  )

  const weeklyMenu = normalizeWeeklyMenu(raw.weeklyMenu)
  const todayMeals = weeklyMenu.items
    .filter((item) => item.menuDate === today)
    .sort((left, right) => (MEAL_ORDER[left.mealType] ?? 99) - (MEAL_ORDER[right.mealType] ?? 99))
    .map((item) => ({ ...item, label: mealLabel(item.mealType) }))

  const shoppingItems = normalizeShoppingItems(weeklyMenu.shoppingItems)
  const pendingShoppingItems = shoppingItems.filter((item) => !isShoppingComplete(item))
  const recentSearches = normalizeRecentSearches(raw.recentSearches)
  const notificationData = normalizeNotificationData(raw.notifications)
  const unreadCount = normalizeCount(
    raw.unreadNotificationCount,
    notificationData.items.filter((item) => item.status === 'UNREAD').length
  )

  return {
    today,
    pantryItems,
    pantryCount: pantryItems.length,
    expiredItems,
    expiringSoonItems,
    attentionCount: expiredItems.length + expiringSoonItems.length,
    weeklyAssignedCount: weeklyMenu.items.length,
    weeklyProgress: Math.min(100, Math.round((weeklyMenu.items.length / MAX_WEEKLY_MEALS) * 100)),
    todayMeals,
    shoppingItems,
    pendingShoppingItems,
    recentSearches,
    notifications: notificationData.items,
    unreadCount,
    priorityTasks: buildPriorityTasks({
      expiredItems,
      expiringSoonItems,
      todayMeals,
      pendingShoppingItems,
      unreadCount
    })
  }
}

export function toDateKey(value) {
  if (value instanceof Date && !Number.isNaN(value.getTime())) {
    return formatDateKey(value)
  }
  if (typeof value !== 'string') {
    return ''
  }
  const match = /^(\d{4})-(\d{2})-(\d{2})/.exec(value.trim())
  return match ? `${match[1]}-${match[2]}-${match[3]}` : ''
}

function normalizeItems(value) {
  return (Array.isArray(value) ? value : [])
    .map((item) => {
      if (typeof item === 'string') {
        return { id: null, ingredientName: item.trim(), expireDate: '' }
      }
      return {
        ...item,
        id: item?.id ?? null,
        ingredientName: String(item?.ingredientName ?? item?.name ?? '').trim(),
        expireDate: toDateKey(item?.expireDate)
      }
    })
    .filter((item) => item.ingredientName)
}

function normalizeWeeklyMenu(value) {
  const source = value && typeof value === 'object' ? value : {}
  const items = (Array.isArray(source.items) ? source.items : [])
    .map((item) => ({
      id: item?.id ?? null,
      menuDate: toDateKey(item?.menuDate),
      mealType: String(item?.mealType || '').trim().toUpperCase(),
      recipeId: item?.recipeId ?? null,
      recipeTitle: String(item?.recipeTitle || '').trim() || '未命名菜谱'
    }))
    .filter((item) => item.menuDate && Object.prototype.hasOwnProperty.call(MEAL_ORDER, item.mealType))

  return {
    items,
    shoppingItems: Array.isArray(source.shoppingItems) ? source.shoppingItems : []
  }
}

function normalizeShoppingItems(value) {
  return value
    .map((item) => ({
      ...item,
      ingredientName: String(item?.ingredientName ?? item?.name ?? '').trim(),
      amount: String(item?.amount || '').trim(),
      status: String(item?.status || (item?.alreadyOwned ? 'READY' : 'PENDING')).trim().toUpperCase(),
      alreadyOwned: Boolean(item?.alreadyOwned)
    }))
    .filter((item) => item.ingredientName)
}

function normalizeRecentSearches(value) {
  const items = Array.isArray(value) ? value : Array.isArray(value?.items) ? value.items : []
  return items
    .map((item) => ({
      id: item?.id ?? null,
      ingredients: String(item?.ingredients || '').trim(),
      mealType: String(item?.mealType || '').trim(),
      goal: String(item?.goal || '').trim(),
      searchMode: String(item?.searchMode || '').trim(),
      createdAt: item?.createdAt || null
    }))
    .filter((item) => item.ingredients || item.mealType || item.goal)
    .slice(0, 4)
}

function normalizeNotificationData(value) {
  const items = Array.isArray(value) ? value : Array.isArray(value?.items) ? value.items : []
  return {
    items: items
      .map((item) => ({
        ...item,
        id: item?.id ?? null,
        title: String(item?.title || '未命名提醒').trim(),
        summary: String(item?.summary || item?.content || '').trim(),
        status: String(item?.status || '').trim().toUpperCase(),
        createdAt: item?.createdAt || null,
        targetPath: item?.targetPath || ''
      }))
      .filter((item) => item.title)
      .slice(0, 4)
  }
}

function deriveExpiryAlerts(items, today) {
  const todayTime = dateTime(today)
  const soonTime = todayTime + 7 * 24 * 60 * 60 * 1000
  return items.reduce((result, item) => {
    const expireTime = dateTime(item.expireDate)
    if (expireTime === null) {
      return result
    }
    if (expireTime <= todayTime) {
      result.expiredItems.push(item)
    } else if (expireTime <= soonTime) {
      result.expiringSoonItems.push(item)
    }
    return result
  }, { expiredItems: [], expiringSoonItems: [] })
}

function buildPriorityTasks({ expiredItems, expiringSoonItems, todayMeals, pendingShoppingItems, unreadCount }) {
  const tasks = []
  if (expiredItems.length) {
    tasks.push({
      id: 'expired-pantry',
      type: 'expired',
      title: '处理过期食材',
      detail: `${formatItemNames(expiredItems)}${expiredItems.length > 2 ? '等' : ''}，建议先检查并处理`,
      to: '/pantry'
    })
  }
  if (expiringSoonItems.length) {
    tasks.push({
      id: 'expiring-pantry',
      type: 'expiringSoon',
      title: '优先使用临期食材',
      detail: `${expiringSoonItems.length} 项食材将在 7 天内到期`,
      to: '/pantry'
    })
  }
  if (!todayMeals.length) {
    tasks.push({
      id: 'today-menu',
      type: 'menu',
      title: '安排今天的菜单',
      detail: '还没有今天的餐次，可以先从一周菜单开始安排',
      to: '/weekly-menu'
    })
  }
  if (pendingShoppingItems.length) {
    tasks.push({
      id: 'shopping-list',
      type: 'shopping',
      title: '补齐采购清单',
      detail: `本周还有 ${pendingShoppingItems.length} 项食材待采购`,
      to: '/weekly-menu'
    })
  }
  if (unreadCount) {
    tasks.push({
      id: 'notifications',
      type: 'notifications',
      title: '查看未读提醒',
      detail: `消息中心有 ${unreadCount} 条未读提醒`,
      to: '/notifications'
    })
  }
  return tasks.length ? tasks.slice(0, 4) : [{
    id: 'all-clear',
    type: 'clear',
    title: '今天安排得不错',
    detail: '暂时没有需要优先处理的厨房事项',
    to: '/weekly-menu'
  }]
}

function uniqueItems(items) {
  const seen = new Set()
  return normalizeItems(items).filter((item) => {
    const key = item.id ?? item.ingredientName
    if (seen.has(key)) return false
    seen.add(key)
    return true
  })
}

function formatItemNames(items) {
  return items.slice(0, 2).map((item) => item.ingredientName).join('、')
}

function isShoppingComplete(item) {
  return item.alreadyOwned || COMPLETE_SHOPPING_STATUSES.has(item.status)
}

function normalizeCount(value, fallback = 0) {
  if (value === null || value === undefined || value === '') {
    return fallback
  }
  const count = Number(value)
  return Number.isFinite(count) && count >= 0 ? count : fallback
}

function dateTime(value) {
  const key = toDateKey(value)
  if (!key) return null
  const time = new Date(`${key}T00:00:00`).getTime()
  return Number.isNaN(time) ? null : time
}

function formatDateKey(date) {
  return [date.getFullYear(), date.getMonth() + 1, date.getDate()]
    .map((part, index) => index === 0 ? String(part).padStart(4, '0') : String(part).padStart(2, '0'))
    .join('-')
}
