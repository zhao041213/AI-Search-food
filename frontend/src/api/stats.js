import { http } from './http'

export function getHotIngredients(period = 'all', limit = 10) {
  return http.get('/stats/hot-ingredients', {
    params: { period, limit }
  })
}
