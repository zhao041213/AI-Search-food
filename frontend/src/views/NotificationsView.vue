<template>
  <main class="notifications-page">
    <header class="notifications-heading">
      <div>
        <p class="eyebrow">个人提醒 / NOTIFICATIONS</p>
        <h1>消息与提醒中心</h1>
        <p class="heading-copy">把库存变化和明日菜单收进一处，先处理最需要你留意的事项。</p>
      </div>
      <RouterLink class="back-link" to="/">
        <ArrowLeft :size="16" aria-hidden="true" />
        <span>返回工作台</span>
      </RouterLink>
    </header>

    <section class="notifications-layout" aria-label="消息与提醒中心">
      <article class="notification-list-panel">
        <div class="panel-heading">
          <div>
            <p class="panel-label">INBOX / 01</p>
            <h2>你的消息</h2>
          </div>
          <button
            class="quiet-button"
            type="button"
            :disabled="unreadCount === 0 || markingAll"
            @click="markAllRead"
          >
            <CheckCheck :size="15" aria-hidden="true" />
            <span>{{ markingAll ? '处理中' : '全部标记已读' }}</span>
          </button>
        </div>

        <nav class="notification-filters" aria-label="消息筛选">
          <button
            v-for="filter in filters"
            :key="filter.value"
            class="filter-button"
            :class="{ active: activeFilter === filter.value }"
            type="button"
            :aria-current="activeFilter === filter.value ? 'page' : undefined"
            @click="selectFilter(filter.value)"
          >
            {{ filter.label }}
            <span v-if="filter.value === 'unread' && unreadCount > 0">{{ unreadCount }}</span>
          </button>
        </nav>

        <div v-if="loading" class="notification-skeleton" aria-label="正在加载消息">
          <el-skeleton v-for="index in 3" :key="index" :rows="2" animated />
        </div>
        <el-empty v-else-if="!items.length" :description="emptyDescription" />
        <div v-else class="notification-list">
          <article
            v-for="item in items"
            :key="item.id"
            class="notification-card"
            :class="[`notification-card-${item.type.toLowerCase().replaceAll('_', '-')}`, { unread: item.status === 'UNREAD' }]"
          >
            <div class="notification-card-topline">
              <button
                class="notification-card-main"
                type="button"
                :aria-expanded="expandedId === item.id"
                @click="toggleDetail(item)"
              >
                <span class="notification-type-mark" aria-hidden="true">
                  <component :is="typeIcons[item.type] || Bell" :size="17" />
                </span>
                <span class="notification-card-copy">
                  <strong>{{ item.title }}</strong>
                  <span>{{ item.summary }}</span>
                </span>
              </button>
              <div class="notification-card-meta">
                <time :datetime="item.createdAt">{{ formatDate(item.createdAt) }}</time>
                <span class="status-chip" :class="`status-${item.status.toLowerCase()}`">
                  {{ statusLabel(item.status) }}
                </span>
              </div>
            </div>

            <div v-if="expandedId === item.id" class="notification-detail">
              <p>{{ detailItem?.id === item.id ? detailItem.content : item.content || item.summary }}</p>
              <button v-if="item.targetPath && embedded" type="button" class="notification-target" @click="emit('open-feature', item.targetPath)">
                查看相关内容
                <ArrowUpRight :size="14" aria-hidden="true" />
              </button>
              <RouterLink v-else-if="item.targetPath" class="notification-target" :to="item.targetPath">
                查看相关内容
                <ArrowUpRight :size="14" aria-hidden="true" />
              </RouterLink>
            </div>

            <footer class="notification-card-actions">
              <button v-if="item.status === 'UNREAD'" type="button" @click="markRead(item)">
                <Check :size="14" aria-hidden="true" />
                标记已读
              </button>
              <button v-if="item.status !== 'ARCHIVED'" type="button" @click="archive(item)">
                <Archive :size="14" aria-hidden="true" />
                归档
              </button>
              <span v-if="expandedId !== item.id" class="detail-hint">点击消息查看详情</span>
            </footer>
          </article>
        </div>

        <el-pagination
          v-if="total > 0"
          class="notification-pagination"
          background
          layout="prev, pager, next"
          :current-page="page"
          :page-size="size"
          :total="total"
          @current-change="changePage"
        />
      </article>

      <aside class="notification-preferences-panel">
        <div class="panel-heading">
          <div>
            <p class="panel-label">PREFERENCES / 02</p>
            <h2>提醒偏好</h2>
          </div>
          <SlidersHorizontal :size="21" aria-hidden="true" />
        </div>
        <p class="preferences-intro">只影响后续自动生成的站内提醒，不会删除已有消息。</p>

        <div v-if="preferencesLoading" class="preference-skeleton">
          <el-skeleton v-for="index in 3" :key="index" :rows="1" animated />
        </div>
        <div v-else class="preference-list">
          <label class="preference-row">
            <span>
              <strong>临期食材</strong>
              <small>7 天内到期时提醒优先使用</small>
            </span>
            <el-switch v-model="preferences.pantryExpiringEnabled" :loading="savingPreference" :disabled="savingPreference" @change="savePreferences" />
          </label>
          <label class="preference-row">
            <span>
              <strong>过期食材</strong>
              <small>发现已过期库存时提醒处理</small>
            </span>
            <el-switch v-model="preferences.pantryExpiredEnabled" :loading="savingPreference" :disabled="savingPreference" @change="savePreferences" />
          </label>
          <label class="preference-row">
            <span>
              <strong>周菜单准备</strong>
              <small>明日菜单提前准备食材与安排</small>
            </span>
            <el-switch v-model="preferences.weeklyMenuPreparationEnabled" :loading="savingPreference" :disabled="savingPreference" @change="savePreferences" />
          </label>
        </div>
        <p v-if="savingPreference" class="preference-saving" role="status">正在保存偏好…</p>
      </aside>
    </section>
  </main>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Archive, ArrowLeft, ArrowUpRight, Bell, CalendarClock, Check, CheckCheck, SlidersHorizontal, TriangleAlert } from 'lucide-vue-next'
