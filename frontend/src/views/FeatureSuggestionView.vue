<template>
  <main class="suggestion-page" aria-labelledby="suggestion-title">
    <section class="suggestion-shell">
      <header class="suggestion-hero">
        <div>
          <p class="suggestion-kicker">厨房回声 · 一起把它变好</p>
          <h1 id="suggestion-title">功能建议</h1>
          <p>把你在使用中的想法、体验改进或问题告诉我们，我们会在这里同步处理进度。</p>
        </div>
        <div class="suggestion-rule" aria-hidden="true"></div>
      </header>

      <section class="suggestion-layout">
        <el-card class="suggestion-form-card" shadow="never">
          <div class="section-heading">
            <div>
              <span class="section-kicker">现在就说</span>
              <h2>提交一条建议</h2>
            </div>
            <span class="form-note">每小时最多 5 条</span>
          </div>
          <el-form label-position="top" @submit.prevent="submitSuggestion">
            <div class="form-grid">
              <el-form-item label="建议类型">
                <el-select v-model="form.type" aria-label="建议类型" class="full-width">
                  <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>
              <el-form-item label="一句话标题">
                <el-input v-model.trim="form.title" placeholder="例如：希望能按家庭人数调整份量" />
              </el-form-item>
            </div>
            <el-form-item label="详细描述">
              <el-input
                v-model="form.detail"
                type="textarea"
                :rows="6"
                placeholder="请描述使用场景、遇到的问题，以及你期待的结果"
              />
            </el-form-item>
            <el-form-item label="预期效果（可选）">
              <el-input v-model="form.expectedEffect" placeholder="这项改进会怎样帮助你？" />
            </el-form-item>
            <div class="upload-row">
              <label class="upload-label" for="suggestion-screenshot">附一张截图（可选）</label>
              <input id="suggestion-screenshot" ref="fileInput" type="file" accept="image/jpeg,image/png,image/webp" @change="selectScreenshot" />
              <span v-if="screenshot" class="file-name">{{ screenshot.name }}</span>
              <span v-else class="file-hint">JPG / PNG / WebP，5MB 以内</span>
            </div>
            <el-alert v-if="errorMessage" class="inline-alert" type="error" :title="errorMessage" show-icon :closable="false" />
            <div class="form-footer">
              <span>请不要上传包含敏感个人信息的截图。</span>
              <el-button type="primary" native-type="submit" :loading="submitting">提交建议</el-button>
            </div>
          </el-form>
        </el-card>

        <section class="suggestion-list-card" aria-labelledby="my-suggestions-title">
          <div class="section-heading">
            <div>
              <span class="section-kicker">回声记录</span>
              <h2 id="my-suggestions-title">我的建议</h2>
            </div>
            <el-button circle text aria-label="刷新建议列表" title="刷新建议列表" :loading="loading" @click="loadSuggestions">↻</el-button>
          </div>
          <div v-loading="loading" class="suggestion-list">
            <button v-for="item in suggestions" :key="item.id" class="suggestion-item" type="button" @click="openDetail(item)">
              <span class="suggestion-item-top">
                <span class="suggestion-type">{{ typeLabel(item.type) }}</span>
                <span class="status-pill" :class="`status-${item.status?.toLowerCase()}`">{{ statusLabel(item.status) }}</span>
              </span>
              <strong>{{ item.title }}</strong>
              <span class="suggestion-item-meta">{{ formatTime(item.createdAt) }}</span>
            </button>
            <el-empty v-if="!loading && !suggestions.length" description="还没有提交过建议" />
          </div>
          <el-pagination
            v-if="total"
            v-model:current-page="page"
            :page-size="PAGE_SIZE"
            :total="total"
            background
            layout="prev, pager, next"
            @current-change="loadSuggestions"
          />
        </section>
      </section>
    </section>

    <el-drawer v-model="detailVisible" title="建议详情" size="min(580px, 94vw)" destroy-on-close>
      <div v-loading="detailLoading" class="suggestion-detail" v-if="selected">
        <div class="detail-status-row">
          <span class="suggestion-type">{{ typeLabel(selected.type) }}</span>
          <span class="status-pill" :class="`status-${selected.status?.toLowerCase()}`">{{ statusLabel(selected.status) }}</span>
        </div>
        <h2>{{ selected.title }}</h2>
        <p class="detail-time">提交于 {{ formatTime(selected.createdAt) }}</p>
        <dl class="detail-fields">
          <div><dt>详细描述</dt><dd>{{ selected.detail }}</dd></div>
          <div v-if="selected.expectedEffect"><dt>预期效果</dt><dd>{{ selected.expectedEffect }}</dd></div>
        </dl>
        <img v-if="screenshotUrl" class="detail-screenshot" :src="screenshotUrl" alt="建议附带的截图" />
        <section v-if="selected.adminReply" class="reply-box">
          <span>管理员回复</span>
          <p>{{ selected.adminReply }}</p>
        </section>
        <section v-if="selected.additions?.length" class="addition-list">
          <h3>补充记录</h3>
          <p v-for="addition in selected.additions" :key="addition.id">{{ addition.content }}<small>{{ formatTime(addition.createdAt) }}</small></p>
        </section>
        <form class="addition-form" @submit.prevent="submitAddition">
          <el-input v-model="additionContent" type="textarea" :rows="3" placeholder="有新的复现信息或想法，可以继续补充" />
          <el-button type="primary" native-type="submit" :loading="adding" :disabled="!additionContent.trim()">补充说明</el-button>
        </form>
      </div>
    </el-drawer>
  </main>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  appendFeatureSuggestion,
  createFeatureSuggestion,
  getMyFeatureSuggestion,
  getMyFeatureSuggestions,
  loadFeatureSuggestionScreenshot
} from '../api/featureSuggestions'

