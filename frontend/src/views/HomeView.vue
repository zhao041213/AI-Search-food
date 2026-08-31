<template>
  <main class="home-page">
    <section class="command-shell" aria-labelledby="home-title">
      <header class="workspace-heading">
        <div>
          <p class="eyebrow">AI 菜谱指挥舱</p>
          <h1 id="home-title">AI 食材智能工作台</h1>
        </div>
        <div class="signal-strip" aria-label="当前推荐信号">
          <span>{{ searchModeLabel }}</span>
          <span>{{ mealTypeLabel }}</span>
          <span>{{ goalLabel }}</span>
        </div>
      </header>

      <div v-if="auth.isUser && pantryExpiryNotice" class="pantry-expiry-banner" role="alert">
        <TriangleAlert :size="17" aria-hidden="true" />
        <div>
          <strong>库存保质期提醒</strong>
          <span>{{ pantryExpiryNoticeText }}</span>
        </div>
        <RouterLink class="pantry-expiry-link" to="/pantry">查看库存</RouterLink>
      </div>

      <div class="command-grid">
        <section class="search-panel" aria-label="菜谱搜索表单">
          <div class="panel-title">
            <ScanSearch :size="20" aria-hidden="true" />
            <div>
              <span>输入信号</span>
              <strong>食材识别与推荐参数</strong>
            </div>
          </div>

          <el-form class="search-form" label-position="top" @submit.prevent="runSearch">
            <el-form-item label="食材清单">
              <RecentSearchPopover
                :items="recentSearches"
                :loading="recentSearchLoading"
                :visible="auth.isUser && recentSearchVisible"
                @select="applyRecentSearch"
                @update:visible="recentSearchVisible = $event"
              >
                <el-input
                  v-model="ingredients"
                  type="textarea"
                  :rows="3"
                  resize="none"
                  maxlength="240"
                  show-word-limit
                  placeholder="例如：番茄、鸡蛋、菠菜"
                  @focus="openRecentSearches"
                  @click="openRecentSearches"
                />
              </RecentSearchPopover>
            </el-form-item>

            <div class="filters">
              <el-form-item label="餐次">
                <el-select v-model="mealType" placeholder="请选择餐次">
                  <el-option label="不限餐次" value="any" />
                  <el-option label="早餐" value="breakfast" />
                  <el-option label="午餐" value="lunch" />
                  <el-option label="晚餐" value="dinner" />
                </el-select>
              </el-form-item>

              <el-form-item>
                <template #label>
                  <div class="goal-label-row">
                    <span>目标</span>
                    <el-button v-if="auth.isUser" link type="primary" @click="preferenceDialogVisible = true">
                      <SlidersHorizontal :size="14" aria-hidden="true" />
                      <span>饮食偏好</span>
                    </el-button>
                  </div>
                </template>
                <el-select v-model="goal" placeholder="请选择烹饪目标" @change="goalManuallySelected = true">
                  <el-option label="营养均衡" value="balanced" />
                  <el-option label="高蛋白" value="protein" />
                  <el-option label="低热量" value="light" />
                  <el-option label="快速烹饪" value="quick" />
                  <el-option label="减脂" value="fat_loss" />
                  <el-option label="增肌" value="muscle_gain" />
                  <el-option label="控糖" value="low_sugar" />
                </el-select>
              </el-form-item>
            </div>

            <div class="mode-panel" aria-label="搜索方式">
              <span class="field-label">搜索方式</span>
              <div class="mode-grid">
                <button
                  v-for="mode in modeCards"
                  :key="mode.value"
                  class="mode-card"
                  :class="{ active: searchMode === mode.value }"
                  type="button"
                  :aria-pressed="searchMode === mode.value"
                  :disabled="mode.disabled"
                  @click="searchMode = mode.value"
                >
                  <component :is="mode.icon" :size="18" aria-hidden="true" />
                  <strong>{{ mode.label }}</strong>
                  <span>{{ mode.description }}</span>
                </button>
              </div>
            </div>

            <div v-if="showImageUpload" class="image-upload-panel" aria-label="上传图片识别食材">
              <input
                ref="imageInput"
                class="visually-hidden"
                type="file"
                accept="image/jpeg,image/png,image/webp"
                @change="handleImageSelected"
              />

              <button class="upload-dropzone" type="button" @click="openImagePicker">
                <img v-if="selectedImagePreview" :src="selectedImagePreview" alt="已选择的食材图片预览" />
                <span v-else class="upload-empty">
                  <ImagePlus :size="20" aria-hidden="true" />
                  <strong>上传食材图片</strong>
                  <em>支持 JPG、PNG、WebP，最大 5MB</em>
                </span>
              </button>

              <div class="image-actions">
                <el-button plain @click="openImagePicker">
                  <ImagePlus :size="16" aria-hidden="true" />
                  <span>{{ selectedImageFile ? '更换图片' : '选择图片' }}</span>
                </el-button>
                <el-button
                  type="primary"
                  :disabled="!selectedImageFile"
                  :loading="recognizing"
                  @click="recognizeUploadedImage"
                >
                  <ScanSearch :size="16" aria-hidden="true" />
                  <span>识别食材</span>
                </el-button>
              </div>

              <div v-if="recognizedIngredients.length" class="recognized-result">
                <span v-for="ingredient in recognizedIngredients" :key="ingredient">
                  {{ ingredient }}
                </span>
              </div>
              <p v-if="recognitionDescription" class="recognition-summary">
                {{ recognitionDescription }}
              </p>
            </div>

            <div v-else-if="showCameraCapture" class="camera-capture-panel" aria-label="拍照识别食材">
              <div class="camera-capture-copy">
                <span class="camera-capture-icon" aria-hidden="true">
                  <Camera :size="20" />
                </span>
                <div>
                  <strong>拍照识别食材</strong>
                  <p>打开摄像头拍摄食材，照片会自动提交给 AI 识别。</p>
                </div>
              </div>
              <el-button type="primary" :loading="recognizing" @click="openCameraCapture">
                <Camera :size="16" aria-hidden="true" />
                <span>打开摄像头</span>
              </el-button>

              <div v-if="recognizedIngredients.length" class="recognized-result camera-recognized-result">
                <span v-for="ingredient in recognizedIngredients" :key="ingredient">
                  {{ ingredient }}
                </span>
              </div>
              <p v-if="recognitionDescription" class="recognition-summary camera-recognition-summary">
                {{ recognitionDescription }}
              </p>
            </div>

            <div class="search-actions">
              <el-button
                type="primary"
                size="large"
                :loading="generating || (auth.isUser && preferenceLoading)"
                @click="runSearch"
              >
                <Sparkles :size="18" aria-hidden="true" />
                <span>生成推荐</span>
              </el-button>
              <el-button size="large" plain @click="resetSearch">
                <RotateCcw :size="18" aria-hidden="true" />
                <span>重置</span>
              </el-button>
            </div>
          </el-form>
        </section>

        <section class="result-panel" v-loading="generating" aria-label="菜谱搜索结果">
          <div class="result-header">
            <div>
              <p class="eyebrow">菜谱输出窗口</p>
              <h2>{{ resultTitle }}</h2>
            </div>
            <div class="result-header-actions">
              <el-button
                v-if="recipe?.steps?.length"
                class="start-cooking-button"
                type="primary"
                @click="openCookingMode"
              >
                <Play :size="16" aria-hidden="true" />
                <span>开始烹饪</span>
              </el-button>
              <el-button
                v-if="recipe"
                class="finished-dish-review-button"
                plain
                :disabled="generating"
                @click="openFinishedDishReview"
              >
                <Sparkles :size="16" aria-hidden="true" />
                <span>评价成品</span>
              </el-button>
              <el-dropdown
                v-if="recipe"
                trigger="click"
                :disabled="generating || savingRecipe"
                @command="regenerateCurrentRecipe"
              >
                <el-button
                  class="regenerate-recipe-button"
                  plain
                  :loading="regenerating"
                  :disabled="generating || savingRecipe"
                >
                  <RefreshCw :size="16" aria-hidden="true" />
                  <span>重新生成</span>
                  <ChevronDown :size="15" aria-hidden="true" />
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item
                      v-for="option in regenerationOptions"
                      :key="option.value"
                      :command="option.value"
                    >
                      {{ option.label }}
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <el-button
                v-if="recipe"
                class="save-recipe-button"
                :type="savedRecipeId ? 'success' : 'primary'"
                plain
                :loading="savingRecipe"
                :disabled="Boolean(savedRecipeId) || generating"
                @click="saveCurrentRecipe"
              >
                <Bookmark :size="16" aria-hidden="true" />
                <span>{{ savedRecipeId ? '已保存' : '保存到我的菜谱' }}</span>
              </el-button>
              <span class="status-pill">{{ generating ? '生成中' : searchModeLabel }}</span>
            </div>
          </div>

          <div v-if="!hasSearch" class="empty-stage">
            <ChefHat :size="44" aria-hidden="true" />
            <strong>等待食材信号</strong>
            <span>输入食材后，系统会在右侧生成菜谱、功效、步骤和视频关键词。</span>
          </div>

          <div v-else class="result-content">
            <dl class="brief-grid">
              <div>
                <dt>食材</dt>
                <dd>{{ cleanIngredients }}</dd>
              </div>
              <div>
                <dt>餐次</dt>
                <dd>{{ mealTypeLabel }}</dd>
              </div>
              <div>
                <dt>目标</dt>
                <dd>{{ goalLabel }}</dd>
              </div>
            </dl>

            <div v-if="recipe" class="recipe-detail">
              <div class="recipe-summary-block">
                <p>{{ recipe.summary }}</p>
                <div v-if="recipe.effects?.length" class="tag-row" aria-label="菜谱功效">
                  <span v-for="effect in recipe.effects" :key="effect" class="system-tag">
                    {{ effect }}
                  </span>
                </div>
              </div>

              <div class="recipe-pages" aria-label="菜谱详情分页">
                <div class="page-toolbar">
                  <button
                    class="page-arrow"
                    type="button"
                    aria-label="上一页"
                    :disabled="recipePages.length <= 1"
                    @click="previousRecipePage"
                  >
                    <ChevronLeft :size="19" aria-hidden="true" />
                  </button>

                  <div class="page-tabs" role="tablist" aria-label="菜谱页面">
                    <button
                      v-for="(page, index) in recipePages"
                      :key="page.key"
                      class="page-tab"
                      :class="{ active: index === activeRecipePageIndex }"
                      type="button"
                      role="tab"
                      :aria-selected="index === activeRecipePageIndex"
                      @click="currentRecipePage = index"
                    >
                      {{ page.label }}
                    </button>
                  </div>

                  <button
                    class="page-arrow"
                    type="button"
                    aria-label="下一页"
                    :disabled="recipePages.length <= 1"
                    @click="nextRecipePage"
                  >
                    <ChevronRight :size="19" aria-hidden="true" />
                  </button>
                </div>

                <section
                  v-if="activeRecipePage"
                  class="recipe-page-window"
                  :aria-labelledby="`recipe-page-${activeRecipePage.key}`"
                >
                  <div class="window-head">
                    <h3 :id="`recipe-page-${activeRecipePage.key}`">{{ activeRecipePage.label }}</h3>
                    <span>{{ activeRecipePageIndex + 1 }} / {{ recipePages.length || 1 }}</span>
                  </div>

                  <el-table v-if="activeRecipePage.key === 'ingredients'" :data="recipe.ingredients" size="large">
                    <el-table-column prop="name" label="食材" min-width="120" />
                    <el-table-column prop="amount" label="用量" min-width="120" />
                  </el-table>

                  <el-table
                    v-else-if="activeRecipePage.key === 'analysis'"
                    :data="ingredientAnalysisRows"
                    size="large"
                  >
                    <el-table-column prop="name" label="食材" min-width="100" />
                    <el-table-column prop="amount" label="用量" min-width="88" />
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
                    v-else-if="activeRecipePage.key === 'shopping'"
                    :items="shoppingList"
                    :overrides="shoppingCheckOverrides"
                    :saving-key="shoppingCheckSavingKey"
                    @status-change="toggleShoppingItem"
                    @purchase-search="preparePlatformSearch"
                  />

                  <div v-else-if="activeRecipePage.key === 'explanation'" class="explanation-grid">
                    <article v-for="item in explanationItems" :key="item.key" class="explanation-item">
                      <component :is="item.icon" :size="18" aria-hidden="true" />
                      <div>
                        <h4>{{ item.label }}</h4>
                        <p>{{ item.content }}</p>
                      </div>
                    </article>
                  </div>

                  <ol v-else-if="activeRecipePage.key === 'steps'" class="step-list">
                    <li v-for="step in recipe.steps" :key="step.order || step.title">
                      <strong>{{ step.title }}</strong>
                      <span v-if="step.durationMinutes">约 {{ step.durationMinutes }} 分钟</span>
                      <p>{{ step.description }}</p>
                    </li>
                  </ol>

                  <ul v-else-if="activeRecipePage.key === 'tips'" class="tip-list">
                    <li v-for="tip in recipe.tips" :key="tip">{{ tip }}</li>
                  </ul>

                  <div v-else-if="activeRecipePage.key === 'videos'" class="video-keywords">
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
                </section>

                <el-empty v-else description="暂无菜谱详情" />
              </div>
            </div>

            <div v-else class="next-steps">
              <div class="window-head">
                <h3>推荐生成队列</h3>
                <span>就绪</span>
              </div>
              <el-table :data="recommendationRows" size="large">
                <el-table-column prop="name" label="关注点" min-width="140" />
                <el-table-column prop="value" label="信号" min-width="140" />
              </el-table>
            </div>
          </div>
        </section>
      </div>
    </section>
  </main>

  <DietPreferenceDialog
    v-if="auth.isUser"
    v-model="preferenceDialogVisible"
    :preference="dietPreference"
    :saving="preferenceSaving"
    @save="persistDietPreference"
  />
  <CameraIngredientCapture v-model="cameraCaptureVisible" @captured="handleCameraCaptured" />
  <CookingModeDialog
    v-if="recipe"
    v-model="cookingModeVisible"
    :recipe="recipe"
    :storage-key="cookingStorageKey"
  />
  <FinishedDishReviewDialog
    v-if="recipe"
    v-model="finishedDishReviewVisible"
    :recipe="recipe"
    :recipe-id="savedRecipeId"
  />
