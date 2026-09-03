import { http } from './http'

export function getAdminErrorLogs({
  sourceType = '',
  keyword = '',
  from = '',
  to = '',
  limit = 20,
  offset = 0
} = {}) {
  return http.get('/admin/error-logs', {
    params: { sourceType, keyword, from, to, limit, offset }
  })
}

export function getAdminErrorLog(id) {
  return http.get(`/admin/error-logs/${id}`)
}
