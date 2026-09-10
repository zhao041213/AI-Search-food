<template>
  <main class="health-nutrition-page">
    <header class="health-nutrition-heading">
      <div>
        <p class="eyebrow">营养咨询室</p>
        <h1>健康与营养</h1>
        <p>把身体指标、饮食限制和每日营养目标放在一起管理，只用于一般饮食参考。</p>
      </div>
      <RouterLink class="back-link" :to="{ name: 'home' }">返回厨房</RouterLink>
    </header>

    <section class="health-nutrition-panel" aria-labelledby="profile-title">
      <div class="panel-heading">
        <div>
          <span class="panel-kicker">01 · 基础信息</span>
          <h2 id="profile-title">身体指标与饮食边界</h2>
        </div>
        <span v-if="profile.updatedAt" class="updated-at">最近更新 {{ formatDate(profile.updatedAt) }}</span>
      </div>

      <el-alert
        class="health-notice"
        title="这里不是医疗诊断工具"
        description="特殊健康情况、未成年人、老年人或孕期等人群仅提供一般饮食参考，请咨询医生或注册营养师。"
        type="warning"
        :closable="false"
      />

      <el-form class="health-form" label-position="top" @submit.prevent="save">
        <div class="health-grid">
          <el-form-item label="性别" required>
            <el-select v-model="form.gender" placeholder="请选择性别">
              <el-option label="男" value="MALE" />
              <el-option label="女" value="FEMALE" />
              <el-option label="不便透露" value="PREFER_NOT_TO_SAY" />
            </el-select>
          </el-form-item>
          <el-form-item label="年龄" required>
            <el-input-number v-model="form.age" :min="13" :max="120" controls-position="right" />
          </el-form-item>
          <el-form-item label="身高（厘米）" required>
            <el-input-number v-model="form.heightCm" :min="100" :max="250" :precision="1" controls-position="right" />
          </el-form-item>
          <el-form-item label="体重（千克）" required>
            <el-input-number v-model="form.weightKg" :min="25" :max="300" :precision="1" controls-position="right" />
          </el-form-item>
          <el-form-item label="活动强度" required>
            <el-select v-model="form.activityLevel" placeholder="请选择活动强度">
              <el-option label="低：久坐或少运动" value="LOW" />
              <el-option label="中等：每周规律运动" value="MODERATE" />
              <el-option label="高：高频或高强度运动" value="HIGH" />
            </el-select>
          </el-form-item>
          <el-form-item label="目标" required>
            <el-select v-model="form.goal" placeholder="请选择目标">
              <el-option label="保持体重" value="MAINTAIN" />
              <el-option label="减脂" value="FAT_LOSS" />
              <el-option label="增重" value="WEIGHT_GAIN" />
              <el-option label="增肌" value="MUSCLE_GAIN" />
            </el-select>
          </el-form-item>
        </div>

        <div class="health-grid health-grid-wide">
          <el-form-item label="饮食禁忌">
            <el-input v-model="form.dietaryRestrictionsText" maxlength="320" placeholder="用逗号分隔，例如：猪肉、酒精" />
          </el-form-item>
          <el-form-item label="过敏信息">
            <el-input v-model="form.allergiesText" maxlength="320" placeholder="用逗号分隔，例如：花生、虾" />
          </el-form-item>
          <el-form-item label="特殊健康情况（可选）" class="health-grid-full">
            <el-input v-model="form.specialHealthCondition" type="textarea" :rows="2" maxlength="160" show-word-limit placeholder="只填写希望在一般饮食建议中被注意的情况" />
          </el-form-item>
        </div>

        <p v-if="validationMessage" class="validation-copy" role="alert">{{ validationMessage }}</p>
        <div class="health-actions">
          <el-button type="primary" :loading="saving" @click="save">保存健康与营养信息</el-button>
        </div>
      </el-form>
    </section>

    <section class="health-nutrition-panel" aria-labelledby="target-title">
      <div class="panel-heading">
        <div>
          <span class="panel-kicker">02 · 每日参考</span>
          <h2 id="target-title">AI参考值与当前采用值</h2>
        </div>
        <el-tag :type="targetMode === 'DISABLED' ? 'info' : 'success'">{{ targetModeLabel }}</el-tag>
      </div>

      <p class="target-intro">AI参考值会结合你的输入生成；当前采用值可以独立自定义、恢复 AI 参考或关闭。</p>

      <div class="target-cards">
        <article class="target-card target-card-ai">
          <div class="target-card-heading"><strong>AI参考值</strong><span>{{ profile.aiReferenceUpdatedAt ? `更新于 ${formatDate(profile.aiReferenceUpdatedAt)}` : '尚未生成' }}</span></div>
          <div v-if="profile.aiReference" class="target-metrics">
            <span>热量 <b>{{ profile.aiReference.caloriesKcal }} 千卡</b></span>
            <span>蛋白质 <b>{{ profile.aiReference.proteinG }} 克</b></span>
            <span>脂肪 <b>{{ profile.aiReference.fatG }} 克</b></span>
            <span>碳水 <b>{{ profile.aiReference.carbohydrateG }} 克</b></span>
          </div>
          <p v-else class="target-empty">保存身体指标后，可生成一组一般饮食参考值。</p>
          <p v-if="profile.aiReferenceBasis" class="target-basis">依据：{{ profile.aiReferenceBasis }}</p>
          <el-button plain :loading="generatingReference" :disabled="!formReady" @click="generateReference">保存并生成 AI 参考值</el-button>
        </article>

        <article class="target-card">
          <div class="target-card-heading"><strong>当前采用值</strong><span>可随时调整</span></div>
          <div class="target-mode-actions" role="group" aria-label="选择营养目标状态">
            <el-button size="small" :type="form.targetMode === 'AI_REFERENCE' ? 'primary' : 'default'" @click="useAiReference">采用 AI 参考</el-button>
            <el-button size="small" :type="form.targetMode === 'CUSTOM' ? 'primary' : 'default'" @click="form.targetMode = 'CUSTOM'">自定义</el-button>
            <el-button size="small" :type="form.targetMode === 'DISABLED' ? 'primary' : 'default'" @click="form.targetMode = 'DISABLED'">关闭目标</el-button>
          </div>
          <div v-if="form.targetMode === 'CUSTOM'" class="custom-target-grid">
            <el-input-number v-model="form.customTarget.caloriesKcal" :min="800" :max="10000" :precision="0" controls-position="right" aria-label="自定义热量" />
            <el-input-number v-model="form.customTarget.proteinG" :min="1" :max="1000" :precision="0" controls-position="right" aria-label="自定义蛋白质" />
            <el-input-number v-model="form.customTarget.fatG" :min="1" :max="1000" :precision="0" controls-position="right" aria-label="自定义脂肪" />
            <el-input-number v-model="form.customTarget.carbohydrateG" :min="1" :max="1000" :precision="0" controls-position="right" aria-label="自定义碳水" />
          </div>
          <div v-if="currentTarget" class="target-metrics current-target-metrics">
            <span>热量 <b>{{ currentTarget.caloriesKcal }} 千卡</b></span>
            <span>蛋白质 <b>{{ currentTarget.proteinG }} 克</b></span>
            <span>脂肪 <b>{{ currentTarget.fatG }} 克</b></span>
            <span>碳水 <b>{{ currentTarget.carbohydrateG }} 克</b></span>
          </div>
          <p v-else class="target-empty">当前未采用营养目标，普通菜谱生成不会被阻止。</p>
          <el-button v-if="form.targetMode !== 'DISABLED'" type="primary" plain :loading="saving" @click="save">保存当前采用值</el-button>
        </article>
      </div>
      <p v-if="profile.generalDietaryReferenceOnly" class="target-disclaimer" role="note">根据当前信息，这里只提供一般饮食参考；如需个性化方案，请咨询专业人士。</p>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { RouterLink } from 'vue-router'
