import { http } from './http'

export function generateRecipe(payload) {
  return http.post('/ai/recipes/generate', payload)
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

export function listSavedRecipes(params = {}) {
  return http.get('/recipes/saved', { params })
}

export function getSavedRecipe(id) {
  return http.get(`/recipes/saved/${id}`)
}
