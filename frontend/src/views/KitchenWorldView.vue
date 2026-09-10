<template>
  <main class="kitchen-world-page" aria-label="AI 智能厨房">
    <section class="kitchen-world-stage" aria-label="厨房功能入口">
      <Suspense>
        <KitchenScene :motion-paused="motionPaused" @select-station="openStation" />
        <template #fallback>
          <div class="kitchen-scene-loading" role="status">正在准备厨房……</div>
        </template>
      </Suspense>
      <div class="scene-caption">
        <span class="caption-key">操作提示</span>
        <span>看铭牌辨功能 · 点击人物打开窗口</span>
        <button
          type="button"
          class="motion-toggle"
          :aria-pressed="motionPaused"
          @click="motionPaused = !motionPaused"
        >
          {{ motionPaused ? '继续动态' : '暂停动态' }}
        </button>
      </div>
    </section>

    <KitchenStationPanel
      :model-value="stationPanelVisible"
      :feature-id="activeFeatureId"
      :station-id="activeStationId"
      @update:model-value="handlePanelVisibility"
    />
  </main>
</template>

<script setup>
import { computed, defineAsyncComponent, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import KitchenStationPanel from '../components/kitchen/KitchenStationPanel.vue'
import { useKitchenStore } from '../stores/kitchen'
import {
  kitchenStationLocation,
  kitchenWorldLocation,
  parseKitchenFeature,
  parseKitchenStation
} from '../utils/kitchenFeatures'

const KitchenScene = defineAsyncComponent(() => import('../components/kitchen/KitchenScene.vue'))
const route = useRoute()
const router = useRouter()
const motionPaused = ref(false)
const kitchen = useKitchenStore()
const activeFeatureId = computed(() => parseKitchenFeature(route.query))
const activeStationId = computed(() => parseKitchenStation(route.query))
const stationPanelVisible = computed(() => Boolean(activeFeatureId.value || activeStationId.value))

watch(
  () => kitchen.requestedStation,
  (stationId) => {
    if (!stationId) return
    openStation(kitchen.consumeRequestedStation())
  },
  { immediate: true }
)

function openStation(stationId) {
  router.push(kitchenStationLocation(stationId, route.query))
}

function handlePanelVisibility(visible) {
  if (!visible) {
    router.push(kitchenWorldLocation(route.query))
  }
}
</script>

<style scoped>
.kitchen-world-page {
  min-height: calc(100vh - 58px);
  padding: 22px clamp(16px, 2.6vw, 34px) 42px;
  color: #3b2b21;
  background:
    linear-gradient(180deg, rgba(242, 226, 190, 0.18), transparent 180px),
    #f8efdc;
}

.kitchen-world-stage {
  max-width: 1520px;
  margin: 0 auto;
}

.kitchen-scene-loading {
  display: grid;
  min-height: 760px;
  place-items: center;
  overflow: hidden;
  border: 1px solid #8b6e4e;
  color: #59432f;
  background: #e6d2a8;
  box-shadow: 0 16px 30px rgba(53, 35, 23, 0.2);
  font-weight: 800;
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

.motion-toggle {
  min-height: 40px;
  margin-left: auto;
  padding: 3px 10px;
  border: 1px solid #967044;
  border-radius: 3px;
  color: #4a3627;
  background: #f5e6c3;
  font: inherit;
  font-size: 11px;
  font-weight: 900;
  cursor: pointer;
}

.motion-toggle:hover {
  background: #fff3d8;
}

.motion-toggle:focus-visible {
  outline: 2px solid #4f8ca5;
  outline-offset: 2px;
}

</style>