import { generateHealthNutritionReference, getHealthNutritionProfile, saveHealthNutritionProfile } from '../api/healthNutrition'

const emptyTarget = () => ({ caloriesKcal: null, proteinG: null, fatG: null, carbohydrateG: null })
const emptyForm = () => ({
  gender: '', age: null, heightCm: null, weightKg: null, activityLevel: '', goal: '',
  dietaryRestrictionsText: '', allergiesText: '', specialHealthCondition: '', targetMode: 'DISABLED', customTarget: emptyTarget()
})
const emptyProfile = () => ({ configured: false, aiReference: null, currentTarget: null, targetMode: 'DISABLED', generalDietaryReferenceOnly: false })
const form = reactive(emptyForm())
const profile = ref(emptyProfile())
const loading = ref(false)
const saving = ref(false)
const generatingReference = ref(false)
const validationMessage = computed(() => {
  if (!form.gender || !form.age || !form.heightCm || !form.weightKg || !form.activityLevel || !form.goal) return '请完整填写性别、年龄、身高、体重、活动强度和目标。'
  if (form.age < 13 || form.age > 120 || form.heightCm < 100 || form.heightCm > 250 || form.weightKg < 25 || form.weightKg > 300) return '身体指标超出合理范围，请检查后再保存。'
  if (form.targetMode === 'CUSTOM' && Object.values(form.customTarget).some((value) => !value || value <= 0)) return '自定义营养目标需要完整填写正数。'
  return ''
})
const formReady = computed(() => !validationMessage.value)
const currentTarget = computed(() => {
  if (form.targetMode === 'CUSTOM') return form.customTarget
  if (form.targetMode === 'AI_REFERENCE') return profile.value.aiReference
  return profile.value.currentTarget
})
const targetMode = computed(() => form.targetMode)
const targetModeLabel = computed(() => ({ DISABLED: '未启用', AI_REFERENCE: '采用 AI 参考', CUSTOM: '自定义采用值' })[targetMode.value] || '未启用')

