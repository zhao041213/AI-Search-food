<template>
  <el-dialog
    class="camera-ingredient-dialog"
    :model-value="modelValue"
    width="min(680px, calc(100vw - 28px))"
    :close-on-click-modal="false"
    :close-on-press-escape="true"
    :show-close="true"
    destroy-on-close
    @open="handleDialogOpened"
    @update:model-value="handleDialogVisibility"
    @closed="handleDialogClosed"
  >
    <template #header>
      <div class="camera-dialog-heading">
        <span class="camera-heading-icon" aria-hidden="true">
          <ScanLine :size="18" :stroke-width="1.8" />
        </span>
        <h2 id="camera-capture-title">拍照识别</h2>
      </div>
    </template>

    <section class="camera-capture-panel" aria-labelledby="camera-capture-title">
      <p class="camera-sr-only" aria-live="polite">{{ statusMessage }}</p>

      <el-alert
        v-if="cameraError"
        class="camera-error"
        :title="cameraError"
        type="error"
        :closable="false"
        show-icon
      />

      <div
        class="camera-stage"
        :class="{
          'camera-stage--previewing': isPreviewing,
          'camera-stage--captured': hasCapturedImage
        }"
      >
        <video
          v-show="isPreviewing"
          ref="videoElement"
          class="camera-video"
          autoplay
          muted
          playsinline
          aria-label="摄像头预览"
        />

        <img v-if="previewUrl" class="camera-image-preview" :src="previewUrl" alt="已拍摄的食材照片" />

        <div v-if="isStartingCamera" class="camera-stage-state" aria-hidden="true">
          <LoaderCircle class="camera-spinner" :size="28" :stroke-width="1.8" />
          <span>正在连接摄像头</span>
        </div>

        <div v-else-if="!isPreviewing && !hasCapturedImage" class="camera-stage-state" aria-hidden="true">
          <Camera :size="32" :stroke-width="1.5" />
          <span>准备拍摄食材</span>
        </div>

        <span v-if="isPreviewing" class="camera-live-status">拍摄中</span>
      </div>

      <canvas ref="canvasElement" class="camera-canvas" aria-hidden="true" />

      <div class="camera-action-bar" aria-label="拍照操作">
        <el-button
          v-if="!isPreviewing && !hasCapturedImage"
          class="camera-action-button"
          type="primary"
          :loading="isStartingCamera"
          :disabled="isStartingCamera"
          @click="openCamera"
        >
          <Camera :size="16" aria-hidden="true" />
          打开摄像头
        </el-button>

        <el-button
          v-if="isPreviewing"
          class="camera-action-button"
          type="primary"
          :disabled="isCapturingPhoto"
          @click="captureImage"
        >
          <Aperture :size="16" aria-hidden="true" />
          {{ isCapturingPhoto ? '正在拍摄' : '拍摄' }}
        </el-button>

        <el-button
          v-if="hasCapturedImage"
          class="camera-action-button"
          :disabled="isStartingCamera"
          @click="retryCamera"
        >
          <ImagePlus :size="16" aria-hidden="true" />
          重新拍摄
        </el-button>
      </div>
    </section>

    <template #footer>
      <div class="camera-dialog-footer">
        <el-button class="camera-footer-button" @click="requestClose('dismissed')">
          <X :size="16" aria-hidden="true" />
          关闭
        </el-button>
        <el-button
          v-if="hasCapturedImage"
          class="camera-footer-button"
          type="primary"
          @click="confirmCapture"
        >
          <Check :size="16" aria-hidden="true" />
          确认使用
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { Aperture, Camera, Check, ImagePlus, LoaderCircle, ScanLine, X } from 'lucide-vue-next'
import {
  CAMERA_CAPTURE_MIME_TYPE,
  createCameraImageFile,
  describeCameraSupport,
  getCameraErrorMessage
} from '../utils/cameraCapture'

const props = defineProps({
  modelValue: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'captured', 'close', 'error'])

const videoElement = ref(null)
const canvasElement = ref(null)
const activeStream = ref(null)
const capturedFile = ref(null)
const previewUrl = ref('')
const cameraError = ref('')
const isStartingCamera = ref(false)
const isCapturingPhoto = ref(false)
const closeReason = ref('dismissed')

