<template>
  <section class="shopping-checklist" aria-label="采购清单">
    <div class="shopping-summary" role="status" aria-atomic="true">
      <span>采购清单</span>
    </div>

    <el-table :data="items" size="large">
      <el-table-column prop="name" label="全部食材" min-width="105" />
      <el-table-column prop="amount" label="用量" min-width="90" />
      <el-table-column label="状态" min-width="126">
        <template #default="scope">
          <el-dropdown
            trigger="click"
            :disabled="isSaving(scope.row)"
            @command="handleStatusSelect(scope.row, $event)"
          >
            <button
              class="status-button"
              type="button"
              :disabled="isSaving(scope.row)"
              aria-haspopup="menu"
              :aria-label="`切换${scope.row.name}的采购状态`"
              title="切换采购状态"
            >
              <span class="ingredient-state" :class="stateClass(scope.row)">
                {{ stateLabel(scope.row) }}
              </span>
              <ChevronDown :size="14" aria-hidden="true" />
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item
                  v-for="option in statusOptions"
                  :key="option.value"
                  :command="option.value"
                >
                  <Check
                    v-if="currentStatus(scope.row) === option.value"
                    :size="14"
                    aria-hidden="true"
                  />
                  <span v-else class="status-option-placeholder" aria-hidden="true" />
                  <span>{{ option.label }}</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </el-table-column>
      <el-table-column label="购买链接" min-width="150">
        <template #default="scope">
          <div class="purchase-links">
            <a
              :href="scope.row.purchaseLinks.dingdong"
              target="_blank"
              rel="noopener noreferrer"
              @click="emit('purchase-search', scope.row.name)"
            >
              叮咚买菜
              <ExternalLink :size="13" aria-hidden="true" />
            </a>
            <a
              :href="scope.row.purchaseLinks.hema"
              target="_blank"
              rel="noopener noreferrer"
              @click="emit('purchase-search', scope.row.name)"
            >
              盒马鲜生
              <ExternalLink :size="13" aria-hidden="true" />
            </a>
          </div>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<script setup>
import { Check, ChevronDown, ExternalLink } from 'lucide-vue-next'
import { getShoppingItemStatus, shoppingChecklistKey } from '../utils/recipeEnhancements'

const statusOptions = [
  { value: 'PENDING', label: '待购' },
  { value: 'PURCHASING', label: '采购中' },
  { value: 'PURCHASED', label: '已购买' },
  { value: 'READY', label: '已备齐' },
  { value: 'SKIPPED', label: '暂不购买' }
]

const statusLabels = Object.fromEntries(statusOptions.map((option) => [option.value, option.label]))

const props = defineProps({
  items: {
    type: Array,
    default: () => []
  },
  overrides: {
    type: Object,
    default: () => ({})
  },
  savingKey: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['status-change', 'purchase-search'])

function currentStatus(item) {
  return getShoppingItemStatus(item, props.overrides)
}

function isSaving(item) {
  return shoppingChecklistKey(item?.name) === props.savingKey
}

function stateLabel(item) {
  return statusLabels[currentStatus(item)] || statusLabels.PENDING
}

function stateClass(item) {
  return `status-${currentStatus(item).toLowerCase()}`
}

function handleStatusSelect(item, status) {
  if (status && status !== currentStatus(item)) {
    emit('status-change', { item, status })
  }
}
</script>

<style scoped>
.shopping-checklist {
  min-width: 0;
}

.shopping-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 34px;
  margin-bottom: 10px;
  color: var(--app-text-muted);
  font-size: 12px;
  font-weight: 800;
}

.status-button {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-height: 36px;
  padding: 0;
  border: 0;
  color: var(--app-text-soft);
  background: transparent;
  cursor: pointer;
}

.status-button:hover .ingredient-state,
.status-button:focus-visible .ingredient-state {
  border-color: var(--app-accent);
}

.status-button:focus-visible {
  border-radius: 5px;
  outline: 2px solid var(--app-accent);
  outline-offset: 3px;
}

.status-button:disabled {
  cursor: wait;
  opacity: 0.55;
}

.ingredient-state {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 7px;
  border: 1px solid var(--app-line-strong);
  border-radius: 4px;
  font-size: 11px;
  font-weight: 800;
  white-space: nowrap;
}

.ingredient-state.status-pending {
  color: var(--app-text-muted);
  background: var(--app-surface);
}

.ingredient-state.status-purchasing {
  border-color: var(--app-accent);
  color: var(--app-text);
  background: var(--app-accent-soft);
}

.ingredient-state.status-purchased {
  border-color: var(--app-text-muted);
  color: var(--app-text-soft);
  background: var(--app-surface-soft);
}

.ingredient-state.status-ready {
  border-color: var(--app-accent);
  color: var(--app-text);
  background: var(--app-accent-soft);
}

.ingredient-state.status-skipped {
  color: var(--app-text-faint);
  background: var(--app-surface-soft);
}

.status-option-placeholder {
  display: inline-block;
  width: 14px;
  height: 14px;
}

.purchase-links {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.purchase-links a {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-height: 28px;
  padding: 0 7px;
  border: 1px solid var(--app-line-strong);
  border-radius: 4px;
  color: var(--app-text-soft);
  background: var(--app-surface);
  font-size: 11px;
  font-weight: 800;
  text-decoration: none;
}

.purchase-links a:hover,
.purchase-links a:focus-visible {
  border-color: var(--app-accent);
  color: var(--app-text);
  background: var(--app-accent-soft);
  outline: none;
}

@media (max-width: 720px) {
  .shopping-summary {
    margin-bottom: 8px;
  }
}
</style>
