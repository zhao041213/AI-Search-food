import assert from 'node:assert/strict'
import test from 'node:test'
import {
  formatNutritionValue,
  normalizeNutritionEstimate,
  normalizeNutritionSummary
} from './nutrition.js'

test('只接受完整且在范围内的营养估算', () => {
  assert.deepEqual(normalizeNutritionEstimate({
    servings: 2,
    caloriesKcal: 420.5,
    proteinG: 24,
    fatG: 16,
    carbohydrateG: 42,
    source: 'AI_ESTIMATE'
  }).caloriesKcal, 420.5)
  assert.equal(normalizeNutritionEstimate({
    servings: 2,
    caloriesKcal: 0,
    proteinG: 24,
    fatG: 16,
    carbohydrateG: 42,
    source: 'AI_ESTIMATE'
  }), null)
})

test('营养汇总保留无有效估算的空状态并格式化数值', () => {
  const summary = normalizeNutritionSummary({
    assignedMealCount: 2,
    validEstimateMealCount: 1,
    daily: [{ date: '2026-08-31', assignedMealCount: 2, validEstimateMealCount: 1, totals: null }],
    weekly: null
  })
  assert.equal(summary.daily[0].totals, null)
  assert.equal(formatNutritionValue(420.567), '420.57')
})
