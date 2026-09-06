import {
  buildDefaultKitchenCharacterNames,
  validateKitchenCharacterNames
} from './kitchenCharacters.js'

export function kitchenCharacterNamesToOverrides(names) {
  const defaults = buildDefaultKitchenCharacterNames()
  return Object.fromEntries(
    Object.entries(names).filter(([characterId, name]) => name !== defaults[characterId])
  )
}

export function hasKitchenCharacterOverrides(names) {
  return Object.keys(kitchenCharacterNamesToOverrides(names)).length > 0
}

export function readStoredKitchenCharacterNames(storage, storageKey) {
  const defaults = buildDefaultKitchenCharacterNames()
  try {
    const saved = JSON.parse(storage?.getItem(storageKey) || '{}')
    const validation = validateKitchenCharacterNames({
      ...defaults,
      ...(saved && typeof saved === 'object' && !Array.isArray(saved) ? saved : {})
    })
    if (!validation.valid) {
      return { names: defaults, hasCustomNames: false, valid: false }
    }
    return {
      names: validation.names,
      hasCustomNames: hasKitchenCharacterOverrides(validation.names),
      valid: true
    }
  } catch {
    return { names: defaults, hasCustomNames: false, valid: false }
  }
}

export function normalizeKitchenCharacterNamesResponse(payload) {
  const defaults = buildDefaultKitchenCharacterNames()
  const candidate = payload?.names && typeof payload.names === 'object' && !Array.isArray(payload.names)
    ? { ...defaults, ...payload.names }
    : defaults
  const validation = validateKitchenCharacterNames(candidate)
  if (!validation.valid) {
    return { names: defaults, hasCustomNames: false, valid: false }
  }
  return {
    names: validation.names,
    hasCustomNames: Boolean(payload?.hasCustomNames) || hasKitchenCharacterOverrides(validation.names),
    valid: true
  }
}

export function shouldMigrateKitchenCharacterNames(localState, cloudState) {
  return Boolean(localState?.hasCustomNames) && !Boolean(cloudState?.hasCustomNames)
}
