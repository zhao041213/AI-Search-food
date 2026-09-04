<template>
  <section
    class="cooking-video-panel"
    aria-labelledby="cooking-video-title"
    :aria-busy="loading"
  >
    <div class="video-panel-head">
      <div>
        <p class="eyebrow">来自 B 站的实操参考</p>
        <h3 id="cooking-video-title">相关烹饪视频</h3>
        <p class="video-panel-description">菜谱生成完成后自动匹配最多 3 个关键词，只展示有有效视频结果的内容。</p>
      </div>
      <div v-if="matchedKeywords.length" class="video-match-summary" aria-live="polite">
        <span class="video-match-summary-label">已自动匹配</span>
        <span v-for="keyword in matchedKeywords" :key="keyword" class="video-match-chip">{{ keyword }}</span>
      </div>
    </div>

    <div v-if="loading && !items.length" class="video-grid video-skeleton-grid" aria-label="正在加载相关视频">
      <div v-for="index in 3" :key="index" class="video-skeleton-card">
        <span class="video-skeleton-cover"></span>
        <span class="video-skeleton-line video-skeleton-line-wide"></span>
        <span class="video-skeleton-line"></span>
      </div>
    </div>

    <div v-else-if="!recipeReady" class="video-state video-state-muted" role="status">
      <Film :size="30" aria-hidden="true" />
      <strong>菜谱完成后加载视频</strong>
      <span>生成过程不会请求视频服务，也不会影响菜谱输出。</span>
    </div>

    <div v-else-if="renderableItems.length" class="video-results" :class="{ 'is-loading': loading }">
      <div v-if="loading" class="video-inline-loading" role="status">正在更新视频结果…</div>
      <div v-if="degraded" class="video-inline-warning" role="status">
        {{ statusCopy.title }}：{{ statusCopy.description }}
      </div>
      <div class="video-grid">
        <a
          v-for="video in renderableItems"
          :key="video.videoId || video.bvid"
          class="video-card"
          :href="safeCookingVideoUrl(video.targetUrl)"
          :aria-label="videoLinkLabel(video)"
          target="_blank"
          rel="noopener noreferrer"
          @click="handleVideoClick($event, video)"
        >
          <div class="video-cover">
            <img
              v-if="video.coverUrl && !failedCoverIds.has(video.videoId || video.bvid)"
              :src="video.coverUrl"
              alt=""
              loading="lazy"
              referrerpolicy="no-referrer"
              @error="markCoverFailed(video.videoId || video.bvid)"
            />
            <div v-else class="video-cover-fallback" aria-hidden="true">
              <Film :size="30" />
              <span>暂无封面</span>
            </div>
            <span class="video-source-badge">哔哩哔哩</span>
          </div>
          <div class="video-card-body">
            <h4>{{ video.title }}</h4>
            <div class="video-card-meta">
              <span v-if="video.author">{{ video.author }}</span>
              <span v-if="formatDuration(video.durationSeconds)">{{ formatDuration(video.durationSeconds) }}</span>
              <span v-if="formatPublishedAt(video.publishedAt)">{{ formatPublishedAt(video.publishedAt) }}</span>
            </div>
            <span v-if="formatPlayCount(video.playCount)" class="video-play-count">{{ formatPlayCount(video.playCount) }}</span>
            <span class="video-open-hint">打开B站视频</span>
          </div>
        </a>
      </div>
    </div>

    <div v-else class="video-state" :class="{ 'video-state-error': degraded }" role="status">
      <Film v-if="!degraded" :size="30" aria-hidden="true" />
      <CircleAlert v-else :size="30" aria-hidden="true" />
      <strong>{{ statusCopy.title }}</strong>
      <span>{{ statusCopy.description }}</span>
      <div class="video-fallback-links">
        <a v-if="resolvedFallbackUrl" :href="resolvedFallbackUrl" target="_blank" rel="noopener noreferrer">
          在 B 站搜索全部相关内容
        </a>
        <a
          v-for="keyword in fallbackKeywords"
          :key="keyword"
          :href="buildBilibiliSearchLink(keyword)"
          target="_blank"
          rel="noopener noreferrer"
        >
          在 B 站搜索“{{ keyword }}”
        </a>
        <button class="video-fallback-retry" type="button" :disabled="loading" @click="retryVideos">
          {{ loading ? '正在加载…' : '重新加载' }}
        </button>
      </div>
    </div>

    <p class="video-disclaimer">视频搜索为实验性网页端能力，可能因平台规则或限流暂时不可用。</p>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { CircleAlert, Film } from 'lucide-vue-next'
