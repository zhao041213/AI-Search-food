<template>
  <section class="recognition-station" aria-labelledby="recognition-station-title">
    <header class="station-intro">
      <span class="station-intro__icon" aria-hidden="true"><ScanSearch :size="24" /></span>
      <div>
        <p>小灶 · 食材识别员</p>
        <h2 id="recognition-station-title">把食材交给我看看</h2>
        <span>上传照片或直接拍摄，我会先识别食材，再交给阿灶生成菜谱。</span>
      </div>
    </header>

    <div class="recognition-layout">
      <section class="capture-card" aria-label="食材图片选择">
        <div class="capture-stage" :class="{ 'capture-stage--ready': previewUrl }">
          <img v-if="previewUrl" :src="previewUrl" alt="待识别的食材图片预览" />
          <div v-else class="capture-empty">
            <ImagePlus :size="42" :stroke-width="1.5" aria-hidden="true" />
            <strong>选择一张食材照片</strong>
            <span>尽量保持光线充足、主体清晰</span>
          </div>
        </div>

        <input
          ref="imageInput"
          class="visually-hidden"
          type="file"
          tabindex="-1"
          aria-hidden="true"
          accept="image/jpeg,image/png,image/webp"
          @change="handleImageSelected"
        />

        <div class="capture-actions">
          <el-button @click="imageInput?.click()">
            <Upload :size="16" aria-hidden="true" />
            选择图片
          </el-button>
          <el-button @click="cameraVisible = true">
            <Camera :size="16" aria-hidden="true" />
            现场拍摄
          </el-button>
          <el-button v-if="selectedFile" text @click="clearSelection">
            <RotateCcw :size="15" aria-hidden="true" />
            重选
          </el-button>
        </div>

        <el-button
          class="recognize-button"
          type="primary"
          :loading="recognizing"
          :disabled="!selectedFile"
          @click="runRecognition"
        >
          <ScanSearch :size="17" aria-hidden="true" />
          {{ recognizing ? '正在识别' : '开始识别食材' }}
        </el-button>
        <p class="capture-note">支持 JPG、PNG、WebP，单张不超过 5MB</p>
      </section>

      <section class="recognition-result" aria-live="polite" aria-label="食材识别结果">
        <div class="result-heading">
          <div>
            <p>识别结果</p>
            <h3>{{ resultTitle }}</h3>
          </div>
          <span>{{ recognizedIngredients.length }} 种</span>
        </div>

        <div v-if="recognizing" class="result-state">
          <LoaderCircle class="spin" :size="30" aria-hidden="true" />
          <strong>正在分析图片中的食材</strong>
          <span>通常需要几秒钟，请保持窗口开启。</span>
        </div>
        <div v-else-if="!recognizedIngredients.length" class="result-state">
          <ScanSearch :size="34" :stroke-width="1.5" aria-hidden="true" />
          <strong>等待识别</strong>
          <span>识别完成后，可以把结果一键交给阿灶。</span>
        </div>
        <template v-else>
          <div class="ingredient-tags" aria-label="识别出的食材">
            <span v-for="ingredient in recognizedIngredients" :key="ingredient">{{ ingredient }}</span>
          </div>
          <p v-if="recognitionDescription" class="recognition-description">{{ recognitionDescription }}</p>
          <button class="handoff-button" type="button" @click="handoffToChef">
            <ChefHat :size="19" aria-hidden="true" />
            <span><strong>交给阿灶继续生成</strong><small>带着这 {{ recognizedIngredients.length }} 种食材进入主厨工作台</small></span>
          </button>
        </template>
      </section>
    </div>

    <CameraIngredientCapture v-model="cameraVisible" @captured="handleCameraCaptured" />
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Camera,
  ChefHat,
  ImagePlus,
  LoaderCircle,
  RotateCcw,
  ScanSearch,
  Upload
} from 'lucide-vue-next'
import { recognizeIngredients } from '../../api/recipes'
import CameraIngredientCapture from '../CameraIngredientCapture.vue'
import { validateIngredientImageFile } from '../../utils/ingredientRecognition'

const emit = defineEmits(['use-ingredients'])
const imageInput = ref(null)
const selectedFile = ref(null)
const previewUrl = ref('')
const recognizing = ref(false)
const recognizedIngredients = ref([])
const recognitionDescription = ref('')
const cameraVisible = ref(false)

const resultTitle = computed(() => (
  recognizedIngredients.value.length ? '已经认出这些食材' : '等待一张清晰照片'
))

function handleImageSelected(event) {
  const [file] = event.target.files || []
  selectFile(file)
  event.target.value = ''
}

async function handleCameraCaptured(file) {
  cameraVisible.value = false
  if (selectFile(file)) await runRecognition()
}

function selectFile(file) {
  const validationMessage = validateIngredientImageFile(file)
  if (validationMessage) {
    ElMessage.warning(validationMessage)
    return false
  }
  clearPreview()
  selectedFile.value = file
  previewUrl.value = URL.createObjectURL(file)
  recognizedIngredients.value = []
  recognitionDescription.value = ''
  return true
}