onMounted(load)

async function load() {
  loading.value = true
  try {
    applyProfile((await getHealthNutritionProfile()).data.data)
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '健康与营养信息加载失败')
  } finally { loading.value = false }
}

async function save(options = {}) {
  const announce = options.announce !== false
  if (saving.value || validationMessage.value) {
    if (validationMessage.value) ElMessage.warning(validationMessage.value)
    return false
  }
  saving.value = true
  try {
    applyProfile((await saveHealthNutritionProfile(payload())).data.data)
    if (announce) ElMessage.success('健康与营养信息已保存')
    return true
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '健康与营养信息保存失败')
    return false
  } finally { saving.value = false }
}

async function generateReference() {
  if (!formReady.value || generatingReference.value) return
  generatingReference.value = true
  try {
    if (!await save({ announce: false })) return
    const response = await generateHealthNutritionReference()
    applyProfile(response.data.data)
    ElMessage.success('健康与营养信息已保存，AI参考值已更新')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || 'AI参考值生成失败')
  } finally { generatingReference.value = false }
}

function useAiReference() {
  if (!profile.value.aiReference) {
    ElMessage.info('请先生成 AI 参考值')
    return
  }
  form.targetMode = 'AI_REFERENCE'
}

function payload() {
  return {
    gender: form.gender, age: form.age, heightCm: form.heightCm, weightKg: form.weightKg,
    activityLevel: form.activityLevel, goal: form.goal,
    dietaryRestrictions: splitList(form.dietaryRestrictionsText), allergies: splitList(form.allergiesText),
    specialHealthCondition: form.specialHealthCondition, targetMode: form.targetMode,
    customTarget: form.targetMode === 'CUSTOM' ? form.customTarget : null
  }
}

function applyProfile(value) {
  const next = { ...emptyProfile(), ...(value || {}) }
  const normalizedGender = next.gender === 'OTHER' ? 'PREFER_NOT_TO_SAY' : next.gender || ''
  profile.value = next
  Object.assign(form, {
    gender: normalizedGender, age: next.age || null, heightCm: next.heightCm || null, weightKg: next.weightKg || null,
    activityLevel: next.activityLevel || '', goal: next.goal || '', dietaryRestrictionsText: (next.dietaryRestrictions || []).join('、'),
    allergiesText: (next.allergies || []).join('、'), specialHealthCondition: next.specialHealthCondition || '',
    targetMode: next.targetMode || 'DISABLED', customTarget: { ...emptyTarget(), ...(next.currentTarget || {}) }
  })
}

