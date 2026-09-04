import assert from 'node:assert/strict'
import test from 'node:test'
import {
  buildNutritionTargetPayload,
  compareNutritionToTarget,
  compareNutritionValues,
  emptyNutritionTarget,
  isNutritionTargetUsable,
  normalizeNutritionTarget,
  nutritionTargetValidationError,
  weeklyNutritionTarget
} from './nutritionTarget.js'

test('营养目标空状态和启用状态可安全归一化', () => {
  assert.deepEqual(normalizeNutritionTarget(), emptyNutritionTarget())
  assert.equal(isNutritionTargetUsable(normalizeNutritionTarget({ configured: true, enabled: false })), false)
  assert.equal(isNutritionTargetUsable(normalizeNutritionTarget({
    configured: true,
    enabled: true,
    caloriesKcal: '2000',
    proteinG: 80,
    fatG: 60,
    carbohydrateG: 260
  })), true)
})

test('启用目标要求四项完整且在合理范围内', () => {
  assert.equal(nutritionTargetValidationError({ enabled: true, caloriesKcal: 0, proteinG: 80, fatG: 60, carbohydrateG: 260 }), '热量目标必须大于 0')
  assert.equal(nutritionTargetValidationError({ enabled: true, caloriesKcal: 2000, proteinG: 1001, fatG: 60, carbohydrateG: 260 }), '蛋白质目标不能超过 1000')
  assert.equal(nutritionTargetValidationError({ enabled: true, caloriesKcal: 2000, proteinG: 80, fatG: 60 }), '请输入碳水目标')
  assert.equal(nutritionTargetValidationError({ enabled: false }), '')
})

test('营养目标对比保留当前值、目标值并允许百分比超过 100', () => {
  const target = normalizeNutritionTarget({
    configured: true,
    enabled: true,
    caloriesKcal: 2000,
    proteinG: 80,
    fatG: 60,
    carbohydrateG: 260
  })
  const comparison = compareNutritionToTarget({ caloriesKcal: 2400, proteinG: 40, fatG: null, carbohydrateG: 520 }, target)
  assert.deepEqual(comparison.caloriesKcal, { current: 2400, target: 2000, percentage: 120 })
  assert.deepEqual(comparison.proteinG, { current: 40, target: 80, percentage: 50 })
  assert.equal(comparison.fatG, null)
  assert.equal(comparison.carbohydrateG.percentage, 200)
})

test('周目标按每日目标乘以 7，未设置时不产生虚假零目标', () => {
  const target = normalizeNutritionTarget({ configured: true, enabled: true, caloriesKcal: 2000, proteinG: 80, fatG: 60, carbohydrateG: 260 })
  assert.deepEqual(weeklyNutritionTarget(target), { caloriesKcal: 14000, proteinG: 560, fatG: 420, carbohydrateG: 1820 })
  assert.equal(compareNutritionValues({ caloriesKcal: 14000, proteinG: 560, fatG: 420, carbohydrateG: 1820 }, weeklyNutritionTarget(target)).caloriesKcal.percentage, 100)
  assert.equal(weeklyNutritionTarget(emptyNutritionTarget()), null)
  assert.deepEqual(buildNutritionTargetPayload({ enabled: false, caloriesKcal: 2000 }), {
    enabled: false,
    caloriesKcal: null,
    proteinG: null,
    fatG: null,
    carbohydrateG: null
  })
})
