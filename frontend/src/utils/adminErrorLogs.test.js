import assert from 'node:assert/strict'
import test from 'node:test'
import {
  errorSourceLabel,
  errorSourceTagType,
  formatErrorLogTime,
  normalizeAdminErrorLog,
  normalizeAdminErrorLogPage
} from './adminErrorLogs.js'

test('异常日志规范化保留定位字段并提供安全默认值', () => {
  assert.deepEqual(normalizeAdminErrorLog({
    id: 8,
    sourceType: 'AI',
    component: 'QwenRecipeClient#generateRecipe',
    exceptionClass: 'org.springframework.web.server.ResponseStatusException',
    message: '千问服务调用失败',
    statusCode: 502,
    stackTrace: 'trace',
    createdAt: '2026-09-03T10:20:30'
  }), {
    id: 8,
    sourceType: 'AI',
    severity: 'ERROR',
    component: 'QwenRecipeClient#generateRecipe',
    exceptionClass: 'org.springframework.web.server.ResponseStatusException',
    message: '千问服务调用失败',
    rootCause: '暂无根因信息',
    requestMethod: '—',
    requestPath: '非 HTTP 请求',
    statusCode: 502,
    userId: null,
    adminId: null,
    ipAddress: '暂无',
    stackTrace: 'trace',
    createdAt: '2026-09-03T10:20:30'
  })
})

test('未知异常来源回退为后端，分页数据安全归一化', () => {
  assert.equal(normalizeAdminErrorLog({ sourceType: 'UNKNOWN' }).sourceType, 'BACKEND')
  assert.deepEqual(normalizeAdminErrorLogPage(), { items: [], total: 0, limit: 20, offset: 0 })
})

test('异常来源映射为中文标签和视觉状态', () => {
  assert.equal(errorSourceLabel('DATABASE'), '数据库')
  assert.equal(errorSourceLabel('UNKNOWN'), '后端')
  assert.equal(errorSourceTagType('AI'), 'warning')
  assert.equal(errorSourceTagType('TOOL'), 'info')
})

test('异常时间格式化对空值和非法值保持可读', () => {
  assert.equal(formatErrorLogTime(), '暂无时间')
  assert.equal(formatErrorLogTime('not-a-date'), 'not-a-date')
  assert.match(formatErrorLogTime('2026-09-03T10:20:30'), /2026/)
})
