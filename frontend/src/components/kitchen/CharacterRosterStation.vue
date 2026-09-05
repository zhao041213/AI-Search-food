<template>
  <section class="character-roster" aria-labelledby="character-roster-title">
    <header class="character-roster__header">
      <div>
        <p class="character-roster__eyebrow">厨房角色管理</p>
        <h2 id="character-roster-title">人物名册</h2>
        <p>名称仅影响当前账号在本机浏览器中的厨房展示，不会改变人物功能。</p>
      </div>
      <span class="character-roster__count">{{ KITCHEN_CHARACTERS.length }} 位伙伴</span>
    </header>

    <div class="character-roster__rooms">
      <section v-for="group in characterGroups" :key="group.room" class="character-room">
        <h3>{{ group.room }}</h3>
        <div class="character-room__grid">
          <label v-for="character in group.characters" :key="character.id" class="character-card">
            <img
              class="character-card__sprite"
              :src="`/sprites/${character.spriteNum}-D-1.png`"
              alt=""
              aria-hidden="true"
            />
            <span class="character-card__identity">
              <strong>{{ character.role }}</strong>
              <small>默认：{{ character.defaultName }}</small>
            </span>
            <span class="character-card__field">
              <span>新名称</span>
              <input
                v-model="draft[character.id]"
                type="text"
                :maxlength="KITCHEN_CHARACTER_NAME_MAX_LENGTH"
                :aria-label="`修改${character.defaultName}的名称`"
                :aria-invalid="Boolean(errors[character.id])"
                :aria-describedby="errors[character.id] ? `character-error-${character.id}` : undefined"
                @input="clearError(character.id)"
              />
              <small
                v-if="errors[character.id]"
                :id="`character-error-${character.id}`"
                class="character-card__error"
                role="alert"
              >
                {{ errors[character.id] }}
              </small>
            </span>
          </label>
        </div>
      </section>
    </div>

    <footer class="character-roster__actions">
      <span>每个名称最多 {{ KITCHEN_CHARACTER_NAME_MAX_LENGTH }} 个字符，且不能重复。</span>
      <div>
        <button type="button" class="roster-button roster-button--secondary" @click="restoreDefaults">
          <RotateCcw :size="15" aria-hidden="true" />
          恢复默认名称
        </button>
        <button type="button" class="roster-button roster-button--primary" @click="saveNames">
          <Save :size="15" aria-hidden="true" />
          保存修改
        </button>
      </div>
    </footer>
  </section>
</template>

<script setup>
import { computed, reactive, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { RotateCcw, Save } from 'lucide-vue-next'
import { useKitchenStore } from '../../stores/kitchen'
import {
  buildDefaultKitchenCharacterNames,
  KITCHEN_CHARACTERS,
  KITCHEN_CHARACTER_NAME_MAX_LENGTH,
  validateKitchenCharacterNames
} from '../../utils/kitchenCharacters'

const kitchen = useKitchenStore()
const draft = reactive(buildDefaultKitchenCharacterNames())
const errors = reactive({})
const characterGroups = computed(() => {
  const groups = new Map()
  KITCHEN_CHARACTERS.forEach((character) => {
    if (!groups.has(character.room)) groups.set(character.room, [])
    groups.get(character.room).push(character)
  })
  return Array.from(groups, ([room, characters]) => ({ room, characters }))
})

watch(
  () => kitchen.characterNames,
  (names) => {
    Object.assign(draft, names)
    clearErrors()
  },
  { immediate: true, deep: true }
)

function clearError(characterId) {
  delete errors[characterId]
}

function clearErrors() {
  Object.keys(errors).forEach((characterId) => delete errors[characterId])
}

function saveNames() {
  clearErrors()
  const validation = validateKitchenCharacterNames(draft)
  if (!validation.valid) {
    Object.assign(errors, validation.errors)
    ElMessage.warning('请先修正人物名称')
    return
  }
  kitchen.saveCharacterNames(validation.names)
  Object.assign(draft, kitchen.characterNames)
  ElMessage.success('人物名称已保存')
}

async function restoreDefaults() {
  try {
    await ElMessageBox.confirm('确定恢复所有人物的默认名称吗？', '恢复默认名称', {
      confirmButtonText: '恢复默认',
      cancelButtonText: '暂不恢复',
      type: 'warning'
    })
    kitchen.resetCharacterNames()
    Object.assign(draft, kitchen.characterNames)
    clearErrors()
    ElMessage.success('已恢复默认名称')
  } catch {
    // The user cancelled the reset.
  }
}
</script>

<style scoped>
.character-roster {
  display: grid;
  gap: 16px;
  min-height: 100%;
  padding: 18px;
  color: #3c2b20;
}

.character-roster__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  padding: 16px 18px;
  border: 1px solid #c8aa7b;
  background: #fff8e8;
  box-shadow: 4px 4px 0 rgba(82, 56, 36, 0.12);
}

