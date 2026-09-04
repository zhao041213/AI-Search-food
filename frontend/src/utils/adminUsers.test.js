import assert from 'node:assert/strict'
import test from 'node:test'
import {
  buildAdminUserParams,
  formatAdminUserTime,
  maskPhone,
  normalizeAdminUser,
  normalizeAdminUserPage
} from './adminUsers.js'

test('管理员用户查询参数会保留筛选并从第一页开始', () => {
  assert.deepEqual(buildAdminUserParams({ keyword: '  小明  ', enabled: false, locked: true, offset: 40 }), {
    keyword: '小明',
    enabled: false,
    locked: true,
    limit: 20,
    offset: 40
  })
})

test('用户响应会脱敏手机号并规范状态字段', () => {
  assert.deepEqual(normalizeAdminUser({
    id: 7,
    nickname: '小明',
    phone: '13800138000',
    enabled: true,
    passwordLocked: 1
  }), {
    id: 7,
    nickname: '小明',
    phone: '138****8000',
    avatarUrl: '',
    enabled: true,
    passwordLocked: true,
    createdAt: '',
    lastLoginAt: ''
  })
  assert.equal(maskPhone('138****8000'), '138****8000')
})

test('异常分页响应会回退到安全默认值', () => {
  const page = normalizeAdminUserPage({ items: 'invalid', total: 'bad', limit: 0, offset: -1 })
  assert.deepEqual(page, { items: [], total: 0, limit: 20, offset: 0 })
  assert.equal(formatAdminUserTime(null), '暂无登录记录')
})
