import { http } from './http'

export function getPantryItems() {
  return http.get('/users/me/pantry')
}

export function createPantryItem(payload) {
  return http.post('/users/me/pantry', payload)
}

export function updatePantryItem(id, payload) {
  return http.put(`/users/me/pantry/${id}`, payload)
}

export function deletePantryItem(id) {
  return http.delete(`/users/me/pantry/${id}`)
}
