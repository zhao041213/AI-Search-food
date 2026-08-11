const IMAGE_ROOT = '/images/ingredients'

const ingredientImages = {
  番茄: `${IMAGE_ROOT}/tomato.jpg`,
  鸡蛋: `${IMAGE_ROOT}/egg.jpg`,
  土豆: `${IMAGE_ROOT}/potato.jpg`,
  胡萝卜: `${IMAGE_ROOT}/carrot.jpg`,
  西兰花: `${IMAGE_ROOT}/broccoli.jpg`,
  豆腐: `${IMAGE_ROOT}/tofu.jpg`
}

export const fallbackIngredientImage = `${IMAGE_ROOT}/mixed.jpg`

export function getIngredientImage(name) {
  return ingredientImages[name] || fallbackIngredientImage
}
