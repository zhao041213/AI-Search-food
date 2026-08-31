<template>
  <main class="pantry-page">
    <header class="pantry-heading">
      <div>
        <p class="eyebrow">个人食材库</p>
        <h1>我的食材</h1>
        <p>管理当前库存，生成菜谱时会自动作为可用食材参与推荐。</p>
      </div>
      <div class="heading-actions">
        <RouterLink class="back-link" to="/">
          <ArrowLeft :size="16" aria-hidden="true" />
          <span>返回工作台</span>
        </RouterLink>
        <el-button type="primary" @click="openCreateDialog">
          <Plus :size="16" aria-hidden="true" />
          <span>添加食材</span>
        </el-button>
      </div>
    </header>

    <section class="pantry-panel" aria-label="库存食材列表">
      <div class="panel-heading">
        <div>
          <span class="panel-kicker">库存清单</span>
          <h2>现有食材</h2>
        </div>
        <div class="panel-metrics">
          <span v-if="expirySummary.expiredItems.length" class="summary-badge expired" role="status">
            已过期 {{ expirySummary.expiredItems.length }}
          </span>
          <span v-if="expirySummary.expiringSoonItems.length" class="summary-badge soon" role="status">
            7天内到期 {{ expirySummary.expiringSoonItems.length }}
          </span>
          <span class="count-badge" role="status" aria-atomic="true">{{ items.length }}</span>
        </div>
      </div>

      <el-skeleton v-if="loading" :rows="7" animated />
      <el-empty v-else-if="!items.length" description="还没有库存食材">
        <el-button type="primary" @click="openCreateDialog">添加第一种食材</el-button>
      </el-empty>
      <template v-else>
        <div v-if="expirySummary.expiredItems.length || expirySummary.expiringSoonItems.length" class="expiry-alert" role="alert">
          <TriangleAlert :size="17" aria-hidden="true" />
          <div>
            <strong>库存保质期提醒</strong>
            <span>{{ expiryNoticeText }}</span>
          </div>
          <el-button link type="primary" @click="statusFilter = 'attention'">查看临期食材</el-button>
        </div>

        <div class="pantry-toolbar" aria-label="库存筛选和排序">
          <el-radio-group v-model="statusFilter" size="small" aria-label="按保质期筛选">
            <el-radio-button label="all">全部 {{ items.length }}</el-radio-button>
            <el-radio-button label="attention">需处理 {{ expirySummary.expiredItems.length + expirySummary.expiringSoonItems.length }}</el-radio-button>
            <el-radio-button label="soon">临期 {{ expirySummary.expiringSoonItems.length }}</el-radio-button>
            <el-radio-button label="expired">已过期 {{ expirySummary.expiredItems.length }}</el-radio-button>
            <el-radio-button label="missing">未填写 {{ expirySummary.statusCounts.missing }}</el-radio-button>
          </el-radio-group>
          <el-select v-model="sortOrder" size="small" aria-label="库存排序" class="sort-select">
            <el-option label="按保质期排序" value="expiry" />
            <el-option label="按名称排序" value="name" />
            <el-option label="按最近更新排序" value="updated" />
          </el-select>
        </div>

        <el-empty v-if="!visibleItems.length" description="没有符合条件的库存">
          <el-button plain @click="statusFilter = 'all'">查看全部库存</el-button>
        </el-empty>
        <template v-else>
          <el-table class="pantry-table" :data="visibleItems" size="large">
            <el-table-column prop="ingredientName" label="食材" min-width="160" />
            <el-table-column label="分类" min-width="110">
              <template #default="scope">
                {{ scope.row.category || '未分类' }}
              </template>
            </el-table-column>
            <el-table-column label="库存" min-width="130">
              <template #default="scope">
                {{ formatQuantity(scope.row) }}
              </template>
            </el-table-column>
            <el-table-column label="保质期" min-width="180">
              <template #default="scope">
                <div class="expiry-cell">
                  <span>{{ scope.row.expireDate || '未填写' }}</span>
                  <span v-if="scope.row.expireDate" class="expiry-tag" :class="expiryClass(scope.row.expireDate)">
                    {{ expiryLabel(scope.row.expireDate) }}
                  </span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="172" fixed="right">
              <template #default="scope">
                <div class="row-actions">
                  <el-tooltip content="消耗库存" placement="top">
                    <button
                      class="icon-button consume"
                      type="button"
                      :aria-label="`消耗${scope.row.ingredientName}的库存`"
                      @click="openConsumeDialog(scope.row)"
                    >
                      <Minus :size="16" aria-hidden="true" />
                    </button>
                  </el-tooltip>
                  <el-tooltip content="编辑食材" placement="top">
                    <button
                      class="icon-button"
                      type="button"
                      :aria-label="`编辑${scope.row.ingredientName}`"
                      @click="openEditDialog(scope.row)"
                    >
                      <Pencil :size="16" aria-hidden="true" />
                    </button>
                  </el-tooltip>
                  <el-tooltip content="删除食材" placement="top">
                    <button
                      class="icon-button danger"
                      type="button"
                      :aria-label="`删除${scope.row.ingredientName}`"
                      :disabled="deletingId !== null"
                      @click="confirmDelete(scope.row)"
                    >
                      <Trash2 :size="16" aria-hidden="true" />
                    </button>
                  </el-tooltip>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <div class="pantry-mobile-list">
            <article v-for="item in visibleItems" :key="item.id" class="pantry-mobile-item">
              <div class="mobile-item-heading">
                <strong>{{ item.ingredientName }}</strong>
                <span>{{ item.category || '未分类' }}</span>
              </div>
              <dl class="mobile-item-details">
                <div>
                  <dt>库存</dt>
                  <dd>{{ formatQuantity(item) }}</dd>
                </div>
                <div>
                  <dt>保质期</dt>
                  <dd>
                    {{ item.expireDate || '未填写' }}
                    <span v-if="item.expireDate" class="expiry-tag" :class="expiryClass(item.expireDate)">
                      {{ expiryLabel(item.expireDate) }}
                    </span>
                  </dd>
                </div>
              </dl>
              <div class="row-actions mobile-row-actions">
                <el-tooltip content="消耗库存" placement="top">
                  <button
                    class="icon-button consume"
                    type="button"
                    :aria-label="`消耗${item.ingredientName}的库存`"
                    @click="openConsumeDialog(item)"
                  >
                    <Minus :size="16" aria-hidden="true" />
                  </button>
                </el-tooltip>
                <el-tooltip content="编辑食材" placement="top">
                  <button
                    class="icon-button"
                    type="button"
                    :aria-label="`编辑${item.ingredientName}`"
                    @click="openEditDialog(item)"
                  >
                    <Pencil :size="16" aria-hidden="true" />
                  </button>
                </el-tooltip>
                <el-tooltip content="删除食材" placement="top">
                  <button
                    class="icon-button danger"
                    type="button"
                    :aria-label="`删除${item.ingredientName}`"
                    :disabled="deletingId !== null"
                    @click="confirmDelete(item)"
                  >
                    <Trash2 :size="16" aria-hidden="true" />
                  </button>
                </el-tooltip>
              </div>
            </article>
          </div>
        </template>
      </template>
    </section>
  </main>

  <el-dialog
    v-model="dialogVisible"
    :title="editingId === null ? '添加食材' : '编辑食材'"
    width="min(520px, calc(100vw - 32px))"
    :close-on-click-modal="!saving"
    @closed="resetForm"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="saveItem">
      <el-form-item label="食材名称" prop="ingredientName">
        <el-input v-model="form.ingredientName" maxlength="128" show-word-limit placeholder="例如：番茄" />
      </el-form-item>
      <div class="form-grid">
        <el-form-item label="分类">
          <el-select v-model="form.category" clearable placeholder="选择分类">
            <el-option label="蔬菜" value="蔬菜" />
            <el-option label="水果" value="水果" />
            <el-option label="肉蛋" value="肉蛋" />
            <el-option label="水产" value="水产" />
            <el-option label="主食" value="主食" />
            <el-option label="调味" value="调味" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="保质期">
          <el-date-picker
            v-model="form.expireDate"
            type="date"
            value-format="YYYY-MM-DD"
            format="YYYY-MM-DD"
            placeholder="选择日期"
          />
        </el-form-item>
      </div>
      <div class="form-grid">
        <el-form-item label="数量" prop="quantity">
          <el-input-number v-model="form.quantity" :min="0.01" :precision="2" :step="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="单位">
          <el-input v-model="form.unit" maxlength="32" placeholder="例如：个、克、包" />
        </el-form-item>
      </div>
    </el-form>
    <template #footer>
      <el-button :disabled="saving" @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="saveItem">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog
    v-model="consumeDialogVisible"
    title="消耗库存"
    width="min(420px, calc(100vw - 32px))"
    :close-on-click-modal="!consuming"
    :close-on-press-escape="!consuming"
    :show-close="!consuming"
    @closed="resetConsumeForm"
  >
    <template v-if="consumingItem">
      <div class="consume-item-summary">
        <span>食材</span>
        <strong>{{ consumingItem.ingredientName }}</strong>
        <span>当前库存</span>
        <strong>{{ formatQuantity(consumingItem) }}</strong>
      </div>
      <el-form ref="consumeFormRef" :model="consumeForm" :rules="consumeRules" label-position="top" @submit.prevent="consumeItem">
        <el-form-item label="消耗数量" prop="quantity">
          <el-input-number
            v-model="consumeForm.quantity"
            :min="0.01"
            :max="availableQuantity"
            :precision="2"
            :step="0.1"
            controls-position="right"
            :disabled="consuming"
            aria-describedby="consume-quantity-hint consume-quantity-feedback"
          />
          <p id="consume-quantity-hint" class="consume-form-hint">最多消耗 {{ formatNumber(availableQuantity) }}{{ consumingItem.unit || '' }}</p>
          <p id="consume-quantity-feedback" class="consume-remaining" aria-live="polite">
            消耗后剩余：{{ formatNumber(remainingQuantity) }}{{ consumingItem.unit || '' }}
          </p>
        </el-form-item>
      </el-form>
    </template>
    <template #footer>
      <el-button :disabled="consuming" @click="consumeDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="consuming" :disabled="consuming" @click="consumeItem">确认消耗</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Minus, Pencil, Plus, Trash2, TriangleAlert } from 'lucide-vue-next'
