<template>
  <main class="shared-page">
    <header class="shared-header">
      <RouterLink class="shared-brand" to="/">
        <span class="shared-brand-mark"><Utensils :size="18" aria-hidden="true" /></span>
        <span>AI 智能菜谱</span>
      </RouterLink>
      <span class="shared-readonly"><Share2 :size="15" aria-hidden="true" /> 只读分享</span>
    </header>

    <section v-if="loading" class="shared-card shared-loading" aria-live="polite">
      <el-skeleton :rows="8" animated />
    </section>
    <section v-else-if="errorMessage" class="shared-card shared-empty" role="alert">
      <CircleAlert :size="42" aria-hidden="true" />
      <h1>分享链接不可用</h1>
      <p>{{ errorMessage }}</p>
      <RouterLink class="shared-home-link" to="/">返回首页</RouterLink>
    </section>
    <article v-else class="shared-card printable-recipe">
      <header class="recipe-hero">
        <div>
          <p class="shared-eyebrow">SHARED RECIPE / 只读菜谱</p>
          <h1>{{ recipe.title || '未命名菜谱' }}</h1>
          <p class="recipe-summary">{{ recipe.summary || '暂无菜谱简介' }}</p>
        </div>
        <button class="print-button no-print" type="button" @click="printRecipe">
          <Printer :size="16" aria-hidden="true" />
          <span>打印菜谱</span>
        </button>
      </header>

      <div class="recipe-grid">
        <section class="recipe-section">
          <div class="section-heading">
            <span class="section-number">01</span>
            <div><p>准备清单</p><h2>所需食材</h2></div>
          </div>
          <el-empty v-if="!recipe.ingredients?.length" description="暂无食材信息" :image-size="56" />
          <ul v-else class="ingredient-list">
            <li v-for="item in recipe.ingredients" :key="`${item.name}-${item.amount}`">
              <span>{{ item.name || '未命名食材' }}</span>
              <strong>{{ item.amount || '适量' }}</strong>
            </li>
          </ul>
        </section>

        <section class="recipe-section steps-section">
          <div class="section-heading">
            <span class="section-number">02</span>
            <div><p>按顺序完成</p><h2>烹饪步骤</h2></div>
          </div>
          <el-empty v-if="!recipe.steps?.length" description="暂无烹饪步骤" :image-size="56" />
          <ol v-else class="step-list">
            <li v-for="step in recipe.steps" :key="`${step.order}-${step.title}`">
              <div class="step-index">{{ step.order }}</div>
              <div>
                <div class="step-title"><strong>{{ step.title || '烹饪步骤' }}</strong><span v-if="step.durationMinutes">约 {{ step.durationMinutes }} 分钟</span></div>
                <p>{{ step.description || '暂无步骤说明' }}</p>
              </div>
            </li>
          </ol>
        </section>
      </div>

      <section v-if="recipe.nutrition || recipe.tips?.length" class="recipe-footer-grid">
        <div v-if="recipe.nutrition" class="nutrition-box">
          <div class="section-heading compact-heading"><span class="section-number">03</span><div><p>每份估算</p><h2>营养信息</h2></div></div>
          <div class="nutrition-values">
            <span><strong>{{ recipe.nutrition.caloriesKcal ?? '—' }}</strong><small>千卡</small></span>
            <span><strong>{{ recipe.nutrition.proteinG ?? '—' }}</strong><small>蛋白质 g</small></span>
            <span><strong>{{ recipe.nutrition.fatG ?? '—' }}</strong><small>脂肪 g</small></span>
            <span><strong>{{ recipe.nutrition.carbohydrateG ?? '—' }}</strong><small>碳水 g</small></span>
          </div>
        </div>
        <div v-if="recipe.tips?.length" class="tips-box">
          <div class="section-heading compact-heading"><span class="section-number">04</span><div><p>最后检查</p><h2>烹饪建议</h2></div></div>
          <ul><li v-for="tip in recipe.tips" :key="tip">{{ tip }}</li></ul>
        </div>
      </section>
    </article>
  </main>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { CircleAlert, Printer, Share2, Utensils } from 'lucide-vue-next'
import { RouterLink, useRoute } from 'vue-router'
import { getPublicSharedRecipe } from '../api/savedRecipes'

const route = useRoute()
const loading = ref(true)
const errorMessage = ref('链接可能已过期、停用，或对应菜谱已取消收藏。')
const recipe = ref(null)

onMounted(async () => {
  try {
    const response = await getPublicSharedRecipe(route.params.token)
    recipe.value = response.data.data
  } catch (error) {
    errorMessage.value = error?.response?.data?.message || errorMessage.value
  } finally {
    loading.value = false
  }
})

function printRecipe() {
  window.print()
}
</script>

<style scoped>
.shared-page {
  min-height: calc(100vh - 58px);
  padding: clamp(18px, 4vw, 48px);
  color: var(--app-text);
}

.shared-header,
.shared-card {
  max-width: 1120px;
  margin: 0 auto;
}

