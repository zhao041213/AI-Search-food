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
      <div class="station-entry-list" aria-label="功能入口">
        <button
          v-for="entry in station.entries"
          :key="entry.label"
          type="button"
          class="station-entry-card"
          :disabled="preferenceLoading && entry.action === 'diet-preference'"
          @click="openStationEntry(entry)"
        >
          <span class="station-entry-mark" aria-hidden="true">{{ entry.icon || '→' }}</span>
          <span class="station-entry-copy">
            <strong>{{ entry.label }}</strong>
            <small>{{ entry.description }}</small>
          </span>
          <span class="station-entry-arrow" aria-hidden="true">›</span>
        </button>
      </div>
      <div class="station-brief-actions">
        <el-button plain @click="emit('update:modelValue', false)">留在厨房</el-button>
      </div>
    </div>
  </el-dialog>

  <DietPreferenceDialog
    v-if="auth.isUser"
    v-model="preferenceDialogVisible"
    :preference="dietPreference"
    :saving="preferenceSaving"
    @save="persistDietPreference"
  />
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { getDietPreference, saveDietPreference } from '../../api/userPreferences'
import { useAuthStore } from '../../stores/auth'
import { normalizeDietPreference } from '../../utils/personalization'
import DietPreferenceDialog from '../DietPreferenceDialog.vue'
import HomeView from '../../views/HomeView.vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  stationId: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue'])
const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const dietPreference = ref(normalizeDietPreference())
const preferenceDialogVisible = ref(false)
const preferenceLoading = ref(false)
const preferenceSaving = ref(false)

const stationMap = {
  pantry: {
    id: 'pantry',
    title: '食材储藏室',
    role: '食材管家',
    icon: '▣',
    accent: '#68a873',
    description: '集中管理冰箱库存、保质期和烹饪消耗，生成菜谱时会自动参与推荐。',
    entries: [
      {
        label: '管理食材库存',
        description: '查看库存、临期提醒、入库、消耗和撤销记录。',
        route: '/pantry',
        icon: '▣'
      }
    ],
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
    entries: [
      {
        label: '打开我的菜谱',
        description: '查看已保存菜谱、收藏夹、标签和分享记录。',
        route: '/recipes/saved',
        icon: '▤'
      }
    ],
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
    entries: [
      {
        label: '健康档案',
        description: '维护身高、体重、年龄和基础身体指标。',
        route: '/health-profile',
        icon: '♥'
      },
      {
        label: '营养目标',
        description: '设置每日热量、蛋白质、碳水和脂肪目标。',
        route: '/nutrition-targets',
        icon: '◎'
      },
      {
        label: '饮食偏好',
        description: '设置默认目标、口味、忌口和过敏食材。',
        action: 'diet-preference',
        icon: '◇'
      }
    ],
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
    entries: [
      {
        label: '安排一周菜单',
        description: '编排每日餐次，并汇总需要采购的食材。',
        route: '/weekly-menu',
        icon: '▦'
      }
    ],
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
    entries: [
      {
        label: '查看热门食材',
        description: '查看 7 天和 30 天的食材搜索趋势。',
        route: '/stats/hot-ingredients',
        icon: '♨'
      }
    ],
    preview: [
      { value: '7天', label: '短期趋势' },
      { value: '30天', label: '长期趋势' },
      { value: '排行', label: '食材热度' }
    ]
  },
  review: {
    id: 'review',
    title: '成品品鉴台',
    role: '成品品鉴员',
    icon: '◆',
    accent: '#c87a8a',
    description: '从已保存菜谱进入成品评价，上传成品图并获取 AI 品鉴建议。',
    entries: [
      {
        label: '选择菜谱开始品鉴',
        description: '打开我的菜谱，选择菜谱后上传成品照片。',
        route: '/recipes/saved',
        icon: '◆'
      }
    ],
    preview: [
      { value: '图片', label: '上传成品' },
      { value: 'AI', label: '品鉴建议' },
      { value: '记录', label: '历史可查' }
    ]
  },
  account: {
    id: 'account',
    title: '厨房服务台',
    role: '厨房管家',
    icon: '◈',
    accent: '#8f8aa8',
    description: '集中处理个人资料、头像、账号安全和厨房通知。',
    entries: [
      {
        label: '账号中心',
        description: '维护昵称、头像和账号安全设置。',
        route: '/account',
        icon: '◈'
      },
      {
        label: '通知中心',
        description: '查看库存提醒和系统消息，调整通知偏好。',
        route: '/notifications',
        icon: '●'
      }
    ],
    preview: [
      { value: '资料', label: '个人信息' },
      { value: '通知', label: '及时提醒' },
      { value: '安全', label: '账号管理' }
    ]
  }
}

