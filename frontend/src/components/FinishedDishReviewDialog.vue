<template>
  <el-dialog
    class="finished-dish-review-dialog"
    :model-value="modelValue"
    width="min(1080px, calc(100vw - 32px))"
    :close-on-click-modal="false"
    :close-on-press-escape="true"
    destroy-on-close
    @open="handleOpen"
    @update:model-value="handleVisibility"
    @closed="handleClosed"
  >
    <template #header>
      <div class="review-dialog-heading">
        <span class="review-heading-icon" aria-hidden="true">
          <Sparkles :size="18" :stroke-width="1.8" />
        </span>
        <div>
          <p>成品图 AI 评价</p>
          <h2 id="finished-dish-review-title">{{ recipeTitle }}</h2>
        </div>
      </div>
    </template>

    <section class="review-workbench" aria-labelledby="finished-dish-review-title">
      <aside class="review-image-panel" aria-label="成品图上传与预览">
        <div class="review-image-head">
          <span>成品图</span>
          <small>{{ selectedHistoryId ? '历史评价' : selectedImageFile ? '待评价' : '未选择' }}</small>
        </div>

        <div class="review-image-stage" :class="{ 'review-image-stage--ready': previewUrl }">
          <img v-if="previewUrl" :src="previewUrl" alt="待评价的菜品成品图" />
          <div v-else class="review-image-empty">
            <ImagePlus :size="34" :stroke-width="1.5" aria-hidden="true" />
            <strong>上传成品图</strong>
            <span>清晰展示菜品主体，评价更准确</span>
          </div>
        </div>

        <input
          ref="imageInput"
          class="review-file-input"
          type="file"
          accept="image/jpeg,image/png,image/webp"
          @change="handleImageSelected"
        />

        <div class="review-image-actions">
          <el-button type="primary" @click="openImagePicker">
            <Upload :size="16" aria-hidden="true" />
            <span>上传图片</span>
          </el-button>
          <el-button @click="cameraCaptureVisible = true">
            <Camera :size="16" aria-hidden="true" />
            <span>拍摄成品图</span>
          </el-button>
        </div>
        <p class="review-image-note">支持 JPG、PNG、WEBP，单张不超过 5MB</p>
      </aside>

      <section class="review-output-panel">
        <header class="review-recipe-context">
          <div>
            <p class="review-eyebrow">关联菜谱</p>
            <h3>{{ recipeTitle }}</h3>
          </div>
          <span>{{ recipeIngredients.length }} 种食材 · {{ recipeSteps.length }} 个步骤</span>
        </header>

        <el-tabs v-model="activeTab" class="review-tabs" stretch>
          <el-tab-pane name="summary">
            <template #label>
              <span>总评</span>
            </template>

            <section v-if="currentReview" class="review-tab-content" aria-label="成品图总评">
              <div class="score-summary">
                <div class="score-value">
                  <strong>{{ currentReview.overallScore ?? 0 }}</strong>
                  <span>综合分</span>
                </div>
                <p>{{ currentReview.summary || '已完成本次成品图评价。' }}</p>
              </div>

              <div class="dimension-grid" aria-label="菜谱对比维度">
                <article v-for="dimension in dimensions" :key="dimension.key" class="dimension-item">
                  <component :is="dimension.icon" :size="18" aria-hidden="true" />
                  <div>
                    <span>{{ dimension.label }}</span>
                    <strong>{{ dimension.value?.score ?? 0 }}</strong>
                    <p>{{ dimension.value?.comment || '暂无评价说明' }}</p>
                  </div>
                </article>
              </div>

              <section v-if="currentReview.strengths?.length" class="strength-section">
                <h4><CheckCircle2 :size="16" aria-hidden="true" /> 做得不错</h4>
                <ul>
                  <li v-for="strength in currentReview.strengths" :key="strength">{{ strength }}</li>
                </ul>
              </section>

              <p v-if="currentReview.safetyNote" class="review-safety-note">{{ currentReview.safetyNote }}</p>
            </section>

            <div v-else class="review-tab-empty">
              <PanelsTopLeft :size="34" :stroke-width="1.45" aria-hidden="true" />
              <strong>等待成品图</strong>
              <span>上传或拍摄成品图后，将结合当前菜谱分析色泽、火候和摆盘。</span>
            </div>
          </el-tab-pane>

          <el-tab-pane name="issues">
            <template #label>
              <span>问题与建议</span>
            </template>

            <section v-if="currentReview?.issues?.length" class="issue-list" aria-label="问题与建议">
              <article v-for="(issue, index) in currentReview.issues" :key="`${issue.title}-${index}`" class="issue-item">
                <span class="issue-level" :class="`issue-level--${issue.severity || 'normal'}`">
                  {{ issueSeverityLabel(issue.severity) }}
                </span>
                <div>
                  <h4>{{ issue.title || '待优化项' }}</h4>
                  <p>{{ issue.evidence || '未提供具体原因' }}</p>
                  <strong>建议：{{ issue.suggestion || '下次烹饪时继续观察这一点。' }}</strong>
                </div>
              </article>
            </section>
            <div v-else class="review-tab-empty">
              <AlertTriangle :size="34" :stroke-width="1.45" aria-hidden="true" />
              <strong>{{ currentReview ? '暂未发现明显问题' : '尚未生成评价' }}</strong>
              <span>{{ currentReview ? '这次成品已经达到当前菜谱的预期。' : '完成评价后，这里会给出可执行的改进建议。' }}</span>
            </div>
          </el-tab-pane>

          <el-tab-pane name="history">
            <template #label>
              <span>历史评价</span>
            </template>

            <section v-if="auth.isUser" class="history-panel" aria-label="历史成品图评价">
              <div v-if="historyLoading" class="history-state">
                <LoaderCircle class="history-spinner" :size="25" aria-hidden="true" />
                正在加载评价历史
              </div>
              <div v-else-if="!history.length" class="review-tab-empty review-tab-empty--compact">
                <History :size="30" :stroke-width="1.45" aria-hidden="true" />
                <strong>还没有历史评价</strong>
                <span>本次评价完成后会自动保存到这里。</span>
              </div>
              <template v-else>
                <article
                  v-for="item in history"
                  :key="item.id"
                  class="history-review-item"
                  :class="{ active: item.id === selectedHistoryId }"
                >
                  <button
                    class="history-review-select"
                    type="button"
                    :disabled="historyImageLoading || Boolean(deletingReviewId)"
                    @click="selectHistoryReview(item)"
                  >
                    <span class="history-review-score">{{ item.result?.overallScore ?? 0 }}</span>
                    <span class="history-review-copy">
                      <strong>{{ item.result?.summary || '成品图评价' }}</strong>
                      <small>{{ formatDate(item.createdAt) }}</small>
                    </span>
                    <ChevronRight :size="17" aria-hidden="true" />
                  </button>
                  <el-tooltip content="删除评价" placement="top">
                    <el-button
                      class="history-review-delete"
                      type="danger"
                      text
                      circle
                      :loading="deletingReviewId === item.id"
                      :disabled="historyImageLoading || Boolean(deletingReviewId)"
                      :aria-label="`删除 ${item.result?.summary || '成品图评价'}`"
                      @click.stop="confirmDeleteHistoryReview(item)"
                    >
                      <Trash2 v-if="deletingReviewId !== item.id" :size="16" aria-hidden="true" />
                    </el-button>
                  </el-tooltip>
                </article>
              </template>
            </section>
            <div v-else class="review-tab-empty">
              <History :size="34" :stroke-width="1.45" aria-hidden="true" />
              <strong>登录后保存评价历史</strong>
              <span>当前评价可以直接查看；使用用户账号评价后，成品图和建议会保存到你的菜谱记录。</span>
            </div>
          </el-tab-pane>
        </el-tabs>
      </section>
    </section>

    <template #footer>
      <div class="review-dialog-footer">
        <p v-if="auth.isUser">评价记录会保存到你的账号</p>
        <p v-else>当前为临时评价，登录后可保存历史</p>
        <div>
          <el-button @click="closeDialog">关闭</el-button>
          <el-button type="primary" :loading="reviewLoading" :disabled="!selectedImageFile" @click="submitReview">
            <Sparkles :size="16" aria-hidden="true" />
            <span>{{ reviewLoading ? '正在评价' : '开始评价' }}</span>
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>

  <CameraIngredientCapture
    v-model="cameraCaptureVisible"
    title="拍摄成品图"
    subject-label="成品图"
    @captured="handleCameraCaptured"
  />
