import assert from 'node:assert/strict'
import test from 'node:test'
import {
  createCookingSession,
  formatCookingDuration,
  formatCookingMinutes,
  getCookingProgress,
  getCookingSessionStorageKey,
  getRecipeTotalMinutes,
  normalizeCookingSession,
  normalizeCookingSteps,
  persistCookingSession,
  restoreCookingSession
} from './cookingSession.js'

function createMemoryStorage() {
  const values = new Map()
  return {
    getItem(key) {
      return values.get(key) ?? null
    },
    setItem(key, value) {
      values.set(key, value)
    }
  }
}

test('规范化烹饪步骤会保留有效数据并兼容历史字段', () => {
  assert.deepEqual(
    normalizeCookingSteps([
      { order: '2', title: '处理食材', description: '清洗并切好。', durationMinutes: '5' },
      { title: '下锅翻炒', instruction: '中火翻炒至熟。', estimatedMinutes: 3.2 },
      null,
      {},
      '装盘即可'
    ]),
    [
      { order: 2, title: '处理食材', description: '清洗并切好。', durationMinutes: 5 },
      { order: 2, title: '下锅翻炒', description: '中火翻炒至熟。', durationMinutes: 4 },
      { order: 3, title: '步骤 3', description: '装盘即可', durationMinutes: null }
    ]
  )
})

test('总时长优先使用菜谱声明值，否则汇总步骤时长', () => {
  assert.equal(getRecipeTotalMinutes({ durationMinutes: 18, steps: [{ durationMinutes: 5 }] }), 18)
  assert.equal(
    getRecipeTotalMinutes({ steps: [{ durationMinutes: 5 }, { estimatedMinutes: 3.2 }, {}] }),
    9
  )
  assert.equal(getRecipeTotalMinutes(), 0)
})

test('烹饪会话会限制无效进度和计时状态', () => {
  const options = { stepCount: 3, totalSeconds: 600 }

  assert.deepEqual(
    normalizeCookingSession({ currentStepIndex: 8, remainingSeconds: 999, timerRunning: true }, options),
    { currentStepIndex: 2, remainingSeconds: 600, timerRunning: true, finished: false }
  )
  assert.deepEqual(
    normalizeCookingSession({ currentStepIndex: 0, remainingSeconds: 0, timerRunning: true, finished: true }, options),
    { currentStepIndex: 2, remainingSeconds: 0, timerRunning: false, finished: true }
  )
  assert.equal(getCookingProgress(0, 4), 25)
  assert.equal(getCookingProgress(1, 4, true), 100)
})

test('会话可安全持久化和恢复，异常存储会回退到初始状态', () => {
  const storage = createMemoryStorage()
  const options = { stepCount: 2, totalSeconds: 420, storage }
  const session = { currentStepIndex: 1, remainingSeconds: 215, timerRunning: false, finished: false }
  const key = getCookingSessionStorageKey('saved-recipe-8')

  assert.equal(persistCookingSession('saved-recipe-8', session, options), true)
  assert.deepEqual(restoreCookingSession('saved-recipe-8', options), session)

  storage.setItem(key, '{not-json')
  assert.deepEqual(restoreCookingSession('saved-recipe-8', options), createCookingSession(options))

  const brokenStorage = {
    getItem() {
      throw new Error('storage unavailable')
    },
    setItem() {
      throw new Error('storage unavailable')
    }
  }
  assert.equal(persistCookingSession('saved-recipe-8', session, { ...options, storage: brokenStorage }), false)
  assert.deepEqual(
    restoreCookingSession('saved-recipe-8', { ...options, storage: brokenStorage }),
    createCookingSession(options)
  )
})

test('计时格式始终可读', () => {
  assert.equal(formatCookingDuration(65), '01:05')
  assert.equal(formatCookingDuration(3723), '01:02:03')
  assert.equal(formatCookingDuration(-1), '00:00')
  assert.equal(formatCookingMinutes(3.2), '约 4 分钟')
  assert.equal(formatCookingMinutes(0), '未标注时长')
})