import { searchCookingVideos } from '../api/videos'
import { buildBilibiliSearchLink } from '../utils/recipeEnhancements'
import {
  getCookingVideoStatusCopy,
  normalizeCookingVideoItems,
  safeCookingVideoUrl,
  selectCookingVideoKeywords
} from '../utils/cookingVideos'

const props = defineProps({
  recipeTitle: {
    type: String,
    default: ''
  },
  keywords: {
    type: Array,
    default: () => []
  },
  recipeReady: {
    type: Boolean,
    default: false
  }
})

const items = ref([])
const loading = ref(false)
const degraded = ref(false)
const message = ref('')
const matchedKeywords = ref([])
const fallbackSearchUrl = ref('')
const failedCoverIds = ref(new Set())
let requestId = 0
let queuedQueryKey = ''

const recipeTitleText = computed(() => (
  typeof props.recipeTitle === 'string' ? props.recipeTitle.trim() : ''
))

const keywordOptions = computed(() => {
  return selectCookingVideoKeywords(props.keywords, recipeTitleText.value)
})

const fallbackKeywords = computed(() => {
  const values = keywordOptions.value.length ? keywordOptions.value : [recipeTitleText.value]
  return values.filter(Boolean).slice(0, 5)
})

const renderableItems = computed(() => normalizeCookingVideoItems(items.value))

const statusCopy = computed(() => getCookingVideoStatusCopy({
  degraded: degraded.value,
  hasItems: renderableItems.value.length > 0,
  message: message.value
}))

const resolvedFallbackUrl = computed(() => {
  const normalized = String(fallbackSearchUrl.value || '')
  return /^https:\/\/search\.bilibili\.com\/all(?:\?|$)/.test(normalized)
    ? normalized
    : buildBilibiliSearchLink(`${recipeTitleText.value} ${keywordOptions.value.join(' ')}`.trim())
})

watch(keywordOptions, (options, previousOptions = []) => {
  if (options.join('\u0000') !== previousOptions.join('\u0000')) {
    resetResults()
    if (props.recipeReady) {
      loadVideos()
    }
  }
})

watch(
  () => [props.recipeTitle, props.recipeReady],
  ([nextTitle, nextReady], [previousTitle, previousReady] = []) => {
    if (nextTitle !== previousTitle) {
      resetResults()
    }
    if (nextReady && (!previousReady || nextTitle !== previousTitle)) {
      loadVideos()
    }
  }
)

onMounted(() => {
  if (props.recipeReady) {
    loadVideos()
  }
})

function retryVideos() {
  loadVideos()
}

function currentQueryKey() {
  return [recipeTitleText.value, ...keywordOptions.value].join('\u0000')
}

async function loadVideos() {
  const queryKey = currentQueryKey()
  if (!props.recipeReady || !recipeTitleText.value || !keywordOptions.value.length) {
    return
  }
  if (loading.value) {
    queuedQueryKey = queryKey
    return
  }

  const currentRequestId = ++requestId
  const searchKeywords = [...keywordOptions.value]
  loading.value = true
  degraded.value = false
  message.value = ''
  try {
    const responses = await Promise.allSettled(searchKeywords.map((keyword) => searchCookingVideos({
      recipeTitle: recipeTitleText.value,
      keyword,
      page: 1,
      limit: 6
    })))
    if (currentRequestId !== requestId || queryKey !== currentQueryKey()) return

    const mergedItems = []
    const seenVideoKeys = new Set()
    const successfulKeywords = []
    const fallbackUrls = []
    const safeMessages = []
    let hadUpstreamFailure = false

    responses.forEach((result, index) => {
      if (result.status === 'rejected') {
        hadUpstreamFailure = true
        return
      }

      const payload = result.value?.data?.data || {}
      const responseItems = normalizeCookingVideoItems(payload.items)
      if (typeof payload.fallbackSearchUrl === 'string' && payload.fallbackSearchUrl.trim()) {
        fallbackUrls.push(payload.fallbackSearchUrl.trim())
      }
      if (typeof payload.message === 'string' && payload.message.trim()) {
        safeMessages.push(payload.message.trim())
      }
      if (payload.degraded) {
        hadUpstreamFailure = true
        return
      }
      if (!responseItems.length) return

      successfulKeywords.push(searchKeywords[index])
      responseItems.forEach((video) => {
        const videoKey = video.videoId || video.bvid || video.targetUrl
        if (!videoKey || seenVideoKeys.has(videoKey) || mergedItems.length >= 6) return
        seenVideoKeys.add(videoKey)
        mergedItems.push(video)
      })
    })

    items.value = mergedItems
    matchedKeywords.value = successfulKeywords
    degraded.value = !mergedItems.length && hadUpstreamFailure
    message.value = degraded.value ? (safeMessages[0] || '视频服务暂时不可用') : ''
    fallbackSearchUrl.value = fallbackUrls[0] || ''
    failedCoverIds.value = new Set()
  } catch {
    if (currentRequestId !== requestId) return
    degraded.value = true
    message.value = '视频服务暂时不可用'
  } finally {
    if (currentRequestId === requestId) {
      loading.value = false
      const nextQueryKey = queuedQueryKey || currentQueryKey()
      if (nextQueryKey !== queryKey) {
        queuedQueryKey = ''
        loadVideos()
      } else {
        queuedQueryKey = ''
      }
    }
  }
}

