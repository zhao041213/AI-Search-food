import assert from 'node:assert/strict'
import test from 'node:test'
import {
  INGREDIENT_IMAGE_MAX_SIZE,
  mergeIngredientNames,
  validateIngredientImageFile
} from './ingredientRecognition.js'

test('食材识别图片校验支持常用格式并限制大小', () => {
  assert.equal(validateIngredientImageFile({ type: 'image/jpeg', size: 1024 }), '')
  assert.equal(validateIngredientImageFile({ type: 'image/gif', size: 1024 }), '仅支持 JPG、PNG、WebP 图片')
  assert.equal(
    validateIngredientImageFile({ type: 'image/png', size: INGREDIENT_IMAGE_MAX_SIZE + 1 }),
    '图片大小不能超过 5MB'
  )
})

test('识别食材合并时去除空值和重复项', () => {
  assert.equal(
    mergeIngredientNames('番茄、鸡蛋', ['鸡蛋', ' 青椒 ', '', '番茄']),
    '番茄、鸡蛋、青椒'
  )
})