.shared-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.shared-brand,
.shared-readonly,
.print-button,
.shared-home-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.shared-brand { font-weight: 900; }
.shared-brand-mark { display: inline-grid; width: 32px; height: 32px; place-items: center; border-radius: 8px; color: #fff; background: var(--app-accent); }
.shared-readonly { color: var(--app-text-muted); font-size: 12px; font-weight: 800; }
.shared-card { padding: clamp(20px, 4vw, 52px); border: 1px solid var(--app-line); background: var(--app-surface); box-shadow: var(--app-panel-shadow); }
.shared-loading { min-height: 420px; }
.shared-empty { display: grid; min-height: 360px; place-items: center; align-content: center; gap: 12px; text-align: center; }
.shared-empty h1, .shared-empty p { margin: 0; }
.shared-empty p { max-width: 420px; color: var(--app-text-muted); line-height: 1.7; }
.shared-home-link { min-height: 36px; padding: 0 14px; border: 1px solid var(--app-line-strong); border-radius: 6px; font-weight: 800; }
.recipe-hero { display: flex; align-items: flex-start; justify-content: space-between; gap: 22px; padding-bottom: 24px; border-bottom: 1px solid var(--app-line); }
.shared-eyebrow { margin: 0; color: var(--app-text-muted); font-size: 11px; font-weight: 900; letter-spacing: .12em; }
.recipe-hero h1 { margin: 8px 0 0; font-size: clamp(30px, 5vw, 58px); letter-spacing: -.04em; }
.recipe-summary { max-width: 700px; margin: 14px 0 0; color: var(--app-text-soft); line-height: 1.8; }
.print-button { min-height: 36px; padding: 0 12px; border: 1px solid var(--app-line-strong); border-radius: 6px; color: var(--app-text); background: var(--app-surface); font: inherit; font-size: 13px; font-weight: 800; cursor: pointer; }
.recipe-grid, .recipe-footer-grid { display: grid; grid-template-columns: minmax(220px, .72fr) minmax(0, 1.28fr); gap: 28px; margin-top: 28px; }
.recipe-footer-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.recipe-section, .nutrition-box, .tips-box { min-width: 0; }
.steps-section, .tips-box { padding-left: 28px; border-left: 1px solid var(--app-line); }
.section-heading { display: flex; align-items: flex-start; gap: 12px; }
.section-heading p, .section-heading h2 { margin: 0; }
.section-heading p { color: var(--app-text-muted); font-size: 11px; font-weight: 800; letter-spacing: .08em; }
.section-heading h2 { margin-top: 4px; font-size: 21px; }
.section-number { color: var(--app-accent); font-size: 14px; font-weight: 900; }
.ingredient-list, .tips-box ul { display: grid; gap: 8px; margin: 18px 0 0; padding: 0; list-style: none; }
.ingredient-list li { display: flex; justify-content: space-between; gap: 12px; padding: 11px 0; border-bottom: 1px dashed var(--app-line-strong); color: var(--app-text-soft); }
.ingredient-list strong { color: var(--app-text); }
.step-list { display: grid; gap: 18px; margin: 18px 0 0; padding: 0; list-style: none; }
.step-list li { display: grid; grid-template-columns: 32px minmax(0, 1fr); gap: 12px; }
.step-index { display: grid; width: 30px; height: 30px; place-items: center; border-radius: 50%; color: var(--app-accent-text); background: var(--app-accent); font-weight: 900; }
.step-title { display: flex; justify-content: space-between; gap: 12px; }
.step-title span { color: var(--app-text-muted); font-size: 12px; white-space: nowrap; }
.step-list p, .tips-box li { margin: 5px 0 0; color: var(--app-text-soft); line-height: 1.7; }
.compact-heading { margin-bottom: 14px; }
.nutrition-box, .tips-box { padding-top: 22px; border-top: 1px solid var(--app-line); }
.nutrition-values { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; }
.nutrition-values span { display: grid; gap: 3px; padding: 12px 8px; border: 1px solid var(--app-line); background: var(--app-surface-soft); text-align: center; }
.nutrition-values strong { font-size: 18px; }
.nutrition-values small { color: var(--app-text-muted); font-size: 10px; }

@media (max-width: 760px) {
  .recipe-hero, .shared-header { align-items: flex-start; flex-direction: column; }
  .recipe-grid, .recipe-footer-grid { grid-template-columns: 1fr; gap: 24px; }
  .steps-section, .tips-box { padding-left: 0; border-left: 0; }
  .nutrition-values { grid-template-columns: repeat(2, 1fr); }
}

@media print {
  .shared-page { padding: 0; }
  .shared-header, .no-print { display: none !important; }
  .shared-card { max-width: none; padding: 0; border: 0; box-shadow: none; }
  .recipe-hero { padding-top: 0; }
  .recipe-hero h1 { font-size: 32px; }
  .recipe-grid { gap: 20px; }
  .steps-section, .tips-box { border-color: #bbb; }
  :global(body), :global(.app-main) { background: #fff !important; }
}
</style>
