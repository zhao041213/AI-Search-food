import { createRouter, createWebHistory } from 'vue-router'
import { getMe } from '../api/auth'
import { useAuthStore } from '../stores/auth'
import KitchenWorldView from '../views/KitchenWorldView.vue'
import {
  getFeatureIdForStation,
  getKitchenFeature,
  getKitchenStation,
  kitchenCanonicalFeatureLocation,
  parseKitchenFeature,
  parseKitchenStation
} from '../utils/kitchenFeatures'

function compatibilityRoute(path, name, featureId) {
  return {
    path,
    name,
    redirect: (to) => kitchenCanonicalFeatureLocation(featureId, to.query)
  }
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: KitchenWorldView
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue')
    },
    {
      path: '/admin',
      name: 'admin',
      component: () => import('../views/AdminDashboardView.vue'),
      meta: { requiresAdmin: true }
    },
    compatibilityRoute('/recipes/saved', 'saved-recipes', 'recipes'),
    compatibilityRoute('/pantry', 'pantry', 'pantry'),
    compatibilityRoute('/health-profile', 'health-profile', 'health-profile'),
    compatibilityRoute('/nutrition-targets', 'nutrition-targets', 'nutrition-targets'),
    compatibilityRoute('/weekly-menu', 'weekly-menu', 'weekly'),
    compatibilityRoute('/kitchen-overview', 'kitchen-overview', 'kitchen-overview'),
    compatibilityRoute('/account', 'user-account', 'account'),
    compatibilityRoute('/notifications', 'notifications', 'notifications'),
    {
      path: '/shared/recipes/:token',
      name: 'shared-recipe',
      component: () => import('../views/PublicSharedRecipeView.vue')
    },
    compatibilityRoute('/stats/hot-ingredients', 'hot-ingredients', 'hot')
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
  const parsedFeatureId = parseKitchenFeature(to.query)
  const stationId = parseKitchenStation(to.query)
  const featureId = parsedFeatureId || getFeatureIdForStation(stationId)

  if (to.name === 'home') {
    const query = { ...to.query }
    let queryChanged = false
    if (to.query.feature !== undefined && !parsedFeatureId) {
      delete query.feature
      queryChanged = true
    }
    if (to.query.station !== undefined && !stationId) {
      delete query.station
      queryChanged = true
    }
    if (queryChanged) return { name: 'home', query }
  }

  const feature = getKitchenFeature(featureId)
  const station = to.name === 'home' && stationId ? getKitchenStation(stationId) : null
  const requiresUser = to.meta.requiresUser || feature?.requiresUser || station?.requiresUser

  if (!to.meta.requiresAdmin && !requiresUser) {
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

  if (requiresUser && !auth.isUser) {
    return loginRedirect(to)
  }

  return true
})

export default router
