<template>
  <el-dialog
    class="cooking-mode-dialog"
    :model-value="modelValue"
    title="烹饪步骤模式"
    width="min(960px, calc(100vw - 32px))"
    top="4vh"
    :close-on-click-modal="false"
    :close-on-press-escape="true"
    :append-to-body="true"
    @open="restoreSession"
    @close="handleDialogClose"
    @update:model-value="updateVisibility"
  >
    <template #header>
      <div class="dialog-heading">
        <span class="heading-icon" aria-hidden="true">
          <ChefHat :size="20" />
        </span>
        <div class="heading-copy">
          <p>烹饪步骤模式</p>
          <h2>{{ recipeTitle }}</h2>
        </div>
        <span class="heading-state">{{ steps.length ? `${steps.length} 个步骤` : '暂无步骤' }}</span>
      </div>
    </template>

    <div class="cooking-shell" @keydown="handleKeyboardNavigation">
      <div v-if="!steps.length" class="empty-cooking-state" role="status">
        <ChefHat :size="36" aria-hidden="true" />
        <strong>暂无可执行的烹饪步骤</strong>
        <span>请返回菜谱详情查看完整内容后再开始烹饪。</span>
      </div>

      <template v-else>
        <section class="timer-console" aria-label="菜谱总倒计时">
          <div class="timer-display">
            <Clock :size="20" aria-hidden="true" />
            <div>
              <span>菜谱总倒计时</span>
              <strong>{{ formatCookingDuration(session.remainingSeconds) }}</strong>
            </div>
          </div>
          <div class="timer-summary">
            <span>{{ totalMinutes ? `总计约 ${totalMinutes} 分钟` : '未标注总时长' }}</span>
            <span v-if="session.remainingSeconds === 0 && totalSeconds" class="timer-finished">倒计时结束</span>
          </div>
          <div class="timer-actions">
            <el-tooltip :content="session.timerRunning ? '暂停总计时' : '开始总计时'" placement="top">
              <el-button
                circle
                type="primary"
                :disabled="!hasTimer || session.finished || session.remainingSeconds === 0"
                :aria-label="session.timerRunning ? '暂停总计时' : '开始总计时'"
                @click="toggleTimer"
              >
                <Pause v-if="session.timerRunning" :size="16" aria-hidden="true" />
                <Play v-else :size="16" aria-hidden="true" />
              </el-button>
            </el-tooltip>
            <el-tooltip content="重置总计时" placement="top">
              <el-button
                circle
                :disabled="!hasTimer"
                aria-label="重置总计时"
                @click="resetTimer"
              >
                <RotateCcw :size="16" aria-hidden="true" />
              </el-button>
            </el-tooltip>
          </div>
        </section>

        <div class="progress-block">
          <el-progress :percentage="progress" :show-text="false" :stroke-width="7" />
          <div class="progress-copy">
            <span>{{ session.finished ? '本次烹饪已完成' : `正在进行第 ${session.currentStepIndex + 1} 步` }}</span>
            <strong>{{ session.currentStepIndex + 1 }} / {{ steps.length }}</strong>
          </div>
        </div>

        <div class="cooking-layout">
          <nav class="step-rail" aria-label="烹饪步骤导航">
            <button
              v-for="(step, index) in steps"
              :key="`${step.order}-${step.title}-${index}`"
              class="step-selector"
              :class="{ active: index === session.currentStepIndex, completed: session.finished || index < session.currentStepIndex }"
              type="button"
              :aria-current="index === session.currentStepIndex ? 'step' : undefined"
              :aria-label="`查看第 ${index + 1} 步：${step.title}`"
              @click="goToStep(index)"
            >
              <span class="step-number" aria-hidden="true">
                <Check v-if="session.finished || index < session.currentStepIndex" :size="15" />
                <template v-else>{{ index + 1 }}</template>
              </span>
              <span class="step-selector-copy">
                <strong>{{ step.title }}</strong>
                <small>{{ step.durationMinutes ? formatCookingMinutes(step.durationMinutes) : '未标注时长' }}</small>
              </span>
            </button>
          </nav>

          <section class="current-step" aria-live="polite">
            <p class="current-step-label">当前步骤</p>
            <div class="current-step-heading">
              <span>{{ String(session.currentStepIndex + 1).padStart(2, '0') }}</span>
              <h3>{{ currentStep?.title }}</h3>
            </div>
            <p class="current-step-description">
              {{ currentStep?.description || '按当前步骤完成处理后，再进入下一步。' }}
            </p>
            <div class="current-step-meta">
              <Clock :size="16" aria-hidden="true" />
              <span>{{ formatCookingMinutes(currentStep?.durationMinutes) }}</span>
            </div>
          </section>
        </div>
      </template>
    </div>

    <template #footer>
      <div class="cooking-footer">
        <div class="step-actions" v-if="steps.length">
          <el-tooltip content="上一步" placement="top">
            <el-button
              circle
              :disabled="session.currentStepIndex === 0"
              aria-label="上一步"
              @click="previousStep"
            >
              <ChevronLeft :size="18" aria-hidden="true" />
            </el-button>
          </el-tooltip>
          <el-tooltip :content="isLastStep ? '完成烹饪' : '下一步'" placement="top">
            <el-button
              circle
              type="primary"
              :disabled="session.finished"
              :aria-label="isLastStep ? '完成烹饪' : '下一步'"
              @click="nextStep"
            >
              <Check v-if="isLastStep" :size="17" aria-hidden="true" />
              <ChevronRight v-else :size="18" aria-hidden="true" />
            </el-button>
          </el-tooltip>
        </div>
        <div class="footer-actions">
          <el-button v-if="steps.length && !session.finished" type="primary" @click="finishCooking">
            <Check :size="16" aria-hidden="true" />
            完成烹饪
          </el-button>
          <el-button @click="closeDialog">关闭</el-button>
        </div>
      </div>
    </template>
  </el-dialog>

  <el-dialog v-model="finishConfirmVisible" title="完成烹饪前确认库存" width="min(620px, calc(100vw - 32px))" :close-on-click-modal="false">
    <section v-if="recipeId && inventoryLoading" class="finish-inventory-summary" aria-live="polite">
      <p>正在核对可用库存，请稍候。</p>
      <el-skeleton :rows="4" animated />
    </section>
    <section v-else-if="recipeId && inventoryPreview" class="finish-inventory-summary" aria-live="polite">
      <div class="servings-control">
        <span>本次份数</span>
        <el-input-number v-model="actualServings" :min="1" :max="20" :precision="0" controls-position="right" size="small" aria-label="本次烹饪份数" :disabled="inventoryLoading" @change="reloadPreviewForServings" />
        <small>默认按菜谱份数，可按实际烹饪量调整</small>
      </div>
      <p>本次按 {{ inventoryPreview.actualServings }} 份处理。库存不足或用量待确认的食材不会被默认扣减。</p>
      <div class="finish-inventory-list">
        <div v-for="item in consumptionItems" :key="item.ingredientName" class="finish-inventory-item">
          <el-checkbox v-model="item.selected" :disabled="!item.expectedQuantity || !item.expectedUnit" :aria-label="`选择扣减${item.ingredientName}`">
            <strong>{{ item.ingredientName }}</strong>
          </el-checkbox>
          <span v-if="item.selected && item.expectedQuantity" class="consumption-quantity">
            <el-input-number v-model="item.quantity" :min="0.01" :precision="2" :controls="false" size="small" :aria-label="`${item.ingredientName}扣减数量`" />
            <em>{{ item.expectedUnit }}</em>
          </span>
          <span v-else>{{ item.expectedQuantity || item.rawAmount || '用量待确认' }}{{ item.expectedUnit || '' }}</span>
          <span :class="['inventory-badge', `inventory-${String(item.status || '').toLowerCase()}`]">{{ inventoryLabel(item.status) }}</span>
        </div>
      </div>
    </section>
    <p v-else-if="!recipeId" class="finish-inventory-empty">当前菜谱未保存到个人菜谱，仅标记本地烹饪完成，不会修改服务器库存。</p>
    <p v-else class="finish-inventory-empty">库存预览暂时不可用，请稍后重试；也可以选择“仅标记完成”。</p>
    <template #footer>
      <el-button :disabled="consuming" @click="finishAsCompleted">返回烹饪</el-button>
      <el-button :disabled="consuming" @click="finishWithoutConsumption">仅标记完成</el-button>
      <el-button type="primary" :loading="consuming" @click="finishAndConsume">完成并扣减库存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { Check, ChefHat, ChevronLeft, ChevronRight, Clock, Pause, Play, RotateCcw } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import { getCookingPreview, submitCookingConsumption } from '../api/pantry'
