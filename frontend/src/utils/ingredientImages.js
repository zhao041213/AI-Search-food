const IMAGE_ROOT = '/images/ingredients'

const ingredientImages = {
  番茄: `${IMAGE_ROOT}/tomato.jpg`,
  鸡蛋: `${IMAGE_ROOT}/egg.jpg`,
  土豆: `${IMAGE_ROOT}/potato.jpg`,
  胡萝卜: `${IMAGE_ROOT}/carrot.jpg`,
  西兰花: `${IMAGE_ROOT}/broccoli.jpg`,
  豆腐: `${IMAGE_ROOT}/tofu.jpg`
}

const aliases = {
  西红柿: '番茄',
  马铃薯: '土豆',
  花椰菜: '西兰花',
  菜花: '西兰花',
  北豆腐: '豆腐',
  老豆腐: '豆腐',
  嫩豆腐: '豆腐'
}

export function getIngredientImage(name) {
  const normalizedName = typeof name === 'string' ? name.trim() : ''
  const canonicalName = aliases[normalizedName] || normalizedName
  return ingredientImages[canonicalName] || null
}
