<template>
  <section class="shopping-checklist" aria-label="采购清单">
    <div v-if="!compact" class="shopping-summary" role="status" aria-atomic="true">
      <span v-if="!compact">采购清单</span>
      <span v-if="showInventory">已同步库存状态</span>
    </div>

    <el-table class="shopping-table" :data="items" size="large">
      <el-table-column prop="name" label="食材" min-width="100" />
      <el-table-column prop="amount" label="用量" min-width="78" />
      <el-table-column label="采购状态" min-width="110">
        <template #default="scope">
          <el-dropdown trigger="click" :disabled="isSaving(scope.row)" @command="handleStatusSelect(scope.row, $event)">
            <button class="status-button" type="button" :disabled="isSaving(scope.row)" aria-haspopup="menu" :aria-label="`切换${scope.row.name}的采购状态`">
              <span class="ingredient-state" :class="stateClass(scope.row)">{{ stateLabel(scope.row) }}</span>
              <ChevronDown :size="14" aria-hidden="true" />
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-for="option in statusOptions" :key="option.value" :command="option.value">
                  <Check v-if="currentStatus(scope.row) === option.value" :size="14" aria-hidden="true" />
                  <span v-else class="status-option-placeholder" aria-hidden="true" />
                  <span>{{ option.label }}</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </el-table-column>
      <el-table-column label="购买链接" min-width="126">
        <template #default="scope">
          <div class="purchase-links">
            <a v-if="scope.row.purchaseLinks?.dingdong" :href="scope.row.purchaseLinks.dingdong" target="_blank" rel="noopener noreferrer" @click="emit('purchase-search', scope.row.name)">叮咚买菜 <ExternalLink :size="13" aria-hidden="true" /></a>
            <a v-if="scope.row.purchaseLinks?.hema" :href="scope.row.purchaseLinks.hema" target="_blank" rel="noopener noreferrer" @click="emit('purchase-search', scope.row.name)">盒马鲜生 <ExternalLink :size="13" aria-hidden="true" /></a>
          </div>
        </template>
      </el-table-column>
      <el-table-column v-if="showInventory" label="库存情况" min-width="140">
        <template #default="scope">
          <div class="inventory-cell">
            <span class="inventory-state" :class="`inventory-${inventoryStatus(scope.row).toLowerCase()}`">{{ inventoryLabel(scope.row) }}</span>
            <small v-if="scope.row.inventoryMessage">{{ scope.row.inventoryMessage }}</small>
          </div>
        </template>
      </el-table-column>
      <el-table-column v-if="showInventory" label="入库" min-width="112">
        <template #default="scope">
          <el-button v-if="currentStatus(scope.row) === 'PURCHASED'" type="primary" plain size="small" :loading="stockInKey === shoppingChecklistKey(scope.row?.name)" :disabled="Boolean(stockInKey)" @click="emit('stock-in', scope.row)">加入库存</el-button>
          <span v-else class="stock-in-muted">—</span>
        </template>
      </el-table-column>
    </el-table>

    <div class="shopping-mobile-list">
      <article v-for="item in items" :key="item.name" class="shopping-mobile-card">
        <div class="mobile-card-heading">
          <strong>{{ item.name }}</strong>
          <span>{{ item.amount || '用量待确认' }}</span>
        </div>
        <div class="mobile-card-status-row">
          <div class="mobile-card-meta">
            <span :class="['inventory-state', `inventory-${inventoryStatus(item).toLowerCase()}`]">{{ inventoryLabel(item) }}</span>
            <el-dropdown trigger="click" :disabled="isSaving(item)" @command="handleStatusSelect(item, $event)">
              <button class="status-button mobile-status-button" type="button" :disabled="isSaving(item)" aria-haspopup="menu" :aria-label="`切换${item.name}的采购状态`">
                <span class="ingredient-state" :class="stateClass(item)">{{ stateLabel(item) }}</span>
                <ChevronDown :size="14" aria-hidden="true" />
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-for="option in statusOptions" :key="option.value" :command="option.value">
                    <Check v-if="currentStatus(item) === option.value" :size="14" aria-hidden="true" />
                    <span v-else class="status-option-placeholder" aria-hidden="true" />
                    <span>{{ option.label }}</span>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
          <div class="mobile-card-stock-action">
            <el-button v-if="currentStatus(item) === 'PURCHASED'" type="primary" plain :loading="stockInKey === shoppingChecklistKey(item.name)" :disabled="Boolean(stockInKey)" @click="emit('stock-in', item)">加入库存</el-button>
            <span v-else class="stock-in-muted">入库待购买</span>
          </div>
        </div>
        <div v-if="item.purchaseLinks?.dingdong || item.purchaseLinks?.hema" class="purchase-links mobile-purchase-links" aria-label="购买链接">
          <a v-if="item.purchaseLinks?.dingdong" :href="item.purchaseLinks.dingdong" target="_blank" rel="noopener noreferrer" @click="emit('purchase-search', item.name)">叮咚买菜 <ExternalLink :size="13" aria-hidden="true" /></a>
          <a v-if="item.purchaseLinks?.hema" :href="item.purchaseLinks.hema" target="_blank" rel="noopener noreferrer" @click="emit('purchase-search', item.name)">盒马鲜生 <ExternalLink :size="13" aria-hidden="true" /></a>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { Check, ChevronDown, ExternalLink } from 'lucide-vue-next'
