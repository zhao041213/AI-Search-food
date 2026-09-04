<template>
  <el-dialog
    :model-value="modelValue"
    :title="station ? `${station.title} · ${station.role}` : '厨房功能'"
    width="min(920px, 92vw)"
    top="4vh"
    class="kitchen-station-dialog"
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div v-if="station?.id === 'chef'" class="recipe-workbench-host">
      <HomeView />
    </div>

    <div v-else-if="station" class="station-brief">
      <div class="station-brief-icon" :style="{ '--station-accent': station.accent }">{{ station.icon }}</div>
      <div>
        <p class="station-kicker">{{ station.role }}</p>
        <h2>{{ station.title }}</h2>
        <p class="station-description">{{ station.description }}</p>
      </div>
      <div class="station-preview-grid">
        <div v-for="item in station.preview" :key="item.label" class="station-preview-card">
          <span>{{ item.value }}</span>
          <strong>{{ item.label }}</strong>
        </div>
      </div>
      <div class="station-brief-actions">
        <el-button plain @click="emit('update:modelValue', false)">留在厨房</el-button>
        <el-button type="primary" @click="openStationRoute">进入{{ station.entryLabel }}</el-button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import HomeView from '../../views/HomeView.vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  stationId: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue'])
const router = useRouter()

const stationMap = {
  pantry: {
    id: 'pantry',
    title: '食材储藏室',
    role: '食材管家',
    icon: '▣',
    accent: '#68a873',
    description: '集中管理冰箱库存、保质期和烹饪消耗，生成菜谱时会自动参与推荐。',
    entryLabel: '我的食材',
    route: '/pantry',
    preview: [
      { value: '库存', label: '自动参与推荐' },
      { value: '临期', label: '及时提醒' },
      { value: '入库', label: '操作可追溯' }
    ]
  },
  recipes: {
    id: 'recipes',
    title: '菜谱书房',
    role: '菜谱管理员',
    icon: '▤',
    accent: '#b083c7',
    description: '重新打开已保存的 AI 菜谱，整理收藏夹、标签和分享记录。',
    entryLabel: '我的菜谱',
    route: '/recipes/saved',
    preview: [
      { value: '保存', label: '不重复调用模型' },
      { value: '标签', label: '方便分类查找' },
      { value: '分享', label: '生成公开链接' }
    ]
  },
  nutrition: {
    id: 'nutrition',
    title: '营养咨询室',
    role: '营养师',
    icon: '♥',
    accent: '#e2816c',
    description: '维护基础健康档案和每日营养目标，为菜谱搭配提供一般饮食参考。',
    entryLabel: '健康管理',
    route: '/health-profile',
    preview: [
      { value: 'BMI', label: '基础指标' },
      { value: '目标', label: '每日营养参考' },
      { value: 'AI', label: '参与菜谱建议' }
    ]
  },
  weekly: {
    id: 'weekly',
    title: '菜单计划室',
    role: '菜单规划师',
    icon: '▦',
    accent: '#6d9cc3',
    description: '从已保存菜谱中安排一周餐次，并自动汇总采购清单。',
    entryLabel: '一周菜单',
    route: '/weekly-menu',
    preview: [
      { value: '21', label: '个餐次位置' },
      { value: 'AI', label: '自动安排' },
      { value: '清单', label: '自动汇总' }
    ]
  },
  hot: {
    id: 'hot',
    title: '美食情报站',
    role: '市场观察员',
    icon: '♨',
    accent: '#d48c52',
    description: '观察全站食材搜索趋势，发现大家最近正在寻找什么。',
    entryLabel: '热门食材',
    route: '/stats/hot-ingredients',
    preview: [
      { value: '7天', label: '短期趋势' },
      { value: '30天', label: '长期趋势' },
      { value: '排行', label: '食材热度' }
    ]
  }
}

const station = computed(() => {
  if (props.stationId === 'chef') {
    return { id: 'chef', title: '主厨料理大厅', role: 'AI 主厨', icon: '✦' }
  }
  return stationMap[props.stationId] || null
})

function openStationRoute() {
  if (!station.value?.route) return
  emit('update:modelValue', false)
  router.push(station.value.route)
}
</script>

<style scoped>
:global(.kitchen-station-dialog.el-dialog) {
  overflow: hidden;
  border: 2px solid #8b6e4e;
  border-radius: 4px;
  background: #f2e5c7;
  box-shadow: 0 24px 70px rgba(45, 28, 17, 0.38);
}

:global(.kitchen-station-dialog .el-dialog__header) {
  margin-right: 0;
  padding: 14px 18px;
  border-bottom: 1px solid #c8aa7b;
  background: #ead7ae;
}

:global(.kitchen-station-dialog .el-dialog__title) {
  color: #3c2b20;
  font-weight: 900;
}

:global(.kitchen-station-dialog .el-dialog__body) {
  max-height: 78vh;
  padding: 0;
  overflow: auto;
  background: #f8f1e2;
}

.recipe-workbench-host {
  min-height: 520px;
  padding: 18px;
  background:
    linear-gradient(rgba(112, 84, 54, 0.07) 1px, transparent 1px),
    linear-gradient(90deg, rgba(112, 84, 54, 0.07) 1px, transparent 1px),
    #f8f1e2;
  background-size: 24px 24px;
}

.station-brief {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 16px 18px;
  padding: 34px;
  color: #3c2b20;
}

.station-brief-icon {
  display: grid;
  width: 58px;
  height: 58px;
  place-items: center;
  border: 3px solid var(--station-accent);
  color: var(--station-accent);
  background: #221c19;
  font-size: 28px;
  font-weight: 900;
  image-rendering: pixelated;
}

.station-kicker {
  margin: 3px 0 4px;
  color: #966f43;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.12em;
}

.station-brief h2 {
  margin: 0;
  font-size: 24px;
}

.station-description {
  grid-column: 1 / -1;
  margin: 0;
  color: #745b43;
  line-height: 1.7;
}

.station-preview-grid {
  display: grid;
  grid-column: 1 / -1;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.station-preview-card {
  display: grid;
  gap: 5px;
  min-height: 78px;
  padding: 13px;
  border: 1px solid #c8aa7b;
  background: #fffaf0;
}

.station-preview-card span {
  color: #a4763f;
  font-size: 20px;
  font-weight: 900;
}

.station-preview-card strong {
  color: #6a4f37;
  font-size: 12px;
}

.station-brief-actions {
  display: flex;
  grid-column: 1 / -1;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 4px;
}

@media (max-width: 620px) {
  .station-preview-grid {
    grid-template-columns: 1fr;
  }
}
</style>
