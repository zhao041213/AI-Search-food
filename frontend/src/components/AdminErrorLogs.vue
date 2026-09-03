<template>
  <section class="error-logs-workspace" aria-labelledby="error-logs-title">
    <header class="error-logs-toolbar">
      <div>
        <p class="error-logs-kicker"><TriangleAlert :size="13" aria-hidden="true" />运行故障信号</p>
        <h2 id="error-logs-title">异常日志</h2>
        <span>集中查看后端、AI、数据库和 Tool 调用失败，帮助快速定位问题。</span>
      </div>
      <div class="error-logs-summary" aria-live="polite">
        <strong>{{ total }}</strong>
        <span>条异常</span>
        <el-button
          :loading="loading"
          circle
          aria-label="刷新异常日志"
          title="刷新异常日志"
          @click="loadLogs"
        >
          <RefreshCw :size="16" aria-hidden="true" />
        </el-button>
      </div>
    </header>

    <form class="error-log-filters" @submit.prevent="applyFilters">
      <el-input
        v-model.trim="keyword"
        clearable
        aria-label="搜索异常日志"
        placeholder="搜索组件、异常类型、接口或错误信息"
      />
      <el-select v-model="sourceFilter" aria-label="筛选异常来源" placeholder="异常来源">
        <el-option
          v-for="option in sourceOptions"
          :key="option.value || 'all'"
          :label="option.label"
          :value="option.value"
        />
      </el-select>
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        value-format="YYYY-MM-DD"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        aria-label="筛选异常日期范围"
      />
      <el-button type="primary" native-type="submit" :loading="loading">查询</el-button>
    </form>

    <el-alert
      v-if="errorMessage"
      class="error-logs-error"
      type="error"
      :title="errorMessage"
      show-icon
      :closable="false"
    />

    <div class="error-logs-table">
      <el-table
        v-loading="loading"
        :data="logs"
        row-key="id"
        height="100%"
        empty-text="暂无异常记录"
        @row-click="openDetail"
      >
        <el-table-column label="发生时间" min-width="174">
          <template #default="scope">{{ formatErrorLogTime(scope.row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="来源" width="104" align="center">
          <template #default="scope">
            <el-tag :type="errorSourceTagType(scope.row.sourceType)" effect="light">
              {{ errorSourceLabel(scope.row.sourceType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="component" label="组件" min-width="190" show-overflow-tooltip />
        <el-table-column label="错误摘要" min-width="300" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.message }}</template>
        </el-table-column>
        <el-table-column prop="requestPath" label="接口 / 任务" min-width="240" show-overflow-tooltip />
        <el-table-column prop="statusCode" label="状态" width="78" align="center" />
        <el-table-column label="详情" width="82" align="center" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click.stop="openDetail(scope.row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <footer class="error-logs-footer">
      <span>点击任意记录查看根因与堆栈信息</span>
      <el-pagination
        v-if="total"
        v-model:current-page="page"
        :page-size="PAGE_SIZE"
        :total="total"
        background
        layout="prev, pager, next"
        @current-change="changePage"
      />
    </footer>

    <el-drawer v-model="detailVisible" title="异常详情" size="min(560px, 92vw)" destroy-on-close>
      <div v-loading="detailLoading" class="error-detail">
        <template v-if="selectedError">
          <div class="error-detail-banner">
            <div>
              <span class="detail-label">错误摘要</span>
              <strong>{{ selectedError.message }}</strong>
            </div>
            <el-tag :type="errorSourceTagType(selectedError.sourceType)">
              {{ errorSourceLabel(selectedError.sourceType) }}
            </el-tag>
          </div>

          <dl class="error-detail-grid">
            <div><dt>发生时间</dt><dd>{{ formatErrorLogTime(selectedError.createdAt) }}</dd></div>
            <div><dt>组件</dt><dd>{{ selectedError.component }}</dd></div>
            <div><dt>异常类型</dt><dd>{{ selectedError.exceptionClass }}</dd></div>
            <div><dt>HTTP 状态</dt><dd>{{ selectedError.statusCode || '—' }}</dd></div>
            <div class="detail-wide"><dt>接口 / 任务</dt><dd>{{ selectedError.requestMethod }} {{ selectedError.requestPath }}</dd></div>
            <div><dt>操作者</dt><dd>{{ actorLabel(selectedError) }}</dd></div>
            <div><dt>来源 IP</dt><dd>{{ selectedError.ipAddress }}</dd></div>
          </dl>

          <section class="error-detail-section">
            <h3>根因</h3>
            <pre>{{ selectedError.rootCause }}</pre>
          </section>
          <section class="error-detail-section">
            <h3>堆栈信息</h3>
            <pre>{{ selectedError.stackTrace || '暂无堆栈信息' }}</pre>
          </section>
        </template>
      </div>
    </el-drawer>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { RefreshCw, TriangleAlert } from 'lucide-vue-next'
import { getAdminErrorLog, getAdminErrorLogs } from '../api/adminErrorLogs'
import {
  errorSourceLabel,
  errorSourceTagType,
  formatErrorLogTime,
  normalizeAdminErrorLog,
  normalizeAdminErrorLogPage,
  ERROR_SOURCE_OPTIONS
} from '../utils/adminErrorLogs'

const PAGE_SIZE = 20
const sourceOptions = ERROR_SOURCE_OPTIONS
const logs = ref([])
const total = ref(0)
const page = ref(1)
const keyword = ref('')
const sourceFilter = ref('')
const dateRange = ref([])
const loading = ref(false)
const errorMessage = ref('')
const detailVisible = ref(false)
const detailLoading = ref(false)
const selectedError = ref(null)

onMounted(loadLogs)

async function loadLogs() {
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await getAdminErrorLogs({
      sourceType: sourceFilter.value,
      keyword: keyword.value,
      from: dateRange.value?.[0] || '',
      to: dateRange.value?.[1] || '',
      limit: PAGE_SIZE,
      offset: (page.value - 1) * PAGE_SIZE
    })
    const result = normalizeAdminErrorLogPage(response.data.data)
    logs.value = result.items
    total.value = result.total
  } catch (error) {
    logs.value = []
    total.value = 0
    errorMessage.value = resolveErrorMessage(error)
  } finally {
    loading.value = false
  }
}

