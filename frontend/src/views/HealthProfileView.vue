<template>
  <main class="health-profile-page">
    <header class="health-profile-heading">
      <div>
        <p class="eyebrow">个性化营养</p>
        <h1>健康档案</h1>
        <p>基础身体指标将自动参与后续 AI 菜谱推荐。</p>
      </div>
      <RouterLink class="back-link" to="/">
        <ArrowLeft :size="16" aria-hidden="true" />
        <span>返回工作台</span>
      </RouterLink>
    </header>

    <section class="health-profile-overview" aria-label="健康档案概览">
      <div class="overview-heading">
        <span class="panel-kicker">推荐状态</span>
        <strong>{{ profile.completed ? '已参与 AI 推荐' : '等待填写' }}</strong>
      </div>
      <dl class="profile-metrics">
        <div>
          <dt>BMI</dt>
          <dd>{{ formatBmi }}</dd>
        </div>
        <div>
          <dt>年龄段</dt>
          <dd>{{ ageRangeLabel }}</dd>
        </div>
        <div>
          <dt>活动量</dt>
          <dd>{{ activityLevelLabel }}</dd>
        </div>
      </dl>
    </section>

    <section class="health-profile-form-panel" aria-labelledby="health-profile-form-title" v-loading="loading">
      <div class="panel-heading">
        <div>
          <span class="panel-kicker">基础指标</span>
          <h2 id="health-profile-form-title">身体信息</h2>
        </div>
        <span class="updated-at">{{ updatedAtLabel }}</span>
      </div>

      <el-alert
        class="health-notice"
        title="仅用于一般饮食推荐，不提供诊断或治疗建议。"
        type="info"
        :closable="false"
        show-icon
      />

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="saveProfile">
        <el-form-item label="年龄段" prop="ageRange">
          <el-radio-group v-model="form.ageRange" class="age-range-options">
            <el-radio-button v-for="option in AGE_RANGE_OPTIONS" :key="option.value" :value="option.value">
              {{ option.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>

        <div class="metric-grid">
          <el-form-item label="身高（厘米）" prop="heightCm">
            <el-input-number
              v-model="form.heightCm"
              :min="100"
              :max="250"
              :precision="1"
              :step="0.5"
              controls-position="right"
              placeholder="例如：172"
            />
          </el-form-item>
          <el-form-item label="体重（千克）" prop="weightKg">
            <el-input-number
              v-model="form.weightKg"
              :min="25"
              :max="300"
              :precision="1"
              :step="0.5"
              controls-position="right"
              placeholder="例如：64"
            />
          </el-form-item>
        </div>

        <el-form-item label="日常活动量" prop="activityLevel">
          <el-radio-group v-model="form.activityLevel" class="activity-options">
            <el-radio-button v-for="option in ACTIVITY_LEVEL_OPTIONS" :key="option.value" :value="option.value">
              {{ option.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <div class="form-actions">
        <el-button v-if="profile.completed" plain :disabled="saving" @click="confirmDelete">
          <Trash2 :size="16" aria-hidden="true" />
          <span>清空档案</span>
        </el-button>
        <el-button type="primary" :loading="saving" @click="saveProfile">
          <Save :size="16" aria-hidden="true" />
          <span>保存健康档案</span>
        </el-button>
      </div>
    </section>

    <section class="nutrition-target-panel" aria-labelledby="nutrition-target-title" v-loading="nutritionTargetLoading">
      <div class="panel-heading">
        <div>
          <span class="panel-kicker">每日营养目标</span>
          <h2 id="nutrition-target-title">营养目标</h2>
        </div>
        <span class="updated-at">{{ nutritionTargetUpdatedAtLabel }}</span>
      </div>

      <el-alert
        class="health-notice"
        title="可选设置，仅作为 AI 菜谱搭配和分量的软偏好；营养数据为估算，仅供一般饮食参考。"
        type="info"
        :closable="false"
        show-icon
      />

      <div class="target-switch-row">
        <div>
          <strong>启用每日营养目标</strong>
          <span>{{ nutritionTargetStatus }}</span>
        </div>
        <el-switch
          v-model="targetForm.enabled"
          active-text="已启用"
          inactive-text="已停用"
          :disabled="targetSaving"
        />
      </div>

      <el-alert
        v-if="targetForm.enabled && targetValidationMessage"
        class="target-validation-alert"
        :title="targetValidationMessage"
        type="warning"
        :closable="false"
        show-icon
      />

      <el-form v-if="targetForm.enabled" class="nutrition-target-form" label-position="top" @submit.prevent="saveTarget">
        <div class="nutrition-target-grid">
          <el-form-item label="每日热量（千卡）">
            <el-input-number v-model="targetForm.caloriesKcal" :min="1" :max="10000" :precision="2" :step="50" controls-position="right" placeholder="例如：2000" />
          </el-form-item>
          <el-form-item label="每日蛋白质（克）">
            <el-input-number v-model="targetForm.proteinG" :min="1" :max="1000" :precision="2" :step="5" controls-position="right" placeholder="例如：80" />
          </el-form-item>
          <el-form-item label="每日脂肪（克）">
            <el-input-number v-model="targetForm.fatG" :min="1" :max="1000" :precision="2" :step="5" controls-position="right" placeholder="例如：60" />
          </el-form-item>
          <el-form-item label="每日碳水（克）">
            <el-input-number v-model="targetForm.carbohydrateG" :min="1" :max="1000" :precision="2" :step="5" controls-position="right" placeholder="例如：260" />
          </el-form-item>
        </div>
      </el-form>

      <p v-else class="target-disabled-copy">停用后，菜谱生成和营养对比不会使用每日目标。</p>

      <div class="form-actions">
        <el-button v-if="nutritionTarget.configured" plain :disabled="targetSaving" @click="confirmDeleteTarget">
          <Trash2 :size="16" aria-hidden="true" />
          <span>清空目标</span>
        </el-button>
        <el-button type="primary" :loading="targetSaving" @click="saveTarget">
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
import { deleteHealthProfile, getHealthProfile, saveHealthProfile } from '../api/healthProfile'
import { deleteNutritionTarget, getNutritionTarget, saveNutritionTarget } from '../api/nutritionTargets'
import {
  ACTIVITY_LEVEL_OPTIONS,
  AGE_RANGE_OPTIONS,
  buildHealthProfilePayload,
  emptyHealthProfile,
  healthProfileLabel,
  normalizeHealthProfile
} from '../utils/healthProfile'
import {
  buildNutritionTargetPayload,
  emptyNutritionTarget,
  normalizeNutritionTarget,
  nutritionTargetValidationError
} from '../utils/nutritionTarget'

const formRef = ref(null)
const loading = ref(false)
const saving = ref(false)
const profile = ref(emptyHealthProfile())
const form = reactive(emptyHealthProfile())
const nutritionTarget = ref(emptyNutritionTarget())
const targetForm = reactive(emptyNutritionTarget())
const nutritionTargetLoading = ref(false)
const targetSaving = ref(false)

const rules = {
  ageRange: [{ required: true, message: '请选择年龄段', trigger: 'change' }],
  heightCm: [{ required: true, message: '请输入身高', trigger: 'change' }],
  weightKg: [{ required: true, message: '请输入体重', trigger: 'change' }],
  activityLevel: [{ required: true, message: '请选择活动量', trigger: 'change' }]
}

const ageRangeLabel = computed(() => profile.value.completed
  ? healthProfileLabel(AGE_RANGE_OPTIONS, profile.value.ageRange)
  : '--')
const activityLevelLabel = computed(() => profile.value.completed
  ? healthProfileLabel(ACTIVITY_LEVEL_OPTIONS, profile.value.activityLevel)
  : '--')
const formatBmi = computed(() => profile.value.bmi === null ? '--' : profile.value.bmi.toFixed(1))
const updatedAtLabel = computed(() => {
  if (!profile.value.updatedAt) {
    return '尚未保存'
  }
  return `更新于 ${new Date(profile.value.updatedAt).toLocaleString('zh-CN', { hour12: false })}`
})
const nutritionTargetStatus = computed(() => {
  if (nutritionTarget.value.enabled) {
    return '已启用并参与菜谱参考'
  }
  return nutritionTarget.value.configured ? '已停用' : '尚未设置'
})
const nutritionTargetUpdatedAtLabel = computed(() => {
  if (!nutritionTarget.value.updatedAt) {
    return '尚未保存'
  }
  return `更新于 ${new Date(nutritionTarget.value.updatedAt).toLocaleString('zh-CN', { hour12: false })}`
})
const targetValidationMessage = computed(() => nutritionTargetValidationError(targetForm))

onMounted(() => {
  loadProfile()
  loadNutritionTarget()
})

async function loadProfile() {
  loading.value = true
  try {
    const response = await getHealthProfile()
    applyProfile(response.data.data)
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '健康档案加载失败'))
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  if (saving.value) {
    return
  }
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  saving.value = true
  try {
    const response = await saveHealthProfile(buildHealthProfilePayload(form))
    applyProfile(response.data.data)
    ElMessage.success('健康档案已保存，将自动参与后续菜谱推荐')
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '健康档案保存失败'))
  } finally {
    saving.value = false
  }
}

async function loadNutritionTarget() {
  nutritionTargetLoading.value = true
  try {
    const response = await getNutritionTarget()
    applyNutritionTarget(response.data.data)
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '每日营养目标加载失败'))
  } finally {
    nutritionTargetLoading.value = false
  }
}

