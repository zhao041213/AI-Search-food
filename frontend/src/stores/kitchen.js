import { computed, ref, watch } from 'vue'
import { defineStore } from 'pinia'
import {
  getKitchenCharacterNames,
  resetKitchenCharacterNames,
  saveKitchenCharacterNames
} from '../api/kitchenCharacters.js'
import { useAuthStore } from './auth.js'
import {
  buildDefaultKitchenCharacterNames,
  getKitchenCharacterOwnerId,
  validateKitchenCharacterNames
} from '../utils/kitchenCharacters.js'
import {
  kitchenCharacterNamesToOverrides,
  normalizeKitchenCharacterNamesResponse,
  readStoredKitchenCharacterNames,
  shouldMigrateKitchenCharacterNames
} from '../utils/kitchenCharacterSync.js'

const STORAGE_PREFIX = 'ai_smart_recipe_kitchen_characters'

export const useKitchenStore = defineStore('kitchen', () => {
  const auth = useAuthStore()
  const nameOverrides = ref({})
  const requestedStation = ref('')
  const characterNamesStatus = ref('idle')
  const characterNamesError = ref('')
  const characterNamesSaving = ref(false)
  const characterNamesResetting = ref(false)
  const ownerId = computed(() => getKitchenCharacterOwnerId(auth.token))
  const storageKey = computed(() => `${STORAGE_PREFIX}:${ownerId.value}`)
  const characterNames = computed(() => ({
    ...buildDefaultKitchenCharacterNames(),
    ...nameOverrides.value
  }))
  let syncRequestId = 0
  let saveOperationId = 0
  let resetOperationId = 0

  watch(storageKey, loadCharacterNames, { immediate: true })

  function getCharacterName(characterId, fallback = '') {
    return characterNames.value[characterId] || fallback
  }

  async function saveCharacterNames(values) {
    const validation = validateKitchenCharacterNames(values)
    if (!validation.valid) return validation

    if (!auth.isUser) {
      applyCharacterNames(validation.names)
      persistCharacterNames()
      characterNamesStatus.value = 'ready'
      characterNamesError.value = ''
      return { ...validation, synced: false, applied: true }
    }

    const requestId = ++syncRequestId
    const token = auth.token
    const key = storageKey.value
    const operationId = ++saveOperationId
    characterNamesSaving.value = true
    characterNamesStatus.value = 'saving'
    characterNamesError.value = ''
    try {
      const response = await saveKitchenCharacterNames(validation.names)
      const cloud = normalizeKitchenCharacterNamesResponse(response.data.data)
      const applied = isCurrentContext(requestId, token, key)
      if (applied) {
        applyCharacterNames(cloud.names)
        persistCharacterNames()
        characterNamesStatus.value = 'ready'
      }
      return { ...validation, ...cloud, synced: true, applied }
    } catch (error) {
      if (isCurrentContext(requestId, token, key)) {
        characterNamesStatus.value = 'error'
        characterNamesError.value = getErrorMessage(error, '人物名称保存失败，请稍后重试')
      }
      throw error
    } finally {
      if (saveOperationId === operationId) {
        characterNamesSaving.value = false
      }
    }
  }

  async function resetCharacterNames() {
    if (!auth.isUser) {
      applyCharacterNames(buildDefaultKitchenCharacterNames())
      removeStoredCharacterNames()
      characterNamesStatus.value = 'ready'
      characterNamesError.value = ''
      return { synced: false, applied: true }
    }

    const requestId = ++syncRequestId
    const token = auth.token
    const key = storageKey.value
    const operationId = ++resetOperationId
    characterNamesResetting.value = true
    characterNamesStatus.value = 'resetting'
    characterNamesError.value = ''
    try {
      await resetKitchenCharacterNames()
      const applied = isCurrentContext(requestId, token, key)
      if (applied) {
        applyCharacterNames(buildDefaultKitchenCharacterNames())
        removeStoredCharacterNames()
        characterNamesStatus.value = 'ready'
      }
      return { synced: true, applied }
    } catch (error) {
      if (isCurrentContext(requestId, token, key)) {
        characterNamesStatus.value = 'error'
        characterNamesError.value = getErrorMessage(error, '人物名称恢复失败，请稍后重试')
      }
      throw error
    } finally {
      if (resetOperationId === operationId) {
        characterNamesResetting.value = false
      }
    }
  }

  function requestStation(stationId) {
    requestedStation.value = String(stationId || '')
  }

  function consumeRequestedStation() {
    const stationId = requestedStation.value
    requestedStation.value = ''
    return stationId
  }

  async function loadCharacterNames() {
    const requestId = ++syncRequestId
    const token = auth.token
    const key = storageKey.value
    const localState = readStoredKitchenCharacterNames(window.localStorage, key)
    applyCharacterNames(localState.names)
    characterNamesError.value = ''

    if (!auth.isUser) {
      characterNamesStatus.value = 'ready'
      return
    }

    characterNamesStatus.value = 'loading'
    try {
      const response = await getKitchenCharacterNames()
      if (!isCurrentContext(requestId, token, key)) return

      const cloudState = normalizeKitchenCharacterNamesResponse(response.data.data)
      if (shouldMigrateKitchenCharacterNames(localState, cloudState)) {
        const migrationResponse = await saveKitchenCharacterNames(localState.names)
        if (!isCurrentContext(requestId, token, key)) return
        const migratedState = normalizeKitchenCharacterNamesResponse(migrationResponse.data.data)
        applyCharacterNames(migratedState.names)
        persistCharacterNames()
      } else {
        applyCharacterNames(cloudState.names)
        persistCharacterNames()
      }
      characterNamesStatus.value = 'ready'
    } catch (error) {
      if (isCurrentContext(requestId, token, key)) {
        characterNamesStatus.value = 'error'
        characterNamesError.value = getErrorMessage(error, '人物名称加载失败，请稍后重试')
      }
    }
  }

  function retryCharacterNames() {
    return loadCharacterNames()
  }

  function applyCharacterNames(values) {
    const validation = validateKitchenCharacterNames(values)
    nameOverrides.value = validation.valid ? kitchenCharacterNamesToOverrides(validation.names) : {}
  }

  function persistCharacterNames() {
    try {
      window.localStorage.setItem(storageKey.value, JSON.stringify(nameOverrides.value))
    } catch {
      // The in-memory customization still works when local storage is blocked.
    }
  }

  function removeStoredCharacterNames() {
    try {
      window.localStorage.removeItem(storageKey.value)
    } catch {
      // Storage may be unavailable in a restricted browser context.
    }
  }

  function isCurrentContext(requestId, token, key) {
    return requestId === syncRequestId
      && auth.token === token
      && storageKey.value === key
  }

  function getErrorMessage(error, fallback) {
    return error?.response?.data?.message || fallback
  }

  return {
    characterNames,
    requestedStation,
    characterNamesStatus,
    characterNamesError,
    characterNamesLoading: computed(() => characterNamesStatus.value === 'loading'),
    characterNamesSaving,
    characterNamesResetting,
    getCharacterName,
    saveCharacterNames,
    resetCharacterNames,
    retryCharacterNames,
    requestStation,
    consumeRequestedStation
  }
})
