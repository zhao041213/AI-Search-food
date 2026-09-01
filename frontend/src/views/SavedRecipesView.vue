<template>
  <main class="saved-page">
    <header class="saved-heading">
      <div>
        <p class="eyebrow">个人菜谱库</p>
        <h1>我的菜谱</h1>
        <p>查看你主动保存的 AI 菜谱，重新打开时不会再次调用模型。</p>
      </div>
      <RouterLink class="back-link" to="/">
        <ArrowLeft :size="16" aria-hidden="true" />
        <span>返回工作台</span>
      </RouterLink>
    </header>

    <div class="history-layout">
      <section class="history-panel" aria-label="已保存菜谱列表">
        <div class="panel-heading">
          <div>
            <span class="panel-kicker">已保存菜谱</span>
            <h2>已保存</h2>
          </div>
          <span class="count-badge">{{ recipes.length }}</span>
        </div>

        <form class="recipe-filters" role="search" @submit.prevent="applyFilters">
          <div class="filter-search-row">
            <el-input
              v-model="keywordDraft"
              aria-label="按菜名搜索"
              clearable
              placeholder="搜索菜名"
              @clear="applyFilters"
            >
              <template #prefix>
                <Search :size="15" aria-hidden="true" />
              </template>
            </el-input>
            <el-tooltip content="搜索菜谱" placement="top">
              <button class="search-button" type="submit" aria-label="搜索菜谱">
                <Search :size="16" aria-hidden="true" />
              </button>
            </el-tooltip>
          </div>
          <div class="filter-select-row">
            <el-select v-model="mealType" aria-label="筛选餐次" placeholder="不限餐次" @change="applyFilters">
              <el-option label="不限餐次" value="" />
              <el-option label="早餐" value="breakfast" />
              <el-option label="午餐" value="lunch" />
              <el-option label="晚餐" value="dinner" />
            </el-select>
            <el-select v-model="goal" aria-label="筛选目标" placeholder="不限目标" @change="applyFilters">
              <el-option label="不限目标" value="" />
              <el-option label="营养均衡" value="balanced" />
              <el-option label="高蛋白" value="protein" />
              <el-option label="低热量" value="light" />
              <el-option label="快速烹饪" value="quick" />
              <el-option label="减脂" value="fat_loss" />
              <el-option label="增肌" value="muscle_gain" />
              <el-option label="控糖" value="low_sugar" />
            </el-select>
          </div>
        </form>

        <el-skeleton v-if="loading" :rows="5" animated />
        <el-empty v-else-if="!recipes.length" description="还没有保存菜谱" />
        <div v-else class="history-items">
          <div
            v-for="item in recipes"
            :key="item.id"
            class="history-item-row"
            :class="{ active: selected?.id === item.id }"
          >
            <button class="history-item" type="button" @click="openRecipe(item.id)">
              <span class="history-item-title">{{ item.title }}</span>
              <span class="history-item-ingredients">{{ item.searchIngredients || '未记录食材' }}</span>
              <span class="history-item-meta">
                <span>{{ mealLabel(item.mealType) }}</span>
                <span>{{ formatDate(item.savedAt) }}</span>
              </span>
            </button>
            <el-tooltip content="删除菜谱" placement="top">
              <button
                class="delete-button"
                type="button"
                :aria-label="`删除菜谱：${item.title}`"
                :disabled="deletingId !== null"
                @click="confirmDeleteRecipe(item)"
              >
                <Trash2 :size="16" aria-hidden="true" />
              </button>
            </el-tooltip>
          </div>
        </div>
      </section>

      <section class="detail-panel" aria-label="菜谱详情">
        <el-skeleton v-if="detailLoading" :rows="9" animated />
        <el-empty v-else-if="!selected" description="选择一个菜谱查看详情" />
        <template v-else>
          <header class="detail-heading">
            <div>
              <p class="eyebrow">已保存菜谱</p>
              <h2>{{ selected.recipe?.title || '菜谱详情' }}</h2>
              <p class="saved-time">保存于 {{ formatDate(selected.savedAt) }}</p>
            </div>
            <div class="detail-heading-actions">
              <el-button
                v-if="selected.recipe"
                class="finished-dish-review-button"
                plain
                @click="openFinishedDishReview"
              >
                <Sparkles :size="16" aria-hidden="true" />
                <span>评价成品</span>
              </el-button>
              <el-button
                v-if="selected.recipe?.steps?.length"
                class="start-cooking-button"
                type="primary"
                @click="openCookingMode"
              >
                <Play :size="16" aria-hidden="true" />
                <span>开始烹饪</span>
              </el-button>
              <span class="detail-id">#{{ selected.id }}</span>
            </div>
          </header>

          <div v-if="selected.recipe" class="recipe-detail">
            <p class="summary">{{ selected.recipe.summary || '暂无菜谱简介' }}</p>
            <div v-if="selected.recipe.effects?.length" class="tag-row" aria-label="菜谱功效">
              <span v-for="effect in selected.recipe.effects" :key="effect" class="system-tag">
                {{ effect }}
              </span>
            </div>

            <RecommendationFeedbackButtons
              :reaction="feedbackReaction"
              :cooked="feedbackCooked"
              :loading="feedbackLoading"
              :disabled="!selected.searchLogId"
              @toggle-reaction="toggleRecommendationReaction"
            />

            <NutritionEstimateCard :nutrition="selected.recipe.nutritionEstimate" />

            <div class="recipe-pages">
              <div class="page-tabs" role="tablist" aria-label="菜谱详情分页">
                <button
                  v-for="page in recipePages"
                  :key="page.key"
                  class="page-tab"
                  :class="{ active: activePage === page.key }"
                  type="button"
                  role="tab"
                  :aria-selected="activePage === page.key"
                  @click="activePage = page.key"
                >
                  {{ page.label }}
                </button>
              </div>

              <div class="recipe-window">
                <el-table v-if="activePage === 'ingredients'" :data="selected.recipe.ingredients" size="large">
                  <el-table-column prop="name" label="食材" min-width="150" />
                  <el-table-column prop="amount" label="用量" min-width="120" />
                </el-table>

                <el-table v-else-if="activePage === 'analysis'" :data="ingredientAnalysisRows" size="large">
                  <el-table-column prop="name" label="食材" min-width="105" />
                  <el-table-column prop="amount" label="用量" min-width="90" />
                  <el-table-column label="状态" min-width="82">
                    <template #default="scope">
                      <span class="ingredient-state" :class="scope.row.alreadyOwned ? 'owned' : 'missing'">
                        {{ scope.row.alreadyOwned ? '已有' : '缺失' }}
                      </span>
                    </template>
                  </el-table-column>
                  <el-table-column label="替代建议" min-width="160">
                    <template #default="scope">
                      {{ scope.row.substitutesText || '暂无替代建议' }}
                    </template>
                  </el-table-column>
                  <el-table-column label="说明" min-width="180">
                    <template #default="scope">
                      {{ scope.row.reason || (scope.row.alreadyOwned ? '可直接使用现有食材' : '建议按清单补充') }}
                    </template>
                  </el-table-column>
                </el-table>

                <ShoppingChecklistTable
                  v-else-if="activePage === 'shopping'"
                  :items="shoppingList"
                  :overrides="shoppingCheckOverrides"
                  :saving-key="shoppingCheckSavingKey"
                  source-type="RECIPE"
                  :source-id="selected?.id"
                  :stock-in-key="stockInKey"
                  @status-change="toggleShoppingItem"
                  @purchase-search="preparePlatformSearch"
                  @stock-in="handleStockIn"
                />

                <div v-else-if="activePage === 'explanation'" class="explanation-grid">
                  <article v-for="item in explanationItems" :key="item.key" class="explanation-item">
                    <component :is="item.icon" :size="18" aria-hidden="true" />
                    <div>
                      <h3>{{ item.label }}</h3>
                      <p>{{ item.content }}</p>
                    </div>
                  </article>
                </div>

                <ol v-else-if="activePage === 'steps'" class="step-list">
                  <li v-for="step in selected.recipe.steps" :key="step.order || step.title">
                    <div class="step-title-row">
                      <strong>{{ step.title }}</strong>
                      <span v-if="step.durationMinutes">约 {{ step.durationMinutes }} 分钟</span>
                    </div>
                    <p>{{ step.description }}</p>
                  </li>
                </ol>

                <ul v-else-if="activePage === 'tips'" class="tip-list">
                  <li v-for="tip in selected.recipe.tips" :key="tip">{{ tip }}</li>
                </ul>

                <div v-else class="video-keywords">
                  <a
                    v-for="keyword in videoKeywords"
                    :key="keyword"
                    :href="buildBilibiliSearchLink(keyword)"
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    <Video :size="15" aria-hidden="true" />
                    {{ keyword }}
                    <ExternalLink :size="13" aria-hidden="true" />
                  </a>
                </div>
              </div>
            </div>
          </div>
        </template>
      </section>
    </div>
  </main>
  <CookingModeDialog
    v-if="selected?.recipe"
    v-model="cookingModeVisible"
    :recipe="selected.recipe"
    :storage-key="cookingStorageKey"
    :recipe-id="selected.id"
    :search-log-id="selected.searchLogId"
  />
  <StockInDialog
    v-model="stockInDialogVisible"
    :item="stockInItem"
    :loading="Boolean(stockInKey)"
    @confirm="handleStockInConfirm"
  />
  <FinishedDishReviewDialog
    v-if="selected?.recipe"
    v-model="finishedDishReviewVisible"
    :recipe="selected.recipe"
    :recipe-id="selected.id"
  />
