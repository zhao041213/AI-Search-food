<template>
  <main class="nutrition-target-page">
    <header class="nutrition-target-heading">
      <div>
        <p class="eyebrow">个性化营养</p>
        <h1>每日营养目标</h1>
        <p>设置后，菜谱生成、营养对比和周菜单会将目标作为一般饮食参考。</p>
      </div>
      <RouterLink class="back-link" to="/">
        <ArrowLeft :size="16" aria-hidden="true" />
        <span>返回工作台</span>
      </RouterLink>
    </header>

    <section class="nutrition-target-panel" aria-labelledby="nutrition-target-title" v-loading="loading">
      <div class="panel-heading">
        <div>
          <span class="panel-kicker">每日营养目标</span>
          <h2 id="nutrition-target-title">目标设置</h2>
        </div>
        <span class="updated-at">{{ updatedAtLabel }}</span>
      </div>

      <el-alert
        class="nutrition-notice"
        title="可选设置，仅作为 AI 菜谱搭配和分量的软偏好；营养数据为估算，仅供一般饮食参考。"
        type="info"
        :closable="false"
        show-icon
      />

      <div class="target-switch-row">
        <div>
          <strong>启用每日营养目标</strong>
          <span>{{ targetStatus }}</span>
        </div>
        <el-switch
          v-model="targetForm.enabled"
          active-text="已启用"
          inactive-text="已停用"
          :disabled="saving"
        />
      </div>

      <el-alert
        v-if="targetForm.enabled && validationMessage"
        class="target-validation-alert"
        :title="validationMessage"
        type="warning"
        :closable="false"
        show-icon
      />

      <el-form v-if="targetForm.enabled" class="nutrition-target-form" label-position="top" @submit.prevent="saveTarget">
        <div class="nutrition-target-grid">
          <el-form-item label="每日热量（千卡）">
            <el-input-number
              v-model="targetForm.caloriesKcal"
              :min="1"
              :max="10000"
              :precision="2"
              :step="50"
              controls-position="right"
              placeholder="例如：2000"
            />
          </el-form-item>
          <el-form-item label="每日蛋白质（克）">
            <el-input-number
              v-model="targetForm.proteinG"
              :min="1"
              :max="1000"
              :precision="2"
              :step="5"
              controls-position="right"
              placeholder="例如：80"
            />
          </el-form-item>
          <el-form-item label="每日脂肪（克）">
            <el-input-number
              v-model="targetForm.fatG"
              :min="1"
              :max="1000"
              :precision="2"
              :step="5"
              controls-position="right"
              placeholder="例如：60"
            />
          </el-form-item>
          <el-form-item label="每日碳水（克）">
            <el-input-number
              v-model="targetForm.carbohydrateG"
              :min="1"
              :max="1000"
              :precision="2"
              :step="5"
              controls-position="right"
              placeholder="例如：260"
            />
          </el-form-item>
        </div>
      </el-form>

      <p v-else class="target-disabled-copy">停用后，菜谱生成和营养对比不会使用每日目标。</p>

      <div class="form-actions">
        <el-button v-if="target.configured" plain :disabled="saving" @click="confirmDelete">
          <Trash2 :size="16" aria-hidden="true" />
          <span>清空目标</span>
        </el-button>
        <el-button type="primary" :loading="saving" @click="saveTarget">
          <Save :size="16" aria-hidden="true" />
          <span>保存营养目标</span>
        </el-button>
      </div>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Save, Trash2 } from 'lucide-vue-next'
import { deleteNutritionTarget, getNutritionTarget, saveNutritionTarget } from '../api/nutritionTargets'
import {
  buildNutritionTargetPayload,
  emptyNutritionTarget,
  normalizeNutritionTarget,
  nutritionTargetValidationError
} from '../utils/nutritionTarget'

const target = ref(emptyNutritionTarget())
const targetForm = reactive(emptyNutritionTarget())
const loading = ref(false)
const saving = ref(false)

const targetStatus = computed(() => {
  if (targetForm.enabled) {
    return '已启用并参与菜谱参考'
  }
  return target.value.configured ? '已停用' : '尚未设置'
})
const updatedAtLabel = computed(() => {
  if (!target.value.updatedAt) {
    return '尚未保存'
  }
  return `更新于 ${new Date(target.value.updatedAt).toLocaleString('zh-CN', { hour12: false })}`
})
const validationMessage = computed(() => nutritionTargetValidationError(targetForm))