import { getShoppingItemStatus, shoppingChecklistKey } from '../utils/recipeEnhancements'

const statusOptions = [
  { value: 'PENDING', label: '待购买' },
  { value: 'PURCHASING', label: '采购中' },
  { value: 'PURCHASED', label: '已购买' },
  { value: 'READY', label: '已备齐' },
  { value: 'SKIPPED', label: '暂不购买' }
]
const statusLabels = Object.fromEntries(statusOptions.map((option) => [option.value, option.label]))
const props = defineProps({
  items: { type: Array, default: () => [] },
  overrides: { type: Object, default: () => ({}) },
  savingKey: { type: String, default: '' },
  sourceType: { type: String, default: '' },
  sourceId: { type: [Number, String], default: null },
  stockInKey: { type: String, default: '' },
  compact: { type: Boolean, default: false }
})
const emit = defineEmits(['status-change', 'purchase-search', 'stock-in'])
const showInventory = computed(() => Boolean(props.sourceType && props.sourceId))
function currentStatus(item) { return getShoppingItemStatus(item, props.overrides) }
function isSaving(item) { return shoppingChecklistKey(item?.name) === props.savingKey }
function stateLabel(item) { return statusLabels[currentStatus(item)] || statusLabels.PENDING }
function stateClass(item) { return `status-${currentStatus(item).toLowerCase()}` }
function handleStatusSelect(item, status) { if (status && status !== currentStatus(item)) emit('status-change', { item, status }) }
function inventoryStatus(item) { return item?.inventoryStatus || (item?.alreadyOwned ? 'ENOUGH' : 'MISSING') }
function inventoryLabel(item) { return ({ ENOUGH: '库存充足', PARTIAL: '库存不足', MISSING: '缺少库存', UNQUANTIFIED: '用量待确认', UNIT_MISMATCH: '单位不匹配', EXPIRED_ONLY: '仅有过期库存' })[inventoryStatus(item)] || '待检查' }
</script>

