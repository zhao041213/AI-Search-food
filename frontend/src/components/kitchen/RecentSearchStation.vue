<template>
  <section class="history-station" aria-labelledby="history-station-title">
    <header class="history-intro">
      <div>
        <p>小谱 · 生成记录员</p>
        <h2 id="history-station-title">找回最近的菜谱灵感</h2>
        <span>选择一条生成记录，原来的食材、餐次和目标会交还给阿灶。</span>
      </div>
      <el-button :loading="loading" @click="loadHistory">
        <RefreshCw :size="16" aria-hidden="true" />
        刷新记录
      </el-button>
    </header>

    <div v-if="loading" class="history-state">
      <LoaderCircle class="spin" :size="30" aria-hidden="true" />
      <strong>小谱正在翻找记录</strong>
    </div>
    <div v-else-if="!items.length" class="history-state">
      <History :size="38" :stroke-width="1.5" aria-hidden="true" />
      <strong>还没有生成记录</strong>
      <span>先找阿灶生成一份菜谱，之后就能从这里快速继续。</span>
      <el-button type="primary" @click="emit('use-search', {})">去找阿灶</el-button>
    </div>
    <div v-else class="history-list" role="list" aria-label="最近菜谱生成记录">
      <article v-for="(item, index) in items" :key="item.id || `${item.ingredients}-${item.createdAt}`" class="history-card" role="listitem">
        <span class="history-index" aria-hidden="true">{{ String(index + 1).padStart(2, '0') }}</span>
        <div class="history-copy">
          <strong>{{ item.ingredients || '未记录食材' }}</strong>
          <span>{{ mealLabel(item.mealType) }} · {{ goalLabel(item.goal) }} · {{ modeLabel(item.searchMode) }}</span>
          <time :datetime="item.createdAt || undefined">{{ formatTime(item.createdAt) }}</time>
        </div>
        <button type="button" class="continue-button" :aria-label="`继续生成：${item.ingredients || '历史记录'}`" @click="continueSearch(item)">
          <ChefHat :size="17" aria-hidden="true" />
          交给阿灶
        </button>
      </article>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ChefHat, History, LoaderCircle, RefreshCw } from 'lucide-vue-next'
import { getRecentSearches } from '../../api/searchHistory'
import { toSearchForm } from '../../utils/personalization'

const emit = defineEmits(['use-search'])
const items = ref([])
const loading = ref(false)

const mealLabels = { any: '不限餐次', breakfast: '早餐', lunch: '午餐', dinner: '晚餐' }
const goalLabels = {
  balanced: '营养均衡', protein: '高蛋白', light: '低热量', quick: '快速烹饪',
  fat_loss: '减脂', muscle_gain: '增肌', low_sugar: '控糖'
}
const modeLabels = { text: '文字输入', image: '图片识别', camera: '拍照识别' }

onMounted(loadHistory)

async function loadHistory() {
  if (loading.value) return
  loading.value = true
  try {
    const response = await getRecentSearches()
    items.value = Array.isArray(response.data.data) ? response.data.data : []
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '生成记录加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

function continueSearch(item) {
  emit('use-search', toSearchForm(item))
}

function mealLabel(value) { return mealLabels[value] || mealLabels.any }
function goalLabel(value) { return goalLabels[value] || goalLabels.balanced }
function modeLabel(value) { return modeLabels[value] || modeLabels.text }
function formatTime(value) {
  if (!value) return '时间未知'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '时间未知'
  return new Intl.DateTimeFormat('zh-CN', {
    month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit'
  }).format(date)
}
</script>

<style scoped>
.history-station { min-height: 100%; padding: 18px; }
.history-intro {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 14px;
  padding: 14px 16px;
  border: 1px solid #b48a58;
  background: #f2e5c7;
  box-shadow: 3px 3px 0 rgba(97, 71, 52, 0.24);
}
.history-intro p,
.history-intro h2,
.history-intro span { margin: 0; }
.history-intro p { color: #9a7142; font-size: 11px; font-weight: 900; letter-spacing: 0.08em; }
.history-intro h2 { margin: 2px 0 3px; color: #3c2b20; font-size: 20px; }
.history-intro span { color: #80664a; font-size: 12px; }
.history-list { display: grid; gap: 10px; }
.history-card {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  min-height: 82px;
  padding: 11px 12px;
  border: 1px solid #c8aa7b;
  background: rgba(255, 250, 240, 0.94);
  box-shadow: 0 5px 14px rgba(69, 48, 34, 0.08);
}
.history-index {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border: 2px solid #614734;
  color: #d6a43b;
  background: #2b211d;
  font-family: Consolas, monospace;
  font-weight: 900;
}
.history-copy { display: grid; gap: 4px; min-width: 0; }
.history-copy strong { overflow: hidden; color: #3c2b20; font-size: 15px; text-overflow: ellipsis; white-space: nowrap; }
.history-copy span,
.history-copy time { color: #80664a; font-size: 11px; }
.continue-button {
  display: inline-flex;
  min-height: 44px;
  align-items: center;
  gap: 7px;
  padding: 0 13px;
  border: 2px solid #614734;
  color: #f7e6b5;
  background: #2b211d;
  font: inherit;
  font-size: 12px;
  font-weight: 900;
  cursor: pointer;
}
.continue-button:hover,
.continue-button:focus-visible { color: #2b211d; background: #d6a43b; outline: 2px solid #614734; outline-offset: 2px; }
.history-state {
  display: grid;
  min-height: 360px;
  place-items: center;
  align-content: center;
  gap: 10px;
  border: 1px dashed #b49467;
  color: #9a7142;
  background: rgba(255, 250, 240, 0.78);
  text-align: center;
}
.history-state strong { color: #4b3829; }
.history-state span { color: #80664a; font-size: 12px; }
.spin { animation: history-spin 900ms linear infinite; }
@keyframes history-spin { to { transform: rotate(360deg); } }
@media (prefers-reduced-motion: reduce) { .spin { animation: none; } }
@container scene-window-content (max-width: 640px) {
  .history-intro { align-items: flex-start; flex-direction: column; }
  .history-card { grid-template-columns: 42px minmax(0, 1fr); }
  .continue-button { grid-column: 1 / -1; justify-content: center; }
}
</style>
