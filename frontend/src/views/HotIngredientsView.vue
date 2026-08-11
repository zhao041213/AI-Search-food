<template>
  <main class="hot-page">
    <section class="hot-shell" aria-labelledby="hot-title">
      <header class="hot-header">
        <div class="title-block">
          <span class="title-icon" aria-hidden="true"><Flame :size="20" /></span>
          <div>
            <p>全站搜索趋势</p>
            <h1 id="hot-title">热门食材</h1>
          </div>
        </div>

        <div class="header-actions">
          <span class="summary-text">
            {{ stats.totalSearches }} 次搜索 · {{ stats.totalIngredientOccurrences }} 条食材记录
          </span>
          <el-radio-group v-model="period" size="small" aria-label="统计周期" @change="loadRanking">
            <el-radio-button v-for="option in periodOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </el-radio-button>
          </el-radio-group>
          <el-button :loading="loading" circle aria-label="刷新排行榜" title="刷新排行榜" @click="loadRanking">
            <RefreshCw :size="16" aria-hidden="true" />
          </el-button>
        </div>
      </header>

      <section v-if="loading && !stats.items.length" class="state-panel" v-loading="true" aria-label="排行榜加载中" />

      <el-result
        v-else-if="errorMessage"
        icon="warning"
        title="排行榜加载失败"
        :sub-title="errorMessage"
      >
        <template #extra>
          <el-button type="primary" @click="loadRanking">重新加载</el-button>
        </template>
      </el-result>

      <el-empty v-else-if="!stats.items.length" description="当前周期还没有食材搜索记录" />

      <div v-else class="ranking-grid" aria-label="热门食材前十名">
        <button
          v-for="item in stats.items"
          :key="item.name"
          class="ingredient-card"
          type="button"
          :aria-label="`第 ${item.rank} 名 ${item.name}，${item.searchCount} 次搜索`"
          @click="searchIngredient(item.name)"
        >
          <span class="rank-badge" :class="`rank-${item.rank}`">{{ item.rank }}</span>
          <img
            :src="getIngredientImage(item.name)"
            :alt="item.name"
            loading="eager"
            @error="useFallbackImage"
          />
          <span class="card-shade" aria-hidden="true" />
          <span class="card-copy">
            <strong>{{ item.name }}</strong>
            <span>{{ item.searchCount }} 次搜索</span>
          </span>
          <span class="card-action" aria-hidden="true"><Search :size="16" /></span>
        </button>
      </div>
    </section>
  </main>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Flame, RefreshCw, Search } from 'lucide-vue-next'
import { getHotIngredients } from '../api/stats'
import { fallbackIngredientImage, getIngredientImage } from '../utils/ingredientImages'

const router = useRouter()
const period = ref('all')
const loading = ref(false)
const errorMessage = ref('')
const stats = ref(emptyStats())

const periodOptions = [
  { value: 'all', label: '全部' },
  { value: '7d', label: '近 7 天' },
  { value: '30d', label: '近 30 天' }
]

onMounted(loadRanking)

async function loadRanking() {
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await getHotIngredients(period.value, 10)
    stats.value = response.data.data || emptyStats()
  } catch (error) {
    errorMessage.value = error?.response?.data?.message || error?.message || '请检查后端服务后重试'
  } finally {
    loading.value = false
  }
}

function searchIngredient(name) {
  router.push({ name: 'home', query: { ingredients: name } })
}

function useFallbackImage(event) {
  if (event.target.src.endsWith(fallbackIngredientImage)) {
    return
  }
  event.target.src = fallbackIngredientImage
}

function emptyStats() {
  return {
    totalSearches: 0,
    totalIngredientOccurrences: 0,
    items: []
  }
}
</script>

<style scoped>
.hot-page {
  box-sizing: border-box;
  height: calc(100vh - 58px);
  overflow: hidden;
  padding: clamp(12px, 1.5vw, 20px);
  color: var(--app-text);
}

.hot-shell {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 14px;
  width: min(1460px, 100%);
  height: 100%;
  margin: 0 auto;
}

.hot-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 56px;
}

.title-block,
.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.title-icon {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  border: 1px solid var(--app-accent);
  border-radius: 8px;
  color: var(--app-accent-text);
  background: var(--app-accent);
}

.title-block p,
.title-block h1 {
  margin: 0;
}

.title-block p {
  margin-bottom: 3px;
  color: var(--app-text-muted);
  font-size: 12px;
  font-weight: 800;
}

.title-block h1 {
  font-size: 26px;
  line-height: 1.1;
}

.summary-text {
  color: var(--app-text-muted);
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
}

.state-panel,
.hot-shell :deep(.el-result),
.hot-shell :deep(.el-empty) {
  min-height: 260px;
  border: 1px solid var(--app-line);
  border-radius: 8px;
  background: var(--app-surface);
}

.ranking-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  grid-template-rows: repeat(2, minmax(0, 1fr));
  gap: 12px;
  min-height: 0;
}

.ingredient-card {
  position: relative;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  padding: 0;
  border: 1px solid var(--app-line);
  border-radius: 8px;
  color: #fff;
  background: var(--app-surface-strong);
  text-align: left;
  cursor: pointer;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.14);
  transition: border-color 160ms ease, transform 160ms ease, box-shadow 160ms ease;
}

.ingredient-card:hover,
.ingredient-card:focus-visible {
  border-color: var(--app-accent);
  outline: none;
  transform: translateY(-2px);
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.2);
}

.ingredient-card img {
  width: 100%;
  height: 100%;
  min-height: 150px;
  object-fit: cover;
  transition: transform 280ms ease;
}

.ingredient-card:hover img {
  transform: scale(1.035);
}

.card-shade {
  position: absolute;
  inset: 38% 0 0;
  background: linear-gradient(transparent, rgba(5, 11, 20, 0.9));
}

.rank-badge {
  position: absolute;
  z-index: 2;
  top: 10px;
  left: 10px;
  display: grid;
  width: 30px;
  height: 30px;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.55);
  border-radius: 50%;
  color: #fff;
  background: rgba(10, 16, 28, 0.72);
  font-weight: 900;
  backdrop-filter: blur(8px);
}

.rank-1 { background: #b7791f; }
.rank-2 { background: #64748b; }
.rank-3 { background: #9a5b34; }

.card-copy {
  position: absolute;
  z-index: 2;
  right: 46px;
  bottom: 14px;
  left: 14px;
  display: grid;
  gap: 4px;
}

.card-copy strong {
  overflow: hidden;
  font-size: 20px;
  line-height: 1.15;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-copy span {
  color: rgba(255, 255, 255, 0.78);
  font-size: 12px;
  font-weight: 700;
}

.card-action {
  position: absolute;
  z-index: 2;
  right: 14px;
  bottom: 17px;
  display: grid;
  width: 28px;
  height: 28px;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.42);
  border-radius: 50%;
  background: rgba(10, 16, 28, 0.5);
}

@media (max-width: 1180px) {
  .hot-page {
    height: auto;
    min-height: calc(100vh - 58px);
    overflow: visible;
  }

  .ranking-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
    grid-template-rows: none;
    grid-auto-rows: 220px;
  }
}

@media (max-width: 760px) {
  .hot-page {
    min-height: calc(100vh - 121px);
    padding: 14px;
  }

  .hot-header,
  .header-actions {
    align-items: flex-start;
    flex-direction: column;
  }

  .header-actions {
    width: 100%;
  }

  .ranking-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    grid-auto-rows: 210px;
  }

  .summary-text {
    white-space: normal;
  }
}

@media (max-width: 430px) {
  .ranking-grid {
    grid-template-columns: 1fr;
  }
}
</style>