import {
  consumePantryItem,
  createPantryItem,
  deletePantryItem,
  getPantryItems,
  updatePantryItem
} from '../api/pantry'
import {
  getDaysUntilExpiry,
  getExpiryClass as expiryClass,
  getExpiryLabel as expiryLabel,
  getExpiryStatus,
  hasAvailableQuantity,
  summarizePantryExpiry
} from '../utils/pantryExpiry'

const items = ref([])
const statusFilter = ref('all')
const sortOrder = ref('expiry')
const loading = ref(false)
const saving = ref(false)
const deletingId = ref(null)
const dialogVisible = ref(false)
const editingId = ref(null)
const formRef = ref(null)
const form = reactive(emptyForm())
const consumeDialogVisible = ref(false)
const consumeFormRef = ref(null)
const consumingItem = ref(null)
const consuming = ref(false)
const consumeForm = reactive({ quantity: null })

const expirySummary = computed(() => summarizePantryExpiry(items.value))
const expiryNoticeText = computed(() => {
  const messages = []
  if (expirySummary.value.expiredItems.length) {
    messages.push(`${expirySummary.value.expiredItems.length} 种食材已过期`)
  }
  if (expirySummary.value.expiringSoonItems.length) {
    messages.push(`${expirySummary.value.expiringSoonItems.length} 种食材将在 7 天内到期`)
  }
  return messages.join('，')
})
const visibleItems = computed(() => {
  const filtered = items.value.filter((item) => {
    const status = getExpiryStatus(item.expireDate)
    if (statusFilter.value === 'all') {
      return true
    }
    if (statusFilter.value === 'attention') {
      return hasAvailableQuantity(item) && (status === 'expired' || status === 'soon')
    }
    if (statusFilter.value === 'expired' || statusFilter.value === 'soon') {
      return hasAvailableQuantity(item) && status === statusFilter.value
    }
    return status === statusFilter.value
  })

  return [...filtered].sort((left, right) => {
    if (sortOrder.value === 'name') {
      return String(left.ingredientName || '').localeCompare(String(right.ingredientName || ''), 'zh-CN')
    }
    if (sortOrder.value === 'updated') {
      return String(right.updatedAt || '').localeCompare(String(left.updatedAt || ''))
    }

    const leftDays = getDaysUntilExpiry(left.expireDate)
    const rightDays = getDaysUntilExpiry(right.expireDate)
    if (leftDays === null && rightDays === null) {
      return Number(right.id || 0) - Number(left.id || 0)
    }
    if (leftDays === null) {
      return 1
    }
    if (rightDays === null) {
      return -1
    }
    return leftDays - rightDays || Number(right.id || 0) - Number(left.id || 0)
  })
})

