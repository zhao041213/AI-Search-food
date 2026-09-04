import { http } from './http'

export function getAdminUsers({ keyword = '', enabled = '', locked = '', limit = 20, offset = 0 } = {}) {
  return http.get('/admin/users', {
    params: {
      keyword: keyword || undefined,
      enabled: enabled === '' ? undefined : enabled,
      locked: locked === '' ? undefined : locked,
      limit,
      offset
    }
  })
}

export function updateAdminUserStatus(id, enabled, reason = '') {
  return http.put(`/admin/users/${id}/status`, { enabled, reason })
}

export function clearAdminUserPasswordLock(id) {
  return http.post(`/admin/users/${id}/password-lock/clear`)
}

export function loadAdminUserAvatar(id) {
  return http.get(`/admin/users/${id}/avatar`, { responseType: 'blob' })
}
