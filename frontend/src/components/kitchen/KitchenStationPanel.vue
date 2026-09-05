<template>
  <SceneWindow
    :model-value="modelValue"
    :title="windowTitle"
    :subtitle="windowSubtitle"
    :icon="windowIcon"
    :accent="windowAccent"
    :content-class="{ 'scene-window-content--workbench': ['chef', 'hot'].includes(activeFeatureId) }"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div v-if="activeFeature" class="scene-feature-host" :class="`scene-feature-host--${activeFeatureId}`">
      <div v-if="showFeatureBack" class="scene-feature-toolbar">
        <button type="button" class="scene-feature-back" @click="returnToStationMenu">
          <span aria-hidden="true">‹</span>
          返回{{ station.title }}
        </button>
        <span>{{ activeFeature.description }}</span>
      </div>

      <DietPreferenceForm
        v-if="activeFeatureId === 'diet-preference'"
        :preference="dietPreference"
        :saving="preferenceSaving"
        @cancel="returnToStationMenu"
        @save="persistDietPreference"
      />

      <Suspense v-else>
        <component
          :is="activeFeature.component"
          :key="activeFeatureId"
          v-bind="activeFeatureProps"
          @select-ingredient="openChefWithIngredient"
          @open-feature="openFeatureFromChild"
        />
        <template #fallback>
          <div class="scene-feature-loading" role="status">
            <span class="scene-feature-loading__pan" aria-hidden="true">◒</span>
            <strong>正在准备功能窗口…</strong>
          </div>
        </template>
      </Suspense>
    </div>

    <div v-else-if="station" class="station-brief">
      <div class="station-brief-icon" :style="{ '--station-accent': station.accent }">{{ station.icon }}</div>
      <div>
        <p class="station-kicker">{{ station.role }}</p>
        <h2>{{ station.title }}</h2>
        <p class="station-description">{{ station.description }}</p>
      </div>
      <div class="station-preview-grid">
        <div v-for="item in station.preview" :key="item.label" class="station-preview-card">
          <span>{{ item.value }}</span>
          <strong>{{ item.label }}</strong>
        </div>
      </div>
      <div class="station-entry-list" aria-label="功能入口">
        <button
          v-for="entry in station.entries"
          :key="entry.label"
          type="button"
          class="station-entry-card"
          :disabled="preferenceLoading && entry.action === 'diet-preference'"
          @click="openStationEntry(entry)"
        >
          <span class="station-entry-mark" aria-hidden="true">{{ entry.icon || '→' }}</span>
          <span class="station-entry-copy">
            <strong>{{ entry.label }}</strong>
            <small>{{ entry.description }}</small>
          </span>
          <span class="station-entry-arrow" aria-hidden="true">›</span>
        </button>
      </div>
      <div class="station-brief-actions">
        <el-button plain @click="emit('update:modelValue', false)">留在厨房</el-button>
      </div>
    </div>
  </SceneWindow>
</template>

