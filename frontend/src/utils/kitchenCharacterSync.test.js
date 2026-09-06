import assert from 'node:assert/strict'
import test from 'node:test'
import {
  buildDefaultKitchenCharacterNames,
  KITCHEN_CHARACTERS
} from './kitchenCharacters.js'
import {
  hasKitchenCharacterOverrides,
  kitchenCharacterNamesToOverrides,
  normalizeKitchenCharacterNamesResponse,
  readStoredKitchenCharacterNames,
  shouldMigrateKitchenCharacterNames
} from './kitchenCharacterSync.js'

function customNames(name = '新主厨') {
  return {
    ...buildDefaultKitchenCharacterNames(),
    chef: name
  }
}

test('云端自定义名称覆盖本地名称并保留完整人物集合', () => {
  const result = normalizeKitchenCharacterNamesResponse({
    names: customNames(),
    hasCustomNames: true
  })

  assert.equal(result.valid, true)
  assert.equal(result.hasCustomNames, true)
  assert.equal(result.names.chef, '新主厨')
  assert.equal(Object.keys(result.names).length, KITCHEN_CHARACTERS.length)
})

test('云端无自定义名称且本地有旧数据时允许首次迁移', () => {
  const local = { names: customNames('旧主厨'), hasCustomNames: true, valid: true }
  const cloud = normalizeKitchenCharacterNamesResponse({
    names: buildDefaultKitchenCharacterNames(),
    hasCustomNames: false
  })

  assert.equal(shouldMigrateKitchenCharacterNames(local, cloud), true)
})

test('云端已有名称时不会触发旧本地覆盖', () => {
  const local = { names: customNames('旧主厨'), hasCustomNames: true, valid: true }
  const cloud = normalizeKitchenCharacterNamesResponse({
    names: customNames('云端主厨'),
    hasCustomNames: true
  })

  assert.equal(shouldMigrateKitchenCharacterNames(local, cloud), false)
  assert.equal(cloud.names.chef, '云端主厨')
})

test('游客可以读取本地名称且不需要云端状态', () => {
  const storage = {
    getItem(key) {
      assert.equal(key, 'guest-key')
      return JSON.stringify({ chef: '本地主厨' })
    }
  }
  const result = readStoredKitchenCharacterNames(storage, 'guest-key')

  assert.equal(result.valid, true)
  assert.equal(result.hasCustomNames, true)
  assert.deepEqual(kitchenCharacterNamesToOverrides(result.names), { chef: '本地主厨' })
  assert.equal(hasKitchenCharacterOverrides(result.names), true)
})

test('本地数据损坏时回退到默认名称', () => {
  const result = readStoredKitchenCharacterNames({ getItem: () => '{bad-json' }, 'guest-key')

  assert.equal(result.valid, false)
  assert.deepEqual(result.names, buildDefaultKitchenCharacterNames())
  assert.equal(result.hasCustomNames, false)
})
