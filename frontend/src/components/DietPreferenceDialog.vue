<template>
  <el-dialog
    class="diet-preference-dialog"
    :model-value="modelValue"
    title="饮食偏好"
    width="min(520px, calc(100vw - 32px))"
    :close-on-click-modal="!saving"
    :close-on-press-escape="!saving"
    :show-close="!saving"
    @open="syncForm"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-form label-position="top" @submit.prevent="submit">
      <el-form-item label="口味">
        <el-radio-group v-model="form.taste" class="taste-options">
          <el-radio-button v-for="option in tasteOptions" :key="option.value" :value="option.value">
            {{ option.label }}
          </el-radio-button>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="默认目标">
        <el-select v-model="form.defaultGoal" placeholder="请选择默认目标">
          <el-option v-for="option in goalOptions" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
      </el-form-item>

      <el-form-item label="忌口食材">
        <el-select
          v-model="form.avoidIngredients"
          multiple
          filterable
          allow-create
          default-first-option
          :reserve-keyword="false"
          placeholder="输入食材后按回车添加"
        />
      </el-form-item>

      <el-form-item label="过敏食材">
        <el-select
          v-model="form.allergenIngredients"
          multiple
          filterable
          allow-create
          default-first-option
          :reserve-keyword="false"
          placeholder="输入食材后按回车添加"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button :disabled="saving" @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">保存偏好</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive } from 'vue'
import { normalizeDietPreference } from '../utils/personalization'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  preference: { type: Object, required: true },
  saving: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'save'])

const tasteOptions = [
  { value: 'any', label: '不限' },
  { value: 'light', label: '清淡' },
  { value: 'home', label: '家常' },
  { value: 'spicy', label: '香辣' },
  { value: 'sweet_sour', label: '酸甜' }
]

const goalOptions = [
  { value: 'balanced', label: '营养均衡' },
  { value: 'fat_loss', label: '减脂' },
  { value: 'muscle_gain', label: '增肌' },
  { value: 'low_sugar', label: '控糖' }
]

const form = reactive(normalizeDietPreference())

function syncForm() {
  Object.assign(form, normalizeDietPreference(props.preference))
}

function submit() {
  if (props.saving) {
    return
  }
  emit('save', normalizeDietPreference(form))
}
</script>

<style scoped>
.diet-preference-dialog :deep(.el-form-item) {
  margin-bottom: 16px;
}

.diet-preference-dialog :deep(.el-select) {
  width: 100%;
}

.taste-options {
  display: flex;
  width: 100%;
  flex-wrap: wrap;
}

.taste-options :deep(.el-radio-button) {
  flex: 1 1 74px;
}

.taste-options :deep(.el-radio-button__inner) {
  width: 100%;
}

@media (max-width: 520px) {
  .taste-options :deep(.el-radio-button) {
    flex-basis: 33.333%;
  }
}
</style>
