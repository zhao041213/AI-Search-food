<template>
  <el-dialog
    v-model="visible"
    title="加入库存"
    width="min(460px, calc(100vw - 32px))"
    :close-on-click-modal="false"
    @closed="resetForm"
  >
    <el-form label-position="top" @submit.prevent="submit">
      <p class="stock-in-hint">{{ item?.name || '采购食材' }}：请确认本次实际入库数量。</p>
      <div class="quantity-row">
        <el-form-item label="数量" required>
          <el-input-number
            v-model="form.quantity"
            class="quantity-input"
            :min="0.01"
            :max="99999999"
            :precision="2"
            controls-position="right"
            placeholder="例如 500"
            @keydown.enter.prevent="submit"
          />
        </el-form-item>
        <el-form-item label="单位" required>
          <el-select v-model="form.unit" class="unit-select" filterable placeholder="选择单位">
            <el-option v-for="unit in units" :key="unit.value" :label="unit.label" :value="unit.value" />
          </el-select>
        </el-form-item>
      </div>
      <el-form-item label="分类（可选）">
        <el-input v-model="form.category" maxlength="64" placeholder="例如：蔬菜、肉类" />
      </el-form-item>
      <el-form-item label="保质期（可选）">
        <el-date-picker v-model="form.expireDate" class="date-input" type="date" value-format="YYYY-MM-DD" :disabled-date="disablePastDate" placeholder="选择到期日期" />
      </el-form-item>
      <p class="stock-in-disclosure">入库后会保留为独立批次，烹饪扣减时按最早到期批次使用。</p>
    </el-form>
    <template #footer>
      <el-button :disabled="loading" @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="loading" @click="submit">确认入库</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  item: { type: Object, default: null },
  loading: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue', 'confirm'])

const units = [
  { value: 'g', label: '克（g）' },
  { value: 'kg', label: '千克（kg）' },
  { value: 'ml', label: '毫升（ml）' },
  { value: 'l', label: '升（l）' },
  { value: '个', label: '个' },
  { value: '只', label: '只' },
  { value: '枚', label: '枚' },
  { value: '颗', label: '颗' },
  { value: '片', label: '片' },
  { value: '根', label: '根' },
  { value: '瓣', label: '瓣' },
  { value: '包', label: '包' },
  { value: '袋', label: '袋' },
  { value: '盒', label: '盒' },
  { value: '杯', label: '杯' },
  { value: '把', label: '把' },
  { value: '条', label: '条' },
  { value: '碗', label: '碗' }
]
const visible = ref(props.modelValue)
const form = reactive({ quantity: null, unit: '', category: '', expireDate: '' })

watch(() => props.modelValue, (value) => {
  visible.value = value
  if (value) resetForm()
})
watch(visible, (value) => emit('update:modelValue', value))

function resetForm() {
  const match = String(props.item?.amount || '').trim().match(/^(\d+(?:\.\d+)?)\s*(克|千克|公斤|g|kg|毫升|升|ml|l|个|只|枚|颗|片|根|瓣|包|袋|盒|杯|把|条|碗)$/i)
  const aliases = { 克: 'g', 千克: 'kg', 公斤: 'kg', g: 'g', kg: 'kg', 毫升: 'ml', 升: 'l', ml: 'ml', l: 'l', 个: '个', 只: '只', 枚: '枚', 颗: '颗', 片: '片', 根: '根', 瓣: '瓣', 包: '包', 袋: '袋', 盒: '盒', 杯: '杯', 把: '把', 条: '条', 碗: '碗' }
  form.quantity = match ? Number(match[1]) : null
  form.unit = match ? aliases[match[2].toLowerCase()] || aliases[match[2]] : ''
  form.category = ''
  form.expireDate = ''
}

function disablePastDate(date) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date < today
}

function submit() {
  if (!Number.isFinite(Number(form.quantity)) || Number(form.quantity) <= 0 || !form.unit) return
  emit('confirm', {
    quantity: Number(form.quantity),
    unit: form.unit,
    category: form.category.trim() || null,
    expireDate: form.expireDate || null
  })
}
</script>

<style scoped>
.stock-in-hint { margin: 0 0 16px; color: var(--app-text-soft); line-height: 1.6; }
.quantity-row { display: grid; grid-template-columns: minmax(0, 1fr) 130px; gap: 12px; }
.quantity-input, .unit-select, .date-input { width: 100%; }
.stock-in-disclosure { margin: 4px 0 0; color: var(--app-text-muted); font-size: 12px; line-height: 1.5; }
@media (max-width: 480px) { .quantity-row { grid-template-columns: 1fr; gap: 0; } }
</style>
