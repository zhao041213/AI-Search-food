<template>
  <section class="feature-suggestions-workspace" aria-labelledby="feature-suggestions-title">
    <header class="suggestions-toolbar">
      <div>
        <p class="suggestions-kicker"><MessageSquare :size="13" aria-hidden="true" />用户声音</p>
        <h2 id="feature-suggestions-title">功能建议</h2>
        <span>集中处理用户提交的新功能、体验改进和问题反馈。</span>
      </div>
      <div class="suggestions-summary" aria-live="polite">
        <strong>{{ total }}</strong><span>条建议</span>
        <el-button :loading="loading" circle aria-label="刷新功能建议" title="刷新功能建议" @click="loadSuggestions"><RefreshCw :size="16" aria-hidden="true" /></el-button>
      </div>
    </header>

    <form class="suggestions-filters" @submit.prevent="applyFilters">
      <el-input v-model.trim="filters.keyword" clearable placeholder="搜索标题或详情" aria-label="搜索功能建议" />
      <el-select v-model="filters.type" placeholder="建议类型" aria-label="筛选建议类型" clearable>
        <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-select v-model="filters.status" placeholder="处理状态" aria-label="筛选处理状态" clearable>
        <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-button type="primary" native-type="submit" :loading="loading">查询</el-button>
      <el-button :loading="exporting" @click="exportSuggestions">导出 CSV</el-button>
    </form>

    <el-alert v-if="errorMessage" class="suggestions-error" type="error" :title="errorMessage" show-icon :closable="false" />
    <div class="suggestions-table">
      <el-table v-loading="loading" :data="items" row-key="id" height="100%" empty-text="暂无功能建议" @row-click="openDetail">
        <el-table-column prop="createdAt" label="提交时间" min-width="170">
          <template #default="scope">{{ formatTime(scope.row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="类型" width="100"><template #default="scope"><el-tag effect="light">{{ typeLabel(scope.row.type) }}</el-tag></template></el-table-column>
        <el-table-column prop="title" label="标题" min-width="230" show-overflow-tooltip />
        <el-table-column label="状态" width="110"><template #default="scope"><span class="status-mark" :class="`status-${scope.row.status?.toLowerCase()}`">{{ statusLabel(scope.row.status) }}</span></template></el-table-column>
        <el-table-column prop="userId" label="用户 ID" width="100" />
        <el-table-column label="详情" width="82" fixed="right"><template #default="scope"><el-button link type="primary" @click.stop="openDetail(scope.row)">处理</el-button></template></el-table-column>
      </el-table>
    </div>
    <footer class="suggestions-footer">
      <span>状态更新和管理员回复会通知提交者</span>
      <el-pagination v-if="total" v-model:current-page="page" :page-size="PAGE_SIZE" :total="total" background layout="prev, pager, next" @current-change="loadSuggestions" />
    </footer>

    <el-drawer v-model="detailVisible" title="处理功能建议" size="min(620px, 94vw)" destroy-on-close>
      <div v-loading="detailLoading" class="suggestion-admin-detail" v-if="selected">
        <div class="detail-status-row">
          <span class="suggestion-type">{{ typeLabel(selected.type) }} · 用户 {{ selected.userId }}</span>
          <span class="status-mark" :class="`status-${selected.status?.toLowerCase()}`">{{ statusLabel(selected.status) }}</span>
        </div>
        <h2>{{ selected.title }}</h2>
        <p class="detail-time">提交于 {{ formatTime(selected.createdAt) }}</p>
        <section class="admin-detail-copy"><h3>详细描述</h3><p>{{ selected.detail }}</p></section>
        <section v-if="selected.expectedEffect" class="admin-detail-copy"><h3>预期效果</h3><p>{{ selected.expectedEffect }}</p></section>
        <img v-if="screenshotUrl" class="detail-screenshot" :src="screenshotUrl" alt="用户附带的截图" />
        <section v-if="selected.additions?.length" class="admin-detail-copy"><h3>用户补充</h3><p v-for="addition in selected.additions" :key="addition.id">{{ addition.content }}<small>{{ formatTime(addition.createdAt) }}</small></p></section>
        <el-form label-position="top" class="admin-edit-form" @submit.prevent="saveDetail">
          <el-form-item label="处理状态"><el-select v-model="edit.status" class="full-width"><el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
          <el-form-item label="管理员回复"><el-input v-model="edit.adminReply" type="textarea" :rows="4" placeholder="写给用户的处理说明" /></el-form-item>
          <el-form-item label="内部备注"><el-input v-model="edit.internalNote" type="textarea" :rows="3" placeholder="仅管理员可见" /></el-form-item>
          <el-form-item label="重复项 ID（可选）"><el-input-number v-model="edit.duplicateOfId" :min="1" controls-position="right" class="full-width" /></el-form-item>
          <div class="admin-detail-actions"><el-button type="danger" plain :loading="deleting" @click="hideSuggestion">隐藏建议</el-button><el-button type="primary" native-type="submit" :loading="saving">保存处理结果</el-button></div>
        </el-form>
      </div>
    </el-drawer>
  </section>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MessageSquare, RefreshCw } from 'lucide-vue-next'