</template>

<script setup>
import { computed, markRaw, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  AlertTriangle,
  Camera,
  CheckCircle2,
  ChevronRight,
  Flame,
  History,
  ImagePlus,
  LoaderCircle,
  Palette,
  PanelsTopLeft,
  Sparkles,
  Trash2,
  Upload
} from 'lucide-vue-next'
import {
  createFinishedDishReview,
  deleteFinishedDishReview,
  getFinishedDishReviewImage,
  getFinishedDishReviews
} from '../api/finishedDishReviews'
import { useAuthStore } from '../stores/auth'
import {
  buildFinishedDishReviewRequest,
  validateFinishedDishReviewImage
} from '../utils/finishedDishReview'
import CameraIngredientCapture from './CameraIngredientCapture.vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  recipe: { type: Object, default: null },
  recipeId: { type: Number, default: null }
})

const emit = defineEmits(['update:modelValue'])
const auth = useAuthStore()
const imageInput = ref(null)
const selectedImageFile = ref(null)
const previewUrl = ref('')
const currentReview = ref(null)
const history = ref([])
const activeTab = ref('summary')
const reviewLoading = ref(false)
const historyLoading = ref(false)
const historyImageLoading = ref(false)
const selectedHistoryId = ref(null)
const deletingReviewId = ref(null)
const cameraCaptureVisible = ref(false)

