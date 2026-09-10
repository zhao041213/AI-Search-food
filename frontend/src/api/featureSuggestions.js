import { http } from './http'

export function createFeatureSuggestion(payload, screenshot) {
  const form = new FormData()
  form.append('request', new Blob([JSON.stringify(payload)], { type: 'application/json' }))
  if (screenshot) form.append('screenshot', screenshot)
  return http.post('/feature-suggestions', form)
}

export function getMyFeatureSuggestions(params = {}) {
  return http.get('/feature-suggestions', { params })
}

export function getMyFeatureSuggestion(id) {
  return http.get(`/feature-suggestions/${id}`)
}

export function appendFeatureSuggestion(id, content) {
  return http.post(`/feature-suggestions/${id}/additions`, { content })
}

export function getAdminFeatureSuggestions(params = {}) {
  return http.get('/admin/feature-suggestions', { params })
}

export function getAdminFeatureSuggestion(id) {
  return http.get(`/admin/feature-suggestions/${id}`)
}

export function updateAdminFeatureSuggestion(id, payload) {
  return http.patch(`/admin/feature-suggestions/${id}`, payload)
}

export function deleteAdminFeatureSuggestion(id) {
  return http.delete(`/admin/feature-suggestions/${id}`)
}

export function getAdminFeatureSuggestionExport(params = {}) {
  return http.get('/admin/feature-suggestions/export', { params, responseType: 'blob' })
}

export function loadFeatureSuggestionScreenshot(id, admin = false) {
  return http.get(`${admin ? '/admin' : ''}/feature-suggestions/${id}/screenshot`, { responseType: 'blob' })
}
