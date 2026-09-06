import axios from 'axios'
import { useAuthStore } from '../stores/auth.js'
import { getAnonymousId } from '../utils/anonymousId.js'

export const http = axios.create({
  baseURL: '/api',
  timeout: 30000
})

http.interceptors.request.use((config) => {
  const auth = useAuthStore()

  if (auth.token) {
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${auth.token}`
  }

  config.headers = config.headers || {}
  config.headers['X-Anonymous-Id'] = getAnonymousId()

  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status

    if (status === 401) {
      const auth = useAuthStore()
      auth.logout()
    }

    return Promise.reject(error)
  }
)