const recipeTitle = computed(() => props.recipe?.title?.trim() || '当前菜谱')
const recipeIngredients = computed(() => Array.isArray(props.recipe?.ingredients) ? props.recipe.ingredients : [])
const recipeSteps = computed(() => Array.isArray(props.recipe?.steps) ? props.recipe.steps : [])
const dimensions = computed(() => [
  { key: 'color', label: '色泽', value: currentReview.value?.color, icon: markRaw(Palette) },
  { key: 'doneness', label: '火候', value: currentReview.value?.doneness, icon: markRaw(Flame) },
  { key: 'plating', label: '摆盘', value: currentReview.value?.plating, icon: markRaw(PanelsTopLeft) }
])

function handleOpen() {
  activeTab.value = 'summary'
  if (auth.isUser) {
    loadHistory()
  }
}

function handleVisibility(visible) {
  emit('update:modelValue', visible)
}

function handleClosed() {
  cameraCaptureVisible.value = false
  clearPreview()
  selectedImageFile.value = null
  selectedHistoryId.value = null
  deletingReviewId.value = null
  currentReview.value = null
  activeTab.value = 'summary'
}

function closeDialog() {
  emit('update:modelValue', false)
}

function openImagePicker() {
  imageInput.value?.click()
}

function handleImageSelected(event) {
  const [file] = event.target?.files || []
  event.target.value = ''
  setImageFile(file)
}

function handleCameraCaptured(file) {
  setImageFile(file)
}

function setImageFile(file) {
  const validationMessage = validateFinishedDishReviewImage(file)
  if (validationMessage) {
    ElMessage.warning(validationMessage)
    return
  }

  clearPreview()
  selectedImageFile.value = file
  previewUrl.value = URL.createObjectURL(file)
  selectedHistoryId.value = null
  currentReview.value = null
  activeTab.value = 'summary'
}

function clearPreview() {
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
  }
  previewUrl.value = ''
}

