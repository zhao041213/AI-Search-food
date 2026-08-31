export const AGE_RANGE_OPTIONS = Object.freeze([
  { value: 'AGE_18_29', label: '18-29 岁' },
  { value: 'AGE_30_44', label: '30-44 岁' },
  { value: 'AGE_45_59', label: '45-59 岁' },
  { value: 'AGE_60_PLUS', label: '60 岁及以上' }
])

export const ACTIVITY_LEVEL_OPTIONS = Object.freeze([
  { value: 'LOW', label: '低活动量' },
  { value: 'MODERATE', label: '中等活动量' },
  { value: 'HIGH', label: '高活动量' }
])

const AGE_RANGE_VALUES = new Set(AGE_RANGE_OPTIONS.map((option) => option.value))
const ACTIVITY_LEVEL_VALUES = new Set(ACTIVITY_LEVEL_OPTIONS.map((option) => option.value))

export function emptyHealthProfile() {
  return {
    completed: false,
    ageRange: 'AGE_18_29',
    heightCm: null,
    weightKg: null,
    activityLevel: 'MODERATE',
    bmi: null,
    updatedAt: null
  }
}

export function normalizeHealthProfile(value = {}) {
  const defaults = emptyHealthProfile()
  const ageRange = AGE_RANGE_VALUES.has(value?.ageRange) ? value.ageRange : defaults.ageRange
  const activityLevel = ACTIVITY_LEVEL_VALUES.has(value?.activityLevel)
    ? value.activityLevel
    : defaults.activityLevel
  const heightCm = normalizeMetric(value?.heightCm)
  const weightKg = normalizeMetric(value?.weightKg)
  const estimatedBmi = calculateBmi(heightCm, weightKg)

  return {
    completed: Boolean(value?.completed && heightCm !== null && weightKg !== null),
    ageRange,
    heightCm,
    weightKg,
    activityLevel,
    bmi: normalizeBmi(value?.bmi) ?? estimatedBmi,
    updatedAt: value?.updatedAt || null
  }
}

export function buildHealthProfilePayload(value = {}) {
  const profile = normalizeHealthProfile({ ...value, completed: true })
  return {
    ageRange: profile.ageRange,
    heightCm: profile.heightCm,
    weightKg: profile.weightKg,
    activityLevel: profile.activityLevel
  }
}

export function calculateBmi(heightCm, weightKg) {
  const height = normalizeMetric(heightCm)
  const weight = normalizeMetric(weightKg)
  if (height === null || weight === null || height <= 0) {
    return null
  }
  return Number((weight * 10000 / (height * height)).toFixed(1))
}

export function healthProfileLabel(options, value, fallback = '未填写') {
  return options.find((option) => option.value === value)?.label || fallback
}

function normalizeMetric(value) {
  const numeric = Number(value)
  return Number.isFinite(numeric) && numeric > 0 ? numeric : null
}

function normalizeBmi(value) {
  const numeric = Number(value)
  return Number.isFinite(numeric) && numeric > 0 ? numeric : null
}