import {
  deleteAdminFeatureSuggestion,
  getAdminFeatureSuggestion,
  getAdminFeatureSuggestionExport,
  getAdminFeatureSuggestions,
  loadFeatureSuggestionScreenshot,
  updateAdminFeatureSuggestion
} from '../api/featureSuggestions'

const PAGE_SIZE = 10
const typeOptions = [
  { value: 'NEW_FEATURE', label: '新功能' },
  { value: 'UX_IMPROVEMENT', label: '体验改进' },
  { value: 'BUG_REPORT', label: '问题反馈' },
  { value: 'OTHER', label: '其他' }
]
const statusOptions = [
  { value: 'PENDING', label: '待处理' },
  { value: 'ACCEPTED', label: '已采纳' },
  { value: 'PLANNED', label: '已排期' },
  { value: 'IN_DEVELOPMENT', label: '开发中' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'DECLINED', label: '暂不处理' }
]
const filters = ref({ keyword: '', type: '', status: '' })
const items = ref([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)
const exporting = ref(false)
const errorMessage = ref('')
const detailVisible = ref(false)
const detailLoading = ref(false)
const saving = ref(false)
const deleting = ref(false)
const selected = ref(null)
const edit = ref({ status: 'PENDING', adminReply: '', internalNote: '', duplicateOfId: null })
const screenshotUrl = ref('')

onMounted(loadSuggestions)
onBeforeUnmount(revokeScreenshot)

async function loadSuggestions() {
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await getAdminFeatureSuggestions({ ...filters.value, page: page.value, size: PAGE_SIZE })
    const result = response.data.data || {}
    items.value = result.items || []
    total.value = result.total || 0
  } catch (error) {
    errorMessage.value = error?.response?.data?.message || error?.message || '功能建议加载失败'
  } finally { loading.value = false }
}

function applyFilters() { page.value = 1; loadSuggestions() }

async function openDetail(row) {
  detailVisible.value = true
  detailLoading.value = true
  selected.value = row
  revokeScreenshot()
  try {
    const response = await getAdminFeatureSuggestion(row.id)
    selected.value = response.data.data
    edit.value = {
      status: selected.value.status || 'PENDING',
      adminReply: selected.value.adminReply || '',
      internalNote: selected.value.internalNote || '',
      duplicateOfId: selected.value.duplicateOfId || null
    }
    if (selected.value.screenshotUrl) {
      const image = await loadFeatureSuggestionScreenshot(row.id, true)
      screenshotUrl.value = URL.createObjectURL(image.data)
    }
  } catch (error) { ElMessage.error(error?.response?.data?.message || '建议详情加载失败') }
  finally { detailLoading.value = false }
}

async function saveDetail() {
  if (!selected.value?.id) return
  saving.value = true
  try {
    const response = await updateAdminFeatureSuggestion(selected.value.id, edit.value)
    selected.value = response.data.data
    ElMessage.success('处理结果已保存')
    await loadSuggestions()
  } catch (error) { ElMessage.error(error?.response?.data?.message || '保存失败，请稍后再试') }
  finally { saving.value = false }
}

