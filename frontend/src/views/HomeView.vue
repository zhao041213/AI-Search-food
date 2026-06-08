<template>
  <main class="home-page">
    <section class="search-workspace" aria-labelledby="home-title">
      <div class="workspace-heading">
        <p class="eyebrow">AI recipe search</p>
        <h1 id="home-title">Find a recipe from what you have</h1>
      </div>

      <div class="search-grid">
        <section class="search-panel" aria-label="Recipe search form">
          <el-form label-position="top" @submit.prevent="runSearch">
            <el-form-item label="Ingredients">
              <el-input
                v-model="ingredients"
                type="textarea"
                :rows="5"
                resize="none"
                maxlength="240"
                show-word-limit
                placeholder="Tomato, eggs, spinach"
              />
            </el-form-item>

            <div class="filters">
              <el-form-item label="Meal">
                <el-select v-model="mealType" placeholder="Meal">
                  <el-option label="Any meal" value="any" />
                  <el-option label="Breakfast" value="breakfast" />
                  <el-option label="Lunch" value="lunch" />
                  <el-option label="Dinner" value="dinner" />
                </el-select>
              </el-form-item>

              <el-form-item label="Goal">
                <el-select v-model="goal" placeholder="Goal">
                  <el-option label="Balanced" value="balanced" />
                  <el-option label="High protein" value="protein" />
                  <el-option label="Lower calorie" value="light" />
                  <el-option label="Quick prep" value="quick" />
                </el-select>
              </el-form-item>
            </div>

            <el-form-item label="Mode">
              <el-radio-group v-model="searchMode" size="large">
                <el-radio-button label="text" value="text">Text</el-radio-button>
                <el-radio-button label="image" value="image">Image</el-radio-button>
                <el-radio-button label="multi" value="multi">Multimodal</el-radio-button>
              </el-radio-group>
            </el-form-item>

            <div class="search-actions">
              <el-button type="primary" size="large" @click="runSearch">
                <Search :size="18" aria-hidden="true" />
                <span>Search recipes</span>
              </el-button>
              <el-button size="large" @click="resetSearch">
                <RotateCcw :size="18" aria-hidden="true" />
                <span>Reset</span>
              </el-button>
            </div>
          </el-form>
        </section>

        <section class="result-panel" aria-label="Recipe search result">
          <div class="result-header">
            <div>
              <p class="eyebrow">Current brief</p>
              <h2>{{ resultTitle }}</h2>
            </div>
            <span class="status-pill">{{ searchModeLabel }}</span>
          </div>

          <el-empty v-if="!hasSearch" description="No search brief yet" />

          <div v-else class="result-content">
            <dl class="brief-list">
              <div>
                <dt>Ingredients</dt>
                <dd>{{ cleanIngredients }}</dd>
              </div>
              <div>
                <dt>Meal</dt>
                <dd>{{ mealTypeLabel }}</dd>
              </div>
              <div>
                <dt>Goal</dt>
                <dd>{{ goalLabel }}</dd>
              </div>
            </dl>

            <el-divider />

            <div class="next-steps">
              <h3>Recommendation queue</h3>
              <el-table :data="recommendationRows" size="large">
                <el-table-column prop="name" label="Focus" min-width="140" />
                <el-table-column prop="value" label="Signal" min-width="140" />
              </el-table>
            </div>
          </div>
        </section>
      </div>
    </section>
  </main>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { RotateCcw, Search } from 'lucide-vue-next'

const ingredients = ref('')
const mealType = ref('any')
const goal = ref('balanced')
const searchMode = ref('text')
const lastSearch = ref(null)

const mealLabels = {
  any: 'Any meal',
  breakfast: 'Breakfast',
  lunch: 'Lunch',
  dinner: 'Dinner'
}

const goalLabels = {
  balanced: 'Balanced',
  protein: 'High protein',
  light: 'Lower calorie',
  quick: 'Quick prep'
}

const modeLabels = {
  text: 'Text',
  image: 'Image',
  multi: 'Multimodal'
}