const availableQuantity = computed(() => Number(consumingItem.value?.quantity || 0))
const remainingQuantity = computed(() => {
  const quantity = Number(consumeForm.quantity || 0)
  return Math.max(0, availableQuantity.value - quantity)
})

const rules = {
  ingredientName: [
    { required: true, message: '请输入食材名称', trigger: 'blur' }
  ]
}

const consumeRules = {
  quantity: [
    {
      validator: validateConsumeQuantity,
      trigger: ['blur', 'change']
    }
  ]
}

loadItems()

async function loadItems() {
  loading.value = true
  try {
    const response = await getPantryItems()
    items.value = response.data.data || []
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '食材库存加载失败'))
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(item) {
  editingId.value = item.id
  Object.assign(form, {
    ingredientName: item.ingredientName || '',
    category: item.category || '',
    quantity: item.quantity ?? null,
    unit: item.unit || '',
    expireDate: item.expireDate || ''
  })
  dialogVisible.value = true
}

function openConsumeDialog(item) {
  const quantity = Number(item.quantity || 0)
  if (!Number.isFinite(quantity) || quantity <= 0) {
    ElMessage.warning(`“${item.ingredientName}”当前没有可消耗的库存，请先补充数量。`)
    return
  }

  consumingItem.value = item
  consumeForm.quantity = null
  consumeDialogVisible.value = true
}