async function submitReview() {
  if (reviewLoading.value) {
    return
  }

  const request = buildFinishedDishReviewRequest(props.recipe, props.recipeId)
  if (!request.recipeTitle) {
    ElMessage.warning('当前菜谱信息不完整，暂时无法评价')
    return
  }

  const validationMessage = validateFinishedDishReviewImage(selectedImageFile.value)
  if (validationMessage) {
    ElMessage.warning(validationMessage)
    return
  }

  reviewLoading.value = true
  try {
    const response = await createFinishedDishReview({
      image: selectedImageFile.value,
      request
    })
    const review = response.data.data
    currentReview.value = review?.result || null
    selectedHistoryId.value = null
    activeTab.value = 'summary'

    if (review?.saved) {
      ElMessage.success('评价已保存到历史记录')
      await loadHistory()
    } else {
      ElMessage.success('已完成本次成品图评价')
    }
  } catch (error) {
    ElMessage.error(getReviewErrorMessage(error))
  } finally {
    reviewLoading.value = false
  }
}

async function loadHistory() {
  if (!auth.isUser) {
    return
  }

  historyLoading.value = true
  try {
    const response = await getFinishedDishReviews({ recipeId: props.recipeId, limit: 10 })
    history.value = response.data.data || []
  } catch (error) {
    history.value = []
    ElMessage.error(getHistoryErrorMessage(error))
  } finally {
    historyLoading.value = false
  }
}

async function selectHistoryReview(item) {
  if (!item?.id || historyImageLoading.value) {
    return
  }

  historyImageLoading.value = true
  try {
    const response = await getFinishedDishReviewImage(item.id)
    clearPreview()
    previewUrl.value = URL.createObjectURL(response.data)
    selectedImageFile.value = null
    selectedHistoryId.value = item.id
    currentReview.value = item.result || null
    activeTab.value = 'summary'
  } catch (error) {
    ElMessage.error(getHistoryErrorMessage(error))
  } finally {
    historyImageLoading.value = false
  }
}

async function confirmDeleteHistoryReview(item) {
  if (!item?.id || deletingReviewId.value || historyImageLoading.value) {
    return
  }

  try {
    await ElMessageBox.confirm(
      '删除后将同时移除这条评价和对应的成品图，且无法恢复。',
      '确认删除评价',
      {
        confirmButtonText: '删除评价',
        cancelButtonText: '取消',
        confirmButtonClass: 'review-delete-confirm-button',
        type: 'warning'
      }
    )
  } catch {
    return
  }

  deletingReviewId.value = item.id
  try {
    await deleteFinishedDishReview(item.id)
    history.value = history.value.filter((historyItem) => historyItem.id !== item.id)

    if (selectedHistoryId.value === item.id) {
      clearPreview()
      selectedImageFile.value = null
      selectedHistoryId.value = null
      currentReview.value = null
      activeTab.value = 'history'
    }

    ElMessage.success('评价和成品图已删除')
  } catch (error) {
    ElMessage.error(getHistoryErrorMessage(error))
  } finally {
    deletingReviewId.value = null
  }
}

function issueSeverityLabel(severity) {
  return {
    high: '优先调整',
    medium: '建议优化',
    low: '可再提升'
  }[severity] || '观察项'
}

function formatDate(value) {
  if (!value) {
    return '刚刚完成'
  }

  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}

function getReviewErrorMessage(error) {
  const status = error?.response?.status
  if (status === 413) {
    return '成品图过大，请选择不超过 5MB 的图片'
  }
  if (status === 503) {
    return '视觉模型暂未配置，请稍后再试'
  }
  return error?.response?.data?.message || error?.message || '成品图评价失败，请稍后重试'
}

function getHistoryErrorMessage(error) {
  const status = error?.response?.status
  if (status === 401 || status === 403) {
    return '登录状态已失效，请重新登录后查看历史评价'
  }
  return error?.response?.data?.message || error?.message || '评价历史加载失败，请稍后重试'
}
</script>

<style scoped>
.finished-dish-review-dialog :deep(.el-dialog) {
  display: flex;
  flex-direction: column;
  max-height: calc(100vh - 32px);
  margin-top: 16px;
  margin-bottom: 16px;
  border: 1px solid var(--app-line-strong);
  border-radius: 8px;
  background: var(--app-surface);
  box-shadow: 0 18px 44px color-mix(in srgb, var(--app-text) 18%, transparent);
}