</template>

<script setup>
import { computed, markRaw, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  ExternalLink,
  Flame,
  HeartPulse,
  Network,
  Play,
  Search,
  Sparkles,
  Trash2,
  Video
} from 'lucide-vue-next'
import {
  clearRecommendationReaction,
  deleteSavedRecipe,
  getRecommendationFeedback,
  getSavedRecipe,
  getSavedRecipes,
  setRecommendationReaction
} from '../api/recipes'
import { getPantryItems, stockInPantry } from '../api/pantry'
import { getShoppingItemChecks, saveShoppingItemCheck } from '../api/shoppingChecks'
import CookingModeDialog from '../components/CookingModeDialog.vue'
import FinishedDishReviewDialog from '../components/FinishedDishReviewDialog.vue'
import ShoppingChecklistTable from '../components/ShoppingChecklistTable.vue'
import StockInDialog from '../components/StockInDialog.vue'
import NutritionEstimateCard from '../components/NutritionEstimateCard.vue'
import RecommendationFeedbackButtons from '../components/RecommendationFeedbackButtons.vue'
import { useAuthStore } from '../stores/auth'
import {
  buildPurchaseLinks,
  buildBilibiliSearchLink,
  buildShoppingList,
  copyIngredientName,
  filterVideoKeywords,
  normalizeShoppingStatus,
  parseIngredientNames,
  shoppingChecklistKey
} from '../utils/recipeEnhancements'
import {
  nextRecommendationReaction,
  normalizeRecommendationFeedback
} from '../utils/recommendationFeedback'

