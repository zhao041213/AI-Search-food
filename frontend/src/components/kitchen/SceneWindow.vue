<template>
  <el-dialog
    :model-value="modelValue"
    :width="width"
    top="4vh"
    class="scene-window"
    modal-class="scene-window-overlay"
    :show-close="false"
    :close-on-click-modal="false"
    destroy-on-close
    :style="{ '--scene-window-accent': accent }"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <template #header="{ close, titleId, titleClass }">
      <div class="scene-window__titlebar">
        <span class="scene-window__title-icon" aria-hidden="true">{{ icon }}</span>
        <div class="scene-window__title-copy">
          <strong :id="titleId" :class="titleClass">{{ title }}</strong>
          <span v-if="subtitle">{{ subtitle }}</span>
        </div>
        <span class="scene-window__status" aria-hidden="true">
          <i /> 场景窗口
        </span>
        <button type="button" class="scene-window__close" aria-label="关闭窗口" @click="close">
          ×
        </button>
      </div>
    </template>

    <div class="scene-window__content" :class="contentClass">
      <slot />
    </div>
  </el-dialog>
</template>

<script setup>
defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, default: '厨房功能' },
  subtitle: { type: String, default: '' },
  icon: { type: String, default: '✦' },
  accent: { type: String, default: '#d6a43b' },
  width: { type: String, default: 'min(1040px, calc(100vw - 64px))' },
  contentClass: { type: [String, Array, Object], default: '' }
})

const emit = defineEmits(['update:modelValue'])
</script>

<style scoped>
:global(.scene-window-overlay) {
  background: rgba(24, 18, 16, 0.54);
  backdrop-filter: blur(1.5px);
}

:global(.scene-window.el-dialog) {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  height: min(760px, calc(100vh - 88px));
  margin-bottom: 0;
  overflow: hidden;
  border: 2px solid #8b6e4e;
  border-radius: 4px;
  background: #f2e5c7;
  box-shadow:
    0 0 0 4px rgba(42, 31, 25, 0.78),
    0 0 0 6px rgba(214, 164, 59, 0.42),
    0 26px 72px rgba(29, 19, 14, 0.5);
}

:global(.scene-window .el-dialog__header) {
  margin: 0;
  padding: 0;
}

:global(.scene-window .el-dialog__body) {
  min-height: 0;
  padding: 0;
  overflow: hidden;
  background: #f8f1e2;
}

.scene-window__titlebar {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 10px;
  min-height: 54px;
  padding: 7px 9px 7px 12px;
  border-bottom: 2px solid #8b6e4e;
  color: #3c2b20;
  background:
    linear-gradient(rgba(255, 255, 255, 0.16) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.12) 1px, transparent 1px),
    #ead7ae;
  background-size: 8px 8px;
}

.scene-window__title-icon,
.scene-window__close {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border: 2px solid #614734;
  border-radius: 3px;
  color: #f8e6a8;
  background: #2b211d;
  box-shadow: 2px 2px 0 rgba(139, 110, 78, 0.58);
}

.scene-window__title-icon {
  color: var(--scene-window-accent);
  font-size: 17px;
  font-weight: 900;
}

.scene-window__title-copy {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.scene-window__title-copy strong {
  overflow: hidden;
  color: #3c2b20;
  font-family: "Microsoft YaHei", "PingFang SC", sans-serif;
  font-size: 16px;
  font-weight: 900;
  letter-spacing: 0.03em;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.scene-window__title-copy span,
.scene-window__status {
  color: #80664a;
  font-family: "Cascadia Mono", Consolas, monospace;
  font-size: 10px;
  font-weight: 800;
}

.scene-window__status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 8px;
  border: 1px solid #b49467;
  background: rgba(255, 250, 240, 0.55);
}

.scene-window__status i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #68a873;
  box-shadow: 0 0 0 3px rgba(104, 168, 115, 0.18);
}

.scene-window__close {
  padding: 0;
  color: #ead7ae;
  font: inherit;
  font-size: 24px;
  font-weight: 900;
  line-height: 1;
  cursor: pointer;
  transition: color 140ms ease, background-color 140ms ease, transform 140ms ease;
}

.scene-window__close:hover,
.scene-window__close:focus-visible {
  color: #2b211d;
  background: var(--scene-window-accent);
  outline: none;
  transform: translateY(-1px);
}

.scene-window__content {
  height: 100%;
  min-height: 0;
  overflow: auto;
  overscroll-behavior: contain;
  container-name: scene-window-content;
  container-type: inline-size;
  scrollbar-color: #9e7b50 #eadfc9;
  scrollbar-width: thin;
}

.scene-window__content.scene-window-content--workbench {
  overflow: hidden;
}

@media (prefers-reduced-motion: reduce) {
  .scene-window__close {
    transition: none;
  }
}

@media (max-width: 720px) {
  :global(.scene-window.el-dialog) {
    width: calc(100vw - 24px) !important;
    height: calc(100vh - 48px);
  }

  .scene-window__status {
    display: none;
  }
}
</style>
