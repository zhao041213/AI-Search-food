import assert from 'node:assert/strict'
import test from 'node:test'
import { buildDefaultKitchenCharacterNames } from '../utils/kitchenCharacters.js'

class MemoryStorage {
  #values = new Map()

  clear() {
    this.#values.clear()
  }

  getItem(key) {
    return this.#values.get(key) ?? null
  }

  setItem(key, value) {
    this.#values.set(String(key), String(value))
  }

  removeItem(key) {
    this.#values.delete(String(key))
  }
}

const storage = new MemoryStorage()
globalThis.localStorage = storage
globalThis.window = { localStorage: storage }

const { createPinia, setActivePinia } = await import('pinia')
const { http } = await import('../api/http.js')
const { useAuthStore } = await import('./auth.js')
const { useKitchenStore } = await import('./kitchen.js')

function tokenFor(userId) {
  const payload = Buffer.from(JSON.stringify({ id: userId })).toString('base64url')
  return `header.${payload}.signature`
}

function response(config, data) {
  return {
    data: { code: 0, message: 'ok', data },
    status: 200,
    statusText: 'OK',
    headers: {},
    config
  }
}

function apiError(status, message) {
  const error = new Error(message)
  error.response = { status, data: { message } }
  return error
}

function mockApi(handler) {
  const calls = []
  http.defaults.adapter = async (config) => {
    const call = {
      method: String(config.method || 'get').toUpperCase(),
      url: config.url,
      body: config.data ? JSON.parse(config.data) : null
    }
    calls.push(call)
    return handler(call, config)
  }
  return calls
}

function createUserStore(userId, localOverrides = {}) {
  storage.clear()
  setActivePinia(createPinia())
  const auth = useAuthStore()
  auth.setAuth({ token: tokenFor(userId), role: 'USER', displayName: '测试用户' })
  storage.setItem(
    `ai_smart_recipe_kitchen_characters:user-${userId}`,
    JSON.stringify(localOverrides)
  )
  return { auth, kitchen: useKitchenStore() }
}

async function waitForStatus(kitchen, expected = 'ready') {
  for (let attempt = 0; attempt < 50; attempt += 1) {
    if (kitchen.characterNamesStatus === expected) return
    await new Promise((resolve) => setTimeout(resolve, 0))
  }
  assert.equal(kitchen.characterNamesStatus, expected)
}

async function waitForCondition(condition) {
  for (let attempt = 0; attempt < 50; attempt += 1) {
    if (condition()) return
    await new Promise((resolve) => setTimeout(resolve, 0))
  }
  assert.equal(condition(), true)
}

test('登录后从云端加载人物名称', async () => {
  const cloudNames = { ...buildDefaultKitchenCharacterNames(), chef: '云端主厨' }
  const calls = mockApi((call, config) => response(config, { names: cloudNames, hasCustomNames: true }))
  const { kitchen } = createUserStore(42)

  await waitForStatus(kitchen)

  assert.equal(kitchen.characterNames.chef, '云端主厨')
  assert.deepEqual(calls.map((call) => call.method), ['GET'])
})

test('云端为空时首次迁移旧本地名称，迁移失败保留本地名称', async (t) => {
  await t.test('迁移成功', async () => {
    const localNames = { ...buildDefaultKitchenCharacterNames(), chef: '旧主厨' }
    const calls = mockApi((call, config) => {
      if (call.method === 'GET') {
        return response(config, { names: buildDefaultKitchenCharacterNames(), hasCustomNames: false })
      }
      return response(config, { names: localNames, hasCustomNames: true })
    })
    const { kitchen } = createUserStore(42, { chef: '旧主厨' })

    await waitForStatus(kitchen)

    assert.equal(kitchen.characterNames.chef, '旧主厨')
    assert.deepEqual(calls.map((call) => call.method), ['GET', 'PUT'])
    assert.equal(calls[1].body.names.chef, '旧主厨')
  })

  await t.test('迁移失败', async () => {
    const calls = mockApi((call, config) => {
      if (call.method === 'GET') {
        return response(config, { names: buildDefaultKitchenCharacterNames(), hasCustomNames: false })
      }
      throw apiError(503, '服务暂不可用')
    })
    const { kitchen } = createUserStore(42, { chef: '旧主厨' })

    await waitForStatus(kitchen, 'error')

    assert.equal(kitchen.characterNames.chef, '旧主厨')
    assert.equal(calls[1].method, 'PUT')
    assert.equal(kitchen.characterNamesError, '服务暂不可用')
  })
})