</template>

<script setup>
import { computed, markRaw, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Camera,
  Bookmark,
  ChefHat,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  ExternalLink,
  Flame,
  HeartPulse,
  ImagePlus,
  Network,
  Play,
  RefreshCw,
  RotateCcw,
  ScanSearch,
  SlidersHorizontal,
  Sparkles,
  TriangleAlert,
  Video
} from 'lucide-vue-next'
import { useRoute, useRouter } from 'vue-router'
import { generateRecipe, recognizeIngredients, saveRecipe } from '../api/recipes'
import { getRecentSearches } from '../api/searchHistory'
import { getDietPreference, saveDietPreference } from '../api/userPreferences'
import { getPantryExpiryAlerts, getPantryItems } from '../api/pantry'
import { getShoppingItemChecks, saveShoppingItemCheck } from '../api/shoppingChecks'
import CameraIngredientCapture from '../components/CameraIngredientCapture.vue'
import CookingModeDialog from '../components/CookingModeDialog.vue'
import DietPreferenceDialog from '../components/DietPreferenceDialog.vue'
import FinishedDishReviewDialog from '../components/FinishedDishReviewDialog.vue'
import RecentSearchPopover from '../components/RecentSearchPopover.vue'
import ShoppingChecklistTable from '../components/ShoppingChecklistTable.vue'
import { useAuthStore } from '../stores/auth'
import {
  buildRecipeDietPreference,
  normalizeDietPreference,
  requiresDietPreferenceLoad,
  resolveGoalWithPreference,
  toSearchForm
} from '../utils/personalization'
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