.finished-dish-review-dialog :deep(.el-dialog__header) {
  flex: 0 0 auto;
  margin-right: 0;
  padding: 18px 22px 14px;
  border-bottom: 1px solid var(--app-line);
}

.finished-dish-review-dialog :deep(.el-dialog__body) {
  flex: 1 1 auto;
  min-height: 0;
  padding: 18px 22px;
  overflow: hidden;
}

.finished-dish-review-dialog :deep(.el-dialog__footer) {
  flex: 0 0 auto;
  padding: 14px 22px 18px;
  border-top: 1px solid var(--app-line);
}

.review-dialog-heading {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  padding-right: 32px;
}

.review-heading-icon {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 1px solid var(--app-line-strong);
  border-radius: 6px;
  color: var(--app-accent);
  background: var(--app-surface-soft);
}

.review-dialog-heading p,
.review-dialog-heading h2,
.review-recipe-context p,
.review-recipe-context h3,
.score-summary p,
.dimension-item p,
.strength-section h4,
.strength-section ul,
.review-safety-note,
.issue-item h4,
.issue-item p,
.issue-item strong,
.review-image-note,
.review-dialog-footer p {
  margin: 0;
}

.review-dialog-heading p,
.review-eyebrow {
  color: var(--app-text-muted);
  font-size: 12px;
  line-height: 1.35;
}

.review-dialog-heading h2 {
  overflow: hidden;
  color: var(--app-text);
  font-size: 18px;
  font-weight: 700;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.review-workbench {
  display: grid;
  grid-template-columns: minmax(280px, 0.85fr) minmax(0, 1.4fr);
  height: min(560px, calc(100vh - 210px));
  min-height: 420px;
  gap: 18px;
}

.review-image-panel,
.review-output-panel {
  min-width: 0;
  min-height: 0;
}

.review-image-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-right: 18px;
  border-right: 1px solid var(--app-line);
}

.review-image-head,
.review-recipe-context,
.review-dialog-footer,
.review-image-actions,
.history-review-item,
.history-review-select {
  display: flex;
  align-items: center;
}

.review-image-head {
  justify-content: space-between;
  color: var(--app-text);
  font-size: 14px;
  font-weight: 700;
}

.review-image-head small,
.review-recipe-context > span {
  color: var(--app-text-muted);
  font-size: 12px;
  font-weight: 500;
}

.review-image-stage {
  position: relative;
  display: grid;
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
  border: 1px dashed var(--app-line-strong);
  border-radius: 8px;
  background: var(--app-surface-soft);
}

.review-image-stage--ready {
  border-style: solid;
}

.review-image-stage img {
  width: 100%;
  height: 100%;
  min-height: 230px;
  object-fit: cover;
}

.review-image-empty,
.review-tab-empty,
.history-state {
  display: grid;
  align-content: center;
  justify-items: center;
  gap: 8px;
  padding: 24px;
  color: var(--app-text-muted);
  text-align: center;
}

.review-image-empty strong,
.review-tab-empty strong {
  color: var(--app-text);
  font-size: 14px;
}

.review-image-empty span,
.review-tab-empty span {
  max-width: 270px;
  font-size: 12px;
  line-height: 1.65;
}

.review-file-input {
  display: none;
}

.review-image-actions {
  gap: 8px;
}

.review-image-actions :deep(.el-button) {
  flex: 1 1 0;
  min-width: 0;
}

