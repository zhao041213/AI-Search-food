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
