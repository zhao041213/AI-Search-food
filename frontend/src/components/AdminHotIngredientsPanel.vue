<template>
  <section
    class="stats-workspace"
    :class="{ 'has-error': errorMessage }"
    aria-labelledby="admin-hot-title"
  >
    <header class="stats-toolbar">
      <div>
        <p>全站搜索数据</p>
        <h2 id="admin-hot-title">热门食材分析</h2>
      </div>
      <div class="toolbar-actions">
        <el-radio-group v-model="period" size="small" aria-label="统计周期" @change="loadStats">
          <el-radio-button v-for="option in periodOptions" :key="option.value" :value="option.value">
            {{ option.label }}
          </el-radio-button>
        </el-radio-group>
        <el-button :loading="loading" circle aria-label="刷新统计" title="刷新统计" @click="loadStats">
          <RefreshCw :size="16" aria-hidden="true" />
        </el-button>
      </div>
    </header>

    <el-alert
      v-if="errorMessage"
      class="stats-error"
      type="error"
      :title="errorMessage"
      show-icon
      :closable="false"
    />

    <dl class="metrics-strip">
      <div>
        <dt>有效搜索</dt>
        <dd>{{ stats.totalSearches }}</dd>
      </div>
      <div>
        <dt>食材记录</dt>
        <dd>{{ stats.totalIngredientOccurrences }}</dd>
      </div>
      <div>
        <dt>上榜食材</dt>
        <dd>{{ stats.items.length }}</dd>
      </div>
      <div>
        <dt>更新时间</dt>
        <dd class="metric-time">{{ formatDate(stats.generatedAt) }}</dd>
      </div>
    </dl>

    <div class="analysis-grid">
      <section class="chart-panel" v-loading="loading" aria-label="热门食材搜索次数图表">
        <div ref="chartElement" class="chart-canvas" />
        <el-empty v-if="!loading && !stats.items.length" class="chart-empty" description="暂无统计数据" />
      </section>

      <section class="table-panel" aria-label="热门食材统计明细">
        <el-table :data="stats.items" height="100%" size="small" empty-text="暂无统计数据">
          <el-table-column prop="rank" label="排名" width="66" align="center" />
          <el-table-column prop="name" label="食材" min-width="104" show-overflow-tooltip />
          <el-table-column prop="searchCount" label="搜索次数" width="94" align="right" />
          <el-table-column label="占比" width="82" align="right">
            <template #default="scope">{{ formatShare(scope.row.share) }}</template>
          </el-table-column>
          <el-table-column label="最近搜索" min-width="150">
            <template #default="scope">{{ formatDate(scope.row.latestSearchAt) }}</template>
          </el-table-column>
        </el-table>
      </section>
    </div>
  </section>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { init, use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { RefreshCw } from 'lucide-vue-next'
import { getHotIngredients } from '../api/stats'

use([BarChart, GridComponent, TooltipComponent, CanvasRenderer])

const chartElement = ref(null)
const loading = ref(false)
const errorMessage = ref('')
const period = ref('all')
const stats = ref(emptyStats())
const periodOptions = [
  { value: 'all', label: '全部' },
  { value: '7d', label: '近 7 天' },
  { value: '30d', label: '近 30 天' }
]

let chart
let resizeObserver
let themeObserver

onMounted(async () => {
  await loadStats()
  setupObservers()
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  themeObserver?.disconnect()
  chart?.dispose()
})

async function loadStats() {
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await getHotIngredients(period.value, 20)
    stats.value = response.data.data || emptyStats()
  } catch (error) {
    errorMessage.value = error?.response?.data?.message || error?.message || '热门食材统计加载失败'
    stats.value = emptyStats()
  } finally {
    loading.value = false
    await nextTick()
    renderChart()
  }
}

