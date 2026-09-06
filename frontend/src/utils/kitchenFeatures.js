import { Bell, Bookmark, CalendarDays, CircleAlert, ClipboardList, Flame, HeartPulse, LayoutDashboard, Package, Target, UserCircle, Users, Utensils } from 'lucide-vue-next'
import { defineAsyncComponent } from 'vue'

const lazy = (loader) => defineAsyncComponent(loader)

const featureComponents = {
  chef: lazy(() => import('../views/HomeView.vue')),
  recognition: lazy(() => import('../components/kitchen/IngredientRecognitionStation.vue')),
  history: lazy(() => import('../components/kitchen/RecentSearchStation.vue')),
  pantry: lazy(() => import('../views/PantryView.vue')),
  recipes: lazy(() => import('../views/SavedRecipesView.vue')),
  review: lazy(() => import('../components/kitchen/FinishedDishReviewStation.vue')),
  'health-profile': lazy(() => import('../views/HealthProfileView.vue')),
  'nutrition-targets': lazy(() => import('../views/NutritionTargetView.vue')),
  weekly: lazy(() => import('../views/WeeklyMenuView.vue')),
  hot: lazy(() => import('../views/HotIngredientsView.vue')),
  account: lazy(() => import('../views/UserAccountView.vue')),
  notifications: lazy(() => import('../views/NotificationsView.vue')),
  characters: lazy(() => import('../components/kitchen/CharacterRosterStation.vue')),
  'kitchen-overview': lazy(() => import('../views/KitchenOverviewView.vue'))
}

function defineFeature(featureId, definition) {
  return Object.freeze({
    featureId,
    id: featureId,
    component: featureComponents[featureId] || null,
    compatibleRoutes: Object.freeze([]),
    embeddedProps: Object.freeze({}),
    ...definition
  })
}