async function saveTarget() {
  if (targetSaving.value) {
    return
  }
  if (targetValidationMessage.value) {
    ElMessage.error(targetValidationMessage.value)
    return
  }

  targetSaving.value = true
  try {
    const response = await saveNutritionTarget(buildNutritionTargetPayload(targetForm))
    applyNutritionTarget(response.data.data)
    ElMessage.success(targetForm.enabled ? '每日营养目标已保存' : '每日营养目标已停用')
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '每日营养目标保存失败'))
  } finally {
    targetSaving.value = false
  }
}

async function confirmDeleteTarget() {
  try {
    await ElMessageBox.confirm('清空后，菜谱生成和营养对比将不再使用每日目标。', '清空每日营养目标', {
      confirmButtonText: '确认清空',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  targetSaving.value = true
  try {
    await deleteNutritionTarget()
    applyNutritionTarget(emptyNutritionTarget())
    ElMessage.success('每日营养目标已清空')
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '每日营养目标清空失败'))
  } finally {
    targetSaving.value = false
  }
}

async function confirmDelete() {
  try {
    await ElMessageBox.confirm('清空后，后续菜谱将不再使用身体指标。', '清空健康档案', {
      confirmButtonText: '确认清空',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  saving.value = true
  try {
    await deleteHealthProfile()
    applyProfile(emptyHealthProfile())
    ElMessage.success('健康档案已清空')
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '健康档案清空失败'))
  } finally {
    saving.value = false
  }
}

function applyProfile(value) {
  const normalized = normalizeHealthProfile(value)
  profile.value = normalized
  Object.assign(form, normalized)
}

function applyNutritionTarget(value) {
  const normalized = normalizeNutritionTarget(value)
  nutritionTarget.value = normalized
  Object.assign(targetForm, normalized)
}

function getErrorMessage(error, fallback) {
  return error?.response?.data?.message || fallback
}
</script>

<style scoped>
.health-profile-page {
  display: grid;
  gap: 18px;
  min-width: 0;
  padding: clamp(16px, 2vw, 28px);
}

.health-profile-heading,
.health-profile-overview,
.health-profile-form-panel,
.nutrition-target-panel {
  min-width: 0;
}

.health-profile-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.health-profile-heading h1,
.panel-heading h2 {
  margin: 0;
  color: var(--app-text);
}

.health-profile-heading h1 {
  font-size: 24px;
}

.health-profile-heading p:not(.eyebrow) {
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
  text-decoration: none;
}

.back-link:hover,
.back-link:focus-visible {
  border-color: var(--app-accent);
  color: var(--app-text);
  background: var(--app-accent-soft);
  outline: none;
}

.health-profile-overview,
.health-profile-form-panel {
  border: 1px solid var(--app-line);
  border-radius: 8px;
  background: var(--app-surface);
  box-shadow: var(--app-panel-shadow);
}

.health-profile-overview {
  display: grid;
  grid-template-columns: minmax(180px, 0.85fr) minmax(0, 2fr);
  gap: 20px;
  padding: clamp(16px, 2vw, 22px);
}

.overview-heading {
  display: grid;
  align-content: center;
  gap: 4px;
}

.overview-heading strong {
  color: var(--app-text);
  font-size: 18px;
}

.profile-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin: 0;
}

.profile-metrics div {
  min-width: 0;
  padding-left: 14px;
  border-left: 2px solid var(--app-accent);
}

.profile-metrics dt {
  color: var(--app-text-muted);
  font-size: 12px;
  font-weight: 800;
}

.profile-metrics dd {
  margin: 5px 0 0;
  overflow-wrap: anywhere;
  color: var(--app-text);
  font-size: 18px;
  font-weight: 900;
}

.health-profile-form-panel {
  padding: clamp(16px, 2vw, 24px);
}

.nutrition-target-panel {
  padding: clamp(16px, 2vw, 24px);
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

.health-notice {
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

.metric-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.metric-grid :deep(.el-input-number),
.health-profile-form-panel :deep(.el-radio-group) {
  width: 100%;
}

.age-range-options,
.activity-options {
  display: flex;
  flex-wrap: wrap;
}

.age-range-options :deep(.el-radio-button) {
  flex: 1 1 116px;
}

.activity-options :deep(.el-radio-button) {
  flex: 1 1 140px;
}

.age-range-options :deep(.el-radio-button__inner),
.activity-options :deep(.el-radio-button__inner) {
  width: 100%;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 4px;
}

@media (max-width: 760px) {
  .health-profile-heading,
  .health-profile-overview {
    grid-template-columns: 1fr;
  }

  .health-profile-heading {
    flex-direction: column;
  }

  .profile-metrics,
  .metric-grid,
  .nutrition-target-grid {
    grid-template-columns: 1fr;
  }

  .profile-metrics div {
    padding: 10px 0 0;
    border-top: 2px solid var(--app-accent);
    border-left: 0;
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
