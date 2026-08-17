import assert from 'node:assert/strict'
import test from 'node:test'
import {
  buildPurchaseLinks,
  buildShoppingList,
  buildBilibiliSearchLink,
  copyIngredientName,
  filterVideoKeywords,
  getShoppingItemStatus,
  isShoppingItemChecked,
  normalizeShoppingStatus,
  parseIngredientNames,
  shoppingChecklistKey
} from './recipeEnhancements.js'

test('parseIngredientNames splits common separators, normalizes aliases, and removes duplicates', () => {
  const input = ' 西红柿, 马铃薯，花椰菜、菜花；柿子椒;\n番茄\n '

  assert.deepEqual(parseIngredientNames(input), ['番茄', '土豆', '西兰花', '青椒'])
})

test('parseIngredientNames handles empty input and case-insensitive duplicates', () => {
  assert.deepEqual(parseIngredientNames(null), [])
  assert.deepEqual(parseIngredientNames('  \n，、；;  '), [])
  assert.deepEqual(parseIngredientNames('Egg, egg'), ['Egg'])
})

test('buildPurchaseLinks uses stable official Dingdong and Hema entrances', () => {
  assert.deepEqual(buildPurchaseLinks(' 小白菜 '), {
    dingdong: 'https://100.me/',
    hema: 'https://www.freshippo.com/down/app.html'
  })
})

test('copyIngredientName copies normalized ingredient for platform search', async () => {
  let copiedText = ''
  const copied = await copyIngredientName(' 小白菜 ', {
    writeText: async (value) => {
      copiedText = value
    }
  })

  assert.equal(copied, true)
  assert.equal(copiedText, '小白菜')
  assert.equal(await copyIngredientName('小白菜', null), false)
})

test('copyIngredientName tolerates denied clipboard permission', async () => {
  const copied = await copyIngredientName('小白菜', {
    writeText: async () => {
      throw new Error('clipboard permission denied')
    }
  })

  assert.equal(copied, false)
})

test('buildBilibiliSearchLink encodes a video keyword and ignores blanks', () => {
  assert.equal(
    buildBilibiliSearchLink(' 番茄炒蛋 做法 '),
    'https://search.bilibili.com/all?keyword=%E7%95%AA%E8%8C%84%E7%82%92%E8%9B%8B%20%E5%81%9A%E6%B3%95'
  )
  assert.equal(buildBilibiliSearchLink('  '), '')
})

test('filterVideoKeywords removes blank and non-text video keywords', () => {
  assert.deepEqual(
    filterVideoKeywords(['番茄炒蛋 做法', '  ', null, '家常菜教学']),
    ['番茄炒蛋 做法', '家常菜教学']
  )
})

test('buildShoppingList returns every recipe ingredient with ownership and purchase links', () => {
  const recipeIngredients = [
    { name: '番茄', amount: '2个' },
    { name: '土豆', amount: '300克' },
    { name: '香油', amount: '5毫升' },
    { name: '菜花', amount: '1颗' }
  ]

  const result = buildShoppingList(recipeIngredients, '西红柿，小土豆，油、花椰菜')

  assert.equal(result.length, recipeIngredients.length)
  assert.deepEqual(
    result.map(({ name, amount, alreadyOwned }) => ({ name, amount, alreadyOwned })),
    [
      { name: '番茄', amount: '2个', alreadyOwned: true },
      { name: '土豆', amount: '300克', alreadyOwned: true },
      { name: '香油', amount: '5毫升', alreadyOwned: false },
      { name: '西兰花', amount: '1颗', alreadyOwned: true }
    ]
  )
  assert.deepEqual(result[0].purchaseLinks, buildPurchaseLinks('番茄'))
  assert.deepEqual(result[2].purchaseLinks, buildPurchaseLinks('香油'))
})

test('buildShoppingList only matches exact names after harmless leading modifiers are removed', () => {
  const result = buildShoppingList(
    [
      { name: '番茄酱', amount: '1瓶' },
      { name: '鸡蛋', amount: '2个' },
      { name: '土豆', amount: '300克' }
    ],
    '番茄，鸡蛋清，小土豆'
  )

  assert.deepEqual(
    result.map(({ name, alreadyOwned }) => ({ name, alreadyOwned })),
    [
      { name: '番茄酱', alreadyOwned: false },
      { name: '鸡蛋', alreadyOwned: false },
      { name: '土豆', alreadyOwned: true }
    ]
  )
})

test('buildShoppingList normalizes aliases after harmless leading modifiers are removed', () => {
  const result = buildShoppingList(
    [
      { name: '番茄', amount: '2个' },
      { name: '土豆', amount: '300克' }
    ],
    '小西红柿，新鲜马铃薯'
  )

  assert.deepEqual(
    result.map(({ name, alreadyOwned }) => ({ name, alreadyOwned })),
    [
      { name: '番茄', alreadyOwned: true },
      { name: '土豆', alreadyOwned: true }
    ]
  )
})

test('buildShoppingList filters recipe ingredients with empty names', () => {
  const result = buildShoppingList(
    [
      { name: '  ', amount: '1份' },
      { amount: '2份' },
      '',
      null,
      { name: '番茄', amount: '2个' }
    ],
    '番茄'
  )

  assert.deepEqual(result, [
    {
      name: '番茄',
      amount: '2个',
      alreadyOwned: true,
      purchaseLinks: buildPurchaseLinks('番茄')
    }
  ])
})

test('shopping checklist preserves selectable procurement statuses', () => {
  const item = { name: 'egg', alreadyOwned: false }

  assert.equal(normalizeShoppingStatus('purchasing'), 'PURCHASING')
  assert.equal(getShoppingItemStatus(item), 'PENDING')
  assert.equal(getShoppingItemStatus(item, { egg: 'PURCHASED' }), 'PURCHASED')
  assert.equal(getShoppingItemStatus({ ...item, status: 'READY' }), 'READY')
  assert.equal(getShoppingItemStatus(item, { egg: 'unknown' }), 'PENDING')
})

test('shopping checklist uses inventory by default and preserves a manual override', () => {
  const item = { name: '西红柿', alreadyOwned: true }

  assert.equal(shoppingChecklistKey('西红柿'), '番茄')
  assert.equal(isShoppingItemChecked(item), true)
  assert.equal(isShoppingItemChecked(item, { 番茄: false }), false)
  assert.equal(isShoppingItemChecked({ name: '鸡蛋', alreadyOwned: false }, { 鸡蛋: true }), true)
})