const ingredients = ref('')
const mealType = ref('any')
const goal = ref('balanced')
const goalManuallySelected = ref(false)
const searchMode = ref('text')
const cameraCaptureVisible = ref(false)
const imageInput = ref(null)
const selectedImageFile = ref(null)
const selectedImagePreview = ref('')
const recognizedIngredients = ref([])
const recognitionDescription = ref('')
const lastSearch = ref(null)
const recipe = ref(null)
const generating = ref(false)
const regenerating = ref(false)
const recognizing = ref(false)
const savingRecipe = ref(false)
const savedRecipeId = ref(null)
const currentRecipePage = ref(0)
const cookingModeVisible = ref(false)
const finishedDishReviewVisible = ref(false)
const dietPreference = ref(normalizeDietPreference())
const preferenceDialogVisible = ref(false)
const preferenceLoaded = ref(false)
const preferenceLoading = ref(false)
const preferenceSaving = ref(false)
const recentSearches = ref([])
const recentSearchLoading = ref(false)
const recentSearchLoaded = ref(false)
const recentSearchVisible = ref(false)
const pantryItems = ref([])
const pantryLoading = ref(false)
const pantryExpirySummary = ref(emptyPantryExpirySummary())
const shoppingCheckOverrides = ref({})
const shoppingCheckSavingKey = ref('')
const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const PENDING_RECIPE_KEY = 'ai_smart_recipe_pending_save'

const regenerationOptions = [
  { value: 'simple', label: '更简单' },
  { value: 'light', label: '低油低卡' },
  { value: 'quick', label: '缩短时间' },
  { value: 'taste', label: '调整口味' }
]

const modeCards = [
  { value: 'text', label: '文字输入', description: '手动输入现有食材', icon: markRaw(ScanSearch) },
  { value: 'image', label: '图片识别', description: '上传图片识别食材', icon: markRaw(ImagePlus) },
  { value: 'camera', label: '拍照识别', description: '使用摄像头拍摄食材', icon: markRaw(Camera) }
]

const mealLabels = {
  any: '不限餐次',
  breakfast: '早餐',
  lunch: '午餐',
  dinner: '晚餐'
}

const goalLabels = {
  balanced: '营养均衡',
  protein: '高蛋白',
  light: '低热量',
  quick: '快速烹饪',
  fat_loss: '减脂',
  muscle_gain: '增肌',
  low_sugar: '控糖'
}

const modeLabels = {
  text: '文字输入',
  image: '图片识别',
  camera: '拍照识别'
}

const hasSearch = computed(() => Boolean(lastSearch.value))
const showImageUpload = computed(() => searchMode.value === 'image')
const showCameraCapture = computed(() => searchMode.value === 'camera')
const cookingStorageKey = computed(() => {
  const userKey = auth.isUser ? auth.displayName || 'user' : 'guest'
  const recipeKey = recipe.value?.searchLogId || recipe.value?.title || 'draft'
  return `ai_smart_recipe:cooking:${encodeURIComponent(userKey)}:${encodeURIComponent(String(recipeKey))}`
})
const cleanIngredients = computed(() => lastSearch.value?.ingredients || '暂无')
const ownedIngredients = computed(() => [
  lastSearch.value?.ingredients || ingredients.value,
  ...pantryItems.value.map((item) => item.ingredientName)
])
const pantryExpiryNotice = computed(() => {
  const summary = pantryExpirySummary.value
  return summary.expiredItems.length + summary.expiringSoonItems.length > 0
})
const pantryExpiryNoticeText = computed(() => {
  const summary = pantryExpirySummary.value
  const messages = []
  if (summary.expiredItems.length) {
    messages.push(`${summary.expiredItems.length} 种食材已过期`)
  }
  if (summary.expiringSoonItems.length) {
    messages.push(`${summary.expiringSoonItems.length} 种食材将在 ${summary.warningDays} 天内到期`)
  }
  return messages.join('，')
})
const mealTypeLabel = computed(() => mealLabels[lastSearch.value?.mealType || mealType.value] || mealLabels.any)
const goalLabel = computed(() => goalLabels[lastSearch.value?.goal || goal.value] || goalLabels.balanced)
const searchModeLabel = computed(() => modeLabels[lastSearch.value?.searchMode || searchMode.value])
const resultTitle = computed(() => {
  if (recipe.value?.title) {
    return recipe.value.title
  }
  return hasSearch.value ? '菜谱匹配简报' : '等待搜索'
})
const recommendationRows = computed(() => [
  { name: '营养目标', value: goalLabel.value },
  { name: '用餐时间', value: mealTypeLabel.value },
  { name: '输入方式', value: modeLabels[lastSearch.value?.searchMode] || modeLabels.text }
])
const shoppingList = computed(() => buildRecipeShoppingList(
  recipe.value,
  ownedIngredients.value
))
const ingredientAnalysisRows = computed(() => shoppingList.value.map((item) => {
  const missing = findMissingIngredient(recipe.value?.missingIngredients, item.name)
  return {
    ...item,
    substitutesText: item.alreadyOwned ? '' : formatSubstitutes(missing?.substitutes),
    reason: item.alreadyOwned ? '可直接使用现有食材' : missing?.reason || ''
  }
}))
const explanationItems = computed(() => {
  const explanation = recipe.value?.explanation || {}
  return [
    { key: 'pairingLogic', label: '搭配逻辑', content: explanation.pairingLogic, icon: markRaw(Network) },
    { key: 'nutrition', label: '营养说明', content: explanation.nutrition, icon: markRaw(HeartPulse) },
    { key: 'cookingPrinciple', label: '烹饪原理', content: explanation.cookingPrinciple, icon: markRaw(Flame) }
  ].filter((item) => item.content)
})
const videoKeywords = computed(() => filterVideoKeywords(recipe.value?.videoKeywords))
const recipePages = computed(() => {
  if (!recipe.value) {
    return []
  }

  return [
    recipe.value.ingredients?.length ? { key: 'ingredients', label: '所需食材' } : null,
    ingredientAnalysisRows.value.length ? { key: 'analysis', label: '食材分析' } : null,
    shoppingList.value.length ? { key: 'shopping', label: '采购清单' } : null,
    explanationItems.value.length ? { key: 'explanation', label: 'AI 解释' } : null,
    recipe.value.steps?.length ? { key: 'steps', label: '烹饪步骤' } : null,
    recipe.value.tips?.length ? { key: 'tips', label: '烹饪建议' } : null,
    videoKeywords.value.length ? { key: 'videos', label: '视频关键词' } : null
  ].filter(Boolean)
})
const activeRecipePageIndex = computed(() => {
  if (!recipePages.value.length) {
    return 0
  }
  return Math.min(currentRecipePage.value, recipePages.value.length - 1)
})
const activeRecipePage = computed(() => recipePages.value[activeRecipePageIndex.value])

