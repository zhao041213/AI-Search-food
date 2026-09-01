import test from 'node:test'
import assert from 'node:assert/strict'
import {
  canSubmitRecommendationFeedback,
  nextRecommendationReaction,
  normalizeRecommendationFeedback
} from './recommendationFeedback.js'

test('normalizes only supported reaction values and cooked state', () => {
  assert.deepEqual(normalizeRecommendationFeedback({ reaction: 'LIKE', cooked: true }), {
    reaction: 'LIKE',
    cooked: true
  })
  assert.deepEqual(normalizeRecommendationFeedback({ reaction: 'UNKNOWN', cooked: 1 }), {
    reaction: null,
    cooked: false
  })
})

test('selecting the current reaction toggles it off', () => {
  assert.equal(nextRecommendationReaction(null, 'LIKE'), 'LIKE')
  assert.equal(nextRecommendationReaction('LIKE', 'LIKE'), null)
  assert.equal(nextRecommendationReaction('LIKE', 'DISLIKE'), 'DISLIKE')
})

test('feedback requires a logged-in user and search log id', () => {
  assert.equal(canSubmitRecommendationFeedback({ isUser: true, searchLogId: 10 }), true)
  assert.equal(canSubmitRecommendationFeedback({ isUser: false, searchLogId: 10 }), false)
  assert.equal(canSubmitRecommendationFeedback({ isUser: true, searchLogId: null }), false)
})
