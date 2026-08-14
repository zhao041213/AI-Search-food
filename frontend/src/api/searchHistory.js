import { http } from './http'

export function getRecentSearches() {
  return http.get('/search-history/recent')
}