onBeforeUnmount(() => {
  cameraCaptureVisible.value = false
  cookingModeVisible.value = false
  finishedDishReviewVisible.value = false
  revokeImagePreview()
})

onMounted(() => {
  if (!applyRouteIngredient()) {
    restorePendingRecipe()
  }
  if (auth.isUser) {
    loadDietPreference()
    loadPantryItems()
  }
})

watch(() => [auth.token, auth.role], () => {
  clearPersonalizationState()
  clearPantryState()
  if (auth.isUser) {
    loadDietPreference()
    loadPantryItems()
  }
})

function applyRouteIngredient() {
  const routeIngredient = Array.isArray(route.query.ingredients)
    ? route.query.ingredients[0]
    : route.query.ingredients
  if (!routeIngredient?.trim()) {
    return false
  }
  ingredients.value = routeIngredient.trim()
  searchMode.value = 'text'
  return true
}

async function runSearch() {
  if (requiresDietPreferenceLoad(auth.isUser, preferenceLoaded.value)) {
    const loaded = await loadDietPreference()
    if (!loaded) {
      ElMessage.warning('饮食偏好加载失败，请重试后再生成')
      return
    }
  }

  const normalizedIngredients = parseIngredientNames(ingredients.value).join(', ')

  if (!normalizedIngredients) {
    ElMessage.warning('请至少输入一种食材')
    return
  }

  const request = {
    ingredients: normalizedIngredients,
    mealType: mealType.value,
    goal: goal.value,
    searchMode: searchMode.value,
    dietPreference: buildRecipeDietPreference(dietPreference.value)
  }
  lastSearch.value = request
  recipe.value = null
  savedRecipeId.value = null
  currentRecipePage.value = 0
  shoppingCheckOverrides.value = {}
  generating.value = true

  try {
    const response = await generateRecipe(request)
    recipe.value = response.data.data
    currentRecipePage.value = 0
    recentSearchLoaded.value = false
    await loadShoppingChecks()
    ElMessage.success('菜谱推荐已生成')
  } catch (error) {
    ElMessage.error(getErrorMessage(error))
  } finally {
    generating.value = false
  }
}

function resetSearch() {
  ingredients.value = ''
  mealType.value = 'any'
  goalManuallySelected.value = false
  goal.value = auth.isUser ? dietPreference.value.defaultGoal : 'balanced'
  searchMode.value = 'text'
  cameraCaptureVisible.value = false
  cookingModeVisible.value = false
  finishedDishReviewVisible.value = false
  clearSelectedImage()
  lastSearch.value = null
  recipe.value = null
  savedRecipeId.value = null
  currentRecipePage.value = 0
  shoppingCheckOverrides.value = {}
  window.sessionStorage.removeItem(PENDING_RECIPE_KEY)
}

async function saveCurrentRecipe() {
  if (!recipe.value || savingRecipe.value || savedRecipeId.value) {
    return
  }

  if (!auth.isUser) {
    window.sessionStorage.setItem(PENDING_RECIPE_KEY, JSON.stringify({
      recipe: recipe.value,
      lastSearch: lastSearch.value
    }))
    ElMessage.warning('请先登录，再保存到我的菜谱')
    router.push({ name: 'login', query: { redirect: '/' } })
    return
  }

  if (!recipe.value.searchLogId) {
    ElMessage.error('当前菜谱缺少搜索记录，无法保存')
    return
  }

  savingRecipe.value = true
  try {
    const response = await saveRecipe({
      searchLogId: recipe.value.searchLogId,
      title: recipe.value.title,
      summary: recipe.value.summary,
      effects: recipe.value.effects,
      ingredients: recipe.value.ingredients,
      steps: recipe.value.steps,
      tips: recipe.value.tips,
      videoKeywords: recipe.value.videoKeywords,
      missingIngredients: recipe.value.missingIngredients,
      explanation: recipe.value.explanation,
      provider: recipe.value.provider,
      model: recipe.value.model
    })
    savedRecipeId.value = response.data.data?.id || null
    window.sessionStorage.removeItem(PENDING_RECIPE_KEY)
    ElMessage.success('菜谱已保存到我的菜谱')
  } catch (error) {
    ElMessage.error(getErrorMessage(error))
  } finally {
    savingRecipe.value = false
  }
}

