<template>
  <section class="operation-logs-workspace" aria-labelledby="operation-logs-title">
    <header class="operation-logs-toolbar">
      <div>
        <p>管理员行为审计</p>
        <h2 id="operation-logs-title">操作日志</h2>
        <span>记录登录与后台接口操作，不展示密码和 API Key。</span>
      </div>
      <el-button
        :loading="loading"
        circle
        aria-label="刷新操作日志"
        title="刷新操作日志"
        @click="loadLogs"
      >
        <RefreshCw :size="16" aria-hidden="true" />
      </el-button>
    </header>

    <form class="operation-log-filters" @submit.prevent="applyFilters">
      <el-input
        v-model.trim="keyword"
        clearable
        aria-label="搜索操作日志"
        placeholder="搜索账号、操作或接口路径"
      />
      <el-select v-model="resultFilter" aria-label="筛选操作结果" placeholder="操作结果">
        <el-option
          v-for="option in resultOptions"
          :key="option.value || 'all'"
          :label="option.label"
          :value="option.value"
        />
      </el-select>
      <el-button type="primary" native-type="submit" :loading="loading">查询</el-button>
    </form>

    <el-alert
      v-if="errorMessage"
      class="operation-logs-error"
      type="error"
      :title="errorMessage"
      show-icon
      :closable="false"
    />

    <div class="operation-logs-table">
      <el-table v-loading="loading" :data="logs" row-key="id" height="100%" empty-text="暂无操作记录">
        <el-table-column label="操作时间" min-width="170">
          <template #default="scope">{{ formatOperationLogTime(scope.row.createdAt) }}</template>
        </el-table-column>
        <el-table-column prop="adminUsername" label="管理员" width="120" show-overflow-tooltip />
        <el-table-column label="操作" width="140" show-overflow-tooltip>
          <template #default="scope">{{ operationTypeLabel(scope.row.operationType) }}</template>
        </el-table-column>
        <el-table-column prop="requestPath" label="接口路径" min-width="250" show-overflow-tooltip />
        <el-table-column prop="httpMethod" label="方法" width="82" align="center" />
        <el-table-column label="结果" width="82" align="center">
          <template #default="scope">
            <el-tag :type="operationResultTagType(scope.row.operationResult)" effect="light">
              {{ operationResultLabel(scope.row.operationResult) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="statusCode" label="状态码" width="82" align="center" />
        <el-table-column prop="ipAddress" label="来源 IP" min-width="130" show-overflow-tooltip />
      </el-table>
    </div>

    <footer class="operation-logs-footer">
      <span>共 {{ total }} 条记录</span>
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
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { RefreshCw } from 'lucide-vue-next'
import { getAdminOperationLogs } from '../api/adminOperationLogs'
import {
  formatOperationLogTime,
  normalizeAdminOperationLogPage,
  operationResultLabel,
  operationResultTagType,
  operationTypeLabel,
  OPERATION_RESULT_OPTIONS
} from '../utils/adminOperationLogs'

const PAGE_SIZE = 20
const resultOptions = OPERATION_RESULT_OPTIONS
const logs = ref([])
const total = ref(0)
const page = ref(1)
const keyword = ref('')
const resultFilter = ref('')
const loading = ref(false)
const errorMessage = ref('')

onMounted(loadLogs)

async function loadLogs() {
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await getAdminOperationLogs({
      keyword: keyword.value,
      result: resultFilter.value,
      limit: PAGE_SIZE,
      offset: (page.value - 1) * PAGE_SIZE
    })
    const result = normalizeAdminOperationLogPage(response.data.data)
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

function applyFilters() {
  page.value = 1
  loadLogs()
}

function changePage(nextPage) {
  page.value = nextPage
  loadLogs()
}

function resolveErrorMessage(error) {
  const message = error?.response?.data?.message || error?.message
  const messages = {
    Unauthorized: '登录状态无效，请重新登录',
    Forbidden: '当前账号没有管理员权限',
    'Invalid operation result': '操作结果筛选条件不合法',
    'Network Error': '网络连接失败，请检查后端服务'
  }
  return messages[message] || message || '操作日志加载失败'
}
</script>

<style scoped>
.operation-logs-workspace {
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
  box-shadow:
    var(--app-panel-shadow),
    inset 0 1px 0 var(--app-grid-line-strong);
}

.operation-logs-toolbar,
.operation-logs-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.operation-logs-toolbar p {
  margin: 0 0 4px;
  color: var(--app-text-muted);
  font-family: "Cascadia Mono", "SFMono-Regular", Consolas, monospace;
  font-size: 11px;
  font-weight: 800;
}

.operation-logs-toolbar h2 {
  margin: 0;
  color: var(--app-text);
  font-size: 18px;
}

.operation-logs-toolbar span,
.operation-logs-footer {
  color: var(--app-text-muted);
  font-size: 12px;
}

.operation-log-filters {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) 160px auto;
  gap: 10px;
}

.operation-logs-error {
  margin: 0;
}

.operation-logs-table {
  min-height: 0;
  overflow: hidden;
}

.operation-logs-table :deep(.el-table) {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: color-mix(in srgb, var(--app-surface-soft) 82%, transparent);
  --el-table-row-hover-bg-color: color-mix(in srgb, var(--app-accent) 8%, transparent);
  height: 100%;
}

.operation-logs-table :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.operation-logs-footer :deep(.el-pagination) {
  margin-left: auto;
}

@media (max-width: 720px) {
  .operation-logs-workspace {
    min-height: 560px;
    padding: 12px;
  }

  .operation-logs-toolbar {
    align-items: flex-start;
  }

  .operation-logs-toolbar span {
    display: block;
    max-width: 250px;
    line-height: 1.5;
  }

  .operation-log-filters {
    grid-template-columns: 1fr;
  }

  .operation-log-filters :deep(.el-button) {
    width: 100%;
  }

  .operation-logs-footer {
    align-items: flex-start;
    flex-direction: column;
  }

  .operation-logs-footer :deep(.el-pagination) {
    margin-left: 0;
  }
}
</style>