onMounted(loadTarget)

async function loadTarget() {
  loading.value = true
  try {
    const response = await getNutritionTarget()
    applyTarget(response.data.data)
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '每日营养目标加载失败'))
  } finally {
    loading.value = false
  }
}

async function saveTarget() {
  if (saving.value) {
    return
  }
  if (validationMessage.value) {
    ElMessage.error(validationMessage.value)
    return
  }

  saving.value = true
  try {
    const response = await saveNutritionTarget(buildNutritionTargetPayload(targetForm))
    applyTarget(response.data.data)
    ElMessage.success(targetForm.enabled ? '每日营养目标已保存' : '每日营养目标已停用')
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '每日营养目标保存失败'))
  } finally {
    saving.value = false
  }
}

async function confirmDelete() {
  try {
    await ElMessageBox.confirm('清空后，菜谱生成和营养对比将不再使用每日目标。', '清空每日营养目标', {
      confirmButtonText: '确认清空',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  saving.value = true
  try {
    await deleteNutritionTarget()
    applyTarget(emptyNutritionTarget())
    ElMessage.success('每日营养目标已清空')
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '每日营养目标清空失败'))
  } finally {
    saving.value = false
  }
}

function applyTarget(value) {
  const normalized = normalizeNutritionTarget(value)
  target.value = normalized
  Object.assign(targetForm, normalized)
}

function getErrorMessage(error, fallback) {
  return error?.response?.data?.message || fallback
}
</script>

<style scoped>
.nutrition-target-page {
  display: grid;
  gap: 18px;
  min-width: 0;
  padding: clamp(16px, 2vw, 28px);
}

.nutrition-target-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.nutrition-target-heading h1,
.panel-heading h2 {
  margin: 0;
  color: var(--app-text);
}

.nutrition-target-heading h1 {
  font-size: 24px;
}

.nutrition-target-heading p:not(.eyebrow) {
  max-width: 620px;
  margin: 6px 0 0;
  color: var(--app-text-muted);
  line-height: 1.65;
}

.eyebrow,
.panel-kicker {
  margin: 0 0 5px;
  color: var(--app-accent);
  font-size: 12px;
  font-weight: 900;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 38px;
  padding: 0 10px;
  border: 1px solid var(--app-line-strong);
  border-radius: 6px;
  color: var(--app-text-soft);
  background: var(--app-surface);
  font-size: 13px;
  font-weight: 800;
}

.back-link:hover,
.back-link:focus-visible {
  border-color: var(--app-accent);
  color: var(--app-text);
  background: var(--app-accent-soft);
  outline: none;
}

.nutrition-target-panel {
  min-width: 0;
  padding: clamp(16px, 2vw, 24px);
  border: 1px solid var(--app-line);
  border-radius: 8px;
  background: var(--app-surface);
  box-shadow: var(--app-panel-shadow);
}

.panel-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.panel-heading h2 {
  font-size: 18px;
}

.updated-at {
  color: var(--app-text-faint);
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.nutrition-notice {
  margin-bottom: 18px;
}

.target-switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 14px 16px;
  border: 1px solid var(--app-line);
  border-radius: 7px;
  background: var(--app-surface-soft);
}

.target-switch-row div {
  display: grid;
  gap: 4px;
}

.target-switch-row strong {
  color: var(--app-text);
  font-size: 14px;
}

.target-switch-row span,
.target-disabled-copy {
  color: var(--app-text-muted);
  font-size: 12px;
  line-height: 1.55;
}

.nutrition-target-form {
  margin-top: 18px;
}

.nutrition-target-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.nutrition-target-grid :deep(.el-input-number) {
  width: 100%;
}

.target-validation-alert {
  margin-top: 14px;
}

.target-disabled-copy {
  margin: 16px 0 0;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 4px;
}

@media (max-width: 760px) {
  .nutrition-target-heading {
    flex-direction: column;
  }

  .nutrition-target-grid {
    grid-template-columns: 1fr;
  }

  .form-actions {
    align-items: stretch;
    flex-direction: column-reverse;
  }

  .form-actions :deep(.el-button) {
    width: 100%;
    min-height: 42px;
  }

  .updated-at {
    white-space: normal;
  }

  .target-switch-row {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
