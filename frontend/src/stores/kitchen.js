import { computed, ref, watch } from 'vue'
import { defineStore } from 'pinia'
import { useAuthStore } from './auth'
import {
  buildDefaultKitchenCharacterNames,
  getKitchenCharacterOwnerId,
  validateKitchenCharacterNames
} from '../utils/kitchenCharacters'

const STORAGE_PREFIX = 'ai_smart_recipe_kitchen_characters'

export const useKitchenStore = defineStore('kitchen', () => {
  const auth = useAuthStore()
  const nameOverrides = ref({})
  const requestedStation = ref('')
  const ownerId = computed(() => getKitchenCharacterOwnerId(auth.token))
  const storageKey = computed(() => `${STORAGE_PREFIX}:${ownerId.value}`)
  const characterNames = computed(() => ({
    ...buildDefaultKitchenCharacterNames(),
    ...nameOverrides.value
  }))

  watch(storageKey, loadCharacterNames, { immediate: true })

  function getCharacterName(characterId, fallback = '') {
    return characterNames.value[characterId] || fallback
  }

  function saveCharacterNames(values) {
    const validation = validateKitchenCharacterNames(values)
    if (!validation.valid) return validation

    const defaults = buildDefaultKitchenCharacterNames()
    nameOverrides.value = Object.fromEntries(
      Object.entries(validation.names).filter(([characterId, name]) => name !== defaults[characterId])
    )
    persistCharacterNames()
    return validation
  }

  function resetCharacterNames() {
    nameOverrides.value = {}
    try {
      window.localStorage.removeItem(storageKey.value)
    } catch {
      // Storage may be unavailable in a restricted browser context.
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

  function loadCharacterNames() {
    nameOverrides.value = {}
    try {
      const saved = JSON.parse(window.localStorage.getItem(storageKey.value) || '{}')
      const validation = validateKitchenCharacterNames({
        ...buildDefaultKitchenCharacterNames(),
        ...(saved && typeof saved === 'object' ? saved : {})
      })
      if (!validation.valid) return
      const defaults = buildDefaultKitchenCharacterNames()
      nameOverrides.value = Object.fromEntries(
        Object.entries(validation.names).filter(([characterId, name]) => name !== defaults[characterId])
      )
    } catch {
      nameOverrides.value = {}
    }
  }

  function persistCharacterNames() {
    try {
      window.localStorage.setItem(storageKey.value, JSON.stringify(nameOverrides.value))
    } catch {
      // The in-memory customization still works when local storage is blocked.
    }
  }

  return {
    characterNames,
    requestedStation,
    getCharacterName,
    saveCharacterNames,
    resetCharacterNames,
    requestStation,
    consumeRequestedStation
  }
})
