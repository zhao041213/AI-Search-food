<template>
  <main class="saved-page">
    <header class="saved-heading">
      <div>
        <p class="eyebrow">PERSONAL RECIPE LIBRARY</p>
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
            <span class="panel-kicker">SAVED RECIPES</span>
            <h2>已保存</h2>
          </div>
          <span class="count-badge">{{ recipes.length }}</span>
        </div>

        <el-skeleton v-if="loading" :rows="5" animated />
        <el-empty v-else-if="!recipes.length" description="还没有保存菜谱" />
        <div v-else class="history-items">
          <button
            v-for="item in recipes"
            :key="item.id"
            class="history-item"
            :class="{ active: selected?.id === item.id }"
            type="button"
            @click="openRecipe(item.id)"
          >
            <span class="history-item-title">{{ item.title }}</span>
            <span class="history-item-ingredients">{{ item.searchIngredients || '未记录食材' }}</span>
            <span class="history-item-meta">
              <span>{{ mealLabel(item.mealType) }}</span>
              <span>{{ formatDate(item.savedAt) }}</span>
            </span>
          </button>
        </div>
      </section>

      <section class="detail-panel" aria-label="菜谱详情">
        <el-skeleton v-if="detailLoading" :rows="9" animated />
        <el-empty v-else-if="!selected" description="选择一个菜谱查看详情" />
        <template v-else>
          <header class="detail-heading">
            <div>
              <p class="eyebrow">SAVED RECIPE</p>
              <h2>{{ selected.recipe?.title || '菜谱详情' }}</h2>
              <p class="saved-time">保存于 {{ formatDate(selected.savedAt) }}</p>
            </div>
            <span class="detail-id">#{{ selected.id }}</span>
          </header>

          <div v-if="selected.recipe" class="recipe-detail">
            <p class="summary">{{ selected.recipe.summary || '暂无菜谱简介' }}</p>
            <div v-if="selected.recipe.effects?.length" class="tag-row" aria-label="菜谱功效">
              <span v-for="effect in selected.recipe.effects" :key="effect" class="system-tag">
                {{ effect }}
              </span>
            </div>

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
                  <span v-for="keyword in selected.recipe.videoKeywords" :key="keyword">
                    <Video :size="15" aria-hidden="true" />
                    {{ keyword }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </template>
      </section>
    </div>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Video } from 'lucide-vue-next'
import { getSavedRecipe, listSavedRecipes } from '../api/recipes'

const recipes = ref([])
const selected = ref(null)
const loading = ref(false)
const detailLoading = ref(false)
const activePage = ref('ingredients')

const recipePages = computed(() => {
  const recipe = selected.value?.recipe
  if (!recipe) {
    return []
  }

  return [
    recipe.ingredients?.length ? { key: 'ingredients', label: '所需食材' } : null,
    recipe.steps?.length ? { key: 'steps', label: '烹饪步骤' } : null,
    recipe.tips?.length ? { key: 'tips', label: '烹饪建议' } : null,
    recipe.videoKeywords?.length ? { key: 'videos', label: '视频关键词' } : null
  ].filter(Boolean)
})

onMounted(loadRecipes)

async function loadRecipes() {
  loading.value = true
  try {
    const response = await listSavedRecipes({ limit: 50, offset: 0 })
    recipes.value = response.data.data || []
    if (recipes.value.length) {
      await openRecipe(recipes.value[0].id)
    }
  } catch (error) {
    ElMessage.error(getErrorMessage(error))
  } finally {
    loading.value = false
  }
}

async function openRecipe(id) {
  detailLoading.value = true
  try {
    const response = await getSavedRecipe(id)
    selected.value = response.data.data
    activePage.value = recipePages.value[0]?.key || 'ingredients'
  } catch (error) {
    ElMessage.error(getErrorMessage(error))
  } finally {
    detailLoading.value = false
  }
}

function mealLabel(value) {
  return {
    any: '不限餐次',
    breakfast: '早餐',
    lunch: '午餐',
    dinner: '晚餐'
  }[value] || '未指定餐次'
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

.panel-heading,
.detail-heading,
.step-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
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
  margin-top: 16px;
}

.history-item {
  display: grid;
  gap: 6px;
  width: 100%;
  padding: 12px;
  border: 1px solid var(--app-line);
  border-radius: 6px;
  color: var(--app-text);
  background: var(--app-surface);
  text-align: left;
  cursor: pointer;
  transition: border-color 180ms ease, background-color 180ms ease;
}

.history-item:hover,
.history-item.active {
  border-color: var(--app-accent);
  background: var(--app-surface-soft);
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
.video-keywords span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 5px 8px;
  border: 1px solid var(--app-line-strong);
  border-radius: 4px;
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
}
</style>
