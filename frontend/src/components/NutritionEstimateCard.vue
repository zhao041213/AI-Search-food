<template>
  <article class="nutrition-card" aria-label="每份营养估算">
    <header class="nutrition-card-heading">
      <div>
        <p class="nutrition-card-kicker">营养参考</p>
        <h3>每份营养估算</h3>
        <span v-if="estimate">预计整道菜 {{ estimate.servings }} 份</span>
      </div>
      <HeartPulse :size="20" aria-hidden="true" />
    </header>

    <div v-if="estimate" class="nutrition-metrics">
      <div v-for="metric in metrics" :key="metric.key" class="nutrition-metric">
        <span>{{ metric.label }}</span>
        <strong>{{ formatNutritionValue(estimate[metric.key]) }}<small>{{ metric.unit }}</small></strong>
      </div>
    </div>
    <p v-else class="nutrition-empty">暂无营养估算</p>
    <p class="nutrition-disclosure">{{ NUTRITION_DISCLOSURE }}</p>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import { HeartPulse } from 'lucide-vue-next'
import {
  formatNutritionValue,
  normalizeNutritionEstimate,
  NUTRITION_DISCLOSURE
} from '../utils/nutrition'

const props = defineProps({
  nutrition: {
    type: Object,
    default: null
  }
})

const estimate = computed(() => normalizeNutritionEstimate(props.nutrition))
const metrics = [
  { key: 'caloriesKcal', label: '热量', unit: '千卡' },
  { key: 'proteinG', label: '蛋白质', unit: '克' },
  { key: 'fatG', label: '脂肪', unit: '克' },
  { key: 'carbohydrateG', label: '碳水', unit: '克' }
]
</script>

<style scoped>
.nutrition-card {
  display: grid;
  gap: 14px;
  padding: 14px;
  border: 1px solid var(--app-line);
  border-radius: 8px;
  background: var(--app-surface-soft);
}

.nutrition-card-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  color: var(--app-accent);
}

.nutrition-card-kicker,
.nutrition-card-heading h3,
.nutrition-card-heading span,
.nutrition-disclosure,
.nutrition-empty {
  margin: 0;
}

.nutrition-card-kicker {
  color: var(--app-text-muted);
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.08em;
}

.nutrition-card-heading h3 {
  margin-top: 3px;
  color: var(--app-text);
  font-size: 16px;
}

.nutrition-card-heading span,
.nutrition-disclosure,
.nutrition-empty {
  color: var(--app-text-muted);
  font-size: 12px;
  line-height: 1.55;
}

.nutrition-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.nutrition-metric {
  display: grid;
  gap: 4px;
  min-width: 0;
  padding: 9px;
  border: 1px solid var(--app-line);
  border-radius: 6px;
  background: var(--app-surface);
}

.nutrition-metric span {
  color: var(--app-text-muted);
  font-size: 12px;
}

.nutrition-metric strong {
  color: var(--app-text);
  font-size: 18px;
  line-height: 1.15;
}

.nutrition-metric small {
  margin-left: 3px;
  color: var(--app-text-muted);
  font-size: 11px;
  font-weight: 700;
}

@media (max-width: 560px) {
  .nutrition-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
