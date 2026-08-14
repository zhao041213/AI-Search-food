export const EMPTY_DIET_PREFERENCE = Object.freeze({
  taste: 'any',
  defaultGoal: 'balanced',
  avoidIngredients: Object.freeze([]),
  allergenIngredients: Object.freeze([])
})

function normalizeIngredientList(items) {
  const seen = new Set()

  return (Array.isArray(items) ? items : [])
    .map((item) => String(item || '').trim())
    .filter((item) => {
      const key = item.toLocaleLowerCase()
      if (!item || seen.has(key)) {
        return false
      }
      seen.add(key)
      return true
    })
    .slice(0, 20)
}

export function normalizeDietPreference(value = {}) {
  return {
    taste: value?.taste || EMPTY_DIET_PREFERENCE.taste,
    defaultGoal: value?.defaultGoal || EMPTY_DIET_PREFERENCE.defaultGoal,
    avoidIngredients: normalizeIngredientList(value?.avoidIngredients),
    allergenIngredients: normalizeIngredientList(value?.allergenIngredients)
  }
}

export function buildRecipeDietPreference(value) {
  const normalized = normalizeDietPreference(value)
  return {
    ...normalized,
    avoidIngredients: [...normalized.avoidIngredients],
    allergenIngredients: [...normalized.allergenIngredients]
  }
}

export function toSearchForm(history = {}) {
  return {
    ingredients: String(history?.ingredients || '').trim(),
    mealType: history?.mealType || 'any',
    goal: history?.goal || 'balanced'
  }
}

export function resolveGoalWithPreference(currentGoal, defaultGoal, manuallySelected = false) {
  if (manuallySelected && currentGoal) {
    return currentGoal
  }
  return defaultGoal || currentGoal || EMPTY_DIET_PREFERENCE.defaultGoal
}

export function requiresDietPreferenceLoad(isUser, loaded) {
  return Boolean(isUser) && !loaded
}
