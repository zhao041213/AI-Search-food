import assert from 'node:assert/strict'
import test from 'node:test'
import { getIngredientImage } from './ingredientImages.js'

test('已收录食材和常用别名使用对应实物图片', () => {
  assert.equal(getIngredientImage('番茄'), '/images/ingredients/tomato.jpg')
  assert.equal(getIngredientImage('西红柿'), '/images/ingredients/tomato.jpg')
  assert.equal(getIngredientImage('花椰菜'), '/images/ingredients/broccoli.jpg')
  assert.equal(getIngredientImage('嫩豆腐'), '/images/ingredients/tofu.jpg')
})

test('未知食材不再回退到不相关的混合食材图', () => {
  assert.equal(getIngredientImage('三文鱼'), null)
  assert.equal(getIngredientImage(''), null)
})
