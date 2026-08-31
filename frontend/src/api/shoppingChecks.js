import { http } from './http'

export function getShoppingItemChecks(searchLogId) {
  return http.get('/users/me/shopping-checks', { params: { searchLogId } })
}

export function saveShoppingItemCheck(payload) {
  return http.put('/users/me/shopping-checks', payload)
}
