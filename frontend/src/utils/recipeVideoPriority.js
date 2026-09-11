export function getRecipeVideoSearchKeyword(recipe = {}) {
  const keywords = Array.isArray(recipe?.videoKeywords) ? recipe.videoKeywords : []
  const keyword = keywords.find((item) => typeof item === 'string' && item.trim())
  return keyword?.trim() || (typeof recipe?.title === 'string' ? recipe.title.trim() : '')
}

export function prioritizeRecipeRecommendations(items, bilibiliAvailability = {}) {
  if (!Array.isArray(items)) return []

  return items
    .map((item, index) => ({
      item,
      index,
      matched: Boolean(item?.id && bilibiliAvailability[item.id])
    }))
    .sort((left, right) => Number(right.matched) - Number(left.matched) || left.index - right.index)
    .map(({ item }, index) => ({ ...item, index }))
}