let cameraRequestId = 0

const hasCapturedImage = computed(() => Boolean(capturedFile.value && previewUrl.value))
const isPreviewing = computed(() => Boolean(activeStream.value))
const statusMessage = computed(() => {
  if (cameraError.value) {
    return cameraError.value
  }
  if (isStartingCamera.value) {
    return '正在连接摄像头'
  }
  if (hasCapturedImage.value) {
    return '已拍摄照片，可确认使用或重新拍摄'
  }
  if (isPreviewing.value) {
    return '摄像头已开启，可拍摄食材'
  }
  return '尚未打开摄像头'
})

watch(
  () => props.modelValue,
  (visible) => {
    if (!visible) {
      stopCamera()
    }
  }
)

onBeforeUnmount(() => {
  stopCamera()
  clearCapturedImage()
})

function getBrowserContext() {
  return typeof window === 'undefined' ? {} : window
}

function stopMediaStream(stream) {
  stream?.getTracks?.().forEach((track) => track.stop())
}

function stopCamera() {
  cameraRequestId += 1
  stopMediaStream(activeStream.value)
  activeStream.value = null
  isStartingCamera.value = false
  isCapturingPhoto.value = false

  if (videoElement.value) {
    videoElement.value.srcObject = null
  }
}

function clearCapturedImage() {
  if (previewUrl.value && typeof URL !== 'undefined') {
    URL.revokeObjectURL(previewUrl.value)
  }
  previewUrl.value = ''
  capturedFile.value = null
}

function reportCameraError(error, fallbackMessage = '') {
  const message = fallbackMessage || getCameraErrorMessage(error)
  cameraError.value = message
  emit('error', {
    name: error?.name || 'CameraError',
    message,
    cause: error
  })
}

async function openCamera() {
  const browserContext = getBrowserContext()
  const support = describeCameraSupport(browserContext)
  cameraError.value = ''

  if (!support.supported) {
    reportCameraError({ name: support.reason }, support.message)
    return
  }

  stopCamera()
  clearCapturedImage()

  const requestId = ++cameraRequestId
  isStartingCamera.value = true

  try {
    const stream = await browserContext.navigator.mediaDevices.getUserMedia({
      audio: false,
      video: {
        facingMode: { ideal: 'environment' },
        width: { ideal: 1920 },
        height: { ideal: 1080 }
      }
    })

    if (requestId !== cameraRequestId || !props.modelValue) {
      stopMediaStream(stream)
      return
    }

    activeStream.value = stream
    await nextTick()

    if (!videoElement.value) {
      throw new Error('Camera preview is unavailable.')
    }

    videoElement.value.srcObject = stream
    await videoElement.value.play()
  } catch (error) {
    if (requestId === cameraRequestId) {
      stopCamera()
      reportCameraError(error)
    }
  } finally {
    if (requestId === cameraRequestId) {
      isStartingCamera.value = false
    }
  }
}

function captureImage() {
  const video = videoElement.value
  const canvas = canvasElement.value
  const width = video?.videoWidth || 0
  const height = video?.videoHeight || 0

  if (!activeStream.value || !video || !canvas || !width || !height) {
    reportCameraError({ name: 'CameraNotReadyError' }, '摄像头尚未准备好，请稍后重试。')
    return
  }

  const context = canvas.getContext('2d')
  if (!context) {
    reportCameraError({ name: 'CanvasUnavailableError' }, '当前浏览器无法处理照片，请改用上传图片识别。')
    return
  }

  const requestId = cameraRequestId
  isCapturingPhoto.value = true
  canvas.width = width
  canvas.height = height

  try {
    context.drawImage(video, 0, 0, width, height)
    canvas.toBlob((blob) => {
      if (requestId !== cameraRequestId || !props.modelValue) {
        return
      }

      if (!blob) {
        isCapturingPhoto.value = false
        reportCameraError({ name: 'CaptureFailedError' }, '未能生成照片，请重新拍摄。')
        return
      }

      try {
        const file = createCameraImageFile(blob)
        clearCapturedImage()
        capturedFile.value = file
        previewUrl.value = URL.createObjectURL(file)
        stopCamera()
      } catch (error) {
        isCapturingPhoto.value = false
        reportCameraError(error, '当前浏览器无法生成照片文件，请改用上传图片识别。')
      }
    }, CAMERA_CAPTURE_MIME_TYPE, 0.92)
  } catch (error) {
    isCapturingPhoto.value = false
    reportCameraError(error, '拍摄失败，请重新拍摄。')
  }
}

