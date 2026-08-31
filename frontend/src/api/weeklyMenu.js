import { http } from './http'

export function getWeeklyMenu(weekStart) {
  return http.get('/users/me/weekly-menu', {
    params: weekStart ? { weekStart } : {}
  })
}

export function saveWeeklyMenu(payload) {
  return http.put('/users/me/weekly-menu', payload)
}

export function autoGenerateWeeklyMenu(payload) {
  return http.post('/users/me/weekly-menu/auto-generate', payload)
}

export function saveWeeklyShoppingStatus(payload) {
  return http.put('/users/me/weekly-menu/shopping-status', payload)
}

export function deleteWeeklyMenu(weekStart) {
  return http.delete('/users/me/weekly-menu', {
    params: weekStart ? { weekStart } : {}
  })
}
