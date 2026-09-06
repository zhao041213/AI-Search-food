import { http } from './http.js'

export function getKitchenCharacterNames() {
  return http.get('/users/me/kitchen-character-names')
}

export function saveKitchenCharacterNames(names) {
  return http.put('/users/me/kitchen-character-names', { names })
}

export function resetKitchenCharacterNames() {
  return http.delete('/users/me/kitchen-character-names')
}