export const KITCHEN_FEATURES = Object.freeze({
  chef: defineFeature('chef', {
    title: '主厨料理大厅',
    role: 'AI 主厨',
    actorId: 'chef',
    icon: Utensils,
    sceneIcon: '✦',
    accent: '#d6a43b',
    description: '输入现有食材，生成并保存专属菜谱。',
    requiresUser: false,
    stationId: 'chef',
    directStation: true,
    embeddedProps: Object.freeze({ embedded: true })
  }),
  recognition: defineFeature('recognition', {
    title: '食材识别台',
    role: '食材识别员',
    actorId: 'chef-helper',
    icon: Package,
    sceneIcon: '◫',
    accent: '#79a9a1',
    description: '通过上传或拍照识别食材，再交给阿灶生成菜谱。',
    requiresUser: false,
    stationId: 'recognition',
    directStation: true
  }),
  history: defineFeature('history', {
    title: '菜谱生成记录',
    role: '生成记录员',
    actorId: 'chef-recipes',
    icon: ClipboardList,
    sceneIcon: '◷',
    accent: '#b083c7',
    description: '找回最近的食材条件、餐次和生成目标。',
    requiresUser: true,
    stationId: 'history',
    directStation: true
  }),
  pantry: defineFeature('pantry', {
    title: '我的食材',
    windowTitle: '食材储藏室',
    role: '食材管家',
    actorId: 'pantry',
    icon: Package,
    sceneIcon: '▣',
    accent: '#68a873',
    description: '管理库存、临期提醒和库存变动记录。',
    requiresUser: true,
    stationId: 'pantry',
    directStation: true,
    compatibleRoutes: Object.freeze(['/pantry']),
    navigation: Object.freeze({ area: 'sidebar', order: 20, label: '我的食材', visibleFor: 'user', stationId: 'pantry' })
  }),
  recipes: defineFeature('recipes', {
    title: '我的菜谱',
    windowTitle: '菜谱书房',
    role: '菜谱管理员',
    actorId: 'recipes',
    icon: Bookmark,
    sceneIcon: '▤',
    accent: '#b083c7',
    description: '整理已保存菜谱、收藏夹、标签和分享记录。',
    requiresUser: true,
    stationId: 'recipes',
    directStation: true,
    compatibleRoutes: Object.freeze(['/recipes/saved']),
    navigation: Object.freeze({ area: 'sidebar', order: 10, label: '我的菜谱', visibleFor: 'user', stationId: 'recipes' })
  }),
  review: defineFeature('review', {
    title: '成品品鉴台',
    role: '成品品鉴员',
    actorId: 'review',
    icon: CircleAlert,
    sceneIcon: '◆',
    accent: '#c87a8a',
    description: '直接选择已保存菜谱，上传成品并查看品鉴记录。',
    requiresUser: true,
    stationId: 'review',
    directStation: true
  }),
  'health-profile': defineFeature('health-profile', {
    title: '健康档案',
    role: '营养咨询室',
    actorId: 'nutrition',
    icon: HeartPulse,
    sceneIcon: '♥',
    accent: '#e2816c',
    description: '维护用于个性化建议的基础身体指标。',
    requiresUser: true,
    stationId: 'nutrition',
    compatibleRoutes: Object.freeze(['/health-profile']),
    navigation: Object.freeze({ area: 'sidebar', order: 30, label: '健康档案', visibleFor: 'user', stationId: 'nutrition' })
  }),
  'nutrition-targets': defineFeature('nutrition-targets', {
    title: '营养目标',
    role: '营养咨询室',
    actorId: 'nutrition',
    icon: Target,
    sceneIcon: '◎',
    accent: '#e2816c',
    description: '设置热量和主要营养素的每日参考目标。',
    requiresUser: true,
    stationId: 'nutrition',
    compatibleRoutes: Object.freeze(['/nutrition-targets']),
    navigation: Object.freeze({ area: 'sidebar', order: 80, label: '营养目标', visibleFor: 'user', stationId: 'nutrition' })
  }),
  'diet-preference': defineFeature('diet-preference', {
    title: '饮食偏好',
    role: '饮食偏好顾问',
    actorId: 'chef-nutrition',
    icon: HeartPulse,
    sceneIcon: '◇',
    accent: '#e2816c',
    description: '设置默认目标、口味、忌口和过敏食材。',
    requiresUser: true,
    stationId: 'diet-preference',
    directStation: true,
    component: null
  }),
  weekly: defineFeature('weekly', {
    title: '一周菜单',
    windowTitle: '菜单计划室',
    role: '菜单规划师',
    actorId: 'weekly',
    icon: CalendarDays,
    sceneIcon: '▦',
    accent: '#6d9cc3',
    description: '安排每日餐次并汇总采购清单。',
    requiresUser: true,
    stationId: 'weekly',
    directStation: true,
    compatibleRoutes: Object.freeze(['/weekly-menu']),
    embeddedProps: Object.freeze({ embedded: true }),
    navigation: Object.freeze({ area: 'sidebar', order: 70, label: '一周菜单', visibleFor: 'user', stationId: 'weekly' })
  }),
  hot: defineFeature('hot', {
    title: '热门食材',
    windowTitle: '美食情报站',
    role: '市场观察员',
    actorId: 'hot',
    icon: Flame,
    sceneIcon: '♨',
    accent: '#d48c52',
    description: '查看近期热门食材和搜索趋势。',
    requiresUser: false,
    stationId: 'hot',
    directStation: true,
    compatibleRoutes: Object.freeze(['/stats/hot-ingredients']),
    embeddedProps: Object.freeze({ embedded: true }),
    navigation: Object.freeze({ area: 'sidebar', order: 60, label: '热门食材', visibleFor: 'guest-or-user', stationId: 'hot' })
  }),
  account: defineFeature('account', {
    title: '账号中心',
    windowTitle: '厨房服务台',
    role: '厨房管家',
    actorId: 'account',
    icon: UserCircle,
    sceneIcon: '◈',
    accent: '#8f8aa8',
    description: '维护个人资料、头像和账号安全。',
    requiresUser: true,
    stationId: 'account',
    directStation: true,
    compatibleRoutes: Object.freeze(['/account']),
    navigation: Object.freeze({ area: 'sidebar', order: 40, label: '账号中心', visibleFor: 'user', stationId: 'account' }),
    headerAction: true
  }),
  notifications: defineFeature('notifications', {
    title: '通知中心',
    role: '厨房服务台',
    actorId: 'account',
    icon: Bell,
    sceneIcon: '●',
    accent: '#8f8aa8',
    description: '处理库存提醒、菜单通知和提醒偏好。',
    requiresUser: true,
    stationId: 'account',
    compatibleRoutes: Object.freeze(['/notifications']),
    embeddedProps: Object.freeze({ embedded: true }),
    headerAction: true
  }),
  characters: defineFeature('characters', {
    title: '人物名册',
    role: '厨房角色管理',
    icon: Users,
    sceneIcon: '◇',
    accent: '#9a7445',
    description: '修改厨房伙伴的显示名称，不改变人物职责。',
    requiresUser: true,
    component: featureComponents.characters,
    navigation: Object.freeze({ area: 'sidebar', order: 90, label: '人物名册', visibleFor: 'user' })
  }),
  'kitchen-overview': defineFeature('kitchen-overview', {
    title: '今日厨房',
    role: '厨房总览',
    icon: LayoutDashboard,
    sceneIcon: '▥',
    accent: '#4f8ca5',
    description: '把库存、菜单和提醒集中在一处，快速找到今天的下一步。',
    requiresUser: true,
    embeddedProps: Object.freeze({ embedded: true }),
    compatibleRoutes: Object.freeze(['/kitchen-overview']),
    navigation: Object.freeze({ area: 'sidebar', order: 5, label: '今日厨房', visibleFor: 'user' })
  })
})