<style scoped>
.shopping-checklist {
  min-width: 0;
  container-name: shopping-checklist;
  container-type: inline-size;
}
.shopping-summary { display: flex; justify-content: space-between; gap: 12px; min-height: 34px; margin-bottom: 10px; color: var(--app-text-muted); font-size: 12px; font-weight: 800; }
.status-button { display: inline-flex; align-items: center; gap: 4px; min-width: 0; max-width: 100%; min-height: 36px; padding: 0; border: 0; color: var(--app-text-soft); background: transparent; cursor: pointer; }
.status-button:focus-visible { border-radius: 5px; outline: 2px solid var(--app-accent); outline-offset: 3px; }
.status-button:disabled { cursor: wait; opacity: .55; }
.ingredient-state,.inventory-state { display: inline-flex; align-items: center; min-height: 24px; padding: 0 7px; border: 1px solid var(--app-line-strong); border-radius: 4px; font-size: 11px; font-weight: 800; white-space: nowrap; }
.ingredient-state.status-purchasing,.ingredient-state.status-ready { border-color: var(--app-accent); color: var(--app-text); background: var(--app-accent-soft); }
.ingredient-state.status-purchased { color: var(--app-text-soft); background: var(--app-surface-soft); }
.ingredient-state.status-skipped { color: var(--app-text-faint); background: var(--app-surface-soft); }
.status-option-placeholder { display: inline-block; width: 14px; height: 14px; }
.purchase-links { display: flex; flex-wrap: wrap; gap: 7px; min-width: 0; }
.purchase-links a { display: inline-flex; align-items: center; gap: 4px; min-width: 0; max-width: 100%; min-height: 28px; padding: 0 7px; border: 1px solid var(--app-line-strong); border-radius: 4px; color: var(--app-text-soft); background: var(--app-surface); font-size: 11px; font-weight: 800; text-decoration: none; overflow-wrap: anywhere; }
.inventory-cell { display: grid; gap: 3px; min-width: 0; }
.inventory-cell small { color: var(--app-text-faint); font-size: 11px; line-height: 1.35; }
.inventory-enough { border-color: var(--el-color-success); color: var(--el-color-success); }
.inventory-partial,.inventory-expired_only { border-color: var(--el-color-warning); color: var(--el-color-warning); }
.inventory-missing,.inventory-unit_mismatch,.inventory-unquantified { border-color: var(--app-line-strong); color: var(--app-text-muted); }
.stock-in-muted { color: var(--app-text-faint); }
.shopping-mobile-list { display: none; gap: 8px; }
.shopping-mobile-card { display: grid; gap: 8px; min-width: 0; padding: 12px; border: 1px solid var(--app-line); border-radius: 8px; background: var(--app-surface); }
.mobile-card-heading { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, .65fr); align-items: start; gap: 8px; min-width: 0; }
.mobile-card-heading strong,
.mobile-card-heading > span { min-width: 0; overflow-wrap: anywhere; }
.mobile-card-heading strong { color: var(--app-text); }
.mobile-card-heading > span { color: var(--app-text-muted); font-size: 12px; text-align: right; }
.mobile-card-status-row { display: grid; grid-template-columns: minmax(0, 1fr) auto; align-items: center; gap: 8px; min-width: 0; }
.mobile-card-meta { display: flex; flex-wrap: wrap; align-items: center; justify-content: flex-start; gap: 6px; min-width: 0; color: var(--app-text-muted); font-size: 12px; }
.mobile-card-meta .el-dropdown { min-width: 0; }
.mobile-card-meta .ingredient-state,
.mobile-card-meta .inventory-state { white-space: normal; }
.mobile-status-button { min-height: 44px; }
.mobile-card-stock-action { min-width: 0; justify-self: end; }
.mobile-card-stock-action .el-button { min-height: 44px; margin: 0; }
.mobile-card-stock-action .stock-in-muted { display: block; max-width: 100%; color: var(--app-text-faint); font-size: 11px; line-height: 1.35; text-align: right; }
.mobile-purchase-links { display: grid; gap: 6px; min-width: 0; padding-top: 4px; border-top: 1px solid var(--app-line); }
.mobile-purchase-links a { width: 100%; min-width: 0; min-height: 44px; justify-content: space-between; }

@container shopping-checklist (max-width: 760px) {
  .shopping-table { display: none; }
  .shopping-mobile-list { display: grid; }
}

@media (max-width: 767px) {
  .shopping-table { display: none; }
  .shopping-mobile-list { display: grid; }
}
</style>
