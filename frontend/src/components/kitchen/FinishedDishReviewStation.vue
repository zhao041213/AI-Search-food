<template>
  <section class="review-station" aria-labelledby="review-station-title">
    <header class="review-intro">
      <div>
        <p>味味 · 成品品鉴员</p>
        <h2 id="review-station-title">选择一道完成的菜</h2>
        <span>选中菜谱后直接上传成品照片，查看色泽、火候和摆盘建议。</span>
      </div>
      <el-button :loading="loading" @click="loadRecipes">
        <RefreshCw :size="16" aria-hidden="true" />
        刷新菜谱
      </el-button>
    </header>

    <div v-if="loading" class="review-state">
      <LoaderCircle class="spin" :size="30" aria-hidden="true" />
      <strong>正在准备可品鉴的菜谱</strong>
    </div>
    <div v-else-if="!recipes.length" class="review-state">
      <UtensilsCrossed :size="38" :stroke-width="1.5" aria-hidden="true" />
      <strong>还没有可以品鉴的菜谱</strong>
      <span>先让阿灶生成并保存一道菜，再回来上传成品。</span>
      <el-button type="primary" @click="emit('open-feature', 'chef')">去主厨料理大厅</el-button>
    </div>
    <div v-else class="recipe-choice-grid" role="group" aria-label="选择需要品鉴的菜谱">
      <button
        v-for="item in recipes"
        :key="item.id"
        class="recipe-choice"
        type="button"
        :disabled="detailLoadingId !== null"
        @click="startReview(item)"
      >
        <span class="recipe-choice__icon" aria-hidden="true"><ChefHat :size="21" /></span>
        <span class="recipe-choice__copy">
          <strong>{{ item.title || '未命名菜谱' }}</strong>
          <small>{{ item.searchIngredients || '未记录食材' }}</small>
          <span>{{ mealLabel(item.mealType) }} · {{ goalLabel(item.goal) }}</span>
        </span>
        <span class="recipe-choice__action">
          <LoaderCircle v-if="detailLoadingId === item.id" class="spin" :size="18" aria-hidden="true" />
          <Sparkles v-else :size="18" aria-hidden="true" />
          {{ detailLoadingId === item.id ? '加载中' : '开始品鉴' }}
        </span>
      </button>
    </div>

    <FinishedDishReviewDialog
      v-if="selectedRecipe"
      v-model="reviewVisible"
      :recipe="selectedRecipe.recipe"
      :recipe-id="selectedRecipe.id"
    />
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ChefHat, LoaderCircle, RefreshCw, Sparkles, UtensilsCrossed } from 'lucide-vue-next'
import { getSavedRecipe, getSavedRecipes } from '../../api/recipes'
import FinishedDishReviewDialog from '../FinishedDishReviewDialog.vue'

const emit = defineEmits(['open-feature'])
const recipes = ref([])
const loading = ref(false)
const detailLoadingId = ref(null)
const selectedRecipe = ref(null)
const reviewVisible = ref(false)

const mealLabels = { breakfast: '早餐', lunch: '午餐', dinner: '晚餐' }
const goalLabels = {
  balanced: '营养均衡', protein: '高蛋白', light: '低热量', quick: '快速烹饪',
  fat_loss: '减脂', muscle_gain: '增肌', low_sugar: '控糖'
}

onMounted(loadRecipes)

async function loadRecipes() {
  if (loading.value) return
  loading.value = true
  try {
    const response = await getSavedRecipes({ limit: 50, offset: 0 })
    recipes.value = Array.isArray(response.data.data) ? response.data.data : []
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '已保存菜谱加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

async function startReview(item) {
  if (!item?.id || detailLoadingId.value !== null) return
  detailLoadingId.value = item.id
  try {
    const response = await getSavedRecipe(item.id)
    selectedRecipe.value = response.data.data || null
    if (!selectedRecipe.value?.recipe) {
      ElMessage.warning('这份菜谱信息不完整，暂时无法品鉴')
      return
    }
    reviewVisible.value = true
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '菜谱详情加载失败，请稍后重试')
  } finally {
    detailLoadingId.value = null
  }
}

function mealLabel(value) { return mealLabels[value] || '不限餐次' }
function goalLabel(value) { return goalLabels[value] || goalLabels.balanced }
</script>

<style scoped>
.review-station { min-height: 100%; padding: 18px; }
.review-intro {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 14px;
  padding: 14px 16px;
  border: 1px solid #b26f7c;
  background: #f2ddd9;
  box-shadow: 3px 3px 0 rgba(97, 71, 52, 0.24);
}
.review-intro p,
.review-intro h2,
.review-intro span { margin: 0; }
.review-intro p { color: #a05d6b; font-size: 11px; font-weight: 900; letter-spacing: 0.08em; }
.review-intro h2 { margin: 2px 0 3px; color: #3c2b20; font-size: 20px; }
.review-intro span { color: #7b5960; font-size: 12px; }
.recipe-choice-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
.recipe-choice {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr) auto;
  align-items: center;
  gap: 11px;
  min-height: 88px;
  padding: 11px 12px;
  border: 1px solid #c49aa2;
  color: #3c2b20;
  background: rgba(255, 248, 244, 0.94);
  text-align: left;
  cursor: pointer;
  box-shadow: 0 5px 14px rgba(69, 48, 34, 0.08);
}
.recipe-choice:hover,
.recipe-choice:focus-visible { border-color: #a65465; background: #f8e3e3; outline: 2px solid #a65465; outline-offset: 2px; }
.recipe-choice:disabled { cursor: wait; opacity: 0.72; }
.recipe-choice__icon {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border: 2px solid #704852;
  color: #f1b2bd;
  background: #36252a;
}
.recipe-choice__copy { display: grid; gap: 3px; min-width: 0; }
.recipe-choice__copy strong,
.recipe-choice__copy small { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.recipe-choice__copy strong { font-size: 14px; }
.recipe-choice__copy small,
.recipe-choice__copy span { color: #7b5960; font-size: 11px; }
.recipe-choice__action { display: inline-flex; align-items: center; gap: 5px; color: #a65465; font-size: 11px; font-weight: 900; white-space: nowrap; }
.review-state {
  display: grid;
  min-height: 360px;
  place-items: center;
  align-content: center;
  gap: 10px;
  border: 1px dashed #b26f7c;
  color: #a65465;
  background: rgba(255, 248, 244, 0.82);
  text-align: center;
}
.review-state strong { color: #4b3036; }
.review-state span { color: #7b5960; font-size: 12px; }
.spin { animation: review-station-spin 900ms linear infinite; }
@keyframes review-station-spin { to { transform: rotate(360deg); } }
@media (prefers-reduced-motion: reduce) { .spin { animation: none; } }
@container scene-window-content (max-width: 760px) {
  .recipe-choice-grid { grid-template-columns: 1fr; }
  .review-intro { align-items: flex-start; flex-direction: column; }
}
</style>
