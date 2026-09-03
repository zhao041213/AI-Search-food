import { http } from './http'

export function requestUserCode(phone) {
  return http.post('/auth/user/code', { phone })
}

export function requestRegistrationCode(phone) {
  return http.post('/auth/user/register/code', { phone })
}

export function registerUser(phone, code, nickname, password) {
  return http.post('/auth/user/register', { phone, code, nickname, password })
}

export function loginUser(phone, code) {
  return http.post('/auth/user/login', { phone, code })
}

export function loginUserWithPassword(phone, password) {
  return http.post('/auth/user/password-login', { phone, password })
}

export function requestPasswordResetCode(phone) {
  return http.post('/auth/user/password/reset/code', { phone })
}

export function resetUserPassword(phone, code, newPassword) {
  return http.post('/auth/user/password/reset', { phone, code, newPassword })
}

export function loginAdmin(username, password) {
  return http.post('/auth/admin/login', { username, password })
}

export function getMe() {
  return http.get('/auth/me')
}
