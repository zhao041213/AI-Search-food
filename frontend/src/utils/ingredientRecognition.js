export const INGREDIENT_IMAGE_TYPES = Object.freeze([
  'image/jpeg',
  'image/png',
  'image/webp'
])

export const INGREDIENT_IMAGE_MAX_SIZE = 5 * 1024 * 1024

export function validateIngredientImageFile(file) {
  if (!file) return '请选择食材图片'
  if (!INGREDIENT_IMAGE_TYPES.includes(file.type)) return '仅支持 JPG、PNG、WebP 图片'
  if (file.size > INGREDIENT_IMAGE_MAX_SIZE) return '图片大小不能超过 5MB'
  return ''
}

export function mergeIngredientNames(currentText, newIngredients) {
  const merged = String(currentText || '')
    .split(/[，,、\n]/)
    .map((item) => item.trim())
    .filter(Boolean)

  const seen = new Set(merged.map((item) => item.toLocaleLowerCase()))
  for (const ingredient of Array.isArray(newIngredients) ? newIngredients : []) {
    const normalized = String(ingredient || '').trim()
    const key = normalized.toLocaleLowerCase()
    if (normalized && !seen.has(key)) {
      seen.add(key)
      merged.push(normalized)
    }
  }

  return merged.join('、')
}
