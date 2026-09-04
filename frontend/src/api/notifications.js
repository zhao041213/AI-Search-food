import { http } from './http'

export function getNotifications(params = {}) {
  return http.get('/users/me/notifications', { params })
}

export function getUnreadNotificationCount() {
  return http.get('/users/me/notifications/unread-count')
}

export function getNotificationDetail(id) {
  return http.get(`/users/me/notifications/${id}`)
}

export function markNotificationRead(id) {
  return http.put(`/users/me/notifications/${id}/read`)
}

export function markAllNotificationsRead() {
  return http.put('/users/me/notifications/read-all')
}

export function archiveNotification(id) {
  return http.put(`/users/me/notifications/${id}/archive`)
}

export function getNotificationPreferences() {
  return http.get('/users/me/notification-preferences')
}

export function updateNotificationPreferences(payload) {
  return http.put('/users/me/notification-preferences', payload)
}
