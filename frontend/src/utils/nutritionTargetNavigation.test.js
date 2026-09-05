import test from 'node:test'
import assert from 'node:assert/strict'
import { getNutritionTargetNavigation } from './hotIngredientNavigation.js'

test('营养目标导航指向独立页面并使用中文标签', () => {
  assert.deepEqual(getNutritionTargetNavigation(), {
    label: '营养目标',
    to: { name: 'nutrition-targets' }
  })
})