import { RouterLink } from 'vue-router'
import {
  archiveNotification,
  getNotificationDetail,
  getNotificationPreferences,
  getNotifications,
  getUnreadNotificationCount,
  markAllNotificationsRead,
  markNotificationRead,
  updateNotificationPreferences
} from '../api/notifications'

defineProps({
  embedded: { type: Boolean, default: false }
})
const emit = defineEmits(['open-feature'])

const filters = [
  { value: 'all', label: '全部' },
  { value: 'unread', label: '未读' },
  { value: 'read', label: '已读' },
  { value: 'archived', label: '已归档' }
]
const typeIcons = {
  PANTRY_EXPIRING: CalendarClock,
  PANTRY_EXPIRED: TriangleAlert,
  WEEKLY_MENU_PREP: Bell
}

const activeFilter = ref('all')
const items = ref([])
const page = ref(1)
const size = 10
const total = ref(0)
const unreadCount = ref(0)
const loading = ref(true)
const expandedId = ref(null)
const detailItem = ref(null)
const markingAll = ref(false)
const preferencesLoading = ref(true)
const savingPreference = ref(false)
const preferences = ref({
  pantryExpiringEnabled: true,
  pantryExpiredEnabled: true,
  weeklyMenuPreparationEnabled: true
})

const emptyDescription = '这里还没有符合条件的消息'

onMounted(() => {
  void loadNotifications()
  void loadPreferences()
})

