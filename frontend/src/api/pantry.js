import { http } from './http'

export function getPantryItems() {
  return http.get('/users/me/pantry')
}

export function getPantryExpiryAlerts() {
  return http.get('/users/me/pantry/expiry-alerts')
}

export function createPantryItem(payload) {
  return http.post('/users/me/pantry', payload)
}

export function updatePantryItem(id, payload) {
  return http.put(`/users/me/pantry/${id}`, payload)
}

export function consumePantryItem(id, quantity) {
  return http.post(`/users/me/pantry/${id}/consume`, { quantity })
}

export function deletePantryItem(id) {
  return http.delete(`/users/me/pantry/${id}`)
}

export function getCookingPreview(recipeId, actualServings) {
  return http.get('/users/me/pantry/cooking-preview', {
    params: { recipeId, actualServings }
  })
}

export function submitCookingConsumption(payload) {
  return http.post('/users/me/pantry/cooking-consumptions', payload)
}

export function stockInPantry(payload) {
  return http.post('/users/me/pantry/stock-ins', payload)
}

export function getPantryOperations(limit = 20) {
  return http.get('/users/me/pantry/operations', { params: { limit } })
}

export function undoPantryOperation(id, idempotencyKey) {
  return http.post(`/users/me/pantry/operations/${id}/undo`, null, {
    params: idempotencyKey ? { idempotencyKey } : undefined
  })
}
