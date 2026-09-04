import { createRouter, createWebHistory } from 'vue-router'
import { getMe } from '../api/auth'
import { useAuthStore } from '../stores/auth'
import HomeView from '../views/HomeView.vue'
import LoginView from '../views/LoginView.vue'
import AdminDashboardView from '../views/AdminDashboardView.vue'
import SavedRecipesView from '../views/SavedRecipesView.vue'
import HotIngredientsView from '../views/HotIngredientsView.vue'
import PantryView from '../views/PantryView.vue'
import HealthProfileView from '../views/HealthProfileView.vue'
import WeeklyMenuView from '../views/WeeklyMenuView.vue'
import UserAccountView from '../views/UserAccountView.vue'
import NotificationsView from '../views/NotificationsView.vue'
import PublicSharedRecipeView from '../views/PublicSharedRecipeView.vue'

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
    },
    {
      path: '/recipes/saved',
      name: 'saved-recipes',
      component: SavedRecipesView,
      meta: { requiresUser: true }
    },
    {
      path: '/pantry',
      name: 'pantry',
      component: PantryView,
      meta: { requiresUser: true }
    },
    {
      path: '/health-profile',
      name: 'health-profile',
      component: HealthProfileView,
      meta: { requiresUser: true }
    },
    {
      path: '/weekly-menu',
      name: 'weekly-menu',
      component: WeeklyMenuView,
      meta: { requiresUser: true }
    },
    {
      path: '/account',
      name: 'user-account',
      component: UserAccountView,
      meta: { requiresUser: true }
    },
    {
      path: '/notifications',
      name: 'notifications',
      component: NotificationsView,
      meta: { requiresUser: true }
    },
    {
      path: '/shared/recipes/:token',
      name: 'shared-recipe',
      component: PublicSharedRecipeView
    },
    {
      path: '/stats/hot-ingredients',
      name: 'hot-ingredients',
      component: HotIngredientsView
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

  if (!to.meta.requiresAdmin && !to.meta.requiresUser) {
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

  if (to.meta.requiresAdmin && !auth.isAdmin) {
    return loginRedirect(to)
  }

  if (to.meta.requiresUser && !auth.isUser) {
    return loginRedirect(to)
  }

  return true
})

export default router
