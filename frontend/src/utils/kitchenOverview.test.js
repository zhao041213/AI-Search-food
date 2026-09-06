import assert from 'node:assert/strict'
import test from 'node:test'
import { buildKitchenOverview, toDateKey } from './kitchenOverview.js'

test('今日厨房总览在缺失数据时安全降级', () => {
  const overview = buildKitchenOverview({}, new Date(2026, 8, 6))

  assert.equal(overview.pantryCount, 0)
  assert.equal(overview.attentionCount, 0)
  assert.equal(overview.weeklyAssignedCount, 0)
  assert.equal(overview.unreadCount, 0)
  assert.deepEqual(overview.todayMeals, [])
  assert.deepEqual(overview.priorityTasks, [{
    id: 'today-menu',
    type: 'menu',
    title: '安排今天的菜单',
    detail: '还没有今天的餐次，可以先从一周菜单开始安排',
    to: '/weekly-menu'
  }])
})

test('今日厨房总览统计库存和保质期提醒', () => {
  const overview = buildKitchenOverview({
    pantryItems: [
      { id: 1, ingredientName: '番茄', expireDate: '2026-09-05' },
      { id: 2, ingredientName: '鸡蛋', expireDate: '2026-09-10' },
      { id: 3, ingredientName: '大米' }
    ]
  }, new Date(2026, 8, 6))

  assert.equal(overview.pantryCount, 3)
  assert.deepEqual(overview.expiredItems.map((item) => item.ingredientName), ['番茄'])
  assert.deepEqual(overview.expiringSoonItems.map((item) => item.ingredientName), ['鸡蛋'])
  assert.equal(overview.attentionCount, 2)
})

test('今日厨房总览识别今日餐次和待采购食材', () => {
  const overview = buildKitchenOverview({
    weeklyMenu: {
      items: [
        { id: 1, menuDate: '2026-09-06', mealType: 'DINNER', recipeId: 10, recipeTitle: '番茄炒蛋' },
        { id: 2, menuDate: '2026-09-06', mealType: 'BREAKFAST', recipeId: 11, recipeTitle: '鸡蛋饼' },
        { id: 3, menuDate: '2026-09-05', mealType: 'LUNCH', recipeId: 12, recipeTitle: '青椒肉丝' }
      ],
      shoppingItems: [
        { ingredientName: '番茄', amount: '2 个', status: 'PENDING' },
        { ingredientName: '鸡蛋', amount: '4 个', alreadyOwned: true },
        { ingredientName: '青椒', amount: '1 个', status: 'READY' }
      ]
    }
  }, new Date(2026, 8, 6))

  assert.equal(overview.weeklyAssignedCount, 3)
  assert.deepEqual(overview.todayMeals.map((item) => item.label), ['早餐', '晚餐'])
  assert.deepEqual(overview.pendingShoppingItems.map((item) => item.ingredientName), ['番茄'])
})

test('今日厨房总览按事项优先级生成可跳转任务', () => {
  const overview = buildKitchenOverview({
    pantryItems: [{ id: 1, ingredientName: '牛奶', expireDate: '2026-09-01' }],
    unreadNotificationCount: 2,
    weeklyMenu: { shoppingItems: [{ ingredientName: '面粉', status: 'PENDING' }] }
  }, new Date(2026, 8, 6))

  assert.deepEqual(overview.priorityTasks.map((task) => task.type), ['expired', 'menu', 'shopping', 'notifications'])
  assert.equal(overview.priorityTasks[0].to, '/pantry')
  assert.equal(overview.priorityTasks.at(-1).to, '/notifications')
})

test('未读数量接口缺失时使用已加载通知中的未读数量', () => {
  const overview = buildKitchenOverview({
    notifications: { items: [{ id: 1, title: '库存提醒', status: 'UNREAD' }] }
  }, new Date(2026, 8, 6))

  assert.equal(overview.unreadCount, 1)
})

test('日期键仅接受安全的日期前缀', () => {
  assert.equal(toDateKey('2026-09-06T09:30:00'), '2026-09-06')
  assert.equal(toDateKey('invalid'), '')
  assert.equal(toDateKey(null), '')
})
