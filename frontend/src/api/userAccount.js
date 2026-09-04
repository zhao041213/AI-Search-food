import { http } from './http'

export function getMyAccount() {
  return http.get('/users/me/account')
}

export function updateMyProfile(nickname) {
  return http.patch('/users/me/account/profile', { nickname })
}

export function uploadMyAvatar(file) {
  const formData = new FormData()
  formData.append('image', file)
  return http.post('/users/me/account/avatar', formData)
}

export function deleteMyAvatar() {
  return http.delete('/users/me/account/avatar')
}

export function loadMyAvatar() {
  return http.get('/users/me/account/avatar', { responseType: 'blob' })
}

export function logoutAllDevices() {
  return http.post('/users/me/account/logout-all')
}

export function requestAccountCancellationCode() {
  return http.post('/users/me/account/cancel/code')
}

export function cancelMyAccount(code, confirmed = true) {
  return http.post('/users/me/account/cancel', { code, confirmed })
}
