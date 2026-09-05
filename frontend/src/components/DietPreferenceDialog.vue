<template>
  <el-dialog
    class="diet-preference-dialog"
    :model-value="modelValue"
    title="饮食偏好"
    width="min(520px, calc(100vw - 32px))"
    :close-on-click-modal="!saving"
    :close-on-press-escape="!saving"
    :show-close="!saving"
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
  >
    <DietPreferenceForm
      :preference="preference"
      :saving="saving"
      @cancel="emit('update:modelValue', false)"
      @save="emit('save', $event)"
    />
  </el-dialog>
</template>

<script setup>
import DietPreferenceForm from './DietPreferenceForm.vue'

defineProps({
  modelValue: { type: Boolean, default: false },
  preference: { type: Object, required: true },
  saving: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'save'])
</script>

<style scoped>
.diet-preference-dialog :deep(.el-dialog__body) {
  padding: 0;
}
</style>