const PAGE_SIZE = 8
const typeOptions = [
  { value: 'NEW_FEATURE', label: '新功能' },
  { value: 'UX_IMPROVEMENT', label: '体验改进' },
  { value: 'BUG_REPORT', label: '问题反馈' },
  { value: 'OTHER', label: '其他' }
]
const statusLabels = { PENDING: '待处理', ACCEPTED: '已采纳', PLANNED: '已排期', IN_DEVELOPMENT: '开发中', COMPLETED: '已完成', DECLINED: '暂不处理' }
const form = ref({ type: 'NEW_FEATURE', title: '', detail: '', expectedEffect: '' })
const screenshot = ref(null)
const fileInput = ref(null)
const suggestions = ref([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)
const submitting = ref(false)
const adding = ref(false)
const errorMessage = ref('')
const detailVisible = ref(false)
const detailLoading = ref(false)
const selected = ref(null)
const additionContent = ref('')
const screenshotUrl = ref('')

onMounted(loadSuggestions)
onBeforeUnmount(revokeScreenshot)

async function loadSuggestions() {
  loading.value = true
  try {
    const response = await getMyFeatureSuggestions({ page: page.value, size: PAGE_SIZE })
    const result = response.data.data || {}
    suggestions.value = result.items || []
    total.value = result.total || 0
  } catch (error) {
    errorMessage.value = resolveError(error, '建议列表加载失败')
  } finally {
    loading.value = false
  }
}

function selectScreenshot(event) {
  const file = event.target.files?.[0] || null
  if (!file) return
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type) || file.size > 5 * 1024 * 1024) {
    errorMessage.value = '截图仅支持 JPG、PNG、WebP，且大小不能超过 5MB'
    event.target.value = ''
    screenshot.value = null
    return
  }
  errorMessage.value = ''
  screenshot.value = file
}

async function submitSuggestion() {
  errorMessage.value = ''
  if (!form.value.title.trim() || !form.value.detail.trim()) {
    errorMessage.value = '请填写标题和详细描述'
    return
  }
  submitting.value = true
  try {
    await createFeatureSuggestion({ ...form.value, title: form.value.title.trim(), detail: form.value.detail.trim() }, screenshot.value)
    ElMessage.success('建议已提交，感谢你的反馈')
    form.value = { type: 'NEW_FEATURE', title: '', detail: '', expectedEffect: '' }
    screenshot.value = null
    if (fileInput.value) fileInput.value.value = ''
    page.value = 1
    await loadSuggestions()
  } catch (error) {
    errorMessage.value = resolveError(error, '建议提交失败，请稍后再试')
  } finally {
    submitting.value = false
  }
}

async function openDetail(item) {
  detailVisible.value = true
  detailLoading.value = true
  selected.value = item
  additionContent.value = ''
  revokeScreenshot()
  try {
    const response = await getMyFeatureSuggestion(item.id)
    selected.value = response.data.data
    if (selected.value?.screenshotUrl) {
      const image = await loadFeatureSuggestionScreenshot(item.id)
      screenshotUrl.value = URL.createObjectURL(image.data)
    }
  } catch (error) {
    ElMessage.error(resolveError(error, '建议详情加载失败'))
  } finally {
    detailLoading.value = false
  }
}

async function submitAddition() {
  if (!selected.value?.id || !additionContent.value.trim()) return
  adding.value = true
  try {
    const response = await appendFeatureSuggestion(selected.value.id, additionContent.value.trim())
    selected.value = response.data.data
    additionContent.value = ''
    ElMessage.success('补充内容已保存')
  } catch (error) {
    ElMessage.error(resolveError(error, '补充失败，请稍后再试'))
  } finally {
    adding.value = false
  }
}

function revokeScreenshot() {
  if (screenshotUrl.value) URL.revokeObjectURL(screenshotUrl.value)
  screenshotUrl.value = ''
}

function typeLabel(value) { return typeOptions.find((item) => item.value === value)?.label || '其他' }
function statusLabel(value) { return statusLabels[value] || '待处理' }
function formatTime(value) { return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '刚刚' }
function resolveError(error, fallback) { return error?.response?.data?.message || error?.message || fallback }
</script>