async function hideSuggestion() {
  if (!selected.value?.id) return
  try { await ElMessageBox.confirm('隐藏后该建议将不再出现在列表中，确定继续吗？', '确认隐藏', { type: 'warning' }) }
  catch { return }
  deleting.value = true
  try { await deleteAdminFeatureSuggestion(selected.value.id); ElMessage.success('建议已隐藏'); detailVisible.value = false; await loadSuggestions() }
  catch (error) { ElMessage.error(error?.response?.data?.message || '隐藏失败') }
  finally { deleting.value = false }
}

async function exportSuggestions() {
  exporting.value = true
  try {
    const response = await getAdminFeatureSuggestionExport(filters.value)
    const url = URL.createObjectURL(response.data)
    const link = document.createElement('a'); link.href = url; link.download = 'feature-suggestions.csv'; link.click(); URL.revokeObjectURL(url)
  } catch (error) { ElMessage.error(error?.response?.data?.message || '导出失败') }
  finally { exporting.value = false }
}

function revokeScreenshot() { if (screenshotUrl.value) URL.revokeObjectURL(screenshotUrl.value); screenshotUrl.value = '' }
function typeLabel(value) { return typeOptions.find((item) => item.value === value)?.label || '其他' }
function statusLabel(value) { return statusOptions.find((item) => item.value === value)?.label || '待处理' }
function formatTime(value) { return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '刚刚' }
</script>

<style scoped>
.feature-suggestions-workspace { display: grid; grid-template-rows: auto auto auto minmax(0, 1fr) auto; gap: 12px; min-height: 0; height: 100%; }
.suggestions-toolbar { display: flex; justify-content: space-between; align-items: end; gap: 20px; }.suggestions-kicker { display: flex; align-items: center; gap: 5px; margin: 0 0 5px; color: var(--app-accent); font-size: 11px; font-weight: 800; letter-spacing: .1em; }.suggestions-toolbar h2 { margin: 0; font-size: 24px; }.suggestions-toolbar span { color: var(--app-text-muted); font-size: 12px; }.suggestions-summary { display: flex; align-items: center; gap: 7px; white-space: nowrap; }.suggestions-summary strong { color: var(--app-accent); font-size: 28px; }.suggestions-filters { display: grid; grid-template-columns: minmax(180px, 1.5fr) 150px 150px auto auto; gap: 8px; }.suggestions-error { margin-bottom: 0; }.suggestions-table { min-height: 0; overflow: hidden; border: 1px solid var(--app-line); border-radius: 12px; background: var(--app-surface); }.suggestions-footer { display: flex; justify-content: space-between; align-items: center; gap: 15px; color: var(--app-text-muted); font-size: 12px; }.status-mark { display: inline-flex; padding: 3px 8px; border-radius: 99px; background: var(--app-surface-soft); color: var(--app-text-muted); font-size: 12px; }.status-completed { color: #267755; background: #e2f3e8; }.status-in_development, .status-accepted { color: #356d9a; background: #e5f0f8; }.status-declined { color: #a75d63; background: #fae8e8; }.suggestion-admin-detail h2 { margin: 15px 0 5px; font-size: 25px; }.detail-status-row { display: flex; justify-content: space-between; gap: 12px; }.suggestion-type, .detail-time { color: var(--app-text-muted); font-size: 12px; }.admin-detail-copy { margin: 20px 0; padding-bottom: 16px; border-bottom: 1px solid var(--app-line); }.admin-detail-copy h3 { margin: 0 0 8px; font-size: 13px; color: var(--app-text-muted); }.admin-detail-copy p { margin: 0; line-height: 1.7; white-space: pre-wrap; }.admin-detail-copy small { display: block; margin-top: 5px; color: var(--app-text-muted); font-size: 11px; }.detail-screenshot { display: block; max-width: 100%; max-height: 350px; margin: 15px 0; border-radius: 10px; }.admin-edit-form { margin-top: 24px; }.full-width { width: 100%; }.admin-detail-actions { display: flex; justify-content: space-between; gap: 10px; }
@media (max-width: 820px) { .suggestions-filters { grid-template-columns: 1fr 1fr; }.suggestions-filters .el-input { grid-column: 1 / -1; }.suggestions-footer { align-items: end; flex-direction: column; } }
@media (max-width: 520px) { .suggestions-toolbar { align-items: start; flex-direction: column; }.suggestions-summary { align-self: stretch; }.suggestions-filters { grid-template-columns: 1fr; }.suggestions-filters .el-input { grid-column: auto; } }
</style>
