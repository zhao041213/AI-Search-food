export const NUTRITION_TARGET_FIELDS = Object.freeze([
  { key: 'caloriesKcal', label: '热量', unit: '千卡', max: 10000 },
  { key: 'proteinG', label: '蛋白质', unit: '克', max: 1000 },
  { key: 'fatG', label: '脂肪', unit: '克', max: 1000 },
  { key: 'carbohydrateG', label: '碳水', unit: '克', max: 1000 }
])

export function emptyNutritionTarget() {
  return {
    configured: false,
    enabled: false,
    caloriesKcal: null,
    proteinG: null,
    fatG: null,
    carbohydrateG: null,
    updatedAt: null
  }
}

export function normalizeNutritionTarget(value) {
  const configured = Boolean(value?.configured)
  const enabled = configured && value?.enabled === true
  const normalized = emptyNutritionTarget()
  normalized.configured = configured
  normalized.enabled = enabled
  normalized.updatedAt = value?.updatedAt || null
  NUTRITION_TARGET_FIELDS.forEach(({ key }) => {
    const number = toNumber(value?.[key])
    normalized[key] = Number.isFinite(number) && number > 0 ? number : null
  })
  if (!enabled) {
    NUTRITION_TARGET_FIELDS.forEach(({ key }) => {
      normalized[key] = null
    })
  }
  return normalized
}

export function buildNutritionTargetPayload(value) {
  const enabled = value?.enabled === true
  return {
    enabled,
    ...Object.fromEntries(NUTRITION_TARGET_FIELDS.map(({ key }) => [
      key,
      enabled ? toNumberOrNull(value?.[key]) : null
    ]))
  }
}

export function nutritionTargetValidationError(value) {
  if (value?.enabled !== true) {
    return ''
  }
  for (const field of NUTRITION_TARGET_FIELDS) {
    const number = toNumber(value?.[field.key])
    if (!Number.isFinite(number)) {
      return `请输入${field.label}目标`
    }
    if (number <= 0) {
      return `${field.label}目标必须大于 0`
    }
    if (number > field.max) {
      return `${field.label}目标不能超过 ${field.max}`
    }
  }
  return ''
}

export function isNutritionTargetUsable(value) {
  return Boolean(value?.enabled) && !nutritionTargetValidationError(value)
}

export function compareNutritionToTarget(current, target) {
  if (!isNutritionTargetUsable(target)) {
    return emptyNutritionComparison()
  }
  return compareNutritionValues(current, target)
}

export function compareNutritionValues(current, target) {
  if (!target || !NUTRITION_TARGET_FIELDS.every(({ key }) => {
    const number = toNumber(target[key])
    return Number.isFinite(number) && number > 0
  })) {
    return emptyNutritionComparison()
  }
  return Object.fromEntries(NUTRITION_TARGET_FIELDS.map(({ key }) => {
    const currentValue = toNumber(current?.[key])
    const targetValue = toNumber(target?.[key])
    if (!Number.isFinite(currentValue) || !Number.isFinite(targetValue) || targetValue <= 0) {
      return [key, null]
    }
    return [key, {
      current: currentValue,
      target: targetValue,
      percentage: Number((currentValue / targetValue * 100).toFixed(1))
    }]
  }))
}

export function weeklyNutritionTarget(value) {
  if (!isNutritionTargetUsable(value)) {
    return null
  }
  return Object.fromEntries(NUTRITION_TARGET_FIELDS.map(({ key }) => [
    key,
    Number((toNumber(value[key]) * 7).toFixed(2))
  ]))
}

function emptyNutritionComparison() {
  return Object.fromEntries(NUTRITION_TARGET_FIELDS.map(({ key }) => [key, null]))
}

function toNumberOrNull(value) {
  const number = toNumber(value)
  return Number.isFinite(number) ? number : null
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
