import test from 'node:test'
import assert from 'node:assert/strict'
import { getPantryReadinessErrorMessage } from './pantryReadiness.js'

test('识别旧后端缺少库存匹配接口的部署不一致', () => {
  assert.match(
    getPantryReadinessErrorMessage({ response: { status: 404 } }),
    /同时重建 backend 与 frontend/
  )
  assert.match(
    getPantryReadinessErrorMessage({ response: { status: 405 } }),
    /同时重建 backend 与 frontend/
  )
})

test('区分未连接、权限和服务端错误', () => {
  assert.equal(
    getPantryReadinessErrorMessage({ request: {} }),
    '后端库存匹配服务未连接，请确认后端已启动'
  )
  assert.equal(
    getPantryReadinessErrorMessage({ response: { status: 403 } }),
    '当前账号没有读取库存的权限'
  )
  assert.equal(
    getPantryReadinessErrorMessage({ response: { status: 503 } }),
    '后端库存匹配服务异常，请稍后重试'
  )
})
