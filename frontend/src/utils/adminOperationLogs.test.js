import assert from 'node:assert/strict'
import test from 'node:test'
import {
  formatOperationLogTime,
  normalizeAdminOperationLog,
  normalizeAdminOperationLogPage,
  operationResultLabel,
  operationResultTagType,
  operationTypeLabel
} from './adminOperationLogs.js'

test('管理员操作日志规范化并提供中文标签', () => {
  const log = normalizeAdminOperationLog({
    id: 7,
    adminUsername: 'admin',
    operationType: 'UPDATE_AI_CONFIG',
    httpMethod: 'PUT',
    requestPath: '/api/admin/ai-config/text-recipe',
    operationResult: 'SUCCESS',
    statusCode: '200',
    ipAddress: '127.0.0.1',
    createdAt: '2026-09-03T10:00:00'
  })

  assert.equal(log.statusCode, 200)
  assert.equal(operationTypeLabel(log.operationType), '更新 AI 配置')
  assert.equal(operationResultLabel(log.operationResult), '成功')
  assert.equal(operationResultTagType(log.operationResult), 'success')
})

test('管理员操作日志页面数据对异常值提供安全默认值', () => {
  const page = normalizeAdminOperationLogPage({
    items: [{ operationResult: 'UNKNOWN', statusCode: 'invalid' }],
    total: '2',
    limit: 20,
    offset: 0
  })

  assert.equal(page.total, 2)
  assert.equal(page.items[0].operationResult, 'FAILURE')
  assert.equal(page.items[0].statusCode, null)
  assert.equal(operationResultTagType('FAILURE'), 'danger')
})

test('管理员操作日志时间格式可回退原值', () => {
  assert.equal(formatOperationLogTime(''), '暂无')
  assert.equal(formatOperationLogTime('not-a-date'), 'not-a-date')
  assert.match(formatOperationLogTime('2026-09-03T10:00:00'), /2026/)
})
