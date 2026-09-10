import { http } from './http'

export function getHealthNutritionProfile() {
  return http.get('/users/me/health-nutrition')
}

export function saveHealthNutritionProfile(payload) {
  return http.put('/users/me/health-nutrition', payload)
}

export function generateHealthNutritionReference() {
  return http.post('/users/me/health-nutrition/ai-reference')
}
