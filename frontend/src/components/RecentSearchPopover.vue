<template>
  <div class="recent-search-anchor" @focusout="handleFocusOut">
    <slot />

    <div v-if="visible" class="recent-search-dropdown" role="listbox" aria-label="最近搜索">
      <div class="recent-search-heading">
        <strong>最近搜索</strong>
        <span>最多 5 条</span>
      </div>

      <div v-if="loading" class="recent-search-state">正在加载...</div>
      <div v-else-if="!visibleItems.length" class="recent-search-state">暂无最近搜索</div>
      <template v-else>
        <button
          v-for="item in visibleItems"
          :key="item.id || `${item.ingredients}-${item.createdAt}`"
          class="recent-search-item"
          type="button"
          role="option"
          @mousedown.prevent
          @click="emit('select', item)"
        >
          <strong>{{ item.ingredients || '未记录食材' }}</strong>
          <span>{{ mealLabel(item.mealType) }} · {{ goalLabel(item.goal) }}</span>
          <time :datetime="item.createdAt || undefined">{{ formatTime(item.createdAt) }}</time>
        </button>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  items: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  visible: { type: Boolean, default: false }
})

const emit = defineEmits(['select', 'update:visible'])

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

const visibleItems = computed(() => props.items.slice(0, 5))

function mealLabel(value) {
  return mealLabels[value] || '不限餐次'
}

function goalLabel(value) {
  return goalLabels[value] || '营养均衡'
}

function formatTime(value) {
  if (!value) {
    return '时间未知'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return '时间未知'
  }
  return new Intl.DateTimeFormat('zh-CN', {
    month: 'numeric',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  }).format(date)
}

function handleFocusOut(event) {
  if (!event.currentTarget.contains(event.relatedTarget)) {
    emit('update:visible', false)
  }
}
</script>

<style scoped>
.recent-search-anchor {
  position: relative;
  width: 100%;
}

.recent-search-dropdown {
  position: absolute;
  z-index: 20;
  top: calc(100% + 6px);
  right: 0;
  left: 0;
  max-height: 286px;
  overflow-y: auto;
  border: 1px solid var(--app-line-strong);
  border-radius: 8px;
  background: var(--app-surface);
  box-shadow: var(--el-box-shadow-light);
}

.recent-search-heading {
  position: sticky;
  z-index: 1;
  top: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 10px;
  border-bottom: 1px solid var(--app-line);
  color: var(--app-text);
  background: var(--app-surface);
  font-size: 12px;
}

.recent-search-heading span,
.recent-search-item span,
.recent-search-item time,
.recent-search-state {
  color: var(--app-text-muted);
  font-size: 11px;
}

.recent-search-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 3px 12px;
  width: 100%;
  padding: 9px 10px;
  border: 0;
  border-bottom: 1px solid var(--app-line);
  color: var(--app-text);
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.recent-search-item:last-child {
  border-bottom: 0;
}

.recent-search-item:hover,
.recent-search-item:focus-visible {
  background: var(--app-surface-soft);
  outline: none;
}

.recent-search-item strong {
  min-width: 0;
  overflow: hidden;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recent-search-item span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recent-search-item time {
  grid-row: 1 / span 2;
  grid-column: 2;
  align-self: center;
  white-space: nowrap;
}

.recent-search-state {
  padding: 16px 10px;
  text-align: center;
}

@media (max-width: 480px) {
  .recent-search-item {
    grid-template-columns: minmax(0, 1fr);
  }

  .recent-search-item time {
    grid-row: auto;
    grid-column: auto;
  }
}
</style>