const recipes = ref([])
const selected = ref(null)
const loading = ref(false)
const detailLoading = ref(false)
const deletingId = ref(null)
const activePage = ref('ingredients')
const cookingModeVisible = ref(false)
const finishedDishReviewVisible = ref(false)
const keywordDraft = ref('')
const keyword = ref('')
const mealType = ref('')
const goal = ref('')
const limit = 50
const offset = ref(0)
const pantryItems = ref([])
const shoppingCheckOverrides = ref({})
const shoppingCheckSavingKey = ref('')
const stockInKey = ref('')
const stockInDialogVisible = ref(false)
const stockInItem = ref(null)
const feedbackReaction = ref(null)
const feedbackCooked = ref(false)
const feedbackLoading = ref(false)
const auth = useAuthStore()
let listRequestId = 0
let detailRequestId = 0

const ownedIngredients = computed(() => [
  selected.value?.searchIngredients || '',
  ...pantryItems.value.map((item) => item.ingredientName)
])
const shoppingList = computed(() => buildRecipeShoppingList(
  selected.value?.recipe,
  ownedIngredients.value
))
const ingredientAnalysisRows = computed(() => shoppingList.value.map((item) => {
  const missing = findMissingIngredient(selected.value?.recipe?.missingIngredients, item.name)
  return {
    ...item,
    substitutesText: item.alreadyOwned ? '' : formatSubstitutes(missing?.substitutes),
    reason: item.alreadyOwned ? '可直接使用现有食材' : missing?.reason || ''
  }
}))
const explanationItems = computed(() => {
  const explanation = selected.value?.recipe?.explanation || {}
  return [
    { key: 'pairingLogic', label: '搭配逻辑', content: explanation.pairingLogic, icon: markRaw(Network) },
    { key: 'nutrition', label: '营养说明', content: explanation.nutrition, icon: markRaw(HeartPulse) },
    { key: 'cookingPrinciple', label: '烹饪原理', content: explanation.cookingPrinciple, icon: markRaw(Flame) }
  ].filter((item) => item.content)
})
const videoKeywords = computed(() => filterVideoKeywords(selected.value?.recipe?.videoKeywords))
const cookingStorageKey = computed(() => `ai_smart_recipe:cooking:saved:${selected.value?.id || 'draft'}`)