test('账号切换后重新加载新账号名称', async () => {
  let loadCount = 0
  const calls = mockApi((call, config) => {
    loadCount += 1
    const names = {
      ...buildDefaultKitchenCharacterNames(),
      chef: loadCount === 1 ? '一号主厨' : '二号主厨'
    }
    return response(config, { names, hasCustomNames: true })
  })
  const { auth, kitchen } = createUserStore(42)
  await waitForStatus(kitchen)

  auth.setAuth({ token: tokenFor(43), role: 'USER', displayName: '另一用户' })
  await waitForCondition(() => calls.length === 2)
  await waitForStatus(kitchen)

  assert.equal(kitchen.characterNames.chef, '二号主厨')
  assert.deepEqual(calls.map((call) => call.method), ['GET', 'GET'])
})

test('保存成功更新云端名称，保存失败保留原名称', async (t) => {
  await t.test('保存成功', async () => {
    const savedNames = { ...buildDefaultKitchenCharacterNames(), chef: '新主厨' }
    mockApi((call, config) => {
      if (call.method === 'GET') {
        return response(config, { names: buildDefaultKitchenCharacterNames(), hasCustomNames: false })
      }
      return response(config, { names: savedNames, hasCustomNames: true })
    })
    const { kitchen } = createUserStore(42)
    await waitForStatus(kitchen)

    const result = await kitchen.saveCharacterNames(savedNames)

    assert.equal(result.synced, true)
    assert.equal(kitchen.characterNames.chef, '新主厨')
    assert.equal(kitchen.characterNamesStatus, 'ready')
  })

  await t.test('保存失败', async () => {
    mockApi((call, config) => {
      if (call.method === 'GET') {
        return response(config, { names: buildDefaultKitchenCharacterNames(), hasCustomNames: false })
      }
      throw apiError(503, '保存失败')
    })
    const { kitchen } = createUserStore(42)
    await waitForStatus(kitchen)

    await assert.rejects(
      kitchen.saveCharacterNames({ ...kitchen.characterNames, chef: '保存失败' })
    )

    assert.equal(kitchen.characterNames.chef, '阿灶')
    assert.equal(kitchen.characterNamesStatus, 'error')
  })
})

test('恢复默认只有云端成功后才更新本地场景，失败保留原名称', async (t) => {
  await t.test('恢复成功', async () => {
    const cloudNames = { ...buildDefaultKitchenCharacterNames(), chef: '云端主厨' }
    mockApi((call, config) => {
      if (call.method === 'GET') return response(config, { names: cloudNames, hasCustomNames: true })
      return response(config, { names: buildDefaultKitchenCharacterNames(), hasCustomNames: false })
    })
    const { kitchen } = createUserStore(42)
    await waitForStatus(kitchen)

    await kitchen.resetCharacterNames()

    assert.equal(kitchen.characterNames.chef, '阿灶')
    assert.equal(kitchen.characterNamesStatus, 'ready')
  })

  await t.test('恢复失败', async () => {
    const cloudNames = { ...buildDefaultKitchenCharacterNames(), chef: '云端主厨' }
    mockApi((call, config) => {
      if (call.method === 'GET') return response(config, { names: cloudNames, hasCustomNames: true })
      throw apiError(503, '恢复失败')
    })
    const { kitchen } = createUserStore(42)
    await waitForStatus(kitchen)

    await assert.rejects(kitchen.resetCharacterNames())

    assert.equal(kitchen.characterNames.chef, '云端主厨')
    assert.equal(kitchen.characterNamesStatus, 'error')
  })
})

test('游客只使用本地名称且不调用云端接口', async () => {
  storage.clear()
  storage.setItem('ai_smart_recipe_kitchen_characters:guest', JSON.stringify({ chef: '游客主厨' }))
  setActivePinia(createPinia())
  const calls = mockApi(() => {
    throw new Error('游客不应调用云端接口')
  })
  const kitchen = useKitchenStore()

  await waitForStatus(kitchen)

  assert.equal(kitchen.characterNames.chef, '游客主厨')
  assert.equal(calls.length, 0)
})
