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
        <span class="count-badge" role="status" aria-atomic="true">{{ items.length }}</span>
      </div>

      <el-skeleton v-if="loading" :rows="7" animated />
      <el-empty v-else-if="!items.length" description="还没有库存食材">
        <el-button type="primary" @click="openCreateDialog">添加第一种食材</el-button>
      </el-empty>
      <template v-else>
        <el-table class="pantry-table" :data="items" size="large">
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
          <el-table-column label="操作" width="132" fixed="right">
            <template #default="scope">
              <div class="row-actions">
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
          <article v-for="item in items" :key="item.id" class="pantry-mobile-item">
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
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Pencil, Plus, Trash2 } from 'lucide-vue-next'
import {
  createPantryItem,
  deletePantryItem,
  getPantryItems,
  updatePantryItem
} from '../api/pantry'

const items = ref([])
const loading = ref(false)
const saving = ref(false)
const deletingId = ref(null)
const dialogVisible = ref(false)
const editingId = ref(null)
const formRef = ref(null)
const form = reactive(emptyForm())

const rules = {
  ingredientName: [
    { required: true, message: '请输入食材名称', trigger: 'blur' }
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

function formatQuantity(item) {
  if (item.quantity === null || item.quantity === undefined) {
    return '未填写'
  }
  return `${item.quantity}${item.unit || ''}`
}

function expiryLabel(value) {
  const days = Math.floor((new Date(`${value}T00:00:00`).getTime() - startOfToday()) / 86400000)
  if (days < 0) {
    return '已过期'
  }
  if (days <= 2) {
    return '即将到期'
  }
  return '正常'
}

function expiryClass(value) {
  const label = expiryLabel(value)
  if (label === '已过期') {
    return 'expired'
  }
  if (label === '即将到期') {
    return 'soon'
  }
  return 'normal'
}

function startOfToday() {
  const now = new Date()
  now.setHours(0, 0, 0, 0)
  return now.getTime()
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

  .form-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
</style>