<script setup>
import { computed, defineAsyncComponent, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { getDietPreference, saveDietPreference } from '../../api/userPreferences'
import { useAuthStore } from '../../stores/auth'
import { normalizeDietPreference } from '../../utils/personalization'
import DietPreferenceForm from '../DietPreferenceForm.vue'
import SceneWindow from './SceneWindow.vue'

const featureComponents = {
  chef: defineAsyncComponent(() => import('../../views/HomeView.vue')),
  pantry: defineAsyncComponent(() => import('../../views/PantryView.vue')),
  recipes: defineAsyncComponent(() => import('../../views/SavedRecipesView.vue')),
  review: defineAsyncComponent(() => import('../../views/SavedRecipesView.vue')),
  'health-profile': defineAsyncComponent(() => import('../../views/HealthProfileView.vue')),
  'nutrition-targets': defineAsyncComponent(() => import('../../views/NutritionTargetView.vue')),
  weekly: defineAsyncComponent(() => import('../../views/WeeklyMenuView.vue')),
  hot: defineAsyncComponent(() => import('../../views/HotIngredientsView.vue')),
  account: defineAsyncComponent(() => import('../../views/UserAccountView.vue')),
  notifications: defineAsyncComponent(() => import('../../views/NotificationsView.vue'))
}

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  stationId: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue'])
const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const dietPreference = ref(normalizeDietPreference())
const preferenceLoading = ref(false)
const preferenceSaving = ref(false)
const activeFeatureId = ref('')
const previousFeatureId = ref('')

const stationMap = {
  pantry: {
    id: 'pantry',
    title: '食材储藏室',
    role: '食材管家',
    icon: '▣',
    accent: '#68a873',
    description: '集中管理冰箱库存、保质期和烹饪消耗，生成菜谱时会自动参与推荐。',
    entries: [
      {
        label: '管理食材库存',
        description: '查看库存、临期提醒、入库、消耗和撤销记录。',
        feature: 'pantry',
        route: '/pantry',
        icon: '▣'
      }
    ],
    preview: [
      { value: '库存', label: '自动参与推荐' },
      { value: '临期', label: '及时提醒' },
      { value: '入库', label: '操作可追溯' }
    ]
  },
  recipes: {
    id: 'recipes',
    title: '菜谱书房',
    role: '菜谱管理员',
    icon: '▤',
    accent: '#b083c7',
    description: '重新打开已保存的 AI 菜谱，整理收藏夹、标签和分享记录。',
    entries: [
      {
        label: '打开我的菜谱',
        description: '查看已保存菜谱、收藏夹、标签和分享记录。',
        feature: 'recipes',
        route: '/recipes/saved',
        icon: '▤'
      }
    ],
    preview: [
      { value: '保存', label: '不重复调用模型' },
      { value: '标签', label: '方便分类查找' },
      { value: '分享', label: '生成公开链接' }
    ]
  },
  nutrition: {
    id: 'nutrition',
    title: '营养咨询室',
    role: '营养师',
    icon: '♥',
    accent: '#e2816c',
    description: '维护基础健康档案和每日营养目标，为菜谱搭配提供一般饮食参考。',
    entries: [
      {
        label: '健康档案',
        description: '维护身高、体重、年龄和基础身体指标。',
        feature: 'health-profile',
        route: '/health-profile',
        icon: '♥'
      },
      {
        label: '营养目标',
        description: '设置每日热量、蛋白质、碳水和脂肪目标。',
        feature: 'nutrition-targets',
        route: '/nutrition-targets',
        icon: '◎'
      },
      {
        label: '饮食偏好',
        description: '设置默认目标、口味、忌口和过敏食材。',
        action: 'diet-preference',
        icon: '◇'
      }
    ],
    preview: [
      { value: 'BMI', label: '基础指标' },
      { value: '目标', label: '每日营养参考' },
      { value: 'AI', label: '参与菜谱建议' }
    ]
  },
  weekly: {
    id: 'weekly',
    title: '菜单计划室',
    role: '菜单规划师',
    icon: '▦',
    accent: '#6d9cc3',
    description: '从已保存菜谱中安排一周餐次，并自动汇总采购清单。',
    entries: [
      {
        label: '安排一周菜单',
        description: '编排每日餐次，并汇总需要采购的食材。',
        feature: 'weekly',
        route: '/weekly-menu',
        icon: '▦'
      }
    ],
    preview: [
      { value: '21', label: '个餐次位置' },
      { value: 'AI', label: '自动安排' },
      { value: '清单', label: '自动汇总' }
    ]
  },
  hot: {
    id: 'hot',
    title: '美食情报站',
    role: '市场观察员',
    icon: '♨',
    accent: '#d48c52',
    description: '观察全站食材搜索趋势，发现大家最近正在寻找什么。',
    entries: [
      {
        label: '查看热门食材',
        description: '查看 7 天和 30 天的食材搜索趋势。',
        feature: 'hot',
        route: '/stats/hot-ingredients',
        icon: '♨'
      }
    ],
    preview: [
      { value: '7天', label: '短期趋势' },
      { value: '30天', label: '长期趋势' },
      { value: '排行', label: '食材热度' }
    ]
  },
  review: {
    id: 'review',
    title: '成品品鉴台',
    role: '成品品鉴员',
    icon: '◆',
    accent: '#c87a8a',
    description: '从已保存菜谱进入成品评价，上传成品图并获取 AI 品鉴建议。',
    entries: [
      {
        label: '选择菜谱开始品鉴',
        description: '打开我的菜谱，选择菜谱后上传成品照片。',
        feature: 'review',
        route: '/recipes/saved',
        icon: '◆'
      }
    ],
    preview: [
      { value: '图片', label: '上传成品' },
      { value: 'AI', label: '品鉴建议' },
      { value: '记录', label: '历史可查' }
    ]
  },
  account: {
    id: 'account',
    title: '厨房服务台',
    role: '厨房管家',
    icon: '◈',
    accent: '#8f8aa8',
    description: '集中处理个人资料、头像、账号安全和厨房通知。',
    entries: [
      {
        label: '账号中心',
        description: '维护昵称、头像和账号安全设置。',
        feature: 'account',
        route: '/account',
        icon: '◈'
      },
      {
        label: '通知中心',
        description: '查看库存提醒和系统消息，调整通知偏好。',
        feature: 'notifications',
        route: '/notifications',
        icon: '●'
      }
    ],
    preview: [
      { value: '资料', label: '个人信息' },
      { value: '通知', label: '及时提醒' },
      { value: '安全', label: '账号管理' }
    ]
  }
}

const featureMap = {
  chef: {
    id: 'chef',
    title: '主厨料理大厅',
    role: 'AI 主厨 · 阿灶',
    icon: '✦',
    accent: '#d6a43b',
    description: '输入现有食材，生成并保存专属菜谱。',
    component: featureComponents.chef,
    requiresUser: false
  },
  pantry: {
    id: 'pantry',
    title: '食材储藏室',
    role: '食材管家',
    icon: '▣',
    accent: '#68a873',
    description: '管理库存、临期提醒和库存变动记录。',
    component: featureComponents.pantry,
    requiresUser: true
  },
  recipes: {
    id: 'recipes',
    title: '菜谱书房',
    role: '菜谱管理员',
    icon: '▤',
    accent: '#b083c7',
    description: '整理已保存菜谱、收藏夹、标签和分享记录。',
    component: featureComponents.recipes,
    requiresUser: true
  },
  review: {
    id: 'review',
    title: '成品品鉴台',
    role: '成品品鉴员',
    icon: '◆',
    accent: '#c87a8a',
    description: '选择已保存的菜谱，上传成品并查看品鉴记录。',
    component: featureComponents.review,
    requiresUser: true
  },
  'health-profile': {
    id: 'health-profile',
    title: '健康档案',
    role: '营养咨询室',
    icon: '♥',
    accent: '#e2816c',
    description: '维护用于个性化建议的基础身体指标。',
    component: featureComponents['health-profile'],
    requiresUser: true
  },
  'nutrition-targets': {
    id: 'nutrition-targets',
    title: '每日营养目标',
    role: '营养咨询室',
    icon: '◎',
    accent: '#e2816c',
    description: '设置热量和主要营养素的每日参考目标。',
    component: featureComponents['nutrition-targets'],
    requiresUser: true
  },
  'diet-preference': {
    id: 'diet-preference',
    title: '饮食偏好',
    role: '营养咨询室',
    icon: '◇',
    accent: '#e2816c',
    description: '设置默认目标、口味、忌口和过敏食材。',
    component: null,
    requiresUser: true
  },
  weekly: {
    id: 'weekly',
    title: '一周菜单',
    role: '菜单规划师',
    icon: '▦',
    accent: '#6d9cc3',
    description: '安排每日餐次并汇总采购清单。',
    component: featureComponents.weekly,
    requiresUser: true
  },
  hot: {
    id: 'hot',
    title: '美食情报站',
    role: '市场观察员',
    icon: '♨',
    accent: '#d48c52',
    description: '查看近期热门食材和搜索趋势。',
    component: featureComponents.hot,
    requiresUser: false
  },
  account: {
    id: 'account',
    title: '账号中心',
    role: '厨房服务台',
    icon: '◈',
    accent: '#8f8aa8',
    description: '维护个人资料、头像和账号安全。',
    component: featureComponents.account,
    requiresUser: true
  },
  notifications: {
    id: 'notifications',
    title: '通知中心',
    role: '厨房服务台',
    icon: '●',
    accent: '#8f8aa8',
    description: '处理库存提醒、菜单通知和提醒偏好。',
    component: featureComponents.notifications,
    requiresUser: true
  }
}

const directFeatureByStation = {
  chef: 'chef',
  pantry: 'pantry',
  recipes: 'recipes',
  weekly: 'weekly',
  hot: 'hot',
  review: 'review'
}

const station = computed(() => {
  if (props.stationId === 'chef') {
    return { id: 'chef', title: '主厨料理大厅', role: 'AI 主厨', icon: '✦' }
  }
  return stationMap[props.stationId] || null
})

const activeFeature = computed(() => featureMap[activeFeatureId.value] || null)
const activeFeatureProps = computed(() => {
  return ['chef', 'weekly', 'hot', 'notifications'].includes(activeFeatureId.value)
    ? { embedded: true }
    : {}
})
const showFeatureBack = computed(() => {
  return Boolean(previousFeatureId.value || (station.value?.entries?.length > 1 && activeFeature.value))
})
const windowTitle = computed(() => activeFeature.value?.title || station.value?.title || '厨房功能')
const windowSubtitle = computed(() => activeFeature.value?.role || station.value?.role || '')
const windowIcon = computed(() => activeFeature.value?.icon || station.value?.icon || '✦')
const windowAccent = computed(() => activeFeature.value?.accent || station.value?.accent || '#d6a43b')

watch(
  () => [props.modelValue, props.stationId],
  ([visible, stationId]) => {
    if (!visible) return
    previousFeatureId.value = ''
    const featureId = directFeatureByStation[stationId]
    activeFeatureId.value = featureId || ''
    if (featureId) ensureFeatureAccess(featureId)
  },
  { immediate: true }
)

async function openStationEntry(entry) {
  if (entry.action === 'diet-preference') {
    await openDietPreference()
    return
  }
  if (entry.feature) {
    openFeature(entry.feature)
    return
  }
  if (!entry.route) return
  emit('update:modelValue', false)
  router.push(entry.route)
}

function openFeature(featureId, { rememberCurrent = false } = {}) {
  if (!ensureFeatureAccess(featureId)) return
  previousFeatureId.value = rememberCurrent ? activeFeatureId.value : ''
  activeFeatureId.value = featureId
}

function ensureFeatureAccess(featureId) {
  const feature = featureMap[featureId]
  if (!feature?.requiresUser || auth.isUser) return true
  emit('update:modelValue', false)
  router.push({ name: 'login', query: { redirect: route.fullPath } })
  return false
}

function returnToStationMenu() {
  if (previousFeatureId.value) {
    activeFeatureId.value = previousFeatureId.value
    previousFeatureId.value = ''
    return
  }
  activeFeatureId.value = ''
}

function openChefWithIngredient(ingredientName) {
  const name = String(ingredientName || '').trim()
  if (!name) return
  router.replace({ name: 'home', query: { ...route.query, ingredients: name } })
  openFeature('chef', { rememberCurrent: true })
}

function openFeatureFromChild(target) {
  const routeFeatureMap = {
    '/': 'chef',
    '/pantry': 'pantry',
    '/recipes/saved': 'recipes',
    '/health-profile': 'health-profile',
    '/nutrition-targets': 'nutrition-targets',
    '/weekly-menu': 'weekly',
    '/stats/hot-ingredients': 'hot',
    '/account': 'account',
    '/notifications': 'notifications'
  }
  const featureId = featureMap[target] ? target : routeFeatureMap[target]
  if (featureId) {
    openFeature(featureId, { rememberCurrent: true })
    return
  }
  if (typeof target === 'string' && target.startsWith('/')) {
    emit('update:modelValue', false)
    router.push(target)
  }
}

async function openDietPreference() {
  if (!auth.isUser) {
    emit('update:modelValue', false)
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }

  if (preferenceLoading.value) return
  preferenceLoading.value = true
  try {
    const response = await getDietPreference()
    dietPreference.value = normalizeDietPreference(response.data.data)
    activeFeatureId.value = 'diet-preference'
  } catch (error) {
    handleAuthorizedError(error, '饮食偏好加载失败，请稍后重试')
  } finally {
    preferenceLoading.value = false
  }
}

async function persistDietPreference(value) {
  if (!auth.isUser || preferenceSaving.value) return
  preferenceSaving.value = true
  try {
    const response = await saveDietPreference(normalizeDietPreference(value))
    dietPreference.value = normalizeDietPreference(response.data.data)
    activeFeatureId.value = ''
    ElMessage.success('饮食偏好已保存')
  } catch (error) {
    handleAuthorizedError(error, '饮食偏好保存失败，请稍后重试')
  } finally {
    preferenceSaving.value = false
  }
}

function handleAuthorizedError(error, fallback) {
  if (error?.response?.status === 401) {
    emit('update:modelValue', false)
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  ElMessage.error(getErrorMessage(error, fallback))
}

function getErrorMessage(error, fallback) {
  return error?.response?.data?.message || fallback
}
</script>

<style scoped>
.scene-feature-host {
  min-height: 100%;
  color: var(--app-text);
  background:
    linear-gradient(rgba(112, 84, 54, 0.07) 1px, transparent 1px),
    linear-gradient(90deg, rgba(112, 84, 54, 0.07) 1px, transparent 1px),
    #f8f1e2;
  background-size: 24px 24px;
}

.scene-feature-host--chef,
.scene-feature-host--hot {
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.scene-feature-toolbar {
  position: sticky;
  top: 0;
  z-index: 8;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  min-height: 40px;
  padding: 6px 12px;
  border-bottom: 1px solid #c8aa7b;
  color: #80664a;
  background: rgba(242, 229, 199, 0.96);
  font-size: 11px;
  font-weight: 700;
  backdrop-filter: blur(8px);
}

.scene-feature-back {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 28px;
  padding: 0 9px;
  border: 1px solid #9e7b50;
  border-radius: 3px;
  color: #3c2b20;
  background: #fffaf0;
  font: inherit;
  font-weight: 900;
  cursor: pointer;
}

.scene-feature-back:hover,
.scene-feature-back:focus-visible {
  border-color: var(--app-accent);
  background: #fff4d9;
  outline: none;
}

.scene-feature-back span {
  font-size: 20px;
  line-height: 1;
}

.scene-feature-loading {
  display: grid;
  min-height: 420px;
  place-items: center;
  align-content: center;
  gap: 12px;
  color: #80664a;
}

.scene-feature-loading__pan {
  color: #d6a43b;
  font-size: 36px;
  animation: scene-pan-bounce 900ms steps(2, end) infinite;
}

.scene-feature-host :deep(.workspace-heading),
.scene-feature-host :deep(.health-profile-heading),
.scene-feature-host :deep(.nutrition-target-heading),
.scene-feature-host :deep(.weekly-menu-heading),
.scene-feature-host :deep(.account-heading),
.scene-feature-host :deep(.notifications-heading) {
  display: none;
}

.scene-feature-host :deep(.pantry-heading),
.scene-feature-host :deep(.saved-heading),
.scene-feature-host :deep(.hot-header) {
  justify-content: flex-end;
  gap: 8px;
  margin-bottom: 10px;
}

.scene-feature-host :deep(.pantry-heading > div:first-child),
.scene-feature-host :deep(.saved-heading > div:first-child),
.scene-feature-host :deep(.hot-header > div:first-child),
.scene-feature-host :deep(.back-link) {
  display: none;
}

.scene-feature-host :deep(.home-page) {
  height: 100%;
  min-height: 0;
  padding: 12px;
  overflow: auto;
}

.scene-feature-host :deep(.command-shell) {
  height: auto;
  min-height: 100%;
}

.scene-feature-host :deep(.pantry-page),
.scene-feature-host :deep(.saved-page),
.scene-feature-host :deep(.health-profile-page),
.scene-feature-host :deep(.nutrition-target-page),
.scene-feature-host :deep(.weekly-menu-page),
.scene-feature-host :deep(.account-page),
.scene-feature-host :deep(.notifications-page) {
  min-height: 100%;
  padding: 12px 14px;
}

.scene-feature-host :deep(.hot-page) {
  height: 100%;
  min-height: 0;
  padding: 12px 14px;
  overflow: auto;
}

.scene-feature-host :deep(.hot-shell) {
  height: auto;
  min-height: 100%;
}

.scene-feature-host :deep(.pantry-panel),
.scene-feature-host :deep(.history-layout) {
  min-height: 0;
}

.scene-feature-host :deep(.history-layout) {
  grid-template-columns: minmax(230px, 280px) minmax(0, 1fr);
  gap: 12px;
}

.scene-feature-host :deep(.history-panel),
.scene-feature-host :deep(.detail-panel),
.scene-feature-host :deep(.weekly-menu-workspace),
.scene-feature-host :deep(.notification-list-panel),
.scene-feature-host :deep(.notification-preferences-panel),
.scene-feature-host :deep(.profile-panel),
.scene-feature-host :deep(.security-panel),
.scene-feature-host :deep(.danger-panel),
.scene-feature-host :deep(.identity-card) {
  box-shadow: 0 8px 22px rgba(69, 48, 34, 0.09);
}

.scene-feature-host :deep(.menu-grid) {
  min-width: 980px;
  grid-template-columns: repeat(7, minmax(120px, 1fr));
}

.scene-feature-host :deep(.weekly-menu-workspace) {
  overflow-x: auto;
}

.scene-feature-host :deep(.notifications-layout) {
  grid-template-columns: minmax(0, 1fr) 280px;
  gap: 12px;
}

.scene-feature-host :deep(.notification-list-panel),
.scene-feature-host :deep(.notification-preferences-panel),
.scene-feature-host :deep(.profile-panel),
.scene-feature-host :deep(.security-panel),
.scene-feature-host :deep(.danger-panel) {
  padding: 16px 18px;
}

.scene-feature-host :deep(.account-grid) {
  grid-template-columns: 250px minmax(0, 1fr);
  gap: 12px;
}

.scene-feature-host :deep(.identity-card) {
  top: 12px;
  padding: 18px;
}

@container scene-window-content (max-width: 820px) {
  .scene-feature-host :deep(.history-layout),
  .scene-feature-host :deep(.notifications-layout),
  .scene-feature-host :deep(.account-grid) {
    grid-template-columns: minmax(0, 1fr);
  }

  .scene-feature-host :deep(.identity-card),
  .scene-feature-host :deep(.notification-preferences-panel) {
    position: static;
  }
}

@keyframes scene-pan-bounce {
  50% {
    transform: translateY(-5px) rotate(-5deg);
  }
}

.station-brief {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 16px 18px;
  padding: 34px;
  color: #3c2b20;
}

.station-brief-icon {
  display: grid;
  width: 58px;
  height: 58px;
  place-items: center;
  border: 3px solid var(--station-accent);
  color: var(--station-accent);
  background: #221c19;
  font-size: 28px;
  font-weight: 900;
  image-rendering: pixelated;
}

.station-kicker {
  margin: 3px 0 4px;
  color: #966f43;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.12em;
}

.station-brief h2 {
  margin: 0;
  font-size: 24px;
}

.station-description {
  grid-column: 1 / -1;
  margin: 0;
  color: #745b43;
  line-height: 1.7;
}

.station-preview-grid {
  display: grid;
  grid-column: 1 / -1;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.station-preview-card {
  display: grid;
  gap: 5px;
  min-height: 78px;
  padding: 13px;
  border: 1px solid #c8aa7b;
  background: #fffaf0;
}

.station-preview-card span {
  color: #a4763f;
  font-size: 20px;
  font-weight: 900;
}

.station-preview-card strong {
  color: #6a4f37;
  font-size: 12px;
}

.station-entry-list {
  display: grid;
  grid-column: 1 / -1;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.station-entry-card {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  min-height: 68px;
  padding: 10px 12px;
  border: 1px solid #c8aa7b;
  color: #3c2b20;
  background: #fffaf0;
  text-align: left;
  cursor: pointer;
  transition: border-color 160ms ease, background-color 160ms ease, transform 160ms ease;
}

.station-entry-card:hover,
.station-entry-card:focus-visible {
  border-color: var(--app-accent);
  background: #fff4d9;
  outline: none;
  transform: translateY(-1px);
}

.station-entry-card:disabled {
  cursor: wait;
  opacity: 0.68;
  transform: none;
}

.station-entry-mark {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border: 1px solid #9e7b50;
  color: #f4d37f;
  background: #2b211d;
  font-weight: 900;
}

.station-entry-copy {
  display: grid;
  gap: 4px;
}

.station-entry-copy strong {
  font-size: 13px;
}

.station-entry-copy small {
  color: #80664a;
  font-size: 11px;
  line-height: 1.45;
}

.station-entry-arrow {
  color: #9e7444;
  font-size: 24px;
  font-weight: 900;
}

.station-brief-actions {
  display: flex;
  grid-column: 1 / -1;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 4px;
}

@media (max-width: 620px) {
  .station-preview-grid,
  .station-entry-list {
    grid-template-columns: 1fr;
  }
}
</style>
