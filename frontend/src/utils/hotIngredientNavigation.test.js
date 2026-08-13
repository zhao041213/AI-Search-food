import assert from 'node:assert/strict'
import test from 'node:test'
import {
  buildAdminPanelQuery,
  getHotIngredientNavigation,
  shouldShowSavedRecipesNavigation,
  resolveAdminPanel
} from './hotIngredientNavigation.js'

test('普通用户进入图片排行榜', () => {
  assert.deepEqual(getHotIngredientNavigation(false), {
    label: '热门食材',
    to: { name: 'hot-ingredients' }
  })
})

test('管理员直接进入热门分析面板', () => {
  assert.deepEqual(getHotIngredientNavigation(true), {
    label: '热门分析',
    to: { name: 'admin', query: { panel: 'hot-ingredients' } }
  })
})

test('只有普通用户显示我的菜谱入口', () => {
  assert.equal(shouldShowSavedRecipesNavigation('USER'), true)
  assert.equal(shouldShowSavedRecipesNavigation('ADMIN'), false)
  assert.equal(shouldShowSavedRecipesNavigation(''), false)
  assert.equal(shouldShowSavedRecipesNavigation(null), false)
  assert.equal(shouldShowSavedRecipesNavigation(undefined), false)
})

test('管理后台根据查询参数解析和更新面板', () => {
  assert.equal(resolveAdminPanel('hot-ingredients'), 'hot-ingredients')
  assert.equal(resolveAdminPanel(['hot-ingredients']), 'hot-ingredients')
  assert.equal(resolveAdminPanel('unknown'), 'settings')
  assert.deepEqual(buildAdminPanelQuery({ source: 'sidebar' }, 'hot-ingredients'), {
    source: 'sidebar',
    panel: 'hot-ingredients'
  })
  assert.deepEqual(buildAdminPanelQuery({ source: 'sidebar', panel: 'hot-ingredients' }, 'settings'), {
    source: 'sidebar'
  })
})
