import { createRouter, createWebHistory } from 'vue-router'
import { getMe } from '../api/auth'
import { useAuthStore } from '../stores/auth'
import HomeView from '../views/HomeView.vue'
import LoginView from '../views/LoginView.vue'
import AdminDashboardView from '../views/AdminDashboardView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView
    },
    {
      path: '/admin',
      name: 'admin',
      component: AdminDashboardView,
      meta: { requiresAdmin: true }
    }
  ]
})

function loginRedirect(to) {
  return {
    name: 'login',
    query: { redirect: to.fullPath }
  }
}

router.beforeEach(async (to) => {
  const auth = useAuthStore()

  if (!to.meta.requiresAdmin) {
    return true
  }

  if (!auth.isLoggedIn) {
    return loginRedirect(to)
  }

  try {
    const response = await getMe()
    auth.setPrincipal(response.data.data)
  } catch {
    return loginRedirect(to)
  }

  if (!auth.isAdmin) {
    return loginRedirect(to)
  }

  return true
})

export default router
