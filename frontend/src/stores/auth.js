import { defineStore } from 'pinia'

const TOKEN_KEY = 'ai_smart_recipe_token'
const ROLE_KEY = 'ai_smart_recipe_role'
const DISPLAY_NAME_KEY = 'ai_smart_recipe_display_name'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    role: localStorage.getItem(ROLE_KEY) || '',
    displayName: localStorage.getItem(DISPLAY_NAME_KEY) || '',
    avatarVersion: 0
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    isAdmin: (state) => Boolean(state.token) && state.role === 'ADMIN',
    isUser: (state) => Boolean(state.token) && state.role === 'USER'
  },
  actions: {
    setAuth(auth) {
      const token = auth?.token || ''
      const role = auth?.role || ''
      const displayName = auth?.displayName || ''

      this.token = token
      this.role = role
      this.displayName = displayName
      this.avatarVersion += 1

      localStorage.setItem(TOKEN_KEY, token)
      localStorage.setItem(ROLE_KEY, role)
      localStorage.setItem(DISPLAY_NAME_KEY, displayName)
    },
    setPrincipal(principal) {
      const role = principal?.role || ''
      const displayName = this.displayName || principal?.username || ''

      this.role = role
      this.displayName = displayName

      localStorage.setItem(ROLE_KEY, role)
      localStorage.setItem(DISPLAY_NAME_KEY, displayName)
    },
    setDisplayName(displayName) {
      this.displayName = displayName || ''
      localStorage.setItem(DISPLAY_NAME_KEY, this.displayName)
    },
    refreshAvatar() {
      this.avatarVersion += 1
    },
    logout() {
      this.token = ''
      this.role = ''
      this.displayName = ''
      this.avatarVersion += 1

      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(ROLE_KEY)
      localStorage.removeItem(DISPLAY_NAME_KEY)
    }
  }
})
