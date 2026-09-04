import { http } from './http'

export function getNutritionTarget() {
  return http.get('/users/me/nutrition-target')
}

export function saveNutritionTarget(payload) {
  return http.put('/users/me/nutrition-target', payload)
}

export function deleteNutritionTarget() {
  return http.delete('/users/me/nutrition-target')
}