async function runRecognition() {
  if (!selectedFile.value || recognizing.value) return
  recognizing.value = true
  try {
    const response = await recognizeIngredients(selectedFile.value)
    const result = response.data.data || {}
    recognizedIngredients.value = Array.isArray(result.ingredients) ? result.ingredients.filter(Boolean) : []
    recognitionDescription.value = result.description || ''
    if (!recognizedIngredients.value.length) {
      ElMessage.warning('未识别到明确食材，请更换图片后重试')
      return
    }
    ElMessage.success('小灶已经完成食材识别')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '食材识别失败，请稍后重试')
  } finally {
    recognizing.value = false
  }
}

function handoffToChef() {
  emit('use-ingredients', recognizedIngredients.value.join('、'))
}

function clearSelection() {
  selectedFile.value = null
  recognizedIngredients.value = []
  recognitionDescription.value = ''
  clearPreview()
}

function clearPreview() {
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = ''
}

onBeforeUnmount(() => {
  cameraVisible.value = false
  clearPreview()
})
</script>

<style scoped>
.recognition-station {
  min-height: 100%;
  padding: 18px;
}

.station-intro {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 14px;
  padding: 14px 16px;
  border: 1px solid #b48a58;
  background: #f2e5c7;
  box-shadow: 3px 3px 0 rgba(97, 71, 52, 0.24);
}

.station-intro__icon {
  display: grid;
  width: 46px;
  height: 46px;
  flex: 0 0 auto;
  place-items: center;
  border: 2px solid #614734;
  color: #f2c654;
  background: #2b211d;
}

.station-intro p,
.station-intro h2,
.station-intro span,
.result-heading p,
.result-heading h3,
.capture-note,
.recognition-description {
  margin: 0;
}

.station-intro p,
.result-heading p {
  color: #9a7142;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.08em;
}

.station-intro h2 {
  margin: 2px 0 3px;
  color: #3c2b20;
  font-size: 20px;
}

.station-intro span,
.capture-note,
.recognition-description,
.result-state span {
  color: #80664a;
  font-size: 12px;
  line-height: 1.6;
}

.recognition-layout {
  display: grid;
  grid-template-columns: minmax(300px, 0.9fr) minmax(340px, 1.1fr);
  gap: 14px;
}

.capture-card,
.recognition-result {
  min-height: 430px;
  padding: 16px;
  border: 1px solid #c8aa7b;
  background: rgba(255, 250, 240, 0.92);
  box-shadow: 0 8px 22px rgba(69, 48, 34, 0.1);
}

.capture-stage {
  display: grid;
  height: 270px;
  overflow: hidden;
  place-items: center;
  border: 2px dashed #b49467;
  background: #e9ddc7;
}

.capture-stage--ready {
  border-style: solid;
  background: #211c1a;
}

.capture-stage img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.capture-empty,
.result-state {
  display: grid;
  place-items: center;
  align-content: center;
  gap: 8px;
  color: #9a7142;
  text-align: center;
}

.capture-empty strong,
.result-state strong {
  color: #4b3829;
  font-size: 15px;
}

.capture-empty span {
  color: #80664a;
  font-size: 12px;
}

.capture-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 12px 0;
}

.recognize-button {
  width: 100%;
  min-height: 44px;
}

.capture-note {
  margin-top: 8px;
  text-align: center;
}

.result-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #dcc7a4;
}

.result-heading h3 {
  margin-top: 3px;
  color: #3c2b20;
  font-size: 18px;
}

.result-heading > span {
  padding: 5px 9px;
  border: 1px solid #b49467;
  color: #80664a;
  background: #f2e5c7;
  font-size: 11px;
  font-weight: 900;
}

.result-state {
  min-height: 310px;
}

.ingredient-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 20px 0 12px;
}

.ingredient-tags span {
  padding: 7px 10px;
  border: 1px solid #78a480;
  color: #305a39;
  background: #e6f2e5;
  font-size: 13px;
  font-weight: 800;
}

.recognition-description {
  padding: 10px;
  border-left: 3px solid #d6a43b;
  background: #f7eedc;
}

.handoff-button {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  min-height: 58px;
  margin-top: 18px;
  padding: 10px 12px;
  border: 2px solid #614734;
  color: #f7e6b5;
  background: #2b211d;
  text-align: left;
  cursor: pointer;
}

.handoff-button span {
  display: grid;
  gap: 2px;
}

.handoff-button small {
  color: #cbb895;
}

.handoff-button:hover,
.handoff-button:focus-visible {
  color: #2b211d;
  background: #d6a43b;
  outline: 2px solid #614734;
  outline-offset: 2px;
}

.handoff-button:hover small,
.handoff-button:focus-visible small {
  color: #4b3829;
}

.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
  clip-path: inset(50%);
  white-space: nowrap;
}

.spin {
  animation: recognition-spin 900ms linear infinite;
}

@keyframes recognition-spin {
  to { transform: rotate(360deg); }
}

@media (prefers-reduced-motion: reduce) {
  .spin { animation: none; }
}

@container scene-window-content (max-width: 760px) {
  .recognition-layout { grid-template-columns: 1fr; }
  .capture-card,
  .recognition-result { min-height: 0; }
}
</style>