const recipePages = computed(() => {
  const recipe = selected.value?.recipe
  if (!recipe) {
    return []
  }

  return [
    recipe.ingredients?.length ? { key: 'ingredients', label: '所需食材' } : null,
    ingredientAnalysisRows.value.length ? { key: 'analysis', label: '食材分析' } : null,
    shoppingList.value.length ? { key: 'shopping', label: '采购清单' } : null,
    explanationItems.value.length ? { key: 'explanation', label: 'AI 解释' } : null,
    recipe.steps?.length ? { key: 'steps', label: '烹饪步骤' } : null,
    recipe.tips?.length ? { key: 'tips', label: '烹饪建议' } : null,
    videoKeywords.value.length ? { key: 'videos', label: '视频关键词' } : null
  ].filter(Boolean)
})

onMounted(() => {
  loadPantryItems()
  loadRecipes()
})

async function loadRecipes(preferredId = null) {
  const requestId = ++listRequestId
  loading.value = true
  try {
    const response = await getSavedRecipes({
      keyword: keyword.value,
      mealType: mealType.value,
      goal: goal.value,
      limit,
      offset: offset.value
    })
    if (requestId !== listRequestId) {
      return
    }

    const nextRecipes = response.data.data || []
    recipes.value = nextRecipes
    if (!nextRecipes.length) {
      detailRequestId += 1
      detailLoading.value = false
      selected.value = null
      resetRecommendationFeedback()
      return
    }

    const nextId = preferredId && nextRecipes.some((item) => item.id === preferredId)
      ? preferredId
      : nextRecipes[0].id
    selected.value = null
    await openRecipe(nextId)
  } catch (error) {
    if (requestId === listRequestId) {
      ElMessage.error(getErrorMessage(error))
    }
  } finally {
    if (requestId === listRequestId) {
      loading.value = false
    }
  }
}

function applyFilters() {
  keyword.value = keywordDraft.value.trim()
  offset.value = 0
  loadRecipes()
}

async function openRecipe(id) {
  const requestId = ++detailRequestId
  detailLoading.value = true
  try {
    const response = await getSavedRecipe(id)
    if (requestId !== detailRequestId) {
      return
    }
    selected.value = response.data.data
    resetRecommendationFeedback()
    activePage.value = recipePages.value[0]?.key || 'ingredients'
    await loadRecommendationFeedback(selected.value?.searchLogId)
    await loadShoppingChecks()
  } catch (error) {
    if (requestId === detailRequestId) {
      ElMessage.error(getErrorMessage(error))
    }
  } finally {
    if (requestId === detailRequestId) {
      detailLoading.value = false
    }
  }
}

