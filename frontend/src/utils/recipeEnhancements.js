const INGREDIENT_ALIASES = new Map([
  ['西红柿', '番茄'],
  ['马铃薯', '土豆'],
  ['花椰菜', '西兰花'],
  ['菜花', '西兰花'],
  ['柿子椒', '青椒']
])

const INGREDIENT_SEPARATOR = /[,，、;；\r\n]+/
const HARMLESS_LEADING_MODIFIERS = /^(?:(?:新鲜|有机|小|大|嫩|老))+/u

function normalizeIngredientName(name) {
  const trimmedName = String(name ?? '').trim()
  return INGREDIENT_ALIASES.get(trimmedName) ?? trimmedName
}

function ingredientKey(name) {
  return normalizeIngredientName(name).toLocaleLowerCase()
}

function ingredientMatchKey(name) {
  const nameWithoutModifier = ingredientKey(name).replace(HARMLESS_LEADING_MODIFIERS, '')
  return ingredientKey(nameWithoutModifier)
}

function isIngredientOwned(recipeName, ownedName) {
  const recipeKey = ingredientKey(recipeName)
  const ownedKey = ingredientKey(ownedName)

  if (!recipeKey || !ownedKey) {
    return false
  }
  if (recipeKey === ownedKey) {
    return true
  }

  const recipeMatchKey = ingredientMatchKey(recipeName)
  const ownedMatchKey = ingredientMatchKey(ownedName)
  return Boolean(recipeMatchKey && ownedMatchKey && recipeMatchKey === ownedMatchKey)
}

export function parseIngredientNames(input) {
  const values = Array.isArray(input)
    ? input.flatMap((item) => String(item ?? '').split(INGREDIENT_SEPARATOR))
    : String(input ?? '').split(INGREDIENT_SEPARATOR)
  const seen = new Set()

  return values.reduce((ingredients, value) => {
    const name = normalizeIngredientName(value)
    const key = ingredientKey(name)

    if (name && !seen.has(key)) {
      seen.add(key)
      ingredients.push(name)
    }
    return ingredients
  }, [])
}

export function buildPurchaseLinks(name) {
  return {
    dingdong: 'https://100.me/',
    hema: 'https://www.freshippo.com/down/app.html'
  }
}

export function buildBilibiliSearchLink(keyword) {
  const normalizedKeyword = String(keyword || '').trim()
  return normalizedKeyword
    ? `https://search.bilibili.com/all?keyword=${encodeURIComponent(normalizedKeyword)}`
    : ''
}

export function filterVideoKeywords(keywords) {
  return Array.isArray(keywords)
    ? keywords.filter((keyword) => typeof keyword === 'string' && keyword.trim())
    : []
}

export async function copyIngredientName(name, clipboard) {
  const normalizedName = normalizeIngredientName(name)
  if (!normalizedName || typeof clipboard?.writeText !== 'function') {
    return false
  }

  try {
    await clipboard.writeText(normalizedName)
    return true
  } catch {
    return false
  }
}

export function buildShoppingList(recipeIngredients, ownedIngredients) {
  const ownedNames = parseIngredientNames(ownedIngredients)

  return (Array.isArray(recipeIngredients) ? recipeIngredients : [])
    .map((ingredient) => {
      const source = typeof ingredient === 'string' ? { name: ingredient, amount: '' } : ingredient ?? {}
      return { source, name: normalizeIngredientName(source.name) }
    })
    .filter(({ name }) => Boolean(name))
    .map(({ source, name }) => ({
      name,
      amount: source.amount ?? '',
      alreadyOwned: ownedNames.some((ownedName) => isIngredientOwned(name, ownedName)),
      purchaseLinks: buildPurchaseLinks(name)
    }))
}
