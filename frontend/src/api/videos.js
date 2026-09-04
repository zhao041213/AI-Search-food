import { http } from './http'

export function searchCookingVideos({ recipeTitle, keyword = '', page = 1, limit = 6 } = {}) {
  return http.get('/videos/search', {
    params: { recipeTitle, keyword, page, limit }
  })
}
