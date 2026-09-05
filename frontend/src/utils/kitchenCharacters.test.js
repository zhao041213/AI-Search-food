import test from 'node:test'
import assert from 'node:assert/strict'
import {
  buildDefaultKitchenCharacterNames,
  getKitchenCharacterOwnerId,
  validateKitchenCharacterNames
} from './kitchenCharacters.js'

test('人物默认名称完整且互不重复', () => {
  const result = validateKitchenCharacterNames(buildDefaultKitchenCharacterNames())
  assert.equal(result.valid, true)
  assert.deepEqual(result.errors, {})
})

test('人物名称会拒绝空值、过长内容和重复值', () => {
  const names = buildDefaultKitchenCharacterNames()
  names.chef = '  '
  names['chef-helper'] = '超过六个字符的名字'
  names.recipes = names.pantry

  const result = validateKitchenCharacterNames(names)
  assert.equal(result.valid, false)
  assert.equal(result.errors.chef, '请输入人物名称')
  assert.equal(result.errors['chef-helper'], '最多 6 个字符')
  assert.equal(result.errors.pantry, '人物名称不能重复')
  assert.equal(result.errors.recipes, '人物名称不能重复')
})

test('人物名称会拒绝未知人物编号', () => {
  const result = validateKitchenCharacterNames({
    ...buildDefaultKitchenCharacterNames(),
    unknown: '陌生人'
  })

  assert.equal(result.valid, false)
  assert.equal(result.errors._unknownCharacterId, '包含未知人物编号')
})

test('人物名称存储按 JWT 用户编号区分', () => {
  const payload = Buffer.from(JSON.stringify({ id: 42, sub: 'tester' })).toString('base64url')
  assert.equal(getKitchenCharacterOwnerId(`header.${payload}.signature`), 'user-42')
  assert.equal(getKitchenCharacterOwnerId('invalid-token'), 'guest')
})
