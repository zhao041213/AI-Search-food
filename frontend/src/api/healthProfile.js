import { http } from './http'

export function getHealthProfile() {
  return http.get('/users/me/health-profile')
}

export function saveHealthProfile(payload) {
  return http.put('/users/me/health-profile', payload)
}

export function deleteHealthProfile() {
  return http.delete('/users/me/health-profile')
}
