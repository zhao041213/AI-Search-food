import { http } from './http'

export function requestUserCode(phone) {
  return http.post('/auth/user/code', { phone })
}

export function requestRegistrationCode(phone) {
  return http.post('/auth/user/register/code', { phone })
}

export function registerUser(phone, code, nickname) {
  return http.post('/auth/user/register', { phone, code, nickname })
}

export function loginUser(phone, code) {
  return http.post('/auth/user/login', { phone, code })
}

export function loginAdmin(username, password) {
  return http.post('/auth/admin/login', { username, password })
}

export function getMe() {
  return http.get('/auth/me')
}