async function loadNotifications() {
  loading.value = true
  expandedId.value = null
  detailItem.value = null
  try {
    const [listResponse, countResponse] = await Promise.all([
      getNotifications({ status: activeFilter.value, page: page.value, size }),
      getUnreadNotificationCount()
    ])
    const data = listResponse.data.data || {}
    items.value = data.items || []
    total.value = Number(data.total || 0)
    unreadCount.value = Number(countResponse.data.data || 0)
  } catch (error) {
    ElMessage.error(messageFrom(error, '消息加载失败，请稍后重试'))
  } finally {
    loading.value = false
  }
}

async function loadPreferences() {
  preferencesLoading.value = true
  try {
    const response = await getNotificationPreferences()
    preferences.value = {
      ...preferences.value,
      ...(response.data.data || {})
    }
  } catch (error) {
    ElMessage.error(messageFrom(error, '提醒偏好加载失败，请稍后重试'))
  } finally {
    preferencesLoading.value = false
  }
}

function selectFilter(value) {
  if (activeFilter.value === value) return
  activeFilter.value = value
  page.value = 1
  void loadNotifications()
}

function changePage(value) {
  page.value = value
  void loadNotifications()
}

async function toggleDetail(item) {
  if (expandedId.value === item.id) {
    expandedId.value = null
    detailItem.value = null
    return
  }
  expandedId.value = item.id
  detailItem.value = null
  try {
    const response = await getNotificationDetail(item.id)
    const detail = response.data.data || item
    replaceItem(detail)
    detailItem.value = detail
    if (item.status === 'UNREAD' && detail.status !== 'UNREAD') {
      unreadCount.value = Math.max(0, unreadCount.value - 1)
      notifyShell()
    }
  } catch (error) {
    expandedId.value = null
    ElMessage.error(messageFrom(error, '消息详情加载失败，请稍后重试'))
  }
}

async function markRead(item) {
  try {
    const response = await markNotificationRead(item.id)
    const updated = response.data.data || { ...item, status: 'READ' }
    replaceItem(updated)
    unreadCount.value = Math.max(0, unreadCount.value - (item.status === 'UNREAD' ? 1 : 0))
    notifyShell()
  } catch (error) {
    ElMessage.error(messageFrom(error, '标记已读失败，请稍后重试'))
  }
}

async function markAllRead() {
  if (markingAll.value || unreadCount.value === 0) return
  markingAll.value = true
  try {
    await markAllNotificationsRead()
    items.value = items.value.map(item => item.status === 'UNREAD' ? { ...item, status: 'READ' } : item)
    unreadCount.value = 0
    notifyShell()
    ElMessage.success('全部消息已标记为已读')
  } catch (error) {
    ElMessage.error(messageFrom(error, '批量标记失败，请稍后重试'))
  } finally {
    markingAll.value = false
  }
}

async function archive(item) {
  try {
    const response = await archiveNotification(item.id)
    const updated = response.data.data || { ...item, status: 'ARCHIVED' }
    replaceItem(updated)
    if (item.status === 'UNREAD') {
      unreadCount.value = Math.max(0, unreadCount.value - 1)
      notifyShell()
    }
    if (activeFilter.value !== 'all') {
      await loadNotifications()
    }
  } catch (error) {
    ElMessage.error(messageFrom(error, '归档失败，请稍后重试'))
  }
}

async function savePreferences() {
  if (savingPreference.value) return
  savingPreference.value = true
  try {
    const response = await updateNotificationPreferences(preferences.value)
    preferences.value = { ...preferences.value, ...(response.data.data || {}) }
    ElMessage.success('提醒偏好已保存')
  } catch (error) {
    ElMessage.error(messageFrom(error, '提醒偏好保存失败，请稍后重试'))
    await loadPreferences()
  } finally {
    savingPreference.value = false
  }
}

function replaceItem(updated) {
  const index = items.value.findIndex(item => item.id === updated.id)
  if (index >= 0) items.value[index] = updated
}

function statusLabel(status) {
  return { UNREAD: '未读', READ: '已读', ARCHIVED: '已归档' }[status] || status
}

function formatDate(value) {
  if (!value) return '刚刚'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' }).format(date)
}

