<template>
  <main class="kitchen-overview-page" aria-labelledby="kitchen-overview-title" :aria-busy="loading">
    <header class="overview-heading">
      <div>
        <p class="eyebrow">功能扩展 / 今日厨房</p>
        <h1 id="kitchen-overview-title">今天先做什么？</h1>
        <p class="heading-description">把库存、菜单和提醒放在一起，打开页面就知道下一步该安排什么。</p>
      </div>
      <div class="heading-actions">
        <span class="date-badge">
          <CalendarDays :size="16" aria-hidden="true" />
          {{ dateLabel }}
        </span>
        <button class="quiet-button" type="button" :disabled="loading || refreshing" @click="loadOverview({ refresh: true })">
          <RefreshCw :size="16" aria-hidden="true" :class="{ spinning: refreshing }" />
          <span>{{ refreshing ? '正在更新' : '刷新数据' }}</span>
        </button>
        <RouterLink class="primary-link" to="/">
          去生成菜谱
          <ArrowUpRight :size="15" aria-hidden="true" />
        </RouterLink>
      </div>
    </header>

    <p v-if="loading" class="load-status" role="status">正在整理厨房数据…</p>
    <p v-else-if="partialError" class="load-status warning" role="alert">
      <TriangleAlert :size="16" aria-hidden="true" />
      <span>{{ partialError }}</span>
      <button type="button" @click="loadOverview({ refresh: true })">重新加载</button>
    </p>

    <section class="metric-grid" aria-label="厨房概览指标">
      <article class="metric-card">
        <span class="metric-icon inventory"><Package :size="19" aria-hidden="true" /></span>
        <span class="metric-label">库存食材</span>
        <strong>{{ overview.pantryCount }}</strong>
        <span class="metric-note">当前可用于生成菜谱</span>
      </article>
      <article class="metric-card">
        <span class="metric-icon attention"><TriangleAlert :size="19" aria-hidden="true" /></span>
        <span class="metric-label">需要处理</span>
        <strong>{{ overview.attentionCount }}</strong>
        <span class="metric-note">过期或 7 天内到期</span>
      </article>
      <article class="metric-card">
        <span class="metric-icon menu"><CalendarDays :size="19" aria-hidden="true" /></span>
        <span class="metric-label">今日餐次</span>
        <strong>{{ overview.todayMeals.length }}<small>/ 3</small></strong>
        <span class="metric-note">本周已安排 {{ overview.weeklyAssignedCount }} 餐</span>
      </article>
      <article class="metric-card">
        <span class="metric-icon notice"><Bell :size="19" aria-hidden="true" /></span>
        <span class="metric-label">待采购 / 未读</span>
        <strong>{{ overview.pendingShoppingItems.length }}<small> / {{ overview.unreadCount }}</small></strong>
        <span class="metric-note">采购清单与消息提醒</span>
      </article>
    </section>

    <section class="overview-grid" aria-label="今日厨房安排">
      <article class="overview-panel priority-panel">
        <header class="panel-heading">
          <div>
            <p class="panel-kicker">NEXT / 01</p>
            <h2>今天先处理</h2>
          </div>
          <Sparkles :size="20" aria-hidden="true" />
        </header>
        <div class="priority-list">
          <RouterLink v-for="task in overview.priorityTasks" :key="task.id" class="priority-item" :class="`priority-${task.type}`" :to="task.to">
            <span class="priority-icon"><component :is="taskIcon(task)" :size="17" aria-hidden="true" /></span>
            <span class="priority-copy">
              <strong>{{ task.title }}</strong>
              <span>{{ task.detail }}</span>
            </span>
            <ArrowUpRight :size="16" aria-hidden="true" />
          </RouterLink>
        </div>
      </article>

      <article class="overview-panel menu-panel">
        <header class="panel-heading">
          <div>
            <p class="panel-kicker">WEEKLY MENU / 02</p>
            <h2>本周菜单进度</h2>
          </div>
          <RouterLink class="panel-link" to="/weekly-menu">管理菜单 <ArrowUpRight :size="14" aria-hidden="true" /></RouterLink>
        </header>
        <div class="progress-row">
          <strong>{{ overview.weeklyAssignedCount }}<small> / 21 餐</small></strong>
          <span>{{ overview.weeklyProgress }}%</span>
        </div>
        <div class="progress-track" role="progressbar" aria-label="本周菜单完成度" :aria-valuenow="overview.weeklyProgress" aria-valuemin="0" aria-valuemax="100">
          <span :style="{ width: `${overview.weeklyProgress}%` }" />
        </div>
        <div class="today-menu">
          <div class="subsection-heading">
            <span>今天的安排</span>
            <span>{{ dateLabel }}</span>
          </div>
          <div v-if="overview.todayMeals.length" class="meal-list">
            <div v-for="meal in overview.todayMeals" :key="`${meal.menuDate}-${meal.mealType}`" class="meal-row">
              <span class="meal-label">{{ meal.label }}</span>
              <strong>{{ meal.recipeTitle }}</strong>
            </div>
          </div>
          <div v-else class="empty-note">
            <CalendarDays :size="17" aria-hidden="true" />
            <span>今天还没有安排餐次</span>
          </div>
        </div>
      </article>

      <article class="overview-panel shopping-panel">
        <header class="panel-heading">
          <div>
            <p class="panel-kicker">SHOPPING / 03</p>
            <h2>采购清单</h2>
          </div>
          <RouterLink class="panel-link" to="/weekly-menu">查看全部 <ArrowUpRight :size="14" aria-hidden="true" /></RouterLink>
        </header>
        <ul v-if="overview.pendingShoppingItems.length" class="compact-list">
          <li v-for="item in overview.pendingShoppingItems.slice(0, 5)" :key="item.ingredientName">
            <span class="list-dot" aria-hidden="true" />
            <strong>{{ item.ingredientName }}</strong>
            <span>{{ item.amount || '待补充' }}</span>
          </li>
        </ul>
        <div v-else class="empty-note">
          <ShoppingBasket :size="17" aria-hidden="true" />
          <span>本周暂时没有待采购食材</span>
        </div>
        <p v-if="overview.pendingShoppingItems.length > 5" class="panel-footnote">还有 {{ overview.pendingShoppingItems.length - 5 }} 项待查看</p>
      </article>

      <article class="overview-panel recent-panel">
        <header class="panel-heading">
          <div>
            <p class="panel-kicker">RECENT SEARCHES / 04</p>
            <h2>最近生成</h2>
          </div>
          <RouterLink class="panel-link" to="/">再生成一次 <ArrowUpRight :size="14" aria-hidden="true" /></RouterLink>
        </header>
        <ul v-if="overview.recentSearches.length" class="recent-list">
          <li v-for="item in overview.recentSearches" :key="item.id || `${item.ingredients}-${item.createdAt}`">
            <span class="recent-mark"><ChefHat :size="15" aria-hidden="true" /></span>
            <span class="recent-copy">
              <strong>{{ item.ingredients || '未填写食材' }}</strong>
              <span>{{ searchSummary(item) }}</span>
            </span>
            <time v-if="item.createdAt" :datetime="item.createdAt">{{ formatTime(item.createdAt) }}</time>
          </li>
        </ul>
        <div v-else class="empty-note">
          <ChefHat :size="17" aria-hidden="true" />
          <span>还没有最近生成记录</span>
        </div>
      </article>
    </section>

    <section class="quick-actions" aria-label="常用入口">
      <span class="quick-label">常用入口</span>
      <RouterLink to="/"><Sparkles :size="16" aria-hidden="true" />生成菜谱</RouterLink>
      <RouterLink to="/pantry"><Package :size="16" aria-hidden="true" />管理食材</RouterLink>
      <RouterLink to="/weekly-menu"><CalendarDays :size="16" aria-hidden="true" />安排一周菜单</RouterLink>
      <RouterLink to="/notifications"><Bell :size="16" aria-hidden="true" />查看提醒</RouterLink>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowUpRight, Bell, CalendarDays, ChefHat, Package, RefreshCw, ShoppingBasket, Sparkles, TriangleAlert } from 'lucide-vue-next'
