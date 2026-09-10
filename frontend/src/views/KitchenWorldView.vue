<template>
  <main class="kitchen-world-page" aria-label="AI 智能厨房">
    <div v-if="!portraitHintDismissed" class="landscape-hint" role="status" aria-live="polite">
      <RotateCw :size="20" aria-hidden="true" />
      <div class="landscape-hint-copy">
        <strong>为了您的体验，请横屏使用</strong>
        <span>横屏后可以更完整地浏览厨房场景。</span>
      </div>
      <button type="button" @click="portraitHintDismissed = true">继续竖屏使用</button>
    </div>

    <section class="kitchen-world-stage" aria-label="厨房功能入口">
      <div class="kitchen-scene-viewport" aria-label="完整厨房场景将自动适配当前窗口">
        <Suspense>
          <KitchenScene :motion-paused="motionPaused" @select-station="openStation" />
          <template #fallback>
            <div class="kitchen-scene-loading" role="status">正在准备厨房……</div>
          </template>
        </Suspense>
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
import { RotateCw } from 'lucide-vue-next'
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
const portraitHintDismissed = ref(false)
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
  display: flex;
  width: 100%;
  height: 100%;
  min-width: 0;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
  padding: clamp(8px, 1.2vw, 16px);
  color: #3b2b21;
  background:
    linear-gradient(180deg, rgba(242, 226, 190, 0.18), transparent 180px),
    #f8efdc;
}

.kitchen-world-stage {
  display: grid;
  grid-template-rows: minmax(0, 1fr);
  width: 100%;
  min-height: 0;
  flex: 1 1 auto;
  min-width: 0;
  margin: 0 auto;
}

.kitchen-scene-viewport {
  display: grid;
  width: 100%;
  min-width: 0;
  min-height: 0;
  place-items: center;
  overflow: hidden;
}

.landscape-hint {
  display: none;
}

.kitchen-scene-loading {
  display: grid;
  width: 100%;
  height: 100%;
  min-height: 0;
  place-items: center;
  overflow: hidden;
  border: 1px solid #8b6e4e;
  color: #59432f;
  background: #e6d2a8;
  box-shadow: 0 16px 30px rgba(53, 35, 23, 0.2);
  font-weight: 800;
}

@media (max-width: 1023px) and (orientation: portrait) and (pointer: coarse) {
  .landscape-hint {
    display: flex;
    align-items: center;
    gap: 10px;
    margin: 0 auto 12px;
    max-width: 1520px;
    padding: 10px 12px;
    border: 1px solid #c8aa7b;
    color: #59432f;
    background: #fff4d6;
    box-shadow: 0 4px 12px rgba(53, 35, 23, 0.08);
  }

  .landscape-hint > svg {
    flex: 0 0 auto;
    color: #a36e2d;
  }

  .landscape-hint-copy {
    display: grid;
    min-width: 0;
    gap: 2px;
    flex: 1;
  }

  .landscape-hint-copy strong {
    font-size: 13px;
  }

  .landscape-hint-copy span {
    color: #8b765c;
    font-size: 11px;
    line-height: 1.45;
  }

  .landscape-hint button {
    min-height: 44px;
    padding: 0 10px;
    border: 1px solid #967044;
    color: #59432f;
    background: #fffdf5;
    font: inherit;
    font-size: 11px;
    font-weight: 800;
    cursor: pointer;
  }

  .landscape-hint button:hover,
  .landscape-hint button:focus-visible {
    border-color: #4f8ca5;
    background: #fff8e9;
    outline: 2px solid #4f8ca5;
    outline-offset: 2px;
  }
}

</style>