function messageFrom(error, fallback) {
  return error?.response?.data?.message || fallback
}

function notifyShell() {
  window.dispatchEvent(new Event('notifications-updated'))
}
</script>

<style scoped>
.notifications-page {
  width: min(1180px, 100%);
  margin: 0 auto;
  padding: 10px 0 72px;
}

.notifications-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 28px;
}

.eyebrow,
.panel-label {
  margin: 0 0 8px;
  color: var(--app-accent);
  font-size: 11px;
  font-weight: 900;
  letter-spacing: .18em;
}

h1,
h2,
p {
  margin-top: 0;
}

h1 {
  margin-bottom: 9px;
  color: var(--app-text);
  font-size: clamp(30px, 4vw, 48px);
  letter-spacing: -.06em;
  line-height: 1;
}

.heading-copy {
  max-width: 620px;
  margin-bottom: 0;
  color: var(--app-text-muted);
  font-size: 14px;
  line-height: 1.7;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex: 0 0 auto;
  color: var(--app-text-muted);
  font-size: 13px;
  font-weight: 800;
}

.back-link:hover,
.notification-target:hover {
  color: var(--app-accent);
}

.notifications-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  align-items: start;
  gap: 18px;
}

.notification-list-panel,
.notification-preferences-panel {
  min-width: 0;
  padding: 25px 28px;
  border: 1px solid var(--app-line);
  border-radius: 8px;
  background: var(--app-surface);
  box-shadow: var(--app-panel-shadow);
}

.notification-preferences-panel {
  position: sticky;
  top: 78px;
}

.panel-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.panel-heading h2 {
  margin-bottom: 0;
  color: var(--app-text);
  font-size: 23px;
  letter-spacing: -.04em;
}

.panel-heading > svg {
  color: var(--app-accent);
}

.quiet-button,
.filter-button,
.notification-card-actions button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 0;
  color: var(--app-text-muted);
  background: transparent;
  font: inherit;
  font-weight: 800;
  cursor: pointer;
}

.quiet-button {
  min-height: 32px;
  padding: 0 9px;
  border: 1px solid var(--app-line);
  border-radius: 6px;
  font-size: 12px;
}

.quiet-button:hover:not(:disabled),
.filter-button:hover,
.notification-card-actions button:hover {
  color: var(--app-accent);
}

.quiet-button:disabled {
  cursor: not-allowed;
  opacity: .48;
}

.notification-filters {
  display: flex;
  gap: 6px;
  margin-bottom: 18px;
  padding-bottom: 12px;
  overflow-x: auto;
  border-bottom: 1px solid var(--app-line);
}

.filter-button {
  flex: 0 0 auto;
  min-height: 32px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 13px;
}

.filter-button span {
  display: inline-grid;
  min-width: 17px;
  height: 17px;
  place-items: center;
  padding: 0 4px;
  border-radius: 999px;
  color: #fff;
  background: #d3544b;
  font-size: 10px;
}

.filter-button.active {
  color: var(--app-accent-text);
  background: var(--app-accent-soft);
}

.notification-skeleton {
  display: grid;
  gap: 22px;
  padding: 12px 0;
}

.notification-list {
  display: grid;
  gap: 10px;
}

.notification-card {
  position: relative;
  padding: 16px 17px 12px;
  border: 1px solid var(--app-line);
  border-radius: 8px;
  background: var(--app-surface);
  transition: border-color 180ms ease, background-color 180ms ease;
}

.notification-card.unread {
  border-color: color-mix(in srgb, var(--app-accent) 45%, var(--app-line));
  background: linear-gradient(110deg, var(--app-surface-strong), var(--app-surface));
}

.notification-card.unread::before {
  position: absolute;
  top: 17px;
  left: 0;
  width: 3px;
  height: 32px;
  border-radius: 0 4px 4px 0;
  background: var(--app-accent);
  content: '';
}