.character-roster__eyebrow,
.character-roster__header h2,
.character-roster__header p {
  margin: 0;
}

.character-roster__eyebrow {
  color: #a36e2d;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.12em;
}

.character-roster__header h2 {
  margin-top: 3px;
  font-size: 22px;
}

.character-roster__header p:last-child {
  margin-top: 5px;
  color: #80664a;
  font-size: 12px;
  line-height: 1.6;
}

.character-roster__count {
  flex: 0 0 auto;
  padding: 5px 9px;
  border: 1px solid #9e7b50;
  color: #59412d;
  background: #f1dfb8;
  font-size: 11px;
  font-weight: 900;
}

.character-roster__rooms {
  display: grid;
  gap: 12px;
}

.character-room {
  padding: 13px;
  border: 1px solid #b99362;
  background: rgba(239, 224, 187, 0.72);
}

.character-room h3 {
  margin: 0 0 10px;
  color: #6f4c30;
  font-size: 13px;
}

.character-room__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 9px;
}

.character-card {
  display: grid;
  grid-template-columns: 46px minmax(110px, 0.8fr) minmax(140px, 1fr);
  align-items: center;
  gap: 10px;
  min-width: 0;
  padding: 10px;
  border: 1px solid #d4bc91;
  background: #fffaf0;
}

.character-card__sprite {
  width: 42px;
  height: 48px;
  object-fit: contain;
  image-rendering: pixelated;
}

.character-card__identity,
.character-card__field {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.character-card__identity strong,
.character-card__identity small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.character-card__identity strong,
.character-card__field > span {
  font-size: 11px;
  font-weight: 900;
}

.character-card__identity small,
.character-card__field > span {
  color: #80664a;
}

.character-card__field input {
  width: 100%;
  min-height: 32px;
  padding: 5px 8px;
  border: 1px solid #aa885d;
  border-radius: 3px;
  color: #3c2b20;
  background: #fffef9;
  font: inherit;
  font-size: 12px;
  font-weight: 800;
}

.character-card__field input:focus-visible {
  border-color: #4f8ca5;
  outline: 2px solid rgba(79, 140, 165, 0.34);
  outline-offset: 1px;
}

.character-card__field input[aria-invalid='true'] {
  border-color: #b84c43;
}

.character-card__error {
  color: #a9342d;
  font-size: 10px;
  font-weight: 800;
}

.character-roster__actions {
  position: sticky;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 11px 13px;
  border: 1px solid #b99362;
  background: rgba(248, 239, 220, 0.96);
  backdrop-filter: blur(6px);
}

.character-roster__actions > span {
  color: #80664a;
  font-size: 11px;
  font-weight: 700;
}

.character-roster__actions > div {
  display: flex;
  gap: 8px;
}

.roster-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 34px;
  padding: 6px 11px;
  border: 1px solid #8d6945;
  border-radius: 3px;
  font: inherit;
  font-size: 11px;
  font-weight: 900;
  cursor: pointer;
}

.roster-button--secondary {
  color: #5b422f;
  background: #fffaf0;
}

.roster-button--primary {
  color: #fff8e7;
  background: #74512f;
}

.roster-button:hover {
  filter: brightness(1.06);
}

.roster-button:focus-visible {
  outline: 2px solid #4f8ca5;
  outline-offset: 2px;
}

@container scene-window-content (max-width: 820px) {
  .character-room__grid {
    grid-template-columns: 1fr;
  }
}

@container scene-window-content (max-width: 520px) {
  .character-roster__header,
  .character-roster__actions {
    align-items: stretch;
    flex-direction: column;
  }

  .character-roster__count {
    align-self: flex-start;
  }

  .character-card {
    grid-template-columns: 42px minmax(0, 1fr);
  }

  .character-card__field {
    grid-column: 1 / -1;
  }

  .character-roster__actions > div {
    flex-wrap: wrap;
  }
}
</style>
