import assert from 'node:assert/strict'
import test from 'node:test'
import {
  getFeatureIdForRoute,
  getFeatureIdForStation,
  getKitchenFeature,
  getKitchenNavigation,
  getKitchenNavigationTarget,
  isSameKitchenFeature,
  isKitchenNavigationActive,
  kitchenCanonicalFeatureLocation,
  kitchenFeatureLocation,
  kitchenNavigationLocation,
  kitchenStationLocation,
  kitchenWorldLocation,
  parseKitchenFeature,
  parseKitchenStation
} from './kitchenFeatures.js'

test('统一注册表覆盖人物入口、动态组件和权限', () => {
  assert.equal(getKitchenFeature('recipes').actorId, 'recipes')
  assert.equal(getKitchenFeature('recipes').requiresUser, true)
  assert.ok(getKitchenFeature('recipes').component)
  assert.equal(getKitchenFeature('kitchen-overview').compatibleRoutes[0], '/kitchen-overview')
  assert.equal(getKitchenFeature('hot').requiresUser, false)
})

test('交接功能暴露面板实际监听的事件名', () => {
  assert.equal(getKitchenFeature('recognition').handoffEvent, 'use-ingredients')
  assert.equal(getKitchenFeature('history').handoffEvent, 'use-search')
  assert.equal(getKitchenFeature('hot').handoffEvent, 'select-ingredient')
})

test('用户导航由注册表排序，公共热门食材保留给未登录用户', () => {
  assert.deepEqual(
    getKitchenNavigation('sidebar', 'USER').map((item) => item.featureId),
    ['kitchen-overview', 'recipes', 'pantry', 'health-profile', 'account', 'hot', 'weekly', 'nutrition-targets', 'characters']
  )
  assert.deepEqual(
    getKitchenNavigation('sidebar', '').map((item) => item.featureId),
    ['hot']
  )
})

test('有对应人物的导航目标与人物点击解析为同一个 station 请求和规范位置', () => {
  for (const featureId of ['recipes', 'pantry', 'weekly', 'hot', 'account']) {
    const target = getKitchenNavigationTarget(featureId)
    assert.deepEqual(target, { type: 'station', stationId: featureId, featureId })
    assert.deepEqual(kitchenNavigationLocation(featureId), kitchenStationLocation(target.stationId))
    assert.equal(getFeatureIdForStation(target.stationId), featureId)
  }

  assert.deepEqual(getKitchenNavigationTarget('health-profile'), {
    type: 'station',
    stationId: 'nutrition',
    featureId: 'health-profile'
  })
  assert.deepEqual(kitchenNavigationLocation('health-profile'), kitchenStationLocation('nutrition'))
  assert.deepEqual(kitchenNavigationLocation('nutrition-targets'), kitchenStationLocation('nutrition'))
  assert.equal(getFeatureIdForStation('nutrition'), '')
  assert.equal(getKitchenNavigationTarget('kitchen-overview').type, 'feature')
})

test('规范 station 位置只高亮对应人物入口，营养咨询室菜单不误高亮具体子功能', () => {
  assert.equal(isKitchenNavigationActive('recipes', { station: 'recipes' }), true)
  assert.equal(isKitchenNavigationActive('pantry', { station: 'recipes' }), false)
  assert.equal(isKitchenNavigationActive('health-profile', { station: 'nutrition' }), false)
  assert.equal(isKitchenNavigationActive('health-profile', { station: 'nutrition', feature: 'health-profile' }), true)
  assert.equal(isKitchenNavigationActive('nutrition-targets', { station: 'nutrition', feature: 'health-profile' }), false)
})

test('功能参数和人物入口映射到同一个 featureId', () => {
  assert.equal(parseKitchenFeature({ feature: 'pantry' }), 'pantry')
  assert.equal(parseKitchenFeature({ feature: ['notifications'] }), 'notifications')
  assert.equal(parseKitchenFeature({ feature: 'unknown' }), '')
  assert.equal(parseKitchenStation({ station: 'nutrition' }), 'nutrition')
  assert.equal(parseKitchenStation({ station: 'unknown' }), '')
  assert.equal(getFeatureIdForStation('pantry'), 'pantry')
  assert.equal(getFeatureIdForStation('review'), 'review')
  assert.equal(getFeatureIdForStation('account'), 'account')
  assert.equal(getFeatureIdForStation('diet-preference'), 'diet-preference')
  assert.equal(getFeatureIdForStation('nutrition'), '')
  assert.equal(getFeatureIdForRoute('/stats/hot-ingredients'), 'hot')
  assert.equal(getFeatureIdForRoute('/weekly-menu'), 'weekly')
})

test('旧路由和新链接都回到厨房世界，并支持清除窗口参数', () => {
  assert.deepEqual(kitchenCanonicalFeatureLocation('recipes'), kitchenStationLocation('recipes'))
  assert.deepEqual(kitchenCanonicalFeatureLocation('health-profile'), kitchenFeatureLocation('health-profile'))
  assert.deepEqual(kitchenFeatureLocation('pantry'), { name: 'home', query: { feature: 'pantry' } })
  assert.deepEqual(
    kitchenFeatureLocation('account', { feature: 'pantry', station: 'nutrition', tab: 'profile' }),
    { name: 'home', query: { tab: 'profile', feature: 'account' } }
  )
  assert.deepEqual(kitchenStationLocation('nutrition'), { name: 'home', query: { station: 'nutrition' } })
  assert.deepEqual(kitchenWorldLocation({ feature: 'pantry', station: 'nutrition', tab: 'profile' }), {
    name: 'home',
    query: { tab: 'profile' }
  })
})

test('当前功能重复点击可被识别为同一窗口，不触发重新打开', () => {
  assert.equal(isSameKitchenFeature('pantry', 'pantry'), true)
  assert.equal(isSameKitchenFeature('pantry', 'recipes'), false)
  assert.equal(isSameKitchenFeature('', 'pantry'), false)
  assert.equal(getKitchenFeature('pantry').requiresUser, true)
})