function resetResults() {
  items.value = []
  matchedKeywords.value = []
  degraded.value = false
  message.value = ''
  fallbackSearchUrl.value = ''
  failedCoverIds.value = new Set()
}

function markCoverFailed(videoId) {
  if (!videoId) return
  failedCoverIds.value = new Set([...failedCoverIds.value, videoId])
}

function handleVideoClick(event, video) {
  if (!safeCookingVideoUrl(video?.targetUrl)) {
    event.preventDefault()
  }
}

function videoLinkLabel(video) {
  const title = String(video?.title || '').trim()
  return title ? `打开B站视频：${title}` : '打开B站视频'
}

function formatDuration(seconds) {
  const value = Number(seconds)
  if (!Number.isInteger(value) || value < 0) return ''
  const minutes = Math.floor(value / 60)
  const remainingSeconds = String(value % 60).padStart(2, '0')
  return `${minutes}:${remainingSeconds}`
}

function formatPublishedAt(value) {
  const timestamp = Number(value)
  if (!Number.isFinite(timestamp) || timestamp <= 0) return ''
  const date = new Date(timestamp * 1000)
  if (Number.isNaN(date.getTime())) return ''
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  }).format(date)
}

function formatPlayCount(value) {
  const count = Number(value)
  if (!Number.isFinite(count) || count <= 0) return ''
  if (count >= 10000) return `播放 ${(count / 10000).toFixed(1).replace(/\.0$/, '')} 万`
  return `播放 ${count}`
}
</script>

<style scoped>
.cooking-video-panel {
  margin-top: 22px;
  padding: 18px;
  border: 1px solid var(--app-line);
  background: var(--app-surface-soft);
}

.video-panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.video-panel-head h3,
.video-panel-description {
  margin: 0;
}

.video-panel-head h3 {
  margin-top: 5px;
  font-size: 19px;
}

.video-panel-description {
  margin-top: 7px;
  color: var(--app-text-muted);
  font-size: 12px;
  line-height: 1.6;
}

.video-match-summary {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 5px;
  color: var(--app-text-muted);
  font-size: 11px;
}

.video-match-summary-label {
  font-weight: 800;
}

.video-match-chip {
  padding: 4px 7px;
  border: 1px solid var(--app-line-strong);
  border-radius: 999px;
  color: var(--app-text);
  background: var(--app-surface);
  font-weight: 800;
}

.video-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.video-card {
  min-width: 0;
  overflow: hidden;
  border: 1px solid var(--app-line);
  color: inherit;
  background: var(--app-surface);
  text-decoration: none;
  transition: border-color .18s ease, transform .18s ease, box-shadow .18s ease;
}

.video-card:hover,
.video-card:focus-visible {
  border-color: var(--app-accent);
  box-shadow: 0 8px 20px rgba(23, 37, 34, .08);
  transform: translateY(-2px);
}

.video-cover {
  position: relative;
  aspect-ratio: 16 / 9;
  overflow: hidden;
  background: var(--app-line);
}