.review-image-actions :deep(.el-button span),
.review-dialog-footer :deep(.el-button span) {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.review-image-note {
  color: var(--app-text-muted);
  font-size: 12px;
  line-height: 1.35;
}

.review-output-panel {
  display: flex;
  flex-direction: column;
}

.review-recipe-context {
  flex: 0 0 auto;
  justify-content: space-between;
  gap: 16px;
  min-height: 42px;
  margin-bottom: 8px;
}

.review-recipe-context h3 {
  overflow: hidden;
  max-width: 360px;
  color: var(--app-text);
  font-size: 16px;
  font-weight: 700;
  line-height: 1.45;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.review-tabs {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
}

.review-tabs :deep(.el-tabs__header) {
  flex: 0 0 auto;
  margin: 0;
}

.review-tabs :deep(.el-tabs__item) {
  height: 38px;
  color: var(--app-text-muted);
  font-size: 13px;
  font-weight: 650;
}

.review-tabs :deep(.el-tabs__item.is-active) {
  color: var(--app-text);
}

.review-tabs :deep(.el-tabs__active-bar) {
  background: var(--app-accent);
}

.review-tabs :deep(.el-tabs__nav-wrap::after) {
  background: var(--app-line);
}

.review-tabs :deep(.el-tabs__content) {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
}

.review-tabs :deep(.el-tab-pane) {
  min-height: 100%;
}

.review-tab-content,
.issue-list,
.history-panel {
  display: grid;
  gap: 12px;
  padding: 16px 2px 2px;
}

.score-summary {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 14px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--app-line);
}

.score-value {
  display: grid;
  justify-items: center;
  width: 74px;
  min-height: 74px;
  align-content: center;
  border: 1px solid var(--app-line-strong);
  border-radius: 8px;
  background: var(--app-surface-soft);
}

.score-value strong {
  color: var(--app-accent);
  font-size: 28px;
  line-height: 1;
}

.score-value span {
  margin-top: 5px;
  color: var(--app-text-muted);
  font-size: 11px;
}

.score-summary p {
  color: var(--app-text);
  font-size: 14px;
  line-height: 1.7;
}

.dimension-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.dimension-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: start;
  gap: 8px;
  min-width: 0;
  padding: 10px;
  border: 1px solid var(--app-line);
  border-radius: 6px;
}

.dimension-item > svg {
  color: var(--app-accent);
}

.dimension-item div {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 2px 6px;
}

.dimension-item span {
  color: var(--app-text-muted);
  font-size: 12px;
}

.dimension-item strong {
  color: var(--app-text);
  font-size: 15px;
}

.dimension-item p {
  grid-column: 1 / -1;
  display: -webkit-box;
  overflow: hidden;
  color: var(--app-text-muted);
  font-size: 12px;
  line-height: 1.5;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

.strength-section {
  display: grid;
  gap: 8px;
}

.strength-section h4 {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--app-text);
  font-size: 13px;
}

.strength-section h4 svg {
  color: var(--app-accent);
}

.strength-section ul {
  display: grid;
  gap: 5px;
  padding-left: 18px;
  color: var(--app-text-muted);
  font-size: 13px;
  line-height: 1.5;
}

.review-safety-note {
  padding: 8px 10px;
  border-left: 2px solid var(--app-accent);
  color: var(--app-text-muted);
  background: var(--app-surface-soft);
  font-size: 12px;
  line-height: 1.55;
}

.review-tab-empty {
  min-height: 280px;
}

.review-tab-empty--compact {
  min-height: 220px;
}

.issue-list {
  align-content: start;
}

.issue-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: start;
  gap: 10px;
  padding: 12px 0;
  border-bottom: 1px solid var(--app-line);
}

.issue-item:first-child {
  padding-top: 2px;
}

.issue-level {
  min-width: 62px;
  padding: 4px 6px;
  border: 1px solid var(--app-line-strong);
  border-radius: 4px;
  color: var(--app-text-muted);
  background: var(--app-surface-soft);
  font-size: 11px;
  line-height: 1.25;
  text-align: center;
}