async function consumeItem() {
  if (consuming.value || !consumingItem.value) {
    return
  }

  try {
    await consumeFormRef.value?.validate()
  } catch {
    return
  }

  consuming.value = true
  try {
    const response = await consumePantryItem(consumingItem.value.id, consumeForm.quantity)
    const updatedItem = response.data.data
    items.value = items.value.map((item) => (item.id === updatedItem.id ? updatedItem : item))
    ElMessage.success(`已消耗${consumeForm.quantity}${consumingItem.value.unit || ''}“${consumingItem.value.ingredientName}”`)
    consumeDialogVisible.value = false
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '库存消耗失败'))
  } finally {
    consuming.value = false
  }
}

async function saveItem() {
  if (saving.value) {
    return
  }

  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  saving.value = true
  const payload = {
    ingredientName: form.ingredientName.trim(),
    category: form.category || null,
    quantity: form.quantity,
    unit: form.unit.trim() || null,
    expireDate: form.expireDate || null
  }

  try {
    if (editingId.value === null) {
      await createPantryItem(payload)
      ElMessage.success('食材已加入库存')
    } else {
      await updatePantryItem(editingId.value, payload)
      ElMessage.success('食材库存已更新')
    }
    dialogVisible.value = false
    await loadItems()
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '食材保存失败'))
  } finally {
    saving.value = false
  }
}

async function confirmDelete(item) {
  if (deletingId.value !== null) {
    return
  }

  try {
    await ElMessageBox.confirm(
      `删除“${item.ingredientName}”后不会影响已经保存的菜谱，确定继续吗？`,
      '删除食材',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    return
  }

  deletingId.value = item.id
  try {
    await deletePantryItem(item.id)
    items.value = items.value.filter((candidate) => candidate.id !== item.id)
    ElMessage.success('食材已删除')
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '食材删除失败'))
  } finally {
    deletingId.value = null
  }
}

function emptyForm() {
  return {
    ingredientName: '',
    category: '',
    quantity: null,
    unit: '',
    expireDate: ''
  }
}

function resetForm() {
  Object.assign(form, emptyForm())
  formRef.value?.clearValidate()
  editingId.value = null
}

function resetConsumeForm() {
  consumeForm.quantity = null
  consumeFormRef.value?.clearValidate()
  consumingItem.value = null
}