async function regenerateCurrentRecipe(preference) {
  if (!recipe.value || !lastSearch.value || generating.value) {
    return
  }

  if (requiresDietPreferenceLoad(auth.isUser, preferenceLoaded.value)) {
    const loaded = await loadDietPreference()
    if (!loaded) {
      ElMessage.warning('饮食偏好加载失败，请重试后再生成')
      return
    }
  }

  const currentRecipe = recipe.value
  const currentSavedRecipeId = savedRecipeId.value
  const request = {
    ingredients: lastSearch.value.ingredients,
    mealType: lastSearch.value.mealType,
    goal: lastSearch.value.goal,
    searchMode: lastSearch.value.searchMode,
    regenerationPreference: preference,
    previousTitle: currentRecipe.title,
    dietPreference: buildRecipeDietPreference(dietPreference.value)
  }

  generating.value = true
  regenerating.value = true
  try {
    const response = await generateRecipe(request)
    const generatedRecipe = response.data.data
    if (!generatedRecipe) {
      throw new Error('模型未返回新菜谱')
    }
    recipe.value = generatedRecipe
    savedRecipeId.value = null
    currentRecipePage.value = 0
    recentSearchLoaded.value = false
    await loadShoppingChecks()
    window.sessionStorage.removeItem(PENDING_RECIPE_KEY)
    ElMessage.success('新版本菜谱已生成')
  } catch (error) {
    recipe.value = currentRecipe
    savedRecipeId.value = currentSavedRecipeId
    ElMessage.error(getErrorMessage(error))
  } finally {
    regenerating.value = false
    generating.value = false
  }
}

function restorePendingRecipe() {
  const pending = window.sessionStorage.getItem(PENDING_RECIPE_KEY)
  if (!pending) {
    return
  }

  try {
    const draft = JSON.parse(pending)
    if (draft?.recipe && draft?.lastSearch) {
      recipe.value = draft.recipe
      lastSearch.value = draft.lastSearch
      ingredients.value = draft.lastSearch.ingredients || ''
      mealType.value = draft.lastSearch.mealType || 'any'
      goal.value = draft.lastSearch.goal || 'balanced'
      goalManuallySelected.value = true
      searchMode.value = draft.lastSearch.searchMode || 'text'
      currentRecipePage.value = 0
      loadShoppingChecks()
      ElMessage.info('已恢复未保存的菜谱，请点击保存')
    }
  } catch {
    window.sessionStorage.removeItem(PENDING_RECIPE_KEY)
  }
}

async function loadDietPreference() {
  if (!auth.isUser) {
    return true
  }
  if (preferenceLoading.value) {
    return false
  }

  const token = auth.token
  preferenceLoading.value = true
  try {
    const response = await getDietPreference()
    if (!auth.isUser || auth.token !== token) {
      return false
    }
    dietPreference.value = normalizeDietPreference(response.data.data)
    goal.value = resolveGoalWithPreference(
      goal.value,
      dietPreference.value.defaultGoal,
      goalManuallySelected.value
    )
    preferenceLoaded.value = true
    return true
  } catch {
    if (auth.isUser && auth.token === token) {
      ElMessage.warning('饮食偏好加载失败，请稍后重试')
    }
    return false
  } finally {
    if (auth.token === token) {
      preferenceLoading.value = false
    }
  }
}

async function persistDietPreference(value) {
  if (!auth.isUser || preferenceSaving.value) {
    return
  }

  const token = auth.token
  preferenceSaving.value = true
  try {
    const response = await saveDietPreference(normalizeDietPreference(value))
    if (!auth.isUser || auth.token !== token) {
      return
    }
    dietPreference.value = normalizeDietPreference(response.data.data)
    preferenceLoaded.value = true
    goal.value = resolveGoalWithPreference(
      goal.value,
      dietPreference.value.defaultGoal,
      goalManuallySelected.value
    )
    preferenceDialogVisible.value = false
    ElMessage.success('饮食偏好已保存')
  } catch (error) {
    if (auth.isUser && auth.token === token) {
      ElMessage.error(getErrorMessage(error))
    }
  } finally {
    if (auth.token === token) {
      preferenceSaving.value = false
    }
  }
}

async function openRecentSearches() {
  if (!auth.isUser) {
    return
  }

  recentSearchVisible.value = true
  if (recentSearchLoaded.value || recentSearchLoading.value) {
    return
  }

  const token = auth.token
  recentSearchLoading.value = true
  try {
    const response = await getRecentSearches()
    if (!auth.isUser || auth.token !== token) {
      return
    }
    recentSearches.value = (response.data.data || []).slice(0, 5)
    recentSearchLoaded.value = true
  } catch {
    if (auth.isUser && auth.token === token) {
      recentSearchVisible.value = false
      ElMessage({
        type: 'warning',
        message: '最近搜索加载失败，可继续手动输入',
        duration: 2200
      })
    }
  } finally {
    if (auth.token === token) {
      recentSearchLoading.value = false
    }
  }
}

function applyRecentSearch(item) {
  const form = toSearchForm(item)
  ingredients.value = form.ingredients
  mealType.value = form.mealType
  goal.value = form.goal
  goalManuallySelected.value = true
  recentSearchVisible.value = false
}

function clearPersonalizationState() {
  dietPreference.value = normalizeDietPreference()
  preferenceDialogVisible.value = false
  preferenceLoaded.value = false
  preferenceLoading.value = false
  preferenceSaving.value = false
  recentSearches.value = []
  recentSearchLoading.value = false
  recentSearchLoaded.value = false
  recentSearchVisible.value = false
  if (!goalManuallySelected.value) {
    goal.value = 'balanced'
  }
}

function clearPantryState() {
  pantryItems.value = []
  pantryLoading.value = false
  pantryExpirySummary.value = emptyPantryExpirySummary()
  shoppingCheckOverrides.value = {}
  shoppingCheckSavingKey.value = ''
}

function emptyPantryExpirySummary() {
  return {
    asOf: null,
    warningDays: 7,
    expiredItems: [],
    expiringSoonItems: []
  }
}

async function loadPantryItems() {
  if (!auth.isUser || pantryLoading.value) {
    return
  }

  const token = auth.token
  pantryLoading.value = true
  try {
    const [pantryResult, expiryResult] = await Promise.allSettled([
      getPantryItems(),
      getPantryExpiryAlerts()
    ])
    if (pantryResult.status === 'rejected') {
      throw pantryResult.reason
    }
    if (!auth.isUser || auth.token !== token) {
      return
    }
    pantryItems.value = pantryResult.value.data.data || []
    pantryExpirySummary.value = expiryResult.status === 'fulfilled'
      ? expiryResult.value.data.data || emptyPantryExpirySummary()
      : emptyPantryExpirySummary()
    await loadShoppingChecks()
  } catch (error) {
    if (auth.isUser && auth.token === token) {
      pantryItems.value = []
      pantryExpirySummary.value = emptyPantryExpirySummary()
      ElMessage.warning('食材库存加载失败，本次将仅使用输入食材')
    }
  } finally {
    if (auth.token === token) {
      pantryLoading.value = false
    }
  }
}

