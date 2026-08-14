import { http } from './http'

export function getDietPreference() {
  return http.get('/users/me/diet-preferences')
}

export function saveDietPreference(payload) {
  return http.put('/users/me/diet-preferences', payload)
}
