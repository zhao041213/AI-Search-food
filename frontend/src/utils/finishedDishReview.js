export const FINISHED_DISH_REVIEW_MAX_FILE_SIZE = 5 * 1024 * 1024

const SUPPORTED_IMAGE_TYPES = new Set([
  'image/jpeg',
  'image/png',
  'image/webp'
])

export function validateFinishedDishReviewImage(file) {
  if (!file) {
    return '请先选择一张成品图'
  }

  if (!SUPPORTED_IMAGE_TYPES.has(file.type)) {
    return '仅支持 JPG、PNG 或 WEBP 格式的成品图'
  }

  if (!Number.isFinite(file.size) || file.size <= 0) {
    return '图片文件无效，请重新选择'
  }

  if (file.size > FINISHED_DISH_REVIEW_MAX_FILE_SIZE) {
    return '成品图不能超过 5MB'
  }

  return ''
}

export function buildFinishedDishReviewRequest(recipe, recipeId = null) {
  const normalizedRecipeId = Number.isInteger(recipeId) && recipeId > 0 ? recipeId : null
  const title = normalizeText(recipe?.title, 120)

  return {
    recipeId: normalizedRecipeId,
    recipeTitle: title,
    ingredients: normalizeIngredients(recipe?.ingredients),
    steps: normalizeSteps(recipe?.steps)
  }
}

function normalizeIngredients(ingredients) {
  return (Array.isArray(ingredients) ? ingredients : [])
    .map((ingredient) => {
      if (typeof ingredient === 'string') {
        return normalizeText(ingredient, 100)
      }

      const name = normalizeText(ingredient?.name, 72)
      const amount = normalizeText(ingredient?.amount, 24)
      return normalizeText([name, amount].filter(Boolean).join(' '), 100)
    })
    .filter(Boolean)
    .slice(0, 30)
}

function normalizeSteps(steps) {
  return (Array.isArray(steps) ? steps : [])
    .map((step) => {
      if (typeof step === 'string') {
        return normalizeText(step, 300)
      }

      const title = normalizeText(step?.title, 80)
      const description = normalizeText(step?.description || step?.instruction, 200)
      return normalizeText([title, description].filter(Boolean).join('：'), 300)
    })
    .filter(Boolean)
    .slice(0, 20)
}

function normalizeText(value, maxLength) {
  return String(value || '').trim().replace(/\s+/g, ' ').slice(0, maxLength)
}
