export const DASHBOARD_PERIOD_OPTIONS = [
  { value: '7d', label: '近 7 天' },
  { value: '30d', label: '近 30 天' }
]

const INPUT_SOURCE_LABELS = {
  text: '文字输入',
  image: '上传图片',
  camera: '摄像头',
  other: '其他'
}

export function normalizeDashboardOverview(value) {
  const source = value && typeof value === 'object' ? value : {}
  const metrics = source.metrics && typeof source.metrics === 'object' ? source.metrics : {}

  return {
    period: source.period === '30d' ? '30d' : '7d',
    generatedAt: typeof source.generatedAt === 'string' ? source.generatedAt : null,
    metrics: {
      newUserCount: nonNegativeNumber(metrics.newUserCount),
      generationCount: nonNegativeNumber(metrics.generationCount),
      savedRecipeCount: nonNegativeNumber(metrics.savedRecipeCount),
      reviewCount: nonNegativeNumber(metrics.reviewCount)
    },
    dailyTrend: normalizeDailyTrend(source.dailyTrend),
    inputSources: normalizeInputSources(source.inputSources),
    hotIngredients: normalizeHotIngredients(source.hotIngredients)
  }
}

export function inputSourceLabel(value) {
  const normalized = typeof value === 'string' ? value.trim().toLowerCase() : ''
  return INPUT_SOURCE_LABELS[normalized] || INPUT_SOURCE_LABELS.other
}

export function normalizeHotIngredientStats(value) {
  const source = value && typeof value === 'object' ? value : {}
  const items = Array.isArray(source.items) ? source.items : []

  return {
    totalSearches: nonNegativeNumber(source.totalSearches),
    totalIngredientOccurrences: nonNegativeNumber(source.totalIngredientOccurrences),
    generatedAt: typeof source.generatedAt === 'string' ? source.generatedAt : null,
    items: items
      .filter((item) => item && typeof item.name === 'string' && item.name.trim())
      .slice(0, 20)
      .map((item, index) => ({
        rank: positiveNumber(item.rank) || index + 1,
        name: item.name.trim(),
        searchCount: nonNegativeNumber(item.searchCount),
        share: nonNegativeNumber(item.share),
        latestSearchAt: typeof item.latestSearchAt === 'string' ? item.latestSearchAt : null
      }))
  }
}

export function dashboardInsight(overview) {
  const trend = overview?.dailyTrend || []
  const totalGenerations = trend.reduce((total, item) => total + item.generationCount, 0)
  const totalSavedRecipes = trend.reduce((total, item) => total + item.savedRecipeCount, 0)

  if (!totalGenerations) {
    return '当前周期暂无生成记录，生成菜谱后将展示运营趋势。'
  }

  const conversion = Math.round((totalSavedRecipes / totalGenerations) * 100)
  return `当前周期共生成 ${totalGenerations} 次菜谱，其中 ${conversion}% 被保存。`
}

function normalizeDailyTrend(value) {
  if (!Array.isArray(value)) {
    return []
  }

  return value
    .filter((item) => item && typeof item.date === 'string' && item.date)
    .map((item) => ({
      date: item.date,
      generationCount: nonNegativeNumber(item.generationCount),
      savedRecipeCount: nonNegativeNumber(item.savedRecipeCount)
    }))
}

function normalizeInputSources(value) {
  if (!Array.isArray(value)) {
    return []
  }

  return value
    .filter((item) => item && typeof item.inputType === 'string')
    .map((item) => ({
      inputType: item.inputType,
      label: inputSourceLabel(item.inputType),
      count: nonNegativeNumber(item.count)
    }))
}

function normalizeHotIngredients(value) {
  if (!Array.isArray(value)) {
    return []
  }

  return value
    .filter((item) => item && typeof item.name === 'string' && item.name.trim())
    .slice(0, 5)
    .map((item) => ({
      name: item.name.trim(),
      count: nonNegativeNumber(item.count)
    }))
}

function nonNegativeNumber(value) {
  const number = Number(value)
  return Number.isFinite(number) && number > 0 ? number : 0
}

function positiveNumber(value) {
  const number = Number(value)
  return Number.isFinite(number) && number > 0 ? number : 0
}
