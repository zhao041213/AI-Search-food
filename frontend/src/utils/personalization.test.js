import assert from 'node:assert/strict'
import test from 'node:test'
import {
  buildRecipeDietPreference,
  normalizeDietPreference,
  requiresDietPreferenceLoad,
  resolveGoalWithPreference,
  toSearchForm
} from './personalization.js'

test('规范化饮食偏好并复制生成请求中的标签列表', () => {
  const normalized = normalizeDietPreference({
    taste: 'light',
    defaultGoal: 'fat_loss',
    avoidIngredients: [' 香菜 ', '香菜', '', null],
    allergenIngredients: ['花生', ' 花生 ']
  })

  assert.deepEqual(normalized, {
    taste: 'light',
    defaultGoal: 'fat_loss',
    avoidIngredients: ['香菜'],
    allergenIngredients: ['花生']
  })

  const payload = buildRecipeDietPreference(normalized)
  assert.deepEqual(payload, normalized)
  assert.notEqual(payload.avoidIngredients, normalized.avoidIngredients)
  assert.notEqual(payload.allergenIngredients, normalized.allergenIngredients)
})

test('空偏好使用稳定默认值且标签列表互不共享', () => {
  const first = normalizeDietPreference()
  const second = normalizeDietPreference()

  assert.deepEqual(first, {
    taste: 'any',
    defaultGoal: 'balanced',
    avoidIngredients: [],
    allergenIngredients: []
  })
  assert.notEqual(first.avoidIngredients, second.avoidIngredients)
  assert.notEqual(first.allergenIngredients, second.allergenIngredients)
})

test('最近搜索只映射工作台字段并提供缺省值', () => {
  assert.deepEqual(toSearchForm({
    ingredients: ' 番茄、鸡蛋 ',
    mealType: 'dinner',
    goal: 'fat_loss',
    createdAt: '2026-08-13T12:00:00'
  }), {
    ingredients: '番茄、鸡蛋',
    mealType: 'dinner',
    goal: 'fat_loss'
  })

  assert.deepEqual(toSearchForm(), {
    ingredients: '',
    mealType: 'any',
    goal: 'balanced'
  })
})

test('手动目标优先，否则采用偏好默认目标', () => {
  assert.equal(resolveGoalWithPreference('muscle_gain', 'fat_loss', true), 'muscle_gain')
  assert.equal(resolveGoalWithPreference('balanced', 'fat_loss', false), 'fat_loss')
  assert.equal(resolveGoalWithPreference('', '', false), 'balanced')
})

test('普通用户必须等待饮食偏好加载完成后才能生成', () => {
  assert.equal(requiresDietPreferenceLoad(true, false), true)
  assert.equal(requiresDietPreferenceLoad(true, true), false)
  assert.equal(requiresDietPreferenceLoad(false, false), false)
})