function retryCamera() {
  openCamera()
}

function confirmCapture() {
  if (!capturedFile.value) {
    return
  }

  emit('captured', capturedFile.value)
  requestClose('captured')
}

function requestClose(reason) {
  closeReason.value = reason
  stopCamera()
  emit('update:modelValue', false)
}

function handleDialogOpened() {
  closeReason.value = 'dismissed'
}

function handleDialogVisibility(visible) {
  if (!visible) {
    stopCamera()
  }
  emit('update:modelValue', visible)
}

function handleDialogClosed() {
  stopCamera()
  clearCapturedImage()
  cameraError.value = ''
  emit('close', { reason: closeReason.value })
  closeReason.value = 'dismissed'
}
</script>

<style scoped>
.camera-dialog-heading {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  padding-right: 32px;
}

.camera-dialog-heading h2 {
  margin: 0;
  color: var(--app-text);
  font-size: 18px;
  font-weight: 650;
  line-height: 1.35;
}

.camera-heading-icon {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 1px solid var(--app-line-strong);
  border-radius: 6px;
  color: var(--app-accent);
  background: var(--app-surface-soft);
}

.camera-capture-panel {
  display: grid;
  gap: 14px;
}

.camera-error {
  --el-alert-bg-color: var(--app-surface-soft);
  --el-alert-border-color: var(--app-line-strong);
}

.camera-stage {
  position: relative;
  display: grid;
  min-height: 280px;
  aspect-ratio: 4 / 3;
  overflow: hidden;
  border: 1px solid var(--app-line-strong);
  border-radius: 8px;
  background: var(--app-bg);
  isolation: isolate;
}

.camera-stage--previewing::after,
.camera-stage--captured::after {
  position: absolute;
  z-index: 1;
  inset: 12px;
  border: 1px solid color-mix(in srgb, var(--app-accent) 68%, transparent);
  border-radius: 4px;
  content: '';
  pointer-events: none;
}

.camera-video,
.camera-image-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.camera-stage-state {
  display: grid;
  align-content: center;
  justify-items: center;
  gap: 10px;
  color: var(--app-text-muted);
  font-size: 14px;
}

.camera-spinner {
  animation: camera-spin 900ms linear infinite;
}

.camera-live-status {
  position: absolute;
  z-index: 2;
  top: 12px;
  left: 12px;
  padding: 4px 8px;
  border: 1px solid var(--app-line-strong);
  border-radius: 4px;
  color: var(--app-text);
  background: var(--app-surface-strong);
  font-size: 12px;
  line-height: 1.2;
}

.camera-canvas,
.camera-sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.camera-action-bar,
.camera-dialog-footer {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.camera-dialog-footer {
  justify-content: flex-end;
}

.camera-action-button,
.camera-footer-button {
  min-height: 40px;
}

.camera-action-button:focus-visible,
.camera-footer-button:focus-visible {
  outline: 2px solid var(--app-accent);
  outline-offset: 2px;
}

@keyframes camera-spin {
  to {
    transform: rotate(360deg);
  }
}

@media (prefers-reduced-motion: reduce) {
  .camera-spinner {
    animation: none;
  }
}

@media (max-width: 640px) {
  .camera-ingredient-dialog :deep(.el-dialog__body) {
    padding: 14px 16px 18px;
  }

  .camera-stage {
    min-height: 0;
  }

  .camera-action-bar :deep(.el-button),
  .camera-dialog-footer :deep(.el-button) {
    flex: 1 1 132px;
    margin-left: 0;
  }
}
</style>