function stationFromFeature(featureId, overrides) {
  const feature = KITCHEN_FEATURES[featureId]
  return {
    id: feature.stationId,
    actorId: feature.actorId,
    requiresUser: feature.requiresUser,
    title: feature.windowTitle || feature.title,
    role: feature.role,
    icon: feature.icon,
    sceneIcon: feature.sceneIcon,
    accent: feature.accent,
    ...overrides
  }
}

function stationEntry(featureId, label, description, overrides = {}) {
  return {
    label,
    description,
    feature: featureId,
    icon: KITCHEN_FEATURES[featureId]?.icon,
    ...overrides
  }
}

const stationDefinitions = {
  chef: stationFromFeature('chef', {
    description: '集中生成、识别和找回 AI 菜谱。',
    directFeatureId: 'chef',
    entries: [],
    preview: []
  }),
  recognition: stationFromFeature('recognition', {
    directFeatureId: 'recognition',
    entries: [],
    preview: []
  }),
  history: stationFromFeature('history', {
    directFeatureId: 'history',
    entries: [],
    preview: []
  }),
  pantry: stationFromFeature('pantry', {
    description: '集中管理冰箱库存、保质期和烹饪消耗，生成菜谱时会自动参与推荐。',
    directFeatureId: 'pantry',
    entries: [
      stationEntry('pantry', '管理食材库存', '查看库存、临期提醒、入库、消耗和撤销记录。')
    ],
    preview: [
      { value: '库存', label: '自动参与推荐' },
      { value: '临期', label: '及时提醒' },
      { value: '入库', label: '操作可追溯' }
    ]
  }),
  recipes: stationFromFeature('recipes', {
    description: '重新打开已保存的 AI 菜谱，整理收藏夹、标签和分享记录。',
    directFeatureId: 'recipes',
    entries: [
      stationEntry('recipes', '打开我的菜谱', '查看已保存菜谱、收藏夹、标签和分享记录。')
    ],
    preview: [
      { value: '保存', label: '不重复调用模型' },
      { value: '标签', label: '方便分类查找' },
      { value: '分享', label: '生成公开链接' }
    ]
  }),
  review: stationFromFeature('review', {
    directFeatureId: 'review',
    entries: [],
    preview: []
  }),
  account: stationFromFeature('account', {
    directFeatureId: 'account',
    entries: [],
    preview: []
  }),
  nutrition: stationFromFeature('health-profile', {
    id: 'nutrition',
    title: '营养咨询室',
    role: '营养师',
    description: '维护基础健康档案和每日营养目标，为菜谱搭配提供一般饮食参考。',
    directFeatureId: '',
    entries: [
      stationEntry('health-profile', '健康档案', '维护身高、体重、年龄和基础身体指标。'),
      stationEntry('nutrition-targets', '营养目标', '设置每日热量、蛋白质、碳水和脂肪目标。'),
      stationEntry('diet-preference', '饮食偏好', '设置默认目标、口味、忌口和过敏食材。', { feature: undefined, action: 'diet-preference' })
    ],
    preview: [
      { value: 'BMI', label: '基础指标' },
      { value: '目标', label: '每日营养参考' },
      { value: 'AI', label: '参与菜谱建议' }
    ]
  }),
  'diet-preference': stationFromFeature('diet-preference', {
    directFeatureId: 'diet-preference',
    entries: [],
    preview: []
  }),
  weekly: stationFromFeature('weekly', {
    description: '从已保存菜谱中安排一周餐次，并自动汇总采购清单。',
    directFeatureId: 'weekly',
    entries: [
      stationEntry('weekly', '安排一周菜单', '编排每日餐次，并汇总需要采购的食材。')
    ],
    preview: [
      { value: '21', label: '个餐次位置' },
      { value: 'AI', label: '自动安排' },
      { value: '清单', label: '自动汇总' }
    ]
  }),
  hot: stationFromFeature('hot', {
    description: '观察全站食材搜索趋势，发现大家最近正在寻找什么。',
    directFeatureId: 'hot',
    entries: [
      stationEntry('hot', '查看热门食材', '查看 7 天和 30 天的食材搜索趋势。')
    ],
    preview: [
      { value: '7天', label: '短期趋势' },
      { value: '30天', label: '长期趋势' },
      { value: '排行', label: '食材热度' }
    ]
  })
}

