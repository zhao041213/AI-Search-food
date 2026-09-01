const RANGES = Object.freeze({
  servings: [1, 20],
  caloriesKcal: [1, 3000],
  proteinG: [0, 300],
  fatG: [0, 300],
  carbohydrateG: [0, 500]
})

export const NUTRITION_DISCLOSURE = '数据由 AI 估算，仅供一般饮食参考。'

export function normalizeNutritionEstimate(value) {
  if (!value || String(value.source || '') !== 'AI_ESTIMATE') {
    return null
  }
  const normalized = {
    servings: toNumber(value.servings),
    caloriesKcal: toNumber(value.caloriesKcal),
    proteinG: toNumber(value.proteinG),
    fatG: toNumber(value.fatG),
    carbohydrateG: toNumber(value.carbohydrateG),
    source: 'AI_ESTIMATE'
  }
  return Object.entries(RANGES).every(([field, [min, max]]) => {
    const number = normalized[field]
    return Number.isFinite(number) && number >= min && number <= max
  }) ? normalized : null
}

export function hasNutritionEstimate(value) {
  return normalizeNutritionEstimate(value) !== null
}

export function formatNutritionValue(value) {
  const number = toNumber(value)
  if (!Number.isFinite(number)) {
    return '—'
  }
  return number.toLocaleString('zh-CN', {
    maximumFractionDigits: 2,
    minimumFractionDigits: 0
  })
}

export function normalizeNutritionSummary(value) {
  const daily = Array.isArray(value?.daily) ? value.daily.map((item) => ({
    date: item?.date || '',
    assignedMealCount: toNonNegativeInteger(item?.assignedMealCount),
    validEstimateMealCount: toNonNegativeInteger(item?.validEstimateMealCount),
    totals: normalizeTotals(item?.totals)
  })) : []
  return {
    assignedMealCount: toNonNegativeInteger(value?.assignedMealCount),
    validEstimateMealCount: toNonNegativeInteger(value?.validEstimateMealCount),
    daily,
    weekly: normalizeTotals(value?.weekly)
  }
}

export function normalizeTotals(value) {
  if (!value) {
    return null
  }
  const totals = {
    caloriesKcal: toNumber(value.caloriesKcal),
    proteinG: toNumber(value.proteinG),
    fatG: toNumber(value.fatG),
    carbohydrateG: toNumber(value.carbohydrateG)
  }
  return Object.values(totals).every(Number.isFinite) ? totals : null
}

function toNumber(value) {
  if (typeof value === 'number') {
    return value
  }
  if (typeof value === 'string' && value.trim() !== '') {
    return Number(value)
  }
  return Number.NaN
}

function toNonNegativeInteger(value) {
  const number = toNumber(value)
  return Number.isInteger(number) && number >= 0 ? number : 0
}
