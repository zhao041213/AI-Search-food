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
      <div class="kitchen-scene-viewport" tabindex="0" aria-label="可左右滑动查看完整厨房场景">
        <Suspense>
          <KitchenScene :motion-paused="motionPaused" @select-station="openStation" />
          <template #fallback>
            <div class="kitchen-scene-loading" role="status">正在准备厨房……</div>
          </template>
        </Suspense>
      </div>
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

.kitchen-scene-viewport {
  width: 100%;
}

.landscape-hint {
  display: none;
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

@media (max-width: 1023px) {
  .kitchen-world-page {
    min-height: calc(100vh - 126px);
    min-height: calc(100dvh - 126px);
    padding: 12px 10px 24px;
  }

  .kitchen-scene-viewport {
    max-width: 100%;
    overflow-x: auto;
    overflow-y: hidden;
    overscroll-behavior-x: contain;
    scrollbar-width: thin;
    touch-action: pan-x;
  }

  .kitchen-scene-viewport :deep(.kitchen-scene),
  .kitchen-scene-loading {
    width: 1080px;
    min-width: 1080px;
  }
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

@media (max-width: 720px) {
  .scene-caption {
    flex-wrap: wrap;
    align-items: center;
    padding: 8px 10px;
  }

  .scene-caption > span:nth-child(2) {
    order: 3;
    flex: 1 1 100%;
  }

  .motion-toggle {
    margin-left: auto;
  }
}

</style>
