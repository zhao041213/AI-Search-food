import { http } from './http'

export function getRecipeCollections() {
  return http.get('/users/me/recipe-collections')
}

export function createRecipeCollection(name) {
  return http.post('/users/me/recipe-collections', { name })
}

export function renameRecipeCollection(id, name) {
  return http.put(`/users/me/recipe-collections/${id}`, { name })
}

export function deleteRecipeCollection(id, confirm = false) {
  return http.delete(`/users/me/recipe-collections/${id}`, { params: { confirm } })
}

export function getEnhancedSavedRecipes({
  collectionId = null,
  keyword = '',
  mealType = '',
  goal = '',
  tag = '',
  sort = 'savedAtDesc',
  page = 1,
  size = 20
} = {}) {
  return http.get('/users/me/saved-recipes', {
    params: { collectionId, keyword, mealType, goal, tag, sort, page, size }
  })
}

export function getRecipeTags() {
  return http.get('/users/me/recipe-tags')
}

export function moveSavedRecipe(id, collectionId) {
  return http.put(`/users/me/saved-recipes/${id}/collection`, { collectionId })
}

export function replaceSavedRecipeTags(id, tags) {
  return http.put(`/users/me/saved-recipes/${id}/tags`, { tags })
}

export function batchMoveSavedRecipes(recipeIds, collectionId) {
  return http.post('/users/me/saved-recipes/batch-move', { recipeIds, collectionId })
}

export function batchTagSavedRecipes(recipeIds, addTags = [], removeTags = []) {
  return http.post('/users/me/saved-recipes/batch-tags', { recipeIds, addTags, removeTags })
}

export function batchDeleteSavedRecipes(recipeIds) {
  return http.delete('/users/me/saved-recipes/batch', { data: { recipeIds } })
}

export function createRecipeShare(recipeId, validity) {
  return http.post(`/users/me/saved-recipes/${recipeId}/shares`, { validity })
}

export function getRecipeShares() {
  return http.get('/users/me/recipe-shares')
}

export function disableRecipeShare(id) {
  return http.put(`/users/me/recipe-shares/${id}/disable`)
}

export function getPublicSharedRecipe(token) {
  return http.get(`/shared/recipes/${encodeURIComponent(token)}`)
}
