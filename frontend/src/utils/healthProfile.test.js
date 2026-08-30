import assert from 'node:assert/strict'
import test from 'node:test'
import {
  buildHealthProfilePayload,
  calculateBmi,
  emptyHealthProfile,
  healthProfileLabel,
  normalizeHealthProfile,
  ACTIVITY_LEVEL_OPTIONS,
  AGE_RANGE_OPTIONS
} from './healthProfile.js'

test('健康档案默认值可用于空表单', () => {
  assert.deepEqual(emptyHealthProfile(), {
    completed: false,
    ageRange: 'AGE_18_29',
    heightCm: null,
    weightKg: null,
    activityLevel: 'MODERATE',
    bmi: null,
    updatedAt: null
  })
})

test('健康档案会规范化服务器数据并保留 BMI', () => {
  assert.deepEqual(normalizeHealthProfile({
    completed: true,
    ageRange: 'AGE_30_44',
    heightCm: '172.0',
    weightKg: '64.0',
    activityLevel: 'MODERATE',
    bmi: '21.6',
    updatedAt: '2026-08-30T20:00:00'
  }), {
    completed: true,
    ageRange: 'AGE_30_44',
    heightCm: 172,
    weightKg: 64,
    activityLevel: 'MODERATE',
    bmi: 21.6,
    updatedAt: '2026-08-30T20:00:00'
  })
})

test('健康档案会拒绝无效枚举和无效身体指标', () => {
  const profile = normalizeHealthProfile({
    completed: true,
    ageRange: 'UNKNOWN',
    heightCm: 0,
    weightKg: 'bad',
    activityLevel: 'UNKNOWN'
  })

  assert.equal(profile.completed, false)
  assert.equal(profile.ageRange, 'AGE_18_29')
  assert.equal(profile.activityLevel, 'MODERATE')
  assert.equal(profile.heightCm, null)
  assert.equal(profile.weightKg, null)
  assert.equal(profile.bmi, null)
})

test('健康档案会构建可提交的指标并计算 BMI', () => {
  const payload = buildHealthProfilePayload({
    ageRange: 'AGE_45_59',
    heightCm: 165,
    weightKg: 70,
    activityLevel: 'LOW'
  })

  assert.deepEqual(payload, {
    ageRange: 'AGE_45_59',
    heightCm: 165,
    weightKg: 70,
    activityLevel: 'LOW'
  })
  assert.equal(calculateBmi(165, 70), 25.7)
  assert.equal(calculateBmi(null, 70), null)
})

test('健康档案选项使用稳定中文标签', () => {
  assert.equal(healthProfileLabel(AGE_RANGE_OPTIONS, 'AGE_60_PLUS'), '60 岁及以上')
  assert.equal(healthProfileLabel(ACTIVITY_LEVEL_OPTIONS, 'HIGH'), '高活动量')
})