import { markRecommendationCooked } from '../api/recipes'
import { useAuthStore } from '../stores/auth'
import {
  createCookingSession,
  formatCookingDuration,
  formatCookingMinutes,
  getCookingProgress,
  getRecipeTotalMinutes,
  normalizeCookingSteps,
  persistCookingSession,
  restoreCookingSession
} from '../utils/cookingSession'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  recipe: { type: Object, default: () => ({}) },
  storageKey: { type: String, default: '' },
  recipeId: { type: [Number, String], default: null },
  searchLogId: { type: [Number, String], default: null }
})

const emit = defineEmits(['update:modelValue', 'finished'])

const steps = computed(() => normalizeCookingSteps(props.recipe?.steps))
const totalMinutes = computed(() => getRecipeTotalMinutes(props.recipe))
const totalSeconds = computed(() => totalMinutes.value * 60)
const session = reactive(createCookingSession())
const sessionInitialized = ref(false)
const timerId = ref(null)
const inventoryPreview = ref(null)
const consumptionItems = ref([])
const inventoryLoading = ref(false)
const finishConfirmVisible = ref(false)
const consuming = ref(false)
const actualServings = ref(1)
const auth = useAuthStore()

const recipeTitle = computed(() => String(props.recipe?.title || 'AI 智能菜谱').trim() || 'AI 智能菜谱')
const currentStep = computed(() => steps.value[session.currentStepIndex] || null)
const progress = computed(() => getCookingProgress(session.currentStepIndex, steps.value.length, session.finished))
const hasTimer = computed(() => totalSeconds.value > 0)
const isLastStep = computed(() => session.currentStepIndex === Math.max(steps.value.length - 1, 0))