function splitList(value) { return String(value || '').split(/[，,、\n]/).map((item) => item.trim()).filter(Boolean).slice(0, 8) }
function formatDate(value) { return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '—' }
</script>

<style scoped>
.health-nutrition-page { display: grid; gap: 18px; min-width: 0; padding: clamp(16px, 2vw, 28px); }
.health-nutrition-heading, .panel-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 18px; }
.health-nutrition-heading h1, .panel-heading h2 { margin: 0; color: var(--app-text); }
.health-nutrition-heading h1 { font-size: 24px; }
.health-nutrition-heading p:not(.eyebrow) { max-width: 680px; margin: 6px 0 0; color: var(--app-text-muted); line-height: 1.65; }
.eyebrow, .panel-kicker { margin: 0 0 5px; color: var(--app-accent); font-size: 12px; font-weight: 900; }
.back-link { display: inline-flex; align-items: center; min-height: 40px; padding: 0 11px; border: 1px solid var(--app-line-strong); border-radius: 6px; color: var(--app-text-soft); text-decoration: none; font-size: 13px; font-weight: 800; }
.health-nutrition-panel { min-width: 0; padding: clamp(16px, 2vw, 24px); border: 1px solid var(--app-line); border-radius: 8px; background: var(--app-surface); box-shadow: var(--app-panel-shadow); }
.panel-heading { margin-bottom: 16px; }
.panel-heading h2 { font-size: 18px; }
.updated-at, .target-card-heading span { color: var(--app-text-faint); font-size: 12px; }
.health-notice { margin-bottom: 18px; }
.health-form { display: grid; gap: 8px; }
.health-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; }
.health-grid-wide { grid-template-columns: repeat(2, minmax(0, 1fr)); margin-top: 2px; }
.health-grid-full { grid-column: 1 / -1; }
.health-grid :deep(.el-input-number), .health-grid :deep(.el-select) { width: 100%; }
.validation-copy { margin: 4px 0 0; color: var(--el-color-danger); font-size: 13px; }
.health-actions { display: flex; justify-content: flex-end; padding-top: 4px; }
.target-intro { margin: -4px 0 16px; color: var(--app-text-muted); line-height: 1.6; }
.target-cards { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.target-card { display: grid; align-content: start; gap: 14px; min-width: 0; padding: 16px; border: 1px solid var(--app-line); border-radius: 7px; background: var(--app-surface-soft); }
.target-card-heading { display: flex; align-items: baseline; justify-content: space-between; gap: 12px; }
.target-card-heading strong { color: var(--app-text); }
.target-metrics { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; }
.target-metrics span { display: grid; gap: 3px; padding: 9px; border: 1px solid var(--app-line); border-radius: 6px; color: var(--app-text-muted); font-size: 12px; }
.target-metrics b { color: var(--app-text); font-size: 14px; }
.target-empty, .target-basis, .target-disclaimer { margin: 0; color: var(--app-text-muted); font-size: 12px; line-height: 1.6; }
.target-basis { padding-top: 2px; }
.target-mode-actions { display: flex; flex-wrap: wrap; gap: 7px; }
.custom-target-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; }
.custom-target-grid :deep(.el-input-number) { width: 100%; }
.target-disclaimer { margin-top: 14px; padding: 10px 12px; border: 1px solid var(--app-line); border-radius: 6px; background: var(--app-surface-strong); }
@media (max-width: 820px) { .health-grid, .health-grid-wide, .target-cards { grid-template-columns: 1fr; } .health-nutrition-heading { flex-direction: column; } .back-link { width: fit-content; } .health-actions { justify-content: stretch; } .health-actions :deep(.el-button) { width: 100%; min-height: 42px; } }
</style>