async function loadShoppingChecks() {
  const searchLogId = recipe.value?.searchLogId
  if (!auth.isUser || !searchLogId) {
    shoppingCheckOverrides.value = {}
    return
  }

  shoppingCheckOverrides.value = {}
  const token = auth.token
  try {
    const response = await getShoppingItemChecks(searchLogId)
    if (!auth.isUser || auth.token !== token || recipe.value?.searchLogId !== searchLogId) {
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
    if (auth.isUser && auth.token === token && error?.response?.status !== 403 && error?.response?.status !== 404) {
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

  const searchLogId = recipe.value?.searchLogId
  if (!auth.isUser || !searchLogId) {
    return
  }

  const token = auth.token
  shoppingCheckSavingKey.value = key
  try {
    const response = await saveShoppingItemCheck({
      searchLogId,
      ingredientName: item.name,
      status
    })
    if (auth.isUser && auth.token === token && recipe.value?.searchLogId === searchLogId) {
      shoppingCheckOverrides.value = {
        ...shoppingCheckOverrides.value,
        [key]: normalizeShoppingStatus(response.data.data?.status, status)
      }
    }
  } catch (error) {
    if (auth.isUser && auth.token === token) {
      const restored = { ...shoppingCheckOverrides.value }
      if (previousExists) {
        restored[key] = previousValue
      } else {
        delete restored[key]
      }
      shoppingCheckOverrides.value = restored
      ElMessage.error('采购清单状态保存失败，请重试')
    }
  } finally {
    if (shoppingCheckSavingKey.value === key) {
      shoppingCheckSavingKey.value = ''
    }
  }
}

function openImagePicker() {
  imageInput.value?.click()
}

function openCameraCapture() {
  if (!recognizing.value) {
    cameraCaptureVisible.value = true
  }
}

function handleImageSelected(event) {
  const file = event.target.files?.[0]
  if (!file) {
    return
  }

  if (!selectImageFile(file)) {
    event.target.value = ''
  }
}

function selectImageFile(file) {
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
    ElMessage.warning('仅支持 JPG、PNG、WebP 图片')
    return false
  }

  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过 5MB')
    return false
  }

  selectedImageFile.value = file
  recognizedIngredients.value = []
  recognitionDescription.value = ''
  revokeImagePreview()
  selectedImagePreview.value = URL.createObjectURL(file)
  return true
}

async function handleCameraCaptured(file) {
  cameraCaptureVisible.value = false
  if (!selectImageFile(file)) {
    return
  }

  await recognizeSelectedImage()
}

async function recognizeUploadedImage() {
  await recognizeSelectedImage()
}

async function recognizeSelectedImage() {
  if (!selectedImageFile.value) {
    ElMessage.warning('请先选择食材图片')
    return
  }

  if (recognizing.value) {
    return
  }

  recognizing.value = true
  try {
    const response = await recognizeIngredients(selectedImageFile.value)
    const result = response.data.data
    recognizedIngredients.value = result?.ingredients || []
    recognitionDescription.value = result?.description || ''
    if (!recognizedIngredients.value.length) {
      ElMessage.warning('未识别到明确食材，请更换图片后重试')
      return
    }
    ingredients.value = mergeIngredients(ingredients.value, recognizedIngredients.value)
    ElMessage.success('食材识别已完成')
  } catch (error) {
    ElMessage.error(getErrorMessage(error))
  } finally {
    recognizing.value = false
  }
}

function mergeIngredients(currentText, newIngredients) {
  const currentIngredients = currentText
    .split(/[，,、\n]/)
    .map((item) => item.trim())
    .filter(Boolean)
  const merged = [...currentIngredients]
  newIngredients.forEach((ingredient) => {
    const normalized = ingredient.trim()
    if (normalized && !merged.includes(normalized)) {
      merged.push(normalized)
    }
  })
  return merged.join('、')
}

function buildRecipeShoppingList(recipeData, ownedIngredients) {
  if (!recipeData) {
    return []
  }

  return buildShoppingList(recipeData.ingredients, parseIngredientNames(ownedIngredients)).map((item) => ({
    ...item,
    purchaseLinks: item.purchaseLinks || buildPurchaseLinks(item.name)
  }))
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

async function preparePlatformSearch(ingredientName) {
  try {
    if (await copyIngredientName(ingredientName, navigator.clipboard)) {
      ElMessage.info(`已复制“${ingredientName}”，请在平台中粘贴搜索`)
    }
  } catch {
    ElMessage.info(`请在平台中搜索“${ingredientName}”`)
  }
}

function openCookingMode() {
  if (!recipe.value?.steps?.length) {
    ElMessage.warning('当前菜谱暂无可执行的烹饪步骤')
    return
  }
  cookingModeVisible.value = true
}

function openFinishedDishReview() {
  if (!recipe.value?.title) {
    ElMessage.warning('当前菜谱信息不完整，暂时无法评价')
    return
  }
  finishedDishReviewVisible.value = true
}

function clearSelectedImage() {
  selectedImageFile.value = null
  recognizedIngredients.value = []
  recognitionDescription.value = ''
  revokeImagePreview()
  if (imageInput.value) {
    imageInput.value.value = ''
  }
}

function revokeImagePreview() {
  if (selectedImagePreview.value) {
    URL.revokeObjectURL(selectedImagePreview.value)
    selectedImagePreview.value = ''
  }
}

function previousRecipePage() {
  if (recipePages.value.length <= 1) {
    return
  }
  currentRecipePage.value = (activeRecipePageIndex.value - 1 + recipePages.value.length) % recipePages.value.length
}

function nextRecipePage() {
  if (recipePages.value.length <= 1) {
    return
  }
  currentRecipePage.value = (activeRecipePageIndex.value + 1) % recipePages.value.length
}

function getErrorMessage(error) {
  const message = error?.response?.data?.message || error?.message
  const messages = {
    '千问 API Key 未配置，请设置 DASHSCOPE_API_KEY': '千问 API Key 未配置，请先设置 DASHSCOPE_API_KEY',
    '千问服务调用失败，请稍后重试': '千问服务调用失败，请稍后重试',
    '千问服务未返回菜谱内容': '千问服务未返回菜谱内容',
    '千问返回内容不是有效菜谱 JSON': '千问返回内容格式异常',
    '千问视觉服务调用失败，请稍后重试': '千问视觉服务调用失败，请稍后重试',
    '千问视觉服务未返回识别内容': '千问视觉服务未返回识别内容',
    '千问视觉返回内容不是有效食材 JSON': '千问视觉返回内容格式异常',
    '请上传食材图片': '请先上传食材图片',
    '图片大小不能超过 5MB': '图片大小不能超过 5MB',
    '仅支持 JPG、PNG、WebP 图片': '仅支持 JPG、PNG、WebP 图片',
    'Invalid request parameters': '请求参数不合法',
    'Network Error': '网络连接失败，请检查后端服务'
  }
  if (message?.toLowerCase().includes('timeout')) {
    return '请求超时，请稍后重试'
  }
  return messages[message] || message || '菜谱生成失败'
}
</script>

<style scoped>
.home-page {
  height: calc(100vh - 58px);
  overflow: hidden;
  padding: clamp(10px, 1.4vw, 18px);
  color: var(--app-text);
}

.command-shell {
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  gap: 10px;
  width: min(1360px, 100%);
  height: 100%;
  margin: 0 auto;
}

.workspace-heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 14px;
}

.eyebrow {
  margin: 0 0 4px;
  color: var(--app-text-muted);
  font-family: "Cascadia Mono", "SFMono-Regular", Consolas, monospace;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0;
}

h1,
h2,
h3 {
  margin: 0;
  color: var(--app-text);
}

h1 {
  font-size: clamp(24px, 2.8vw, 38px);
  line-height: 1.02;
}

h2 {
  font-size: clamp(19px, 1.6vw, 25px);
  line-height: 1.15;
}

h3 {
  font-size: 16px;
}

.signal-strip {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}

.signal-strip span,
.status-pill,
.system-tag,
.video-keywords span,
.video-keywords a {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  padding: 0 8px;
  border: 1px solid var(--app-line-strong);
  border-radius: 999px;
  color: var(--app-text);
  background: var(--app-surface);
  font-size: 11px;
  font-weight: 800;
}

.pantry-expiry-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 42px;
  padding: 8px 12px;
  border: 1px solid var(--app-accent);
  border-radius: 8px;
  color: var(--app-text);
  background: var(--app-accent-soft);
}

