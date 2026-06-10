import { http } from './http'

export function generateRecipe(payload) {
  return http.post('/ai/recipes/generate', payload)
}
