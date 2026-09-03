import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import {
  applyRecipeStreamEvent,
  createRecipeDraft,
  isRecipeReady,
  isRecipeResultPriority,
  parseRecipeStreamBlock,
  shouldSubmitIngredientsKey
} from './recipeStream.js'

const homeViewSource = await readFile(new URL('../views/HomeView.vue', import.meta.url), 'utf8')

test('parses structured SSE events and ignores heartbeat comments', () => {
  assert.deepEqual(parseRecipeStreamBlock(': heartbeat\n\n'), null)
  assert.deepEqual(
    parseRecipeStreamBlock('event: overview\ndata: {"title":"番茄炒蛋"}'),
    { event: 'overview', data: { title: '番茄炒蛋' } }
  )
  assert.deepEqual(parseRecipeStreamBlock('data: [DONE]'), { event: 'done', data: null })
})

test('merges progressive fields without dropping previous modules', () => {
  let recipe = createRecipeDraft()
  recipe = applyRecipeStreamEvent(recipe, {
    event: 'overview',
    data: { title: '番茄炒蛋', summary: '家常菜' }
  })
  recipe = applyRecipeStreamEvent(recipe, {
    event: 'details',
    data: { explanation: { nutrition: '均衡搭配' }, nutritionEstimate: null }
  })

  assert.equal(recipe.title, '番茄炒蛋')
  assert.equal(recipe.summary, '家常菜')
  assert.equal(recipe.explanation.nutrition, '均衡搭配')
})

test('keeps the result-priority mode until the user edits conditions', () => {
  assert.equal(isRecipeResultPriority({ ingredients: '番茄' }, false), true)
  assert.equal(isRecipeResultPriority({ ingredients: '番茄' }, true), false)
  assert.equal(isRecipeResultPriority(null, false), false)
})

test('recognizes incomplete and complete progressive recipes', () => {
  let recipe = createRecipeDraft()
  recipe = applyRecipeStreamEvent(recipe, {
    event: 'overview',
    data: { title: '番茄炒蛋', summary: '家常快手菜' }
  })
  assert.equal(isRecipeReady(recipe), false)

  recipe = applyRecipeStreamEvent(recipe, {
    event: 'ingredients',
    data: { ingredients: [{ name: '番茄', amount: '2 个' }] }
  })
  recipe = applyRecipeStreamEvent(recipe, {
    event: 'steps',
    data: { steps: [{ title: '翻炒', description: '炒至熟透' }] }
  })
  recipe = applyRecipeStreamEvent(recipe, {
    event: 'complete',
    data: { searchLogId: 12 }
  })
  assert.equal(isRecipeReady(recipe), true)
  assert.equal(recipe.searchLogId, 12)
})

test('submits Enter but keeps Shift+Enter and IME composition untouched', () => {
  assert.equal(shouldSubmitIngredientsKey({ key: 'Enter' }), true)
  assert.equal(shouldSubmitIngredientsKey({ key: 'Enter', shiftKey: true }), false)
  assert.equal(shouldSubmitIngredientsKey({ key: 'Enter', isComposing: true }), false)
  assert.equal(shouldSubmitIngredientsKey({ key: 'Enter', keyCode: 229 }), false)
  assert.equal(shouldSubmitIngredientsKey({ key: 'Enter' }, { generating: true }), false)
  assert.equal(shouldSubmitIngredientsKey({ key: 'a' }), false)
})

test('keeps streaming feedback local instead of masking the result container', () => {
  assert.doesNotMatch(
    homeViewSource,
    /<section[^>]*class="result-panel"[^>]*\bv-loading(?:=|\s|>)/
  )
  assert.doesNotMatch(homeViewSource, /\.result-panel[^\{]*\{[^\}]*\b(?:opacity|filter)\s*:/s)
  assert.doesNotMatch(homeViewSource, /\.result-panel[^\{]*(?:::before|::after)/)
  assert.match(homeViewSource, /class="stream-status"/)
  assert.match(homeViewSource, /class="stream-progress-panel"/)
  assert.match(homeViewSource, /@media \(prefers-reduced-motion: reduce\)/)
})