function validateConsumeQuantity(rule, value, callback) {
  const quantity = Number(value)
  if (!Number.isFinite(quantity) || quantity <= 0) {
    callback(new Error('请输入大于 0 的消耗数量'))
    return
  }
  if (Math.abs(quantity * 100 - Math.round(quantity * 100)) > 1e-8) {
    callback(new Error('消耗数量最多保留 2 位小数'))
    return
  }
  if (quantity > availableQuantity.value) {
    callback(new Error('消耗数量不能超过当前库存'))
    return
  }
  callback()
}

function formatQuantity(item) {
  if (item.quantity === null || item.quantity === undefined) {
    return '未填写'
  }
  return `${item.quantity}${item.unit || ''}`
}

function formatNumber(value) {
  return Number(value || 0).toFixed(2).replace(/\.00$/, '').replace(/(\.\d)0$/, '$1')
}

function getErrorMessage(error, fallback) {
  const status = error?.response?.status
  if (status === 401) {
    return '登录状态已失效，请重新登录'
  }
  if (status === 403) {
    return '当前账号没有访问库存的权限'
  }
  return error?.response?.data?.message || error?.message || fallback
}
</script>

<style scoped>
.pantry-page {
  min-height: calc(100vh - 58px);
  padding: clamp(20px, 3vw, 36px);
  color: var(--app-text);
}

.pantry-heading,
.panel-heading,
.heading-actions,
.row-actions,
.expiry-cell {
  display: flex;
  align-items: center;
}

.pantry-heading,
.panel-heading {
  justify-content: space-between;
  gap: 18px;
}