.video-cover img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.video-cover-fallback {
  display: grid;
  height: 100%;
  place-items: center;
  align-content: center;
  gap: 4px;
  color: var(--app-text-muted);
  background: linear-gradient(135deg, var(--app-line), var(--app-surface-soft));
  font-size: 11px;
  font-weight: 800;
}

.video-source-badge {
  position: absolute;
  right: 7px;
  bottom: 7px;
  padding: 3px 6px;
  border-radius: 3px;
  color: #fff;
  background: rgba(20, 26, 25, .76);
  font-size: 10px;
  font-weight: 800;
}

.video-card-body {
  position: relative;
  padding: 10px;
}

.video-card h4 {
  display: -webkit-box;
  min-height: 38px;
  margin: 0;
  overflow: hidden;
  font-size: 13px;
  line-height: 1.45;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.video-card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 8px;
  margin-top: 8px;
  color: var(--app-text-muted);
  font-size: 11px;
}

.video-card-meta span + span::before {
  margin-right: 8px;
  content: '·';
  color: var(--app-line-strong);
}

.video-play-count {
  display: block;
  margin-top: 5px;
  color: var(--app-text-faint);
  font-size: 10px;
}

.video-open-hint {
  display: block;
  margin-top: 8px;
  color: var(--app-accent);
  font-size: 11px;
  font-weight: 800;
}

.video-state {
  display: grid;
  min-height: 140px;
  place-items: center;
  align-content: center;
  gap: 7px;
  margin-top: 16px;
  padding: 20px;
  border: 1px dashed var(--app-line-strong);
  color: var(--app-text-muted);
  text-align: center;
}

.video-state strong {
  color: var(--app-text);
  font-size: 14px;
}

.video-state span {
  font-size: 12px;
}

.video-state-error svg {
  color: var(--el-color-warning);
}

.video-fallback-links {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 6px;
  margin-top: 4px;
}

.video-fallback-links a {
  padding: 5px 8px;
  border: 1px solid var(--app-line-strong);
  border-radius: 4px;
  color: var(--app-text);
  background: var(--app-surface);
  font-size: 11px;
  font-weight: 800;
  text-decoration: none;
}

.video-fallback-links a:hover,
.video-fallback-links a:focus-visible {
  border-color: var(--app-accent);
}

.video-fallback-retry {
  padding: 5px 8px;
  border: 1px solid var(--app-line-strong);
  border-radius: 4px;
  color: var(--app-text);
  background: var(--app-surface);
  cursor: pointer;
  font: inherit;
  font-size: 11px;
  font-weight: 800;
}

.video-fallback-retry:hover:not(:disabled),
.video-fallback-retry:focus-visible {
  border-color: var(--app-accent);
}

.video-fallback-retry:disabled {
  cursor: wait;
  opacity: .55;
}

.video-inline-loading {
  margin-top: 12px;
  color: var(--app-text-muted);
  font-size: 11px;
}

.video-results.is-loading .video-grid {
  opacity: .58;
}

.video-skeleton-card {
  display: grid;
  gap: 8px;
  padding-bottom: 12px;
  border: 1px solid var(--app-line);
  background: var(--app-surface);
}

.video-skeleton-cover,
.video-skeleton-line {
  display: block;
  background: linear-gradient(90deg, var(--app-line) 25%, var(--app-surface-soft) 50%, var(--app-line) 75%);
  background-size: 200% 100%;
  animation: video-skeleton-shimmer 1.4s linear infinite;
}

.video-skeleton-cover {
  aspect-ratio: 16 / 9;
}

.video-skeleton-line {
  width: 70%;
  height: 10px;
  margin: 0 10px;
}

.video-skeleton-line-wide {
  width: calc(100% - 20px);
}

.video-disclaimer {
  margin: 12px 0 0;
  color: var(--app-text-faint);
  font-size: 10px;
  line-height: 1.5;
}

@keyframes video-skeleton-shimmer {
  to { background-position: -200% 0; }
}

@media (prefers-reduced-motion: reduce) {
  .video-card { transition: none; }
  .video-skeleton-cover, .video-skeleton-line { animation: none; }
}

@media (max-width: 760px) {
  .video-panel-head { flex-direction: column; }
  .video-match-summary { justify-content: flex-start; }
  .video-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}

@media (max-width: 460px) {
  .cooking-video-panel { padding: 14px; }
  .video-grid { grid-template-columns: 1fr; }
}
</style>