async function openDetail(row) {
  if (!row?.id) return
  detailVisible.value = true
  detailLoading.value = true
  selectedError.value = normalizeAdminErrorLog(row)
  try {
    const response = await getAdminErrorLog(row.id)
    selectedError.value = normalizeAdminErrorLog(response.data.data)
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '异常详情加载失败'))
  } finally {
    detailLoading.value = false
  }
}

function applyFilters() {
  page.value = 1
  loadLogs()
}

function changePage(nextPage) {
  page.value = nextPage
  loadLogs()
}

function actorLabel(item) {
  if (item.adminId != null) return `管理员 #${item.adminId}`
  if (item.userId != null) return `用户 #${item.userId}`
  return '系统任务'
}

function resolveErrorMessage(error, fallback = '异常日志加载失败') {
  const message = error?.response?.data?.message || error?.message
  const messages = {
    Unauthorized: '登录状态无效，请重新登录',
    Forbidden: '当前账号没有管理员权限',
    'Invalid request parameters': '请求参数不合法',
    '异常来源不合法': '异常来源筛选条件不合法',
    '异常时间范围不合法': '结束日期不能早于开始日期',
    'Network Error': '网络连接失败，请检查后端服务'
  }
  return messages[message] || message || fallback
}
</script>

<style scoped>
.error-logs-workspace {
  display: grid;
  grid-template-rows: auto auto auto minmax(0, 1fr) auto;
  gap: 12px;
  min-height: 0;
  padding: 16px;
  overflow: hidden;
  border: 1px solid var(--app-line);
  border-radius: 8px;
  background:
    linear-gradient(90deg, var(--app-grid-line-strong) 1px, transparent 1px),
    linear-gradient(var(--app-grid-line-soft) 1px, transparent 1px),
    var(--app-surface);
  background-size: 32px 32px;
  box-shadow: var(--app-panel-shadow), inset 0 1px 0 var(--app-grid-line-strong);
}

.error-logs-toolbar,
.error-logs-footer,
.error-logs-summary,
.error-logs-kicker {
  display: flex;
  align-items: center;
}

.error-logs-toolbar,
.error-logs-footer {
  justify-content: space-between;
  gap: 12px;
}