watch(
  () => props.modelValue,
  (visible) => {
    if (!visible) {
      handleDialogClose()
    }
  }
)

watch(
  () => [props.storageKey, props.recipe],
  () => {
    if (props.modelValue) {
      restoreSession()
    }
  }
)

onBeforeUnmount(() => {
  handleDialogClose()
})

function getSessionOptions() {
  return {
    stepCount: steps.value.length,
    totalSeconds: totalSeconds.value
  }
}

function restoreSession() {
  stopTimer()
  Object.assign(session, restoreCookingSession(props.storageKey, getSessionOptions()))
  sessionInitialized.value = true
  actualServings.value = validServings(props.recipe?.servings) ? Number(props.recipe.servings) : 1

  // Reloading a page must not restart a timer without an explicit user action.
  if (session.timerRunning) {
    session.timerRunning = false
    persistSession()
  }
  loadInventoryPreview()
}

async function loadInventoryPreview() {
  if (!props.recipeId) {
    inventoryPreview.value = null
    consumptionItems.value = []
    return
  }
  inventoryLoading.value = true
  try {
    const response = await getCookingPreview(props.recipeId, actualServings.value)
    inventoryPreview.value = response.data.data || null
    consumptionItems.value = (inventoryPreview.value?.items || []).map((item) => ({
      ...item,
      selected: item.status === 'ENOUGH',
      quantity: item.expectedQuantity
    }))
  } catch {
    inventoryPreview.value = null
    consumptionItems.value = []
  } finally {
    inventoryLoading.value = false
  }
}