.pantry-expiry-banner > svg {
  flex: 0 0 auto;
}

.pantry-expiry-banner > div {
  display: grid;
  gap: 2px;
  min-width: 0;
  flex: 1;
}

.pantry-expiry-banner strong {
  font-size: 13px;
}

.pantry-expiry-banner span {
  color: var(--app-text-muted);
  font-size: 12px;
}

.pantry-expiry-link {
  flex: 0 0 auto;
  color: var(--app-text);
  font-size: 12px;
  font-weight: 800;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.command-grid {
  display: grid;
  grid-template-columns: minmax(310px, 0.78fr) minmax(0, 1.22fr);
  gap: 12px;
  min-height: 0;
}

.search-panel,
.result-panel {
  position: relative;
  min-height: 0;
  overflow: hidden;
  border: 1px solid var(--app-line);
  border-radius: 8px;
  background:
    linear-gradient(90deg, var(--app-grid-line-strong) 1px, transparent 1px),
    linear-gradient(var(--app-grid-line-soft) 1px, transparent 1px),
    var(--app-surface);
  background-size: 28px 28px;
  box-shadow:
    var(--app-panel-shadow),
    inset 0 1px 0 var(--app-grid-line-strong);
}

.search-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 10px;
  padding: clamp(12px, 1.4vw, 18px);
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--app-text);
}

.panel-title > svg {
  flex: 0 0 auto;
}

.panel-title div {
  display: grid;
  gap: 2px;
}

.panel-title span,
.field-label,
.window-head span {
  color: var(--app-text-muted);
  font-family: "Cascadia Mono", "SFMono-Regular", Consolas, monospace;
  font-size: 12px;
  font-weight: 800;
}

.panel-title strong {
  font-size: 16px;
}

.search-form {
  display: grid;
  align-content: start;
  gap: 9px;
  min-height: 0;
}

.search-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.search-form :deep(.el-select) {
  width: 100%;
}

.search-form :deep(.el-textarea__inner) {
  min-height: 86px !important;
}

.filters {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.goal-label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
}

.goal-label-row :deep(.el-button) {
  height: auto;
  min-height: 22px;
  padding: 0;
}

.goal-label-row :deep(.el-button span) {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.mode-panel {
  display: grid;
  gap: 8px;
}

.mode-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 7px;
}

.mode-card {
  display: grid;
  gap: 4px;
  min-width: 0;
  min-height: 82px;
  padding: 9px;
  border: 1px solid var(--app-line);
  border-radius: 8px;
  color: var(--app-text-muted);
  background: var(--app-surface);
  text-align: left;
  cursor: pointer;
  transition:
    border-color 180ms ease,
    background-color 180ms ease,
    color 180ms ease,
    transform 180ms ease;
}

.mode-card:hover,
.mode-card:focus-visible,
.mode-card.active {
  border-color: var(--app-accent);
  color: var(--app-text);
  background: var(--app-surface-soft);
  outline: none;
}

.mode-card.active {
  transform: translateY(-1px);
}

.mode-card:disabled {
  border-style: dashed;
  color: var(--app-text-faint);
  background: var(--app-surface-strong);
  cursor: not-allowed;
  opacity: 0.75;
}

.mode-card:disabled:hover,
.mode-card:disabled:focus-visible {
  border-color: var(--app-line);
  color: var(--app-text-faint);
  background: var(--app-surface-strong);
  transform: none;
}

.mode-card strong {
  overflow: hidden;
  color: inherit;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mode-card span {
  color: var(--app-text-faint);
  font-size: 11px;
  line-height: 1.35;
}

.image-upload-panel {
  display: grid;
  gap: 8px;
}

.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
  white-space: nowrap;
}

.upload-dropzone {
  display: grid;
  place-items: center;
  min-height: 90px;
  padding: 0;
  overflow: hidden;
  border: 1px dashed var(--app-line-strong);
  border-radius: 8px;
  color: var(--app-text-muted);
  background: var(--app-surface);
  cursor: pointer;
  transition:
    border-color 180ms ease,
    background-color 180ms ease;
}

.upload-dropzone:hover,
.upload-dropzone:focus-visible {
  border-color: var(--app-accent);
  background: var(--app-surface-soft);
  outline: none;
}

.upload-dropzone img {
  width: 100%;
  height: 96px;
  object-fit: cover;
}

.upload-empty {
  display: grid;
  place-items: center;
  gap: 4px;
  padding: 10px;
  text-align: center;
}

.upload-empty strong {
  color: var(--app-text);
  font-size: 13px;
}

.upload-empty em {
  color: var(--app-text-faint);
  font-size: 11px;
  font-style: normal;
}

.image-actions {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 8px;
}

.image-actions :deep(.el-button) {
  min-height: 36px;
  margin-left: 0;
}

