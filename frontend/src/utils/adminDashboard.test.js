import assert from 'node:assert/strict'
import test from 'node:test'
import {
  dashboardInsight,
  inputSourceLabel,
  normalizeDashboardOverview,
  normalizeHotIngredientStats
} from './adminDashboard.js'

test('运营概览规范化缺失数据并过滤无效项目', () => {
  assert.deepEqual(normalizeDashboardOverview(), {
    period: '7d',
    generatedAt: null,
    metrics: {
      newUserCount: 0,
      generationCount: 0,
      savedRecipeCount: 0,
      reviewCount: 0
    },
    dailyTrend: [],
    inputSources: [],
    hotIngredients: []
  })
})

test('运营概览保留可展示统计并映射输入来源', () => {
  const overview = normalizeDashboardOverview({
    period: '30d',
    generatedAt: '2026-08-31T12:00:00',
    metrics: {
      newUserCount: 3,
      generationCount: 8,
      savedRecipeCount: 2,
      reviewCount: 1
    },
    dailyTrend: [{ date: '2026-08-31', generationCount: 8, savedRecipeCount: 2 }],
    inputSources: [{ inputType: 'CAMERA', count: 2 }],
    hotIngredients: [{ name: '番茄', count: 6 }, { name: '', count: 9 }]
  })

  assert.equal(overview.period, '30d')
  assert.deepEqual(overview.inputSources, [{ inputType: 'CAMERA', label: '摄像头', count: 2 }])
  assert.deepEqual(overview.hotIngredients, [{ name: '番茄', count: 6 }])
  assert.equal(dashboardInsight(overview), '当前周期共生成 8 次菜谱，其中 25% 被保存。')
})

test('未知输入来源归为其他', () => {
  assert.equal(inputSourceLabel('voice'), '其他')
  assert.equal(inputSourceLabel(' image '), '上传图片')
})

test('热门食材明细保留排行字段并限制展示数量', () => {
  const stats = normalizeHotIngredientStats({
    totalSearches: 29,
    totalIngredientOccurrences: 40,
    generatedAt: '2026-09-01T09:22:00',
    items: [
      { rank: 2, name: ' 鸡蛋 ', searchCount: 6, share: 0.15, latestSearchAt: '2026-09-01T09:20:00' },
      { rank: -1, name: '猪肉', searchCount: 5, share: 0.125 },
      { rank: 3, name: '', searchCount: 999 }
    ]
  })

  assert.deepEqual(stats, {
    totalSearches: 29,
    totalIngredientOccurrences: 40,
    generatedAt: '2026-09-01T09:22:00',
    items: [
      { rank: 2, name: '鸡蛋', searchCount: 6, share: 0.15, latestSearchAt: '2026-09-01T09:20:00' },
      { rank: 2, name: '猪肉', searchCount: 5, share: 0.125, latestSearchAt: null }
    ]
  })
})
