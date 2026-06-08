import { http } from './http'

export function requestUserCode(phone) {
  return http.post('/auth/user/code', { phone })
}

export function loginUser(phone, code) {
  return http.post('/auth/user/login', { phone, code })
}

export function loginAdmin(username, password) {
  return http.post('/auth/admin/login', { username, password })
}
