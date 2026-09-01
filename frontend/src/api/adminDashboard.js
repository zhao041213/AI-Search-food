import { http } from './http'

export function getAdminDashboardOverview(period = '7d') {
  return http.get('/admin/dashboard/overview', { params: { period } })
}