.issue-level--high {
  border-color: color-mix(in srgb, #d75b5b 55%, var(--app-line-strong));
  color: #b63c3c;
}

.issue-level--medium {
  border-color: color-mix(in srgb, #d8a03d 60%, var(--app-line-strong));
  color: #9a6811;
}

.issue-item div {
  min-width: 0;
}

.issue-item h4 {
  color: var(--app-text);
  font-size: 14px;
  line-height: 1.45;
}

.issue-item p,
.issue-item strong {
  display: block;
  margin-top: 5px;
  color: var(--app-text-muted);
  font-size: 12px;
  font-weight: 500;
  line-height: 1.6;
}

.issue-item strong {
  color: var(--app-text);
}

.history-panel {
  align-content: start;
}

.history-state {
  min-height: 220px;
  font-size: 13px;
}

.history-spinner {
  animation: review-spin 900ms linear infinite;
}

.history-review-item {
  min-width: 0;
  gap: 4px;
  padding: 2px 0;
  border-bottom: 1px solid var(--app-line);
}

.history-review-select {
  flex: 1 1 auto;
  min-width: 0;
  width: 100%;
  gap: 10px;
  padding: 10px 4px;
  border: 0;
  color: var(--app-text);
  background: transparent;
  cursor: pointer;
  text-align: left;
}

.history-review-item.active,
.history-review-select:hover {
  background: var(--app-surface-soft);
}

.history-review-select:focus-visible,
.history-review-delete:focus-visible,
.review-image-actions :deep(.el-button:focus-visible),
.review-dialog-footer :deep(.el-button:focus-visible) {
  outline: 2px solid var(--app-accent);
  outline-offset: 2px;
}

.history-review-score {
  display: inline-grid;
  flex: 0 0 auto;
  width: 34px;
  height: 34px;
  place-items: center;
  border: 1px solid var(--app-line-strong);
  border-radius: 6px;
  color: var(--app-accent);
  background: var(--app-surface);
  font-size: 14px;
  font-weight: 700;
}

.history-review-copy {
  display: grid;
  flex: 1 1 auto;
  min-width: 0;
  gap: 3px;
}

.history-review-copy strong,
.history-review-copy small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-review-copy strong {
  font-size: 13px;
  font-weight: 600;
}

.history-review-copy small {
  color: var(--app-text-muted);
  font-size: 11px;
}

.history-review-select > svg {
  flex: 0 0 auto;
  color: var(--app-text-muted);
}

.history-review-delete {
  flex: 0 0 auto;
}

:global(.review-delete-confirm-button) {
  border-color: #b63c3c !important;
  background: #b63c3c !important;
}

:global(.review-delete-confirm-button:hover),
:global(.review-delete-confirm-button:focus-visible) {
  border-color: #963131 !important;
  background: #963131 !important;
}

.review-dialog-footer {
  justify-content: space-between;
  gap: 14px;
}

.review-dialog-footer p {
  color: var(--app-text-muted);
  font-size: 12px;
}

.review-dialog-footer > div {
  display: flex;
  flex: 0 0 auto;
  gap: 8px;
}

@keyframes review-spin {
  to {
    transform: rotate(360deg);
  }
}

@media (prefers-reduced-motion: reduce) {
  .history-spinner {
    animation: none;
  }
}

@media (max-width: 760px) {
  .finished-dish-review-dialog :deep(.el-dialog) {
    max-height: calc(100vh - 20px);
    margin-top: 10px;
    margin-bottom: 10px;
  }

  .finished-dish-review-dialog :deep(.el-dialog__header),
  .finished-dish-review-dialog :deep(.el-dialog__body),
  .finished-dish-review-dialog :deep(.el-dialog__footer) {
    padding-right: 16px;
    padding-left: 16px;
  }

  .finished-dish-review-dialog :deep(.el-dialog__body) {
    overflow-y: auto;
  }

  .review-workbench {
    display: grid;
    height: auto;
    min-height: 0;
    grid-template-columns: 1fr;
  }

  .review-image-panel {
    min-height: 350px;
    padding-right: 0;
    padding-bottom: 16px;
    border-right: 0;
    border-bottom: 1px solid var(--app-line);
  }

  .review-output-panel {
    min-height: 420px;
  }

  .dimension-grid {
    grid-template-columns: 1fr;
  }

  .review-dialog-footer {
    align-items: stretch;
    flex-direction: column;
  }

  .review-dialog-footer > div {
    width: 100%;
  }

  .review-dialog-footer :deep(.el-button) {
    flex: 1 1 0;
    margin-left: 0;
  }

  .history-review-delete {
    min-width: 44px;
    min-height: 44px;
  }
}
</style>