async function confirmDeleteRecipe(item) {
  if (deletingId.value !== null) {
    return
  }

  try {
    await ElMessageBox.confirm(
      `删除“${item.title}”后无法恢复，确定继续吗？`,
      '删除菜谱',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    return
  }

  const selectedId = selected.value?.id
  deletingId.value = item.id
  try {
    await deleteSavedRecipe(item.id)
    ElMessage.success('菜谱已删除')
    await loadRecipes(selectedId === item.id ? null : selectedId)
  } catch (error) {
    ElMessage.error(getDeleteErrorMessage(error))
  } finally {
    deletingId.value = null
  }
}

function buildRecipeShoppingList(recipe, ownedIngredients) {
  if (!recipe) {
    return []
  }

  return buildShoppingList(recipe.ingredients, parseIngredientNames(ownedIngredients)).map((item) => ({
    ...item,
    purchaseLinks: item.purchaseLinks || buildPurchaseLinks(item.name)
  }))
}

async function loadPantryItems() {
  try {
    const response = await getPantryItems()
    pantryItems.value = response.data.data || []
  } catch (error) {
    pantryItems.value = []
    ElMessage.warning(getErrorMessage(error))
  }
}

async function loadShoppingChecks() {
  const searchLogId = selected.value?.searchLogId
  if (!searchLogId) {
    shoppingCheckOverrides.value = {}
    return
  }

  shoppingCheckOverrides.value = {}
  try {
    const response = await getShoppingItemChecks(searchLogId)
    if (selected.value?.searchLogId !== searchLogId) {
      return
    }
    shoppingCheckOverrides.value = (response.data.data || []).reduce((overrides, item) => {
      const key = shoppingChecklistKey(item.ingredientName)
      if (key) {
        overrides[key] = normalizeShoppingStatus(item.status, item.checked)
      }
      return overrides
    }, {})
  } catch (error) {
    if (error?.response?.status !== 403 && error?.response?.status !== 404) {
      ElMessage.warning('采购清单状态加载失败，可继续手动更新状态')
    }
  }
}

async function toggleShoppingItem({ item, status }) {
  const key = shoppingChecklistKey(item?.name)
  if (!key || !status || shoppingCheckSavingKey.value) {
    return
  }

  const previousExists = Object.prototype.hasOwnProperty.call(shoppingCheckOverrides.value, key)
  const previousValue = shoppingCheckOverrides.value[key]
  shoppingCheckOverrides.value = {
    ...shoppingCheckOverrides.value,
    [key]: status
  }

  const searchLogId = selected.value?.searchLogId
  if (!searchLogId) {
    return
  }

  shoppingCheckSavingKey.value = key

  try {
    const response = await saveShoppingItemCheck({
      searchLogId,
      ingredientName: item.name,
      status
    })
    if (selected.value?.searchLogId === searchLogId) {
      shoppingCheckOverrides.value = {
        ...shoppingCheckOverrides.value,
        [key]: normalizeShoppingStatus(response.data.data?.status, status)
      }
    }
  } catch (error) {
    const restored = { ...shoppingCheckOverrides.value }
    if (previousExists) {
      restored[key] = previousValue
    } else {
      delete restored[key]
    }
    shoppingCheckOverrides.value = restored
    ElMessage.error('采购清单状态保存失败，请重试')
  } finally {
    if (shoppingCheckSavingKey.value === key) {
      shoppingCheckSavingKey.value = ''
    }
  }
}

function handleStockIn(item) {
  const key = shoppingChecklistKey(item?.name)
  if (!key || stockInKey.value || !selected.value?.id) return
  stockInItem.value = item
  stockInDialogVisible.value = true
}

async function handleStockInConfirm(payload) {
  const item = stockInItem.value
  const key = shoppingChecklistKey(item?.name)
  if (!key || stockInKey.value || !selected.value?.id) return
  stockInKey.value = key
  try {
    await stockInPantry({
      sourceType: 'RECIPE',
      sourceId: selected.value.id,
      idempotencyKey: createClientKey(),
      ingredientName: item.name,
      quantity: payload.quantity,
      unit: payload.unit,
      category: payload.category,
      expireDate: payload.expireDate
    })
    shoppingCheckOverrides.value = { ...shoppingCheckOverrides.value, [key]: 'READY' }
    stockInDialogVisible.value = false
    ElMessage.success(`${item.name} 已加入库存`)
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '入库失败，请稍后重试')
  } finally {
    stockInKey.value = ''
  }
}

