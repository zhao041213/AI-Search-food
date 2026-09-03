import { http } from './http'

export function getAdminOperationLogs({ keyword = '', result = '', limit = 20, offset = 0 } = {}) {
  return http.get('/admin/operation-logs', {
    params: {
      keyword: keyword || undefined,
      result: result || undefined,
      limit,
      offset
    }
  })
}
