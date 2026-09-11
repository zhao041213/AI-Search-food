import { http } from './http'

export function getTextRecipeAiConfig() {
  return http.get('/admin/ai-config/text-recipe')
}

export function saveTextRecipeAiConfig(payload) {
  return http.put('/admin/ai-config/text-recipe', payload)
}

export function testTextRecipeAiConfig(payload) {
  return http.post('/admin/ai-config/text-recipe/test', payload)
}