function createClientKey() {
  return globalThis.crypto?.randomUUID?.() || `stock-in-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

function findMissingIngredient(missingIngredients, ingredientName) {
  const target = parseIngredientNames([ingredientName])[0]?.toLocaleLowerCase()
  if (!target) {
    return null
  }

  return (Array.isArray(missingIngredients) ? missingIngredients : []).find((item) => {
    const sourceName = typeof item === 'string' ? item : item?.name
    const normalized = parseIngredientNames([sourceName])[0]?.toLocaleLowerCase()
    return normalized === target
  }) || null
}

function formatSubstitutes(substitutes) {
  if (!Array.isArray(substitutes)) {
    return typeof substitutes === 'string' ? substitutes : ''
  }
  return substitutes
    .map((item) => typeof item === 'string' ? item : item?.name)
    .filter(Boolean)
    .join('、')
}

function mealLabel(value) {
  return {
    any: '不限餐次',
    breakfast: '早餐',
    lunch: '午餐',
    dinner: '晚餐'
  }[value] || '未指定餐次'
}

async function preparePlatformSearch(ingredientName) {
  try {
    if (await copyIngredientName(ingredientName, navigator.clipboard)) {
      ElMessage.info(`已复制“${ingredientName}”，请在平台中粘贴搜索`)
    }
  } catch {
    ElMessage.info(`请在平台中搜索“${ingredientName}”`)
  }
}

function formatDate(value) {
  if (!value) {
    return '刚刚保存'
  }
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}

function openCookingMode() {
  if (!selected.value?.recipe?.steps?.length) {
    ElMessage.warning('当前菜谱暂无可执行的烹饪步骤')
    return
  }
  cookingModeVisible.value = true
}

function openFinishedDishReview() {
  if (!selected.value?.recipe?.title) {
    ElMessage.warning('当前菜谱信息不完整，暂时无法评价')
    return
  }
  finishedDishReviewVisible.value = true
}

function resetRecommendationFeedback() {
  feedbackReaction.value = null
  feedbackCooked.value = false
  feedbackLoading.value = false
}

async function loadRecommendationFeedback(searchLogId) {
  if (!searchLogId) {
    resetRecommendationFeedback()
    return
  }
  const token = auth.token
  try {
    const response = await getRecommendationFeedback(searchLogId)
    if (auth.token !== token || selected.value?.searchLogId !== searchLogId) {
      return
    }
    const feedback = normalizeRecommendationFeedback(response.data.data)
    feedbackReaction.value = feedback.reaction
    feedbackCooked.value = feedback.cooked
  } catch (error) {
    if (auth.token === token && selected.value?.searchLogId === searchLogId) {
      resetRecommendationFeedback()
      if (error?.response?.status !== 404) {
        ElMessage.warning(getFeedbackErrorMessage(error))
      }
    }
  }
}

async function toggleRecommendationReaction(reaction) {
  const searchLogId = selected.value?.searchLogId
  if (!searchLogId || feedbackLoading.value) {
    return
  }
  const previousReaction = feedbackReaction.value
  feedbackLoading.value = true
  try {
    const nextReaction = nextRecommendationReaction(previousReaction, reaction)
    const response = nextReaction === null
      ? await clearRecommendationReaction(searchLogId)
      : await setRecommendationReaction(searchLogId, reaction)
    const feedback = normalizeRecommendationFeedback(response.data.data)
    feedbackReaction.value = feedback.reaction
    feedbackCooked.value = feedback.cooked
    ElMessage.success(feedbackReaction.value ? '推荐偏好已记录' : '推荐偏好已取消')
  } catch (error) {
    feedbackReaction.value = previousReaction
    ElMessage.error(getFeedbackErrorMessage(error))
  } finally {
    feedbackLoading.value = false
  }
}

function getFeedbackErrorMessage(error) {
  const status = error?.response?.status
  if (status === 401) {
    return '登录状态已失效，请重新登录后保存推荐偏好'
  }
  if (status === 403) {
    return '当前账号没有操作该推荐记录的权限'
  }
  if (status === 404) {
    return '推荐记录不存在或已过期'
  }
  return error?.response?.data?.message || '推荐反馈保存失败，请稍后重试'
}

function getErrorMessage(error) {
  const status = error?.response?.status
  if (status === 401) {
    return '请先登录后查看我的菜谱'
  }
  if (status === 403) {
    return '当前账号没有访问权限'
  }
  return error?.response?.data?.message || error?.message || '菜谱加载失败，请稍后重试'
}

function getDeleteErrorMessage(error) {
  const status = error?.response?.status
  if (status === 401) {
    return '登录状态已失效，请重新登录后删除'
  }
  if (status === 403) {
    return '当前账号没有删除权限'
  }
  if (status === 404) {
    return '菜谱不存在或已被删除'
  }
  return '删除失败，请稍后重试'
}
</script>

<style scoped>
.saved-page {
  min-height: calc(100vh - 58px);
  padding: clamp(20px, 3vw, 36px);
  color: var(--app-text);
}

.saved-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  max-width: 1440px;
  margin: 0 auto 20px;
}

.eyebrow,
.panel-kicker {
  margin: 0;
  color: var(--app-text-muted);
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.12em;
}

.saved-heading h1,
.detail-heading h2,
.panel-heading h2 {
  margin: 7px 0 0;
  letter-spacing: 0;
}

.saved-heading p:not(.eyebrow),
.saved-time {
  margin: 8px 0 0;
  color: var(--app-text-muted);
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  min-height: 36px;
  padding: 0 11px;
  border: 1px solid var(--app-line-strong);
  border-radius: 6px;
  color: var(--app-text);
  background: var(--app-surface);
  font-size: 13px;
  font-weight: 800;
}

.history-layout {
  display: grid;
  grid-template-columns: minmax(250px, 330px) minmax(0, 1fr);
  gap: 18px;
  max-width: 1440px;
  min-height: 560px;
  margin: 0 auto;
}

.history-panel,
.detail-panel {
  min-width: 0;
  border: 1px solid var(--app-line);
  background: var(--app-surface);
  box-shadow: var(--app-panel-shadow);
}

.history-panel {
  padding: 16px;
}

.recipe-filters {
  display: grid;
  gap: 8px;
  margin-top: 14px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--app-line);
}

.filter-search-row,
.filter-select-row {
  display: grid;
  gap: 7px;
}

.filter-search-row {
  grid-template-columns: minmax(0, 1fr) 36px;
}

.filter-select-row {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.recipe-filters :deep(.el-select) {
  width: 100%;
}

.search-button,
.delete-button {
  display: inline-grid;
  place-items: center;
  border: 1px solid var(--app-line-strong);
  border-radius: 6px;
  color: var(--app-text);
  background: var(--app-surface);
  cursor: pointer;
  transition: border-color 180ms ease, color 180ms ease, background-color 180ms ease;
}

.search-button {
  width: 36px;
  height: 32px;
}

.search-button:hover,
.search-button:focus-visible,
.delete-button:hover,
.delete-button:focus-visible {
  border-color: var(--app-accent);
  color: var(--app-accent);
  background: var(--app-surface-soft);
  outline: none;
}

.panel-heading,
.detail-heading,
.step-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.detail-heading-actions {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.start-cooking-button :deep(span),
.finished-dish-review-button :deep(span) {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.count-badge,
.detail-id {
  display: inline-grid;
  min-width: 28px;
  min-height: 28px;
  place-items: center;
  border: 1px solid var(--app-line-strong);
  border-radius: 6px;
  color: var(--app-text);
  font-size: 12px;
  font-weight: 900;
}

.history-items {
  display: grid;
  gap: 8px;
  margin-top: 14px;
}

.history-item-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 34px;
  gap: 4px;
  width: 100%;
  border: 1px solid var(--app-line);
  border-radius: 6px;
  color: var(--app-text);
  background: var(--app-surface);
  transition: border-color 180ms ease, background-color 180ms ease;
}

.history-item-row:hover,
.history-item-row.active {
  border-color: var(--app-accent);
  background: var(--app-surface-soft);
}

.history-item {
  display: grid;
  gap: 6px;
  min-width: 0;
  padding: 11px 5px 11px 11px;
  border: 0;
  color: inherit;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.delete-button {
  align-self: center;
  width: 30px;
  height: 30px;
  border-color: transparent;
  color: var(--app-text-muted);
  background: transparent;
}

.delete-button:disabled {
  cursor: wait;
  opacity: 0.45;
}

.history-item-title {
  overflow: hidden;
  font-size: 14px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-item-ingredients {
  overflow: hidden;
  color: var(--app-text-muted);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-item-meta {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  color: var(--app-text-faint);
  font-size: 11px;
}

.detail-panel {
  padding: 22px;
}

.detail-heading {
  padding-bottom: 15px;
  border-bottom: 1px solid var(--app-line);
}

.saved-time {
  font-size: 12px;
}

.recipe-detail {
  padding-top: 17px;
}

.summary {
  margin: 0;
  color: var(--app-text-soft);
  line-height: 1.7;
}

.tag-row,
.video-keywords {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 13px;
}

.system-tag,
.video-keywords span,
.video-keywords a {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 5px 8px;
  border: 1px solid var(--app-line-strong);
  border-radius: 4px;
  color: var(--app-text);
  text-decoration: none;
  color: var(--app-text-soft);
  background: var(--app-surface-soft);
  font-size: 12px;
}

.recipe-pages {
  margin-top: 20px;
}

.page-tabs {
  display: flex;
  gap: 4px;
  overflow-x: auto;
  border-bottom: 1px solid var(--app-line);
}

.page-tab {
  min-height: 37px;
  padding: 0 12px;
  border: 0;
  border-bottom: 2px solid transparent;
  color: var(--app-text-muted);
  background: transparent;
  font: inherit;
  font-size: 13px;
  font-weight: 800;
  white-space: nowrap;
  cursor: pointer;
}

.page-tab.active {
  border-bottom-color: var(--app-accent);
  color: var(--app-text);
}

.recipe-window {
  min-height: 220px;
  padding-top: 14px;
}

.step-list,
.tip-list {
  display: grid;
  gap: 12px;
  margin: 0;
  padding-left: 22px;
}

.step-list li,
.tip-list li {
  color: var(--app-text-soft);
  line-height: 1.7;
}

.step-title-row span {
  color: var(--app-text-muted);
  font-size: 12px;
}

.step-list p {
  margin: 3px 0 0;
}

.ingredient-state {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 7px;
  border: 1px solid var(--app-line-strong);
  border-radius: 4px;
  font-size: 11px;
  font-weight: 900;
}

.ingredient-state.owned {
  color: var(--el-color-success);
  background: var(--app-surface-soft);
}

.ingredient-state.missing {
  color: var(--el-color-warning);
  background: var(--app-surface-soft);
}

.purchase-links {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.purchase-links a {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-height: 26px;
  padding: 0 7px;
  border: 1px solid var(--app-line-strong);
  border-radius: 4px;
  color: var(--app-text);
  background: var(--app-surface-soft);
  font-size: 12px;
  font-weight: 800;
}

.explanation-grid {
  display: grid;
  gap: 10px;
}

.explanation-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 10px;
  padding: 12px;
  border: 1px solid var(--app-line);
  border-radius: 6px;
  background: var(--app-surface-soft);
}

.explanation-item h3,
.explanation-item p {
  margin: 0;
}

.explanation-item h3 {
  color: var(--app-text);
  font-size: 14px;
}

.explanation-item p {
  margin-top: 5px;
  color: var(--app-text-soft);
  line-height: 1.65;
}

@media (max-width: 900px) {
  .history-layout {
    grid-template-columns: 1fr;
  }

  .history-panel {
    min-height: auto;
  }
}

@media (max-width: 560px) {
  .saved-heading {
    flex-direction: column;
  }

  .saved-page {
    padding: 18px 14px;
  }

  .detail-panel {
    padding: 16px;
  }

  .detail-heading {
    align-items: flex-start;
  }

  .detail-heading-actions {
    flex-wrap: wrap;
  }
}
</style>
