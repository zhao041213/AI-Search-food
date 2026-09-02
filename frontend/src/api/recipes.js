import { http } from './http'

export function generateRecipe(payload) {
  return http.post('/ai/recipes/generate', payload, {
    timeout: 120000
  })
}

export function recognizeIngredients(file) {
  const formData = new FormData()
  formData.append('file', file)
  return http.post('/ai/ingredients/recognize', formData, {
    timeout: 60000
  })
}

export function saveRecipe(payload) {
  return http.post('/recipes/saved', payload)
}

export function getSavedRecipes({
  keyword = '',
  mealType = '',
  goal = '',
  limit = 50,
  offset = 0
} = {}) {
  return http.get('/recipes/saved', {
    params: { keyword, mealType, goal, limit, offset }
  })
}

export function listSavedRecipes(params = {}) {
  return getSavedRecipes(params)
}

export function getSavedRecipe(id) {
  return http.get(`/recipes/saved/${id}`)
}

export function deleteSavedRecipe(id) {
  return http.delete(`/recipes/saved/${id}`)
}

export function getRecommendationFeedback(searchLogId) {
  return http.get(`/recommendation-feedbacks/${searchLogId}`)
}

export function setRecommendationReaction(searchLogId, reaction) {
  return http.put(`/recommendation-feedbacks/${searchLogId}/reaction`, { reaction })
}

export function clearRecommendationReaction(searchLogId) {
  return http.delete(`/recommendation-feedbacks/${searchLogId}/reaction`)
}

export function markRecommendationCooked(searchLogId) {
  return http.put(`/recommendation-feedbacks/${searchLogId}/cooked`)
}