export const KITCHEN_STATIONS = Object.freeze(
  Object.fromEntries(Object.entries(stationDefinitions).map(([id, station]) => [id, Object.freeze(station)]))
)

export const KITCHEN_GUIDE_STATIONS = Object.freeze(['chef', 'pantry', 'recipes', 'nutrition', 'weekly', 'hot'])

const featureRouteMap = Object.freeze(
  Object.fromEntries(
    Object.entries(KITCHEN_FEATURES).flatMap(([featureId, feature]) =>
      feature.compatibleRoutes.map((route) => [route, featureId])
    )
  )
)

export function getKitchenFeature(featureId) {
  return KITCHEN_FEATURES[normalizeFeatureId(featureId)] || null
}

export function getKitchenStation(stationId) {
  return KITCHEN_STATIONS[normalizeStationId(stationId)] || null
}

export function getFeatureIdForRoute(path) {
  const value = Array.isArray(path) ? path[0] : path
  return featureRouteMap[value] || ''
}

export function getFeatureIdForStation(stationId) {
  const candidate = Array.isArray(stationId) ? stationId[0] : stationId
  if (typeof candidate !== 'string') return ''
  const stationFeatureId = getKitchenStation(candidate)?.directFeatureId
  if (stationFeatureId) return stationFeatureId
  return Object.values(KITCHEN_FEATURES)
    .find((feature) => feature.directStation && feature.stationId === candidate)?.featureId || ''
}