.pantry-heading {
  align-items: flex-start;
  max-width: 1280px;
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

.pantry-heading h1,
.panel-heading h2 {
  margin: 7px 0 0;
  color: var(--app-text);
}

.pantry-heading p:not(.eyebrow) {
  margin: 8px 0 0;
  color: var(--app-text-muted);
}

.heading-actions {
  flex: 0 0 auto;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.heading-actions :deep(.el-button > span) {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  min-height: 32px;
  padding: 0 10px;
  border: 1px solid var(--app-line-strong);
  border-radius: 6px;
  color: var(--app-text);
  background: var(--app-surface);
  font-size: 13px;
  font-weight: 800;
  text-decoration: none;
}

.pantry-panel {
  max-width: 1280px;
  min-height: 460px;
  margin: 0 auto;
  padding: 20px;
  border: 1px solid var(--app-line);
  border-radius: 8px;
  background: var(--app-surface);
  box-shadow: var(--app-panel-shadow);
}

.count-badge {
  display: inline-grid;
  min-width: 30px;
  min-height: 30px;
  place-items: center;
  border: 1px solid var(--app-line-strong);
  border-radius: 6px;
  color: var(--app-text);
  background: var(--app-surface-soft);
  font-size: 12px;
  font-weight: 900;
}

.panel-metrics {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}

.summary-badge {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 9px;
  border: 1px solid var(--app-line-strong);
  border-radius: 6px;
  color: var(--app-text-soft);
  background: var(--app-surface-soft);
  font-size: 11px;
  font-weight: 800;
}

.summary-badge.soon {
  border-color: var(--app-accent);
  color: var(--app-text);
  background: var(--app-accent-soft);
}

.summary-badge.expired {
  border-color: #d14343;
  color: #b42318;
  background: #fff1f0;
}

.expiry-alert {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 18px;
  padding: 11px 12px;
  border: 1px solid var(--app-accent);
  border-radius: 6px;
  color: var(--app-text);
  background: var(--app-accent-soft);
}

.expiry-alert > svg {
  flex: 0 0 auto;
}

.expiry-alert > div {
  display: grid;
  gap: 2px;
  min-width: 0;
  flex: 1;
}

.expiry-alert strong {
  font-size: 13px;
}

.expiry-alert span {
  color: var(--app-text-muted);
  font-size: 12px;
}

.expiry-alert :deep(.el-button) {
  flex: 0 0 auto;
  min-height: 30px;
}

.pantry-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 18px;
}

.pantry-toolbar :deep(.el-radio-group) {
  display: flex;
  flex-wrap: wrap;
}

.sort-select {
  width: 150px;
  flex: 0 0 auto;
}

.pantry-panel :deep(.el-table) {
  margin-top: 18px;
}

.pantry-mobile-list {
  display: none;
}

.row-actions {
  gap: 6px;
}

.icon-button {
  display: inline-grid;
  width: 32px;
  height: 32px;
  place-items: center;
  border: 1px solid var(--app-line-strong);
  border-radius: 5px;
  color: var(--app-text-soft);
  background: var(--app-surface);
  cursor: pointer;
}

.icon-button:hover,
.icon-button:focus-visible {
  border-color: var(--app-accent);
  color: var(--app-text);
  background: var(--app-accent-soft);
  outline: none;
}

.icon-button.danger:hover,
.icon-button.danger:focus-visible {
  border-color: #d14343;
  color: #b42318;
  background: #fff1f0;
}

.icon-button.consume:hover,
.icon-button.consume:focus-visible {
  border-color: var(--app-accent);
  color: var(--app-text);
  background: var(--app-accent-soft);
}

.icon-button:disabled {
  cursor: wait;
  opacity: 0.45;
}

.expiry-cell {
  flex-wrap: wrap;
  gap: 7px;
}

.expiry-tag {
  display: inline-flex;
  align-items: center;
  min-height: 23px;
  padding: 0 7px;
  border: 1px solid var(--app-line-strong);
  border-radius: 4px;
  font-size: 11px;
  font-weight: 800;
}

.expiry-tag.normal {
  color: var(--app-text-soft);
  background: var(--app-surface-soft);
}

.expiry-tag.soon {
  border-color: var(--app-accent);
  color: var(--app-text);
  background: var(--app-accent-soft);
}

.expiry-tag.expired {
  border-color: #d14343;
  color: #b42318;
  background: #fff1f0;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.form-grid :deep(.el-select),
.form-grid :deep(.el-date-editor),
.form-grid :deep(.el-input-number) {
  width: 100%;
}

.consume-item-summary {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 8px 16px;
  margin-bottom: 18px;
  padding: 12px;
  border: 1px solid var(--app-line);
  border-radius: 6px;
  background: var(--app-surface-soft);
  color: var(--app-text-muted);
  font-size: 13px;
}

.consume-item-summary strong {
  min-width: 0;
  color: var(--app-text);
  overflow-wrap: anywhere;
}

.consume-form-hint,
.consume-remaining {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.5;
}

.consume-form-hint {
  color: var(--app-text-muted);
}

.consume-remaining {
  color: var(--app-text-soft);
  font-weight: 800;
}

@media (max-width: 720px) {
  .pantry-page {
    padding: 18px 14px;
  }

  .pantry-heading {
    flex-direction: column;
  }

  .heading-actions {
    justify-content: flex-start;
  }

  .pantry-panel {
    min-height: 0;
    padding: 14px;
  }

  .panel-heading {
    align-items: flex-start;
  }

  .panel-metrics {
    max-width: 55%;
  }

  .expiry-alert {
    align-items: flex-start;
    flex-wrap: wrap;
  }

  .expiry-alert :deep(.el-button) {
    margin-left: 27px;
  }

  .pantry-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .sort-select {
    width: 100%;
  }

  .pantry-table {
    display: none;
  }

  .pantry-mobile-list {
    display: grid;
    gap: 10px;
    margin-top: 16px;
  }

  .pantry-mobile-item {
    display: grid;
    gap: 12px;
    padding: 14px;
    border: 1px solid var(--app-line);
    border-radius: 6px;
    background: var(--app-surface);
  }

  .mobile-item-heading,
  .mobile-item-details,
  .mobile-item-details div,
  .mobile-row-actions {
    display: flex;
    align-items: center;
  }

  .mobile-item-heading {
    justify-content: space-between;
    gap: 12px;
  }

  .mobile-item-heading strong {
    overflow: hidden;
    color: var(--app-text);
    font-size: 16px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .mobile-item-heading span {
    flex: 0 0 auto;
    color: var(--app-text-muted);
    font-size: 12px;
  }

  .mobile-item-details {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
    margin: 0;
  }

  .mobile-item-details div {
    min-width: 0;
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }

  .mobile-item-details dt {
    color: var(--app-text-faint);
    font-size: 11px;
    font-weight: 800;
  }

  .mobile-item-details dd {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 5px;
    min-width: 0;
    margin: 0;
    color: var(--app-text-soft);
    font-size: 13px;
    font-weight: 800;
  }

  .mobile-row-actions {
    justify-content: flex-end;
  }

  .icon-button {
    width: 44px;
    height: 44px;
  }

  .form-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
</style>