.notification-card-topline {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.notification-card-main {
  display: flex;
  min-width: 0;
  align-items: flex-start;
  gap: 11px;
  padding: 0;
  border: 0;
  color: inherit;
  background: transparent;
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.notification-type-mark {
  display: inline-grid;
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
  place-items: center;
  border-radius: 50%;
  color: var(--app-accent-text);
  background: var(--app-accent-soft);
}

.notification-card-pantry-expired .notification-type-mark {
  color: #8f3f35;
  background: color-mix(in srgb, #d3544b 18%, var(--app-surface));
}

.notification-card-copy {
  display: grid;
  gap: 5px;
  min-width: 0;
}

.notification-card-copy strong {
  overflow: hidden;
  color: var(--app-text);
  font-size: 15px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notification-card-copy span {
  color: var(--app-text-muted);
  font-size: 13px;
  line-height: 1.55;
}

.notification-card-meta {
  display: grid;
  justify-items: end;
  gap: 7px;
  flex: 0 0 auto;
}

.notification-card-meta time {
  color: var(--app-text-faint);
  font-size: 11px;
  white-space: nowrap;
}

.status-chip {
  display: inline-flex;
  align-items: center;
  min-height: 21px;
  padding: 0 7px;
  border: 1px solid var(--app-line);
  border-radius: 999px;
  color: var(--app-text-muted);
  font-size: 10px;
  font-weight: 800;
}

.status-unread {
  border-color: color-mix(in srgb, var(--app-accent) 55%, var(--app-line));
  color: var(--app-accent-text);
  background: var(--app-accent-soft);
}

.notification-detail {
  margin: 14px 0 0 45px;
  padding: 13px 15px;
  border-left: 2px solid var(--app-accent);
  color: var(--app-text-soft);
  background: var(--app-surface-strong);
  font-size: 13px;
  line-height: 1.75;
}

.notification-detail p {
  margin-bottom: 9px;
}

.notification-target {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0;
  border: 0;
  color: var(--app-accent);
  background: transparent;
  font: inherit;
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
}

.notification-card-actions {
  display: flex;
  align-items: center;
  gap: 14px;
  min-height: 26px;
  margin: 10px 0 0 45px;
}

.notification-card-actions button {
  padding: 0;
  font-size: 12px;
}

.detail-hint {
  margin-left: auto;
  color: var(--app-text-faint);
  font-size: 11px;
}

.notification-pagination {
  justify-content: center;
  margin-top: 22px;
}

.preferences-intro {
  margin-bottom: 18px;
  color: var(--app-text-muted);
  font-size: 13px;
  line-height: 1.65;
}

.preference-list {
  display: grid;
  gap: 2px;
}

.preference-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 15px 0;
  border-top: 1px solid var(--app-line);
  cursor: pointer;
}

.preference-row span {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.preference-row strong {
  color: var(--app-text-soft);
  font-size: 13px;
}

.preference-row small {
  color: var(--app-text-faint);
  font-size: 11px;
  line-height: 1.45;
}

.preference-saving {
  margin: 12px 0 0;
  color: var(--app-text-faint);
  font-size: 11px;
}

@media (max-width: 900px) {
  .notifications-layout {
    grid-template-columns: 1fr;
  }

  .notification-preferences-panel {
    position: static;
  }
}

@media (max-width: 620px) {
  .notifications-page {
    padding-top: 0;
  }

  .notifications-heading {
    align-items: flex-start;
    flex-direction: column;
    gap: 14px;
  }

  .notification-list-panel,
  .notification-preferences-panel {
    padding: 20px;
  }

  .notification-card-topline {
    display: grid;
    gap: 10px;
  }

  .notification-card-meta {
    display: flex;
    align-items: center;
    justify-content: flex-start;
    margin-left: 45px;
  }

  .notification-detail,
  .notification-card-actions {
    margin-left: 0;
  }

  .detail-hint {
    display: none;
  }
}
</style>