const hasSearch = computed(() => Boolean(lastSearch.value))
const cleanIngredients = computed(() => lastSearch.value?.ingredients || 'None')
const mealTypeLabel = computed(() => mealLabels[lastSearch.value?.mealType] || mealLabels.any)
const goalLabel = computed(() => goalLabels[lastSearch.value?.goal] || goalLabels.balanced)
const searchModeLabel = computed(() => modeLabels[lastSearch.value?.searchMode || searchMode.value])
const resultTitle = computed(() => (hasSearch.value ? 'Recipe matching brief' : 'Ready for search'))
const recommendationRows = computed(() => [
  { name: 'Nutrition', value: goalLabel.value },
  { name: 'Timing', value: mealTypeLabel.value },
  { name: 'Input type', value: modeLabels[lastSearch.value?.searchMode] || modeLabels.text }
])

function runSearch() {
  const normalizedIngredients = ingredients.value
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
    .join(', ')

  if (!normalizedIngredients) {
    ElMessage.warning('Enter at least one ingredient')
    return
  }

  lastSearch.value = {
    ingredients: normalizedIngredients,
    mealType: mealType.value,
    goal: goal.value,
    searchMode: searchMode.value
  }
  ElMessage.success('Search brief created')
}

function resetSearch() {
  ingredients.value = ''
  mealType.value = 'any'
  goal.value = 'balanced'
  searchMode.value = 'text'
  lastSearch.value = null
}
</script>

<style scoped>
.home-page {
  min-height: calc(100vh - 72px);
  padding: clamp(24px, 5vw, 56px);
  color: #0f172a;
}

.search-workspace {
  width: min(1160px, 100%);
  margin: 0 auto;
}

.workspace-heading {
  max-width: 720px;
  margin: 0 auto 28px;
  text-align: center;
}

.eyebrow {
  margin: 0 0 8px;
  color: #047857;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0;
  text-transform: uppercase;
}

h1,
h2,
h3 {
  margin: 0;
  color: #164e63;
}

h1 {
  font-size: clamp(32px, 5vw, 48px);
  line-height: 1.1;
}

h2 {
  font-size: 24px;
}

h3 {
  margin-bottom: 12px;
  font-size: 18px;
}

.search-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(360px, 0.9fr);
  gap: 24px;
  align-items: start;
}

.search-panel,
.result-panel {
  border: 1px solid #bae6fd;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 18px 45px rgba(8, 145, 178, 0.12);
}

.search-panel {
  padding: 28px;
}

.result-panel {
  min-height: 505px;
  padding: 24px;
}

.filters {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.search-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 8px;
}

.search-actions :deep(.el-button) {
  margin-left: 0;
  font-weight: 700;
}

.search-actions :deep(.el-button span) {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.result-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 12px;
  border: 1px solid #99f6e4;
  border-radius: 999px;
  color: #0f766e;
  background: #f0fdfa;
  font-size: 13px;
  font-weight: 800;
}

.result-content {
  display: grid;
  gap: 4px;
}

.brief-list {
  display: grid;
  gap: 14px;
  margin: 0;
}

.brief-list div {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr);
  gap: 16px;
}

.brief-list dt {
  color: #475569;
  font-weight: 800;
}

.brief-list dd {
  margin: 0;
  color: #0f172a;
  line-height: 1.55;
}

.next-steps :deep(.el-table) {
  border-radius: 8px;
  overflow: hidden;
}

@media (max-width: 900px) {
  .search-grid {
    grid-template-columns: 1fr;
  }

  .result-panel {
    min-height: auto;
  }
}

@media (max-width: 720px) {
  .home-page {
    min-height: calc(100vh - 123px);
    padding: 24px 16px;
  }
}

@media (max-width: 520px) {
  .filters {
    grid-template-columns: 1fr;
  }

  .search-panel,
  .result-panel {
    padding: 20px;
  }

  .brief-list div {
    grid-template-columns: 1fr;
    gap: 4px;
  }

  .search-actions {
    flex-direction: column;
  }
}
</style>