function persistSession() {
  if (!sessionInitialized.value) {
    return false
  }
  return persistCookingSession(props.storageKey, session, getSessionOptions())
}

function updateVisibility(visible) {
  emit('update:modelValue', visible)
  if (!visible) {
    handleDialogClose()
  }
}

function closeDialog() {
  handleDialogClose()
  emit('update:modelValue', false)
}

function handleDialogClose() {
  stopTimer()
  persistSession()
}

function goToStep(index) {
  if (!Number.isInteger(index) || index < 0 || index >= steps.value.length) {
    return
  }

  session.currentStepIndex = index
  session.finished = false
  persistSession()
}

function previousStep() {
  goToStep(session.currentStepIndex - 1)
}

function nextStep() {
  if (isLastStep.value) {
    finishCooking()
    return
  }
  goToStep(session.currentStepIndex + 1)
}

function finishCooking() {
  if (!steps.value.length) {
    return
  }

  if (props.recipeId) {
    finishConfirmVisible.value = true
    loadInventoryPreview()
    return
  }
  void completeCooking('LOCAL_ONLY')
}

function validServings(value) {
  return Number.isInteger(Number(value)) && Number(value) >= 1 && Number(value) <= 20
}

function reloadPreviewForServings(value) {
  if (validServings(value)) actualServings.value = Number(value)
  if (props.recipeId) loadInventoryPreview()
}

function finishAsCompleted() {
  finishConfirmVisible.value = false
}

function finishWithoutConsumption() {
  finishConfirmVisible.value = false
  void completeCooking('MARK_ONLY')
}

async function finishAndConsume() {
  if (!props.recipeId || consuming.value) return
  consuming.value = true
  try {
    if (!inventoryPreview.value) {
      await loadInventoryPreview()
    }
    const preview = inventoryPreview.value
    if (!preview) throw new Error('库存预览失败，请刷新后重试')
    const items = consumptionItems.value.filter((item) => item.selected).map((item) => ({
      ingredientName: item.ingredientName,
      quantity: item.quantity,
      unit: item.expectedUnit,
      selected: true
    }))
    await submitCookingConsumption({
      recipeId: props.recipeId,
      actualServings: preview?.actualServings || props.recipe?.servings,
      idempotencyKey: createClientKey(),
      items
    })
    finishConfirmVisible.value = false
    await completeCooking('COOKING_CONSUME')
    ElMessage.success('烹饪完成，已扣减选中的库存')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '库存扣减失败，请刷新后重试')
  } finally {
    consuming.value = false
  }
}

async function completeCooking(mode) {
  session.currentStepIndex = steps.value.length - 1
  session.finished = true
  stopTimer()
  persistSession()
  await recordRecommendationCooked()
  emit('finished', { recipe: props.recipe, recipeId: props.recipeId, storageKey: props.storageKey, mode, currentStepIndex: session.currentStepIndex, remainingSeconds: session.remainingSeconds })
}

async function recordRecommendationCooked() {
  if (!auth.isUser || !props.searchLogId) {
    return
  }
  try {
    await markRecommendationCooked(props.searchLogId)
  } catch {
    ElMessage.warning('烹饪已完成，但“已做过”反馈保存失败，请稍后重试')
  }
}