<style scoped>
.suggestion-page { min-height: 100%; padding: clamp(20px, 4vw, 54px); color: var(--app-text); }
.suggestion-shell { width: min(1160px, 100%); margin: 0 auto; }
.suggestion-hero { display: flex; justify-content: space-between; gap: 30px; align-items: end; padding-bottom: 25px; border-bottom: 1px solid var(--app-line); }
.suggestion-kicker, .section-kicker { margin: 0 0 8px; color: var(--app-accent); font-size: 12px; font-weight: 800; letter-spacing: .12em; }
.suggestion-hero h1 { margin: 0; font-family: Georgia, 'Times New Roman', serif; font-size: clamp(34px, 6vw, 70px); line-height: .95; letter-spacing: -.04em; }
.suggestion-hero p:last-child { max-width: 520px; margin: 18px 0 0; color: var(--app-text-muted); line-height: 1.75; }
.suggestion-rule { width: min(180px, 25vw); height: 8px; background: var(--app-accent); opacity: .7; }
.suggestion-layout { display: grid; grid-template-columns: minmax(0, 1.25fr) minmax(300px, .75fr); gap: 18px; margin-top: 18px; }
.suggestion-form-card, .suggestion-list-card { min-height: 500px; border: 1px solid var(--app-line); border-radius: 18px; background: var(--app-surface); }
.suggestion-form-card { padding: clamp(18px, 3vw, 30px); }
.suggestion-list-card { padding: clamp(18px, 3vw, 26px); }
.section-heading { display: flex; justify-content: space-between; gap: 12px; align-items: start; margin-bottom: 24px; }
.section-heading h2 { margin: 0; font-size: 23px; }
.form-note, .file-hint, .form-footer, .detail-time, .suggestion-item-meta { color: var(--app-text-muted); font-size: 12px; }
.form-grid { display: grid; grid-template-columns: .65fr 1.35fr; gap: 14px; }
.full-width { width: 100%; }
.upload-row { display: flex; align-items: center; flex-wrap: wrap; gap: 10px; margin: 3px 0 20px; }
.upload-label { color: var(--app-text-soft); font-size: 13px; font-weight: 700; }
.upload-row input { max-width: 220px; font-size: 12px; }
.file-name { max-width: 220px; overflow: hidden; color: var(--app-accent); font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.inline-alert { margin-bottom: 16px; }
.form-footer { display: flex; justify-content: space-between; align-items: center; gap: 15px; }
.suggestion-list { min-height: 330px; }
.suggestion-item { display: block; width: 100%; padding: 14px 2px; border: 0; border-bottom: 1px solid var(--app-line); background: transparent; color: var(--app-text); text-align: left; cursor: pointer; }
.suggestion-item:hover, .suggestion-item:focus-visible { background: var(--app-surface-soft); outline: none; }
.suggestion-item-top, .detail-status-row { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.suggestion-item strong { display: block; margin: 9px 0 7px; font-size: 14px; }
.suggestion-type { color: var(--app-accent); font-size: 11px; font-weight: 800; }
.status-pill { display: inline-flex; padding: 3px 8px; border-radius: 99px; background: var(--app-surface-soft); color: var(--app-text-muted); font-size: 11px; }
.status-completed { color: #267755; background: #e2f3e8; }.status-in_development, .status-accepted { color: #356d9a; background: #e5f0f8; }.status-declined { color: #a75d63; background: #fae8e8; }
.suggestion-detail h2 { margin: 16px 0 5px; font-size: 25px; }.detail-time { margin: 0 0 25px; }
.detail-fields { display: grid; gap: 18px; margin: 0; }.detail-fields div { padding-bottom: 16px; border-bottom: 1px solid var(--app-line); }.detail-fields dt { margin-bottom: 7px; color: var(--app-text-muted); font-size: 12px; font-weight: 800; }.detail-fields dd { margin: 0; line-height: 1.7; white-space: pre-wrap; }
.detail-screenshot { display: block; max-width: 100%; max-height: 360px; margin: 20px 0; border-radius: 12px; object-fit: contain; }
.reply-box { margin: 20px 0; padding: 16px; border-left: 3px solid var(--app-accent); background: var(--app-surface-soft); }.reply-box span { color: var(--app-accent); font-size: 12px; font-weight: 800; }.reply-box p { margin: 8px 0 0; line-height: 1.65; white-space: pre-wrap; }
.addition-list h3 { font-size: 15px; }.addition-list p { padding: 10px 0; border-bottom: 1px solid var(--app-line); line-height: 1.6; white-space: pre-wrap; }.addition-list small { display: block; color: var(--app-text-muted); font-size: 11px; }
.addition-form { display: grid; gap: 10px; margin-top: 25px; }
@media (max-width: 820px) { .suggestion-layout { grid-template-columns: 1fr; }.suggestion-list-card { min-height: 300px; }.suggestion-rule { display: none; } }
@media (max-width: 520px) { .suggestion-page { padding: 15px; }.form-grid { grid-template-columns: 1fr; }.form-footer { align-items: end; flex-direction: column; }.form-footer .el-button { width: 100%; }.suggestion-hero h1 { font-size: 44px; } }
</style>