const station = computed(() => {
  if (props.stationId === 'chef') {
    return { id: 'chef', title: '主厨料理大厅', role: 'AI 主厨', icon: '✦' }
  }
  return stationMap[props.stationId] || null
})

async function openStationEntry(entry) {
  if (entry.action === 'diet-preference') {
    await openDietPreference()
    return
  }
  if (!entry.route) return
  emit('update:modelValue', false)
  router.push(entry.route)
}

async function openDietPreference() {
  if (!auth.isUser) {
    emit('update:modelValue', false)
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }

  if (preferenceLoading.value) return
  preferenceLoading.value = true
  try {
    const response = await getDietPreference()
    dietPreference.value = normalizeDietPreference(response.data.data)
    emit('update:modelValue', false)
    preferenceDialogVisible.value = true
  } catch (error) {
    handleAuthorizedError(error, '饮食偏好加载失败，请稍后重试')
  } finally {
    preferenceLoading.value = false
  }
}

async function persistDietPreference(value) {
  if (!auth.isUser || preferenceSaving.value) return
  preferenceSaving.value = true
  try {
    const response = await saveDietPreference(normalizeDietPreference(value))
    dietPreference.value = normalizeDietPreference(response.data.data)
    preferenceDialogVisible.value = false
    ElMessage.success('饮食偏好已保存')
  } catch (error) {
    handleAuthorizedError(error, '饮食偏好保存失败，请稍后重试')
  } finally {
    preferenceSaving.value = false
  }
}

function handleAuthorizedError(error, fallback) {
  if (error?.response?.status === 401) {
    preferenceDialogVisible.value = false
    emit('update:modelValue', false)
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  ElMessage.error(getErrorMessage(error, fallback))
}

function getErrorMessage(error, fallback) {
  return error?.response?.data?.message || fallback
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

.station-entry-list {
  display: grid;
  grid-column: 1 / -1;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.station-entry-card {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  min-height: 68px;
  padding: 10px 12px;
  border: 1px solid #c8aa7b;
  color: #3c2b20;
  background: #fffaf0;
  text-align: left;
  cursor: pointer;
  transition: border-color 160ms ease, background-color 160ms ease, transform 160ms ease;
}

.station-entry-card:hover,
.station-entry-card:focus-visible {
  border-color: var(--app-accent);
  background: #fff4d9;
  outline: none;
  transform: translateY(-1px);
}

.station-entry-card:disabled {
  cursor: wait;
  opacity: 0.68;
  transform: none;
}

.station-entry-mark {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border: 1px solid #9e7b50;
  color: #f4d37f;
  background: #2b211d;
  font-weight: 900;
}

.station-entry-copy {
  display: grid;
  gap: 4px;
}

.station-entry-copy strong {
  font-size: 13px;
}

.station-entry-copy small {
  color: #80664a;
  font-size: 11px;
  line-height: 1.45;
}

.station-entry-arrow {
  color: #9e7444;
  font-size: 24px;
  font-weight: 900;
}

.station-brief-actions {
  display: flex;
  grid-column: 1 / -1;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 4px;
}

@media (max-width: 620px) {
  .station-preview-grid,
  .station-entry-list {
    grid-template-columns: 1fr;
  }
}
</style>
