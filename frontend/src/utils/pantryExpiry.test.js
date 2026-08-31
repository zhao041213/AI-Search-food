import assert from 'node:assert/strict'
import test from 'node:test'
import {
  getDaysUntilExpiry,
  getExpiryClass,
  getExpiryLabel,
  getExpiryStatus,
  summarizePantryExpiry
} from './pantryExpiry.js'

const today = '2026-08-31'

test('按自然日计算库存保质期状态', () => {
  assert.equal(getDaysUntilExpiry('2026-08-30', today), -1)
  assert.equal(getExpiryStatus('2026-08-31', today), 'soon')
  assert.equal(getExpiryStatus('2026-09-07', today), 'soon')
  assert.equal(getExpiryStatus('2026-09-08', today), 'normal')
  assert.equal(getExpiryStatus(null, today), 'missing')
  assert.equal(getExpiryStatus('2026-02-30', today), 'missing')
})

test('临期标签会说明今天、明天或七天内到期', () => {
  assert.equal(getExpiryLabel('2026-08-31', today), '今天到期')
  assert.equal(getExpiryLabel('2026-09-01', today), '明天到期')
  assert.equal(getExpiryLabel('2026-09-05', today), '5天内到期')
  assert.equal(getExpiryLabel('2026-08-30', today), '已过期')
  assert.equal(getExpiryClass('2026-08-30', today), 'expired')
})

test('库存提醒只包含仍有数量的已过期和临期食材', () => {
  const summary = summarizePantryExpiry([
    { id: 1, ingredientName: '西兰花', quantity: 1, expireDate: '2026-08-30' },
    { id: 2, ingredientName: '牛奶', quantity: 2, expireDate: '2026-09-02' },
    { id: 3, ingredientName: '鸡蛋', quantity: 0, expireDate: '2026-08-29' },
    { id: 4, ingredientName: '大米', quantity: 1, expireDate: null }
  ], today)

  assert.deepEqual(summary.statusCounts, { expired: 2, soon: 1, normal: 0, missing: 1 })
  assert.deepEqual(summary.expiredItems.map((item) => item.ingredientName), ['西兰花'])
  assert.deepEqual(summary.expiringSoonItems.map((item) => item.ingredientName), ['牛奶'])
})