.image-actions :deep(.el-button span) {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.recognized-result {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.recognized-result span {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 8px;
  border: 1px solid var(--app-line-strong);
  border-radius: 999px;
  color: var(--app-text);
  background: var(--app-surface-strong);
  font-size: 11px;
  font-weight: 800;
}

.recognition-summary {
  margin: 0;
  color: var(--app-text-muted);
  font-size: 11px;
  line-height: 1.5;
}

.camera-capture-panel {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px 12px;
  align-items: center;
  padding: 12px;
  border: 1px solid var(--app-line-strong);
  border-radius: 8px;
  background: var(--app-surface-soft);
}

.camera-capture-copy {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 10px;
}

.camera-capture-icon {
  display: inline-grid;
  width: 34px;
  height: 34px;
  flex: 0 0 auto;
  place-items: center;
  border: 1px solid var(--app-line-strong);
  border-radius: 7px;
  color: var(--app-accent);
  background: var(--app-surface);
}

.camera-capture-copy strong {
  display: block;
  color: var(--app-text);
  font-size: 13px;
}

.camera-capture-copy p {
  margin: 3px 0 0;
  color: var(--app-text-muted);
  font-size: 11px;
  line-height: 1.45;
}

.camera-capture-panel :deep(.el-button) {
  min-height: 36px;
  margin-left: 0;
}

.camera-capture-panel :deep(.el-button span) {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.camera-recognized-result,
.camera-recognition-summary {
  grid-column: 1 / -1;
}

.search-actions {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  margin-top: 2px;
}

.search-actions :deep(.el-button) {
  min-height: 38px;
  margin-left: 0;
}

.search-actions :deep(.el-button span) {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.result-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 9px;
  padding: clamp(12px, 1.4vw, 18px);
}

.result-header {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 12px;
}

.result-header-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
}

.save-recipe-button,
.regenerate-recipe-button,
.start-cooking-button,
.finished-dish-review-button {
  white-space: nowrap;
}

.save-recipe-button {
  --el-button-text-color: #000000;
  --el-button-hover-text-color: #000000;
  --el-button-active-text-color: #000000;
  color: #000000 !important;
}

.regenerate-recipe-button :deep(span),
.save-recipe-button :deep(span),
.start-cooking-button :deep(span),
.finished-dish-review-button :deep(span) {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.empty-stage {
  display: grid;
  place-items: center;
  align-content: center;
  gap: 10px;
  min-height: 0;
  border: 1px dashed var(--app-line-strong);
  border-radius: 8px;
  color: var(--app-text-muted);
  text-align: center;
}

.empty-stage strong {
  color: var(--app-text);
  font-size: 18px;
}

.empty-stage span {
  width: min(420px, 92%);
  line-height: 1.7;
}

.result-content {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 9px;
  min-height: 0;
}

.brief-grid {
  display: grid;
  grid-template-columns: 1.5fr 0.7fr 0.8fr;
  gap: 8px;
  margin: 0;
}

.brief-grid div {
  min-width: 0;
  padding: 9px;
  border: 1px solid var(--app-line);
  border-radius: 8px;
  background: var(--app-surface);
}

.brief-grid dt {
  margin-bottom: 4px;
  color: var(--app-text-faint);
  font-family: "Cascadia Mono", "SFMono-Regular", Consolas, monospace;
  font-size: 12px;
  font-weight: 800;
}

.brief-grid dd {
  margin: 0;
  overflow: hidden;
  color: var(--app-text);
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recipe-detail,
.next-steps {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 9px;
  min-height: 0;
}

.recipe-summary-block {
  display: grid;
  gap: 8px;
  padding: 9px;
  border: 1px solid var(--app-line);
  border-radius: 8px;
  background: var(--app-surface-strong);
}

.recipe-summary-block p {
  display: -webkit-box;
  max-height: 44px;
  margin: 0;
  overflow: hidden;
  color: var(--app-text-soft);
  line-height: 1.55;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.recipe-pages {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 8px;
  min-height: 0;
}

.page-toolbar {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) 34px;
  align-items: center;
  gap: 8px;
}

.page-arrow {
  display: inline-grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border: 1px solid var(--app-line-strong);
  border-radius: 6px;
  color: var(--app-text);
  background: var(--app-surface);
  cursor: pointer;
  transition:
    border-color 180ms ease,
    background-color 180ms ease,
    color 180ms ease;
}

.page-arrow:hover:not(:disabled),
.page-arrow:focus-visible {
  border-color: var(--app-accent);
  background: var(--app-surface-soft);
  outline: none;
}

.page-arrow:disabled {
  cursor: not-allowed;
  opacity: 0.42;
}

.page-tabs {
  display: flex;
  gap: 6px;
  min-width: 0;
  overflow-x: auto;
  scrollbar-width: thin;
}

.page-tab {
  flex: 0 0 auto;
  min-width: 82px;
  min-height: 34px;
  padding: 0 8px;
  border: 1px solid var(--app-line);
  border-radius: 6px;
  color: var(--app-text-muted);
  background: var(--app-surface);
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
  transition:
    border-color 180ms ease,
    background-color 180ms ease,
    color 180ms ease;
}

.page-tab:hover,
.page-tab:focus-visible,
.page-tab.active {
  border-color: var(--app-accent);
  color: var(--app-text);
  background: var(--app-surface-soft);
  outline: none;
}

.recipe-page-window,
.next-steps {
  min-height: 0;
  padding: 10px;
  overflow: auto;
  border: 1px solid var(--app-line);
  border-radius: 8px;
  background: var(--app-surface);
}

.window-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
}

.recipe-page-window :deep(.el-table),
.next-steps :deep(.el-table) {
  overflow: hidden;
  border: 1px solid var(--app-line);
  border-radius: 8px;
}

.step-list,
.tip-list {
  display: grid;
  gap: 8px;
  margin: 0;
  padding-left: 22px;
}

.step-list li,
.tip-list li {
  color: var(--app-text-soft);
  line-height: 1.52;
}

.step-list strong {
  color: var(--app-text);
}

.step-list span {
  margin-left: 8px;
  color: var(--app-text-faint);
  font-size: 13px;
  font-weight: 800;
}

.step-list p {
  margin: 4px 0 0;
}

.video-keywords {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.video-keywords span,
.video-keywords a {
  gap: 6px;
  border-radius: 6px;
  color: var(--app-text);
  text-decoration: none;
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
  gap: 8px;
}

.explanation-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 9px;
  padding: 10px;
  border: 1px solid var(--app-line);
  border-radius: 6px;
  background: var(--app-surface-soft);
}

.explanation-item h4,
.explanation-item p {
  margin: 0;
}

.explanation-item h4 {
  color: var(--app-text);
  font-size: 13px;
}

.explanation-item p {
  margin-top: 4px;
  color: var(--app-text-soft);
  font-size: 13px;
  line-height: 1.55;
}

@media (max-width: 980px) {
  .home-page {
    height: auto;
    min-height: calc(100vh - 58px);
    overflow: visible;
  }

  .command-grid {
    grid-template-columns: 1fr;
  }

  .result-panel {
    min-height: 620px;
  }
}

@media (max-width: 720px) {
  .home-page {
    min-height: calc(100vh - 121px);
    padding: 16px;
  }

  .workspace-heading {
    align-items: start;
    flex-direction: column;
  }

  .signal-strip {
    justify-content: flex-start;
  }

  .pantry-expiry-banner {
    align-items: flex-start;
    flex-wrap: wrap;
  }

  .pantry-expiry-link {
    margin-left: 27px;
  }

  .mode-grid,
  .filters,
  .brief-grid,
  .page-tabs {
    grid-template-columns: 1fr;
  }

  .search-actions {
    grid-template-columns: 1fr;
  }

  .camera-capture-panel {
    grid-template-columns: 1fr;
  }

  .camera-capture-panel :deep(.el-button) {
    width: 100%;
  }
}
</style>