function renderChart() {
  if (!chartElement.value) {
    return
  }
  chart ||= init(chartElement.value)
  const items = stats.value.items.slice(0, 10).reverse()
  const styles = getComputedStyle(document.documentElement)
  const textColor = styles.getPropertyValue('--app-text-muted').trim() || '#64748b'
  const lineColor = styles.getPropertyValue('--app-line').trim() || '#d6dee8'
  const accentColor = styles.getPropertyValue('--app-accent').trim() || '#3b82f6'

  chart.setOption({
    animationDuration: 360,
    grid: { top: 12, right: 26, bottom: 24, left: 72, containLabel: false },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => `${params[0].name}<br/>${params[0].value} 次搜索`
    },
    xAxis: {
      type: 'value',
      minInterval: 1,
      axisLabel: { color: textColor },
      splitLine: { lineStyle: { color: lineColor } }
    },
    yAxis: {
      type: 'category',
      data: items.map((item) => item.name),
      axisLabel: { color: textColor, width: 58, overflow: 'truncate' },
      axisLine: { lineStyle: { color: lineColor } },
      axisTick: { show: false }
    },
    series: [{
      type: 'bar',
      data: items.map((item) => item.searchCount),
      barMaxWidth: 18,
      itemStyle: { color: accentColor, borderRadius: [0, 3, 3, 0] },
      label: { show: true, position: 'right', color: textColor, fontWeight: 700 }
    }]
  }, true)
}

function setupObservers() {
  resizeObserver = new ResizeObserver(() => chart?.resize())
  resizeObserver.observe(chartElement.value)
  themeObserver = new MutationObserver(renderChart)
  themeObserver.observe(document.body, { attributes: true, attributeFilter: ['data-theme'] })
}

function formatShare(value) {
  return `${((Number(value) || 0) * 100).toFixed(1)}%`
}

function formatDate(value) {
  if (!value) {
    return '暂无'
  }
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  }).format(new Date(value))
}

function emptyStats() {
  return {
    totalSearches: 0,
    totalIngredientOccurrences: 0,
    generatedAt: null,
    items: []
  }
}
</script>

<style scoped>
.stats-workspace {
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  gap: 12px;
  min-height: 0;
  height: 100%;
}

.stats-workspace.has-error {
  grid-template-rows: auto auto auto minmax(0, 1fr);
}

.stats-toolbar,
.toolbar-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.stats-toolbar p,
.stats-toolbar h2 {
  margin: 0;
}

.stats-toolbar p {
  margin-bottom: 3px;
  color: var(--app-text-muted);
  font-size: 11px;
  font-weight: 800;
}

.stats-toolbar h2 {
  color: var(--app-text);
  font-size: 20px;
}

.stats-error {
  margin: -2px 0;
}

.metrics-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin: 0;
  overflow: hidden;
  border: 1px solid var(--app-line);
  border-radius: 8px;
  background: var(--app-surface);
}

.metrics-strip div {
  display: grid;
  gap: 6px;
  min-width: 0;
  padding: 11px 14px;
  border-right: 1px solid var(--app-line);
}

.metrics-strip div:last-child {
  border-right: 0;
}

.metrics-strip dt {
  color: var(--app-text-muted);
  font-size: 11px;
  font-weight: 800;
}

.metrics-strip dd {
  margin: 0;
  overflow: hidden;
  color: var(--app-text);
  font-size: 22px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.metrics-strip .metric-time {
  font-size: 15px;
}

.analysis-grid {
  display: grid;
  grid-template-columns: minmax(340px, 0.8fr) minmax(520px, 1.2fr);
  gap: 12px;
  min-height: 0;
}

.chart-panel,
.table-panel {
  position: relative;
  min-height: 0;
  overflow: hidden;
  border: 1px solid var(--app-line);
  border-radius: 8px;
  background: var(--app-surface);
}

.chart-canvas {
  width: 100%;
  height: 100%;
  min-height: 280px;
}

.chart-empty {
  position: absolute;
  inset: 0;
  background: var(--app-surface);
}

.table-panel :deep(.el-table) {
  width: 100%;
}

@media (max-width: 1050px) {
  .stats-workspace {
    height: auto;
  }

  .analysis-grid {
    grid-template-columns: 1fr;
  }

  .chart-panel {
    min-height: 360px;
  }

  .table-panel {
    height: 440px;
  }
}

@media (max-width: 650px) {
  .stats-toolbar,
  .toolbar-actions {
    align-items: flex-start;
    flex-direction: column;
  }

  .metrics-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .metrics-strip div:nth-child(2) {
    border-right: 0;
  }

  .metrics-strip div:nth-child(-n + 2) {
    border-bottom: 1px solid var(--app-line);
  }
}
</style>