import { RouterLink } from 'vue-router'
import { getNotifications, getUnreadNotificationCount } from '../api/notifications'
import { getPantryExpiryAlerts, getPantryItems } from '../api/pantry'
import { getRecentSearches } from '../api/searchHistory'
import { getWeeklyMenu } from '../api/weeklyMenu'
import { buildKitchenOverview } from '../utils/kitchenOverview'

const loading = ref(true)
const refreshing = ref(false)
const partialError = ref('')
const overview = ref(buildKitchenOverview())
const requestId = ref(0)

const taskIcons = {
  expired: TriangleAlert,
  expiringSoon: RefreshCw,
  menu: CalendarDays,
  shopping: ShoppingBasket,
  notifications: Bell,
  clear: Sparkles
}

const dateLabel = computed(() => {
  const date = new Date(`${overview.value.today}T00:00:00`)
  return Number.isNaN(date.getTime())
    ? '今天'
    : new Intl.DateTimeFormat('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' }).format(date)
})

onMounted(() => {
  void loadOverview()
})

async function loadOverview(options = {}) {
  if (loading.value && refreshing.value) return
  const isRefresh = Boolean(options.refresh)
  if (isRefresh) {
    refreshing.value = true
  } else {
    loading.value = true
  }
  const currentRequestId = ++requestId.value
  const labels = ['库存', '保质期提醒', '周菜单', '最近生成', '消息', '未读数量']

  try {
    const results = await Promise.allSettled([
      getPantryItems(),
      getPantryExpiryAlerts(),
      getWeeklyMenu(),
      getRecentSearches(),
      getNotifications({ status: 'unread', page: 1, size: 4 }),
      getUnreadNotificationCount()
    ])
    if (currentRequestId !== requestId.value) return

    const values = results.map((result) => result.status === 'fulfilled' ? result.value?.data?.data : null)
    overview.value = buildKitchenOverview({
      pantryItems: values[0],
      expiryAlerts: values[1],
      weeklyMenu: values[2],
      recentSearches: values[3],
      notifications: values[4],
      unreadNotificationCount: values[5]
    })
    const failedLabels = results.flatMap((result, index) => result.status === 'rejected' ? labels[index] : [])
    partialError.value = failedLabels.length
      ? `部分数据暂时不可用（${failedLabels.join('、')}），已展示可用内容。`
      : ''
    if (failedLabels.length === results.length) {
      ElMessage.error('厨房数据加载失败，请稍后重试')
    }
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

function taskIcon(task) {
  return taskIcons[task.type] || Sparkles
}

function searchSummary(item) {
  return [item.mealType, item.goal].filter(Boolean).join(' · ') || '文字输入生成'
}

function formatTime(value) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '最近'
  return new Intl.DateTimeFormat('zh-CN', { month: 'numeric', day: 'numeric' }).format(date)
}
</script>

<style scoped>
.kitchen-overview-page {
  width: min(1180px, 100%);
  margin: 0 auto;
  padding: 28px 24px 72px;
}

.overview-heading,
.panel-heading,
.heading-actions,
.date-badge,
.quiet-button,
.primary-link,
.panel-link,
.load-status,
.priority-item,
.meal-row,
.empty-note,
.compact-list li,
.recent-list li,
.quick-actions,
.quick-actions a {
  display: flex;
  align-items: center;
}

.overview-heading {
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 24px;
}

.eyebrow,
.panel-kicker {
  margin: 0 0 8px;
  color: var(--app-text-muted);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: .15em;
  text-transform: uppercase;
}

h1,
h2,
p {
  margin-top: 0;
}

h1 {
  margin-bottom: 10px;
  color: var(--app-text);
  font-size: clamp(30px, 4vw, 46px);
  line-height: 1.08;
  letter-spacing: -.045em;
}

h2 {
  margin-bottom: 0;
  color: var(--app-text);
  font-size: 19px;
  letter-spacing: -.02em;
}

.heading-description {
  max-width: 610px;
  margin-bottom: 0;
  color: var(--app-text-muted);
  font-size: 14px;
  line-height: 1.7;
}

.heading-actions {
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 9px;
}

.date-badge,
.quiet-button,
.primary-link,
.panel-link,
.quick-actions a,
.load-status button {
  gap: 7px;
  min-height: 44px;
  border: 1px solid var(--app-line);
  border-radius: 10px;
  color: var(--app-text-soft);
  background: var(--app-surface);
  font-size: 13px;
  font-weight: 700;
  text-decoration: none;
}

.date-badge,
.quiet-button,
.primary-link {
  padding: 0 13px;
}

.date-badge {
  min-height: 40px;
}

.date-badge {
  color: var(--app-text-muted);
  background: var(--app-surface-strong);
}

.quiet-button,
.primary-link,
.load-status button {
  cursor: pointer;
  transition: border-color 160ms ease, color 160ms ease, background-color 160ms ease;
}

.quiet-button:hover:not(:disabled),
.panel-link:hover,
.quick-actions a:hover,
.load-status button:hover {
  border-color: var(--app-accent);
  color: var(--app-accent);
}

.quiet-button:disabled {
  cursor: wait;
  opacity: .65;
}

.primary-link {
  border-color: var(--app-accent);
  color: var(--app-accent-text);
  background: var(--app-accent);
}

.primary-link:hover {
  border-color: var(--app-accent-hover);
  background: var(--app-accent-hover);
}

.load-status {
  gap: 8px;
  margin: 0 0 16px;
  padding: 11px 13px;
  border: 1px solid var(--app-line);
  border-radius: 10px;
  color: var(--app-text-muted);
  background: var(--app-surface-strong);
  font-size: 13px;
}

.load-status.warning {
  border-color: color-mix(in srgb, var(--app-accent) 55%, var(--app-line));
  color: var(--app-text-soft);
}

.load-status button {
  margin-left: auto;
  padding: 0 9px;
  background: transparent;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.metric-card,
.overview-panel {
  border: 1px solid var(--app-line);
  background: var(--app-surface);
  box-shadow: var(--app-panel-shadow);
}

.metric-card {
  display: grid;
  grid-template-columns: auto 1fr;
  align-items: center;
  column-gap: 11px;
  padding: 17px;
  border-radius: 13px;
}

.metric-icon {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  grid-row: span 2;
  border-radius: 10px;
  color: var(--app-accent-text);
  background: var(--app-accent);
}

.metric-icon.attention {
  color: #8a4f16;
  background: color-mix(in srgb, #e1a33b 32%, var(--app-surface));
}

.metric-icon.menu {
  color: #3d6682;
  background: color-mix(in srgb, #77b6d1 30%, var(--app-surface));
}

.metric-icon.notice {
  color: #5c4c84;
  background: color-mix(in srgb, #aa91db 28%, var(--app-surface));
}

.metric-label,
.metric-note {
  color: var(--app-text-muted);
  font-size: 12px;
}

.metric-card strong {
  color: var(--app-text);
  font-size: 25px;
  line-height: 1.1;
}

.metric-card strong small,
.progress-row strong small {
  color: var(--app-text-muted);
  font-size: 12px;
  font-weight: 700;
}

.overview-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(0, .9fr);
  gap: 16px;
}

.overview-panel {
  min-width: 0;
  padding: 20px;
  border-radius: 14px;
}

.panel-heading {
  justify-content: space-between;
  gap: 15px;
  margin-bottom: 17px;
}

.panel-heading > svg {
  color: var(--app-accent);
}

.panel-link {
  padding: 0 8px;
  border-color: transparent;
  color: var(--app-text-muted);
  background: transparent;
  font-size: 12px;
}

.priority-list {
  display: grid;
  gap: 8px;
}

.priority-item {
  gap: 11px;
  min-height: 62px;
  padding: 10px 11px;
  border: 1px solid var(--app-line);
  border-radius: 10px;
  color: var(--app-text);
  text-decoration: none;
  transition: border-color 160ms ease, background-color 160ms ease, transform 160ms ease;
}

.priority-item:hover {
  border-color: var(--app-accent);
  background: var(--app-surface-strong);
  transform: translateY(-1px);
}

.priority-item > svg {
  flex: 0 0 auto;
  margin-left: auto;
  color: var(--app-text-faint);
}

.priority-icon,
.recent-mark {
  display: grid;
  flex: 0 0 auto;
  width: 32px;
  height: 32px;
  place-items: center;
  border-radius: 9px;
  color: var(--app-accent-text);
  background: var(--app-accent-soft);
}

.priority-expired .priority-icon {
  color: #9a463b;
  background: color-mix(in srgb, #d45c4c 18%, var(--app-surface));
}

.priority-expiringSoon .priority-icon {
  color: #93641a;
  background: color-mix(in srgb, #e1a33b 25%, var(--app-surface));
}

.priority-copy,
.recent-copy {
  display: grid;
  min-width: 0;
  gap: 4px;
}

.priority-copy strong,
.recent-copy strong {
  overflow: hidden;
  color: var(--app-text);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.priority-copy span,
.recent-copy span {
  overflow: hidden;
  color: var(--app-text-muted);
  font-size: 12px;
  line-height: 1.5;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.progress-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 9px;
}

.progress-row strong {
  color: var(--app-text);
  font-size: 27px;
}

.progress-row > span {
  color: var(--app-accent);
  font-size: 13px;
  font-weight: 800;
}

.progress-track {
  height: 8px;
  overflow: hidden;
  border-radius: 99px;
  background: var(--app-surface-soft);
}

.progress-track span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--app-accent);
  transition: width 220ms ease;
}

.today-menu {
  margin-top: 22px;
  padding-top: 16px;
  border-top: 1px solid var(--app-line);
}

.subsection-heading {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
  color: var(--app-text-muted);
  font-size: 12px;
  font-weight: 800;
}

.subsection-heading span:last-child {
  color: var(--app-text-faint);
  font-weight: 600;
}

.meal-list,
.compact-list,
.recent-list {
  display: grid;
  margin: 0;
  padding: 0;
  list-style: none;
}

.meal-list {
  gap: 8px;
}

.meal-row {
  gap: 10px;
  min-height: 38px;
  padding: 0 10px;
  border-radius: 8px;
  background: var(--app-surface-strong);
}

.meal-label {
  min-width: 35px;
  color: var(--app-accent);
  font-size: 12px;
  font-weight: 800;
}

.meal-row strong {
  overflow: hidden;
  color: var(--app-text-soft);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty-note {
  justify-content: center;
  gap: 8px;
  min-height: 90px;
  color: var(--app-text-muted);
  font-size: 13px;
}

.empty-note svg {
  color: var(--app-text-faint);
}

.compact-list,
.recent-list {
  gap: 1px;
}

.compact-list li,
.recent-list li {
  gap: 9px;
  min-height: 42px;
  padding: 0 3px;
  border-bottom: 1px solid var(--app-line);
}

.compact-list li:last-child,
.recent-list li:last-child {
  border-bottom: 0;
}

.list-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--app-accent);
}

.compact-list strong {
  overflow: hidden;
  flex: 1;
  color: var(--app-text-soft);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.compact-list li > span:last-child,
.recent-list time {
  flex: 0 0 auto;
  color: var(--app-text-faint);
  font-size: 11px;
}

.panel-footnote {
  margin: 12px 0 0;
  color: var(--app-text-faint);
  font-size: 11px;
}

.recent-list li {
  min-height: 52px;
}

.recent-mark {
  width: 30px;
  height: 30px;
  color: var(--app-text-muted);
  background: var(--app-surface-soft);
}

.recent-copy {
  flex: 1;
}

.quick-actions {
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
  padding: 12px 15px;
  border: 1px solid var(--app-line);
  border-radius: 12px;
  background: var(--app-surface-strong);
}

.quick-label {
  margin-right: 4px;
  color: var(--app-text-faint);
  font-size: 12px;
  font-weight: 800;
}

.quick-actions a {
  padding: 0 10px;
  border-color: transparent;
  background: transparent;
  font-size: 12px;
}

.quick-actions a svg {
  color: var(--app-accent);
}

:focus-visible {
  outline: 2px solid var(--app-accent);
  outline-offset: 2px;
}

.spinning {
  animation: spin 800ms linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 980px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .overview-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 620px) {
  .kitchen-overview-page {
    padding: 18px 14px 48px;
  }

  .overview-heading {
    align-items: flex-start;
    flex-direction: column;
    gap: 16px;
  }

  .heading-actions {
    justify-content: flex-start;
  }

  .metric-card {
    padding: 14px;
  }

  .overview-panel {
    padding: 17px;
  }

  .quick-label {
    width: 100%;
  }
}

@media (max-width: 400px) {
  .metric-grid {
    gap: 8px;
  }

  .metric-card {
    grid-template-columns: 1fr;
    gap: 5px;
  }

  .metric-icon {
    grid-row: auto;
  }

  .heading-actions > * {
    flex: 1 1 auto;
    justify-content: center;
  }
}

@media (prefers-reduced-motion: reduce) {
  .priority-item,
  .quiet-button,
  .primary-link,
  .progress-track span {
    transition: none;
  }

  .spinning {
    animation: none;
  }
}
</style>
