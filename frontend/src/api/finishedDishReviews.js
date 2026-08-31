import { http } from './http'

export function createFinishedDishReview({ image, request }) {
  const formData = new FormData()
  formData.append(
    'request',
    new Blob([JSON.stringify(request)], { type: 'application/json' }),
    'request.json'
  )
  formData.append('image', image)

  return http.post('/ai/finished-dish-reviews', formData, {
    timeout: 90000
  })
}

export function getFinishedDishReviews({ recipeId = null, limit = 10 } = {}) {
  const params = { limit }
  if (Number.isInteger(recipeId) && recipeId > 0) {
    params.recipeId = recipeId
  }
  return http.get('/finished-dish-reviews', { params })
}

export function getFinishedDishReviewImage(id) {
  return http.get(`/finished-dish-reviews/${id}/image`, {
    responseType: 'blob'
  })
}

export function deleteFinishedDishReview(id) {
  return http.delete(`/finished-dish-reviews/${id}`)
}
