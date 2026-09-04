<template>
  <main class="kitchen-world-page" aria-labelledby="kitchen-world-title">
    <header class="kitchen-world-heading">
      <h1 id="kitchen-world-title" class="sr-only">AI 智能厨房</h1>
      <div class="kitchen-heading-spacer" aria-hidden="true"></div>
      <div class="kitchen-world-status" aria-label="厨房状态">
        <span><i class="status-dot status-dot-live" />厨房营业中</span>
        <span>点击人物开始</span>
      </div>
    </header>

    <section class="kitchen-world-stage" aria-label="厨房功能入口">
      <KitchenScene @select-station="openStation" />
      <div class="scene-caption">
        <span class="caption-key">操作提示</span>
        <span>悬停查看岗位 · 点击人物打开功能</span>
      </div>
    </section>

    <section class="station-guide" aria-label="功能入口速查">
      <button
        v-for="item in guideItems"
        :key="item.id"
        type="button"
        class="station-guide-item"
        :style="{ '--guide-accent': item.accent }"
        @click="openStation(item.id)"
      >
        <span class="guide-icon">{{ item.icon }}</span>
        <span>
          <strong>{{ item.role }}</strong>
          <small>{{ item.title }}</small>
        </span>
      </button>
    </section>

    <KitchenStationPanel v-model="stationPanelVisible" :station-id="activeStation" />
  </main>
</template>

<script setup>
import { ref } from 'vue'
import KitchenScene from '../components/kitchen/KitchenScene.vue'
import KitchenStationPanel from '../components/kitchen/KitchenStationPanel.vue'

const guideItems = [
  { id: 'chef', title: '主厨料理大厅', role: 'AI 主厨', icon: '✦', accent: '#d6a43b' },
  { id: 'pantry', title: '食材储藏室', role: '食材管家', icon: '▣', accent: '#68a873' },
  { id: 'recipes', title: '菜谱书房', role: '菜谱管理员', icon: '▤', accent: '#b083c7' },
  { id: 'nutrition', title: '营养咨询室', role: '营养师', icon: '♥', accent: '#e2816c' },
  { id: 'weekly', title: '菜单计划室', role: '菜单规划师', icon: '▦', accent: '#6d9cc3' },
  { id: 'hot', title: '美食情报站', role: '市场观察员', icon: '♨', accent: '#d48c52' }
]

const activeStation = ref('')
const stationPanelVisible = ref(false)

function openStation(stationId) {
  activeStation.value = stationId
  stationPanelVisible.value = true
}
</script>

<style scoped>
.kitchen-world-page {
  min-height: calc(100vh - 58px);
  padding: 22px clamp(16px, 2.6vw, 34px) 42px;
  color: #3b2b21;
  background: linear-gradient(180deg, rgba(242, 226, 190, 0.18), transparent 180px);
}

.kitchen-world-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  min-height: 28px;
  max-width: 1520px;
  margin: 0 auto 10px;
}

.kitchen-heading-spacer {
  min-height: 24px;
  flex: 1;
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.kitchen-world-status {
  display: flex;
  align-items: center;
  gap: 14px;
  padding-bottom: 5px;
  color: #806345;
  font-size: 12px;
  font-weight: 800;
}

.kitchen-world-status span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.status-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-dot-live {
  background: #5c9d63;
  box-shadow: 0 0 0 4px rgba(92, 157, 99, 0.16);
}

.kitchen-world-stage {
  max-width: 1520px;
  margin: 0 auto;
}

.scene-caption {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 34px;
  padding: 0 12px;
  border: 1px solid #c8aa7b;
  border-top: 0;
  color: #7b6044;
  background: #ead7ae;
  font-size: 12px;
  font-weight: 700;
}

.caption-key {
  color: #4a3627;
  font-weight: 900;
  letter-spacing: 0.08em;
}

.station-guide {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 8px;
  max-width: 1520px;
  margin: 14px auto 0;
}

.station-guide-item {
  display: flex;
  align-items: center;
  gap: 9px;
  min-width: 0;
  padding: 9px 10px;
  border: 1px solid #c8aa7b;
  border-left: 4px solid var(--guide-accent);
  color: #4b3728;
  background: #f8efdc;
  text-align: left;
  cursor: pointer;
  transition: transform 160ms ease, background-color 160ms ease, border-color 160ms ease;
}

.station-guide-item:hover,
.station-guide-item:focus-visible {
  border-color: var(--guide-accent);
  background: #fff8e9;
  outline: none;
  transform: translateY(-2px);
}

.guide-icon {
  display: grid;
  width: 28px;
  height: 28px;
  flex: 0 0 28px;
  place-items: center;
  border: 1px solid var(--guide-accent);
  color: var(--guide-accent);
  background: #2b211d;
  font-size: 14px;
  font-weight: 900;
}

.station-guide-item span:last-child {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.station-guide-item strong,
.station-guide-item small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.station-guide-item strong {
  font-size: 12px;
}

.station-guide-item small {
  color: #917653;
  font-size: 10px;
}

@media (max-width: 1100px) {
  .station-guide {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .kitchen-world-status {
    padding-bottom: 0;
  }

  .station-guide {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
