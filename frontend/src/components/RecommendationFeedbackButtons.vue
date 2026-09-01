<template>
  <section class="recommendation-feedback" aria-label="推荐反馈">
    <div class="feedback-copy">
      <strong>这次推荐怎么样？</strong>
      <span v-if="cooked" class="cooked-label">
        <Check :size="14" aria-hidden="true" />
        已做过
      </span>
    </div>
    <div class="feedback-actions" role="group" aria-label="选择推荐反馈">
      <button
        class="feedback-button"
        :class="{ active: reaction === 'LIKE' }"
        type="button"
        :disabled="disabled || loading"
        :aria-pressed="reaction === 'LIKE'"
        aria-label="喜欢这次推荐"
        @click="emit('toggle-reaction', 'LIKE')"
      >
        <ThumbsUp :size="16" aria-hidden="true" />
        <span>喜欢</span>
      </button>
      <button
        class="feedback-button"
        :class="{ active: reaction === 'DISLIKE' }"
        type="button"
        :disabled="disabled || loading"
        :aria-pressed="reaction === 'DISLIKE'"
        aria-label="不合口味这次推荐"
        @click="emit('toggle-reaction', 'DISLIKE')"
      >
        <ThumbsDown :size="16" aria-hidden="true" />
        <span>不合口味</span>
      </button>
      <span v-if="loading" class="feedback-status" role="status">正在保存反馈…</span>
    </div>
  </section>
</template>

<script setup>
import { Check, ThumbsDown, ThumbsUp } from 'lucide-vue-next'

defineProps({
  reaction: { type: String, default: null },
  cooked: { type: Boolean, default: false },
  loading: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false }
})

const emit = defineEmits(['toggle-reaction'])
</script>

<style scoped>
.recommendation-feedback {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid var(--app-line);
  border-radius: 8px;
  background: var(--app-surface-soft);
}

.feedback-copy,
.feedback-actions,
.feedback-button,
.cooked-label {
  display: inline-flex;
  align-items: center;
}

.feedback-copy {
  flex-wrap: wrap;
  gap: 8px;
  color: var(--app-text);
}

.feedback-copy strong {
  font-size: 13px;
}

.feedback-actions {
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.feedback-button {
  min-width: 88px;
  min-height: 44px;
  justify-content: center;
  gap: 6px;
  padding: 0 12px;
  border: 1px solid var(--app-line-strong);
  border-radius: 6px;
  color: var(--app-text);
  background: var(--app-surface);
  font: inherit;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
  transition: border-color 160ms ease, background-color 160ms ease, color 160ms ease, opacity 160ms ease;
  touch-action: manipulation;
}

.feedback-button:hover:not(:disabled) {
  border-color: var(--app-accent);
  background: var(--app-surface-strong);
}

.feedback-button:focus-visible {
  outline: 3px solid color-mix(in srgb, var(--app-accent) 38%, transparent);
  outline-offset: 2px;
}

.feedback-button.active {
  border-color: var(--app-accent);
  color: var(--app-text);
  background: var(--app-accent-soft);
}

.feedback-button:disabled {
  cursor: not-allowed;
  opacity: 0.58;
}

.cooked-label {
  gap: 4px;
  min-height: 28px;
  padding: 0 8px;
  border-radius: 999px;
  color: var(--app-text-soft);
  background: var(--app-surface-strong);
  font-size: 12px;
  font-weight: 800;
}

.feedback-status {
  color: var(--app-text-muted);
  font-size: 12px;
}

@media (max-width: 620px) {
  .recommendation-feedback {
    align-items: flex-start;
    flex-direction: column;
  }

  .feedback-actions {
    width: 100%;
    justify-content: flex-start;
  }

  .feedback-button {
    flex: 1 1 0;
  }
}
</style>