function createClientKey() {
  return globalThis.crypto?.randomUUID?.() || `cooking-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

function inventoryLabel(status) {
  return ({ ENOUGH: '库存充足', PARTIAL: '库存不足', MISSING: '缺少库存', UNQUANTIFIED: '用量待确认', INVALID: '用量无法解析', UNIT_MISMATCH: '单位不匹配', EXPIRED_ONLY: '仅有过期库存' })[status] || '待检查'
}

function toggleTimer() {
  if (session.timerRunning) {
    stopTimer()
    persistSession()
    return
  }
  startTimer()
}

function startTimer() {
  if (!hasTimer.value || session.finished || session.remainingSeconds <= 0 || timerId.value !== null) {
    return
  }

  session.timerRunning = true
  timerId.value = window.setInterval(() => {
    if (session.remainingSeconds <= 1) {
      session.remainingSeconds = 0
      stopTimer()
      persistSession()
      return
    }

    session.remainingSeconds -= 1
    persistSession()
  }, 1000)
  persistSession()
}

function stopTimer() {
  if (timerId.value !== null) {
    window.clearInterval(timerId.value)
    timerId.value = null
  }
  session.timerRunning = false
}

function resetTimer() {
  if (!hasTimer.value) {
    return
  }

  stopTimer()
  session.remainingSeconds = totalSeconds.value
  persistSession()
}

function handleKeyboardNavigation(event) {
  if (event.altKey || event.ctrlKey || event.metaKey || !steps.value.length) {
    return
  }

  if (event.key === 'ArrowLeft' && session.currentStepIndex > 0) {
    event.preventDefault()
    previousStep()
  }

  if (event.key === 'ArrowRight' && !isLastStep.value && !session.finished) {
    event.preventDefault()
    nextStep()
  }
}
</script>

<style scoped>
:deep(.cooking-mode-dialog) {
  display: flex;
  flex-direction: column;
  max-height: calc(100vh - 32px);
  overflow: hidden;
  border: 1px solid var(--app-line);
  border-radius: 8px;
  color: var(--app-text);
  background: var(--app-surface);
  box-shadow: var(--app-panel-shadow);
}

:deep(.cooking-mode-dialog .el-dialog__header) {
  flex: 0 0 auto;
  margin: 0;
  padding: 18px 52px 16px 22px;
  border-bottom: 1px solid var(--app-line);
}

:deep(.cooking-mode-dialog .el-dialog__headerbtn) {
  top: 19px;
  right: 18px;
}

:deep(.cooking-mode-dialog .el-dialog__close) {
  color: var(--app-text-muted);
}

:deep(.cooking-mode-dialog .el-dialog__body) {
  flex: 1 1 auto;
  min-height: 0;
  padding: 0;
  overflow: hidden;
}

:deep(.cooking-mode-dialog .el-dialog__footer) {
  flex: 0 0 auto;
  padding: 14px 22px;
  border-top: 1px solid var(--app-line);
}

.dialog-heading,
.timer-console,
.timer-display,
.timer-actions,
.current-step-heading,
.current-step-meta,
.cooking-footer,
.step-actions,
.footer-actions {
  display: flex;
  align-items: center;
}

.dialog-heading {
  gap: 12px;
  min-width: 0;
}

.heading-icon {
  display: inline-grid;
  width: 38px;
  height: 38px;
  flex: 0 0 auto;
  place-items: center;
  border: 1px solid var(--app-accent);
  border-radius: 6px;
  color: var(--app-accent-text);
  background: var(--app-accent);
}

.heading-copy {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.heading-copy p,
.heading-copy h2 {
  margin: 0;
}

.heading-copy p {
  color: var(--app-text-muted);
  font-size: 12px;
  font-weight: 800;
}

.heading-copy h2 {
  overflow: hidden;
  color: var(--app-text);
  font-size: 18px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.heading-state {
  margin-left: auto;
  color: var(--app-text-muted);
  font-size: 13px;
  font-weight: 800;
  white-space: nowrap;
}

.cooking-shell {
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  min-height: 500px;
  max-height: min(620px, calc(100vh - 214px));
}

.timer-console {
  gap: 14px;
  min-height: 76px;
  padding: 12px 22px;
  border-bottom: 1px solid var(--app-line);
  background: var(--app-surface-strong);
}

.timer-display {
  gap: 10px;
  min-width: 184px;
  color: var(--app-accent);
}

.timer-display > div {
  display: grid;
  gap: 1px;
}

.timer-display span,
.timer-summary {
  color: var(--app-text-muted);
  font-size: 12px;
  font-weight: 800;
}

.timer-display strong {
  color: var(--app-text);
  font-size: 24px;
  font-variant-numeric: tabular-nums;
  font-weight: 900;
  letter-spacing: 0;
}

.timer-summary {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.timer-finished {
  color: var(--app-accent);
}

.timer-actions {
  gap: 8px;
  margin-left: auto;
}

.progress-block {
  padding: 14px 22px 11px;
  border-bottom: 1px solid var(--app-line);
}

.progress-block :deep(.el-progress-bar__outer) {
  background: var(--app-surface-soft);
}

.progress-copy {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 8px;
  color: var(--app-text-muted);
  font-size: 12px;
  font-weight: 800;
}

.progress-copy strong {
  color: var(--app-text);
  font-variant-numeric: tabular-nums;
}

.cooking-layout {
  display: grid;
  grid-template-columns: minmax(220px, 0.72fr) minmax(0, 1.28fr);
  min-height: 0;
}

.step-rail {
  display: grid;
  align-content: start;
  gap: 2px;
  min-height: 0;
  padding: 12px;
  overflow-y: auto;
  border-right: 1px solid var(--app-line);
  background: var(--app-surface-strong);
}

.step-selector {
  display: grid;
  grid-template-columns: 30px minmax(0, 1fr);
  align-items: center;
  gap: 10px;
  width: 100%;
  min-height: 54px;
  padding: 8px;
  border: 1px solid transparent;
  border-radius: 6px;
  color: var(--app-text-muted);
  background: transparent;
  font: inherit;
  text-align: left;
  cursor: pointer;
  transition: border-color 180ms ease, background-color 180ms ease, color 180ms ease;
}

.step-selector:hover,
.step-selector:focus-visible {
  border-color: var(--app-line-strong);
  color: var(--app-text);
  background: var(--app-surface);
  outline: none;
}

.step-selector.active {
  border-color: var(--app-accent);
  color: var(--app-text);
  background: var(--app-surface);
}

.step-selector.completed .step-number {
  border-color: var(--app-accent);
  color: var(--app-accent-text);
  background: var(--app-accent);
}

.step-number {
  display: inline-grid;
  width: 28px;
  height: 28px;
  place-items: center;
  border: 1px solid var(--app-line-strong);
  border-radius: 50%;
  color: currentColor;
  font-size: 12px;
  font-weight: 900;
  font-variant-numeric: tabular-nums;
}

.step-selector-copy {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.step-selector-copy strong,
.step-selector-copy small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.step-selector-copy strong {
  color: currentColor;
  font-size: 13px;
  font-weight: 800;
}

.step-selector-copy small {
  color: var(--app-text-faint);
  font-size: 11px;
  font-weight: 700;
}

.current-step {
  display: flex;
  min-width: 0;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  padding: 30px clamp(28px, 5vw, 56px);
  overflow-y: auto;
  background: var(--app-surface);
}

.current-step-label {
  margin: 0 0 14px;
  color: var(--app-text-muted);
  font-size: 12px;
  font-weight: 900;
}

.current-step-heading {
  gap: 14px;
  min-width: 0;
}

.current-step-heading > span {
  color: var(--app-accent);
  font-size: 34px;
  font-variant-numeric: tabular-nums;
  font-weight: 900;
}

.current-step-heading h3 {
  margin: 0;
  color: var(--app-text);
  font-size: 27px;
  font-weight: 900;
  letter-spacing: 0;
  overflow-wrap: anywhere;
}

.current-step-description {
  max-width: 620px;
  margin: 20px 0;
  color: var(--app-text-soft);
  font-size: 16px;
  font-weight: 600;
  line-height: 1.8;
  white-space: pre-wrap;
}

.current-step-meta {
  gap: 7px;
  color: var(--app-text-muted);
  font-size: 13px;
  font-weight: 800;
}

.empty-cooking-state {
  display: grid;
  min-height: 320px;
  align-content: center;
  justify-items: center;
  gap: 10px;
  padding: 30px;
  color: var(--app-text-muted);
  text-align: center;
}

.empty-cooking-state strong {
  color: var(--app-text);
  font-size: 17px;
}

.empty-cooking-state span {
  max-width: 320px;
  font-size: 13px;
  line-height: 1.6;
}

.cooking-footer {
  justify-content: space-between;
  gap: 14px;
}

.step-actions,
.footer-actions {
  gap: 8px;
}

.finish-inventory-summary { display: grid; gap: 12px; }
.finish-inventory-summary p, .finish-inventory-empty { margin: 0; color: var(--app-text-soft); line-height: 1.6; }
.servings-control { display: flex; align-items: center; gap: 10px; color: var(--app-text); font-weight: 700; }
.servings-control small { color: var(--app-text-muted); font-size: 12px; font-weight: 400; }
.finish-inventory-list { display: grid; gap: 6px; max-height: 280px; overflow: auto; }
.finish-inventory-item { display: grid; grid-template-columns: minmax(0, 1fr) auto auto; align-items: center; gap: 10px; padding: 9px 10px; border: 1px solid var(--app-line); border-radius: 6px; }
.finish-inventory-item strong { min-width: 0; overflow: hidden; color: var(--app-text); text-overflow: ellipsis; white-space: nowrap; }
.finish-inventory-item span { color: var(--app-text-muted); font-size: 12px; }
.consumption-quantity { display: inline-flex; align-items: center; gap: 5px; }
.consumption-quantity em { font-style: normal; white-space: nowrap; }
.consumption-quantity :deep(.el-input-number) { width: 94px; }
.inventory-badge { display: inline-flex; min-height: 24px; align-items: center; padding: 0 7px; border: 1px solid var(--app-line-strong); border-radius: 4px; font-size: 11px !important; font-weight: 800; }
.inventory-enough { border-color: var(--el-color-success); color: var(--el-color-success) !important; }
.inventory-partial, .inventory-expired_only { border-color: var(--el-color-warning); color: var(--el-color-warning) !important; }
@media (max-width: 560px) { .finish-inventory-item { grid-template-columns: 1fr auto; } .finish-inventory-item span:nth-child(2) { grid-column: 1; } .finish-inventory-item .inventory-badge { grid-column: 2; grid-row: 1 / span 2; } }

@media (max-width: 720px) {
  :deep(.cooking-mode-dialog) {
    max-height: calc(100vh - 20px);
    margin: 10px auto !important;
  }

  :deep(.cooking-mode-dialog .el-dialog__header) {
    padding: 15px 48px 14px 16px;
  }

  :deep(.cooking-mode-dialog .el-dialog__footer) {
    padding: 12px 16px;
  }

  .heading-state {
    display: none;
  }

  .cooking-shell {
    min-height: 480px;
    max-height: calc(100vh - 178px);
  }

  .timer-console {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto;
    gap: 8px 12px;
    min-height: 0;
    padding: 12px 16px;
  }

  .timer-summary {
    grid-column: 1 / -1;
  }

  .timer-actions {
    margin-left: 0;
  }

  .progress-block {
    padding: 12px 16px 10px;
  }

  .cooking-layout {
    grid-template-columns: 1fr;
    grid-template-rows: auto minmax(0, 1fr);
  }

  .step-rail {
    display: flex;
    gap: 6px;
    padding: 9px 12px;
    overflow-x: auto;
    overflow-y: hidden;
    border-right: 0;
    border-bottom: 1px solid var(--app-line);
  }

  .step-selector {
    display: inline-grid;
    width: 42px;
    min-width: 42px;
    min-height: 42px;
    padding: 6px;
    place-items: center;
  }

  .step-selector-copy {
    display: none;
  }

  .current-step {
    min-height: 0;
    padding: 24px;
  }

  .current-step-heading h3 {
    font-size: 23px;
  }

  .current-step-description {
    margin: 16px 0;
    font-size: 15px;
  }

  .cooking-footer {
    align-items: flex-end;
  }
}

@media (max-width: 430px) {
  .heading-copy h2 {
    max-width: 200px;
  }

  .timer-display strong {
    font-size: 21px;
  }

  .footer-actions :deep(.el-button) {
    min-width: auto;
  }
}

@media (prefers-reduced-motion: reduce) {
  .step-selector {
    transition: none;
  }
}
</style>