export function getKitchenNavigationTarget(featureId) {
  const feature = getKitchenFeature(featureId)
  if (!feature) return null
  const stationId = normalizeStationId(feature.navigation?.stationId)
  return stationId
    ? { type: 'station', stationId, featureId: feature.featureId }
    : { type: 'feature', featureId: feature.featureId }
}

export function isSameKitchenFeature(currentFeatureId, nextFeatureId) {
  const current = normalizeFeatureId(currentFeatureId)
  const next = normalizeFeatureId(nextFeatureId)
  return Boolean(current && next && current === next)
}

export function getKitchenNavigation(area, role) {
  return Object.values(KITCHEN_FEATURES)
    .filter((feature) => feature.navigation?.area === area)
    .filter((feature) => feature.navigation.visibleFor === 'user' ? role === 'USER' : role !== 'ADMIN')
    .sort((left, right) => left.navigation.order - right.navigation.order)
}

export function getKitchenGuideItems() {
  return KITCHEN_GUIDE_STATIONS.map((stationId) => {
    const station = getKitchenStation(stationId)
    return {
      id: station.id,
      title: station.title,
      role: station.role,
      icon: station.icon,
      sceneIcon: station.sceneIcon,
      accent: station.accent
    }
  })
}

export function isKitchenNavigationActive(featureId, query = {}) {
  const target = getKitchenNavigationTarget(featureId)
  if (!target) return false

  const activeFeatureId = parseKitchenFeature(query)
  const activeStationId = parseKitchenStation(query)
  if (target.type === 'feature') return activeFeatureId === target.featureId
  if (activeFeatureId) return activeFeatureId === target.featureId
  return activeStationId === target.stationId && getFeatureIdForStation(activeStationId) === target.featureId
}

export function normalizeFeatureId(value) {
  const candidate = Array.isArray(value) ? value[0] : value
  return typeof candidate === 'string' && KITCHEN_FEATURES[candidate] ? candidate : ''
}

export function normalizeStationId(value) {
  const candidate = Array.isArray(value) ? value[0] : value
  return typeof candidate === 'string' && KITCHEN_STATIONS[candidate] ? candidate : ''
}

export function parseKitchenFeature(query = {}) {
  return normalizeFeatureId(query.feature)
}

export function parseKitchenStation(query = {}) {
  return normalizeStationId(query.station)
}

export function kitchenFeatureLocation(featureId, currentQuery = {}) {
  const query = { ...currentQuery }
  delete query.feature
  delete query.station
  const normalized = normalizeFeatureId(featureId)
  if (normalized) query.feature = normalized
  return { name: 'home', query }
}

export function kitchenCanonicalFeatureLocation(featureId, currentQuery = {}) {
  const feature = getKitchenFeature(featureId)
  if (feature?.directStation && feature.stationId) {
    return kitchenStationLocation(feature.stationId, currentQuery)
  }
  return kitchenFeatureLocation(featureId, currentQuery)
}

export function kitchenNavigationLocation(featureId, currentQuery = {}) {
  const target = getKitchenNavigationTarget(featureId)
  if (!target) return kitchenWorldLocation(currentQuery)
  return target.type === 'station'
    ? kitchenStationLocation(target.stationId, currentQuery)
    : kitchenFeatureLocation(target.featureId, currentQuery)
}

export function kitchenStationLocation(stationId, currentQuery = {}) {
  const query = { ...currentQuery }
  delete query.feature
  delete query.station
  const normalized = normalizeStationId(stationId)
  if (normalized) query.station = normalized
  return { name: 'home', query }
}

export function kitchenWorldLocation(currentQuery = {}) {
  const query = { ...currentQuery }
  delete query.feature
  delete query.station
  return { name: 'home', query }
}

export function stripKitchenQuery(query = {}) {
  const nextQuery = { ...query }
  delete nextQuery.feature
  delete nextQuery.station
  return nextQuery
}
