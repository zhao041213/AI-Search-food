export const RECOMMENDATION_REACTIONS = Object.freeze(['LIKE', 'DISLIKE'])

export function normalizeRecommendationFeedback(value) {
  const reaction = RECOMMENDATION_REACTIONS.includes(value?.reaction) ? value.reaction : null
  return {
    reaction,
    cooked: value?.cooked === true
  }
}

export function nextRecommendationReaction(current, selected) {
  if (!RECOMMENDATION_REACTIONS.includes(selected)) {
    return current || null
  }
  return current === selected ? null : selected
}

export function canSubmitRecommendationFeedback({ isUser, searchLogId } = {}) {
  return Boolean(isUser && searchLogId)
}
