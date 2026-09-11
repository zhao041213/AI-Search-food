import assert from 'node:assert/strict'
import test from 'node:test'
import {
  getRecipeVideoSearchKeyword,
  prioritizeRecipeRecommendations
} from './recipeVideoPriority.js'

test('uses the first usable recipe video keyword for Bilibili matching', () => {
  assert.equal(
    getRecipeVideoSearchKeyword({ title: '番茄炒蛋', videoKeywords: ['', '番茄炒蛋 家常做法'] }),
    '番茄炒蛋 家常做法'
  )
  assert.equal(getRecipeVideoSearchKeyword({ title: '  土豆炖牛肉  ', videoKeywords: [] }), '土豆炖牛肉')
})

test('moves recipes with Bilibili matches ahead while preserving relative order', () => {
  const recipes = [
    { id: 'recipe-1', title: '番茄炒蛋' },
    { id: 'recipe-2', title: '青椒牛肉' },
    { id: 'recipe-3', title: '土豆炖鸡' }
  ]

  assert.deepEqual(
    prioritizeRecipeRecommendations(recipes, { 'recipe-3': true, 'recipe-1': true }).map((item) => item.id),
    ['recipe-1', 'recipe-3', 'recipe-2']
  )
  assert.deepEqual(recipes.map((item) => item.id), ['recipe-1', 'recipe-2', 'recipe-3'])
})