.error-logs-kicker {
  gap: 5px;
  margin: 0 0 4px;
  color: var(--app-danger, #c24f4f);
  font-family: "Cascadia Mono", "SFMono-Regular", Consolas, monospace;
  font-size: 11px;
  font-weight: 800;
}

.error-logs-toolbar h2 {
  margin: 0;
  color: var(--app-text);
  font-size: 18px;
}

.error-logs-toolbar > div:first-child > span,
.error-logs-footer {
  color: var(--app-text-muted);
  font-size: 12px;
}

.error-logs-summary {
  gap: 5px;
  color: var(--app-text-muted);
  font-size: 12px;
  white-space: nowrap;
}

.error-logs-summary strong {
  color: var(--app-danger, #c24f4f);
  font-size: 24px;
  line-height: 1;
}

.error-logs-summary :deep(.el-button) {
  margin-left: 8px;
}

.error-log-filters {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) 132px 250px auto;
  gap: 10px;
}

.error-logs-error {
  margin: 0;
}

.error-logs-table {
  min-height: 0;
  overflow: hidden;
}

.error-logs-table :deep(.el-table) {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: color-mix(in srgb, var(--app-surface-soft) 82%, transparent);
  --el-table-row-hover-bg-color: color-mix(in srgb, var(--app-danger, #c24f4f) 8%, transparent);
  height: 100%;
}

.error-logs-table :deep(.el-table__row) {
  cursor: pointer;
}

.error-logs-table :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.error-logs-footer :deep(.el-pagination) {
  margin-left: auto;
}

.error-detail {
  min-height: 100%;
}

.error-detail-banner {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 14px;
  border-left: 3px solid var(--app-danger, #c24f4f);
  background: color-mix(in srgb, var(--app-danger, #c24f4f) 8%, var(--app-surface));
}

.error-detail-banner > div {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.detail-label,
.error-detail-grid dt {
  color: var(--app-text-muted);
  font-family: "Cascadia Mono", "SFMono-Regular", Consolas, monospace;
  font-size: 11px;
  font-weight: 800;
}

.error-detail-banner strong {
  overflow-wrap: anywhere;
  color: var(--app-text);
  font-size: 15px;
  line-height: 1.5;
}

.error-detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin: 18px 0;
}

.error-detail-grid div {
  display: grid;
  gap: 5px;
  min-width: 0;
}

.error-detail-grid dd {
  margin: 0;
  overflow-wrap: anywhere;
  color: var(--app-text);
  font-size: 13px;
  line-height: 1.45;
}

.detail-wide {
  grid-column: 1 / -1;
}

.error-detail-section {
  display: grid;
  gap: 7px;
  margin-top: 16px;
}

.error-detail-section h3 {
  margin: 0;
  color: var(--app-text);
  font-size: 13px;
}

.error-detail-section pre {
  max-height: 300px;
  margin: 0;
  padding: 12px;
  overflow: auto;
  border: 1px solid var(--app-line);
  background: var(--app-surface-soft);
  color: var(--app-text);
  font-family: "Cascadia Mono", "SFMono-Regular", Consolas, monospace;
  font-size: 12px;
  line-height: 1.55;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

@media (max-width: 980px) {
  .error-log-filters {
    grid-template-columns: minmax(180px, 1fr) 132px;
  }

  .error-log-filters :deep(.el-date-editor),
  .error-log-filters :deep(.el-button) {
    width: 100%;
  }
}

@media (max-width: 720px) {
  .error-logs-workspace {
    min-height: 620px;
    padding: 12px;
  }

  .error-logs-toolbar {
    align-items: flex-start;
  }

  .error-logs-toolbar > div:first-child > span {
    display: block;
    max-width: 250px;
    line-height: 1.5;
  }

  .error-logs-summary {
    align-items: flex-start;
    flex-wrap: wrap;
    justify-content: flex-end;
  }

  .error-log-filters {
    grid-template-columns: 1fr;
  }

  .error-logs-footer {
    align-items: flex-start;
    flex-direction: column;
  }

  .error-logs-footer :deep(.el-pagination) {
    margin-left: 0;
  }

  .error-detail-grid {
    grid-template-columns: 1fr;
  }

  .detail-wide {
    grid-column: auto;
  }
}
</style>
