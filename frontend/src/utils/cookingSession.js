export const COOKING_SESSION_STORAGE_PREFIX = 'ai-recipe-cooking-session:'

const COOKING_SESSION_VERSION = 1

function isRecord(value) {
  return value !== null && typeof value === 'object' && !Array.isArray(value)
}

function toNonNegativeInteger(value, fallback = 0) {
  const number = Number(value)
  if (!Number.isFinite(number)) {
    return fallback
  }
  return Math.max(0, Math.floor(number))
}

function toPositiveInteger(value, fallback = 0) {
  const number = Number(value)
  if (!Number.isFinite(number) || number <= 0) {
    return fallback
  }
  return Math.ceil(number)
}

function clamp(value, minimum, maximum) {
  return Math.min(Math.max(value, minimum), maximum)
}

function normalizeText(value) {
  return typeof value === 'string' ? value.trim() : ''
}

function normalizeSessionOptions(options = {}) {
  return {
    stepCount: toNonNegativeInteger(options.stepCount),
    totalSeconds: toNonNegativeInteger(options.totalSeconds)
  }
}

function getStorage(storage) {
  if (storage !== undefined) {
    return typeof storage?.getItem === 'function' && typeof storage?.setItem === 'function' ? storage : null
  }

  if (typeof window === 'undefined') {
    return null
  }

  try {
    return window.localStorage
  } catch {
    return null
  }
}

export function normalizeCookingSteps(steps) {
  if (!Array.isArray(steps)) {
    return []
  }

  return steps.reduce((normalized, step) => {
    const source = isRecord(step)
      ? step
      : typeof step === 'string'
        ? { description: step }
        : null

    if (!source) {
      return normalized
    }

    const title = normalizeText(source.title)
    const description = normalizeText(source.description ?? source.instruction)
    const durationMinutes = toPositiveInteger(source.durationMinutes ?? source.estimatedMinutes) || null

    if (!title && !description && !durationMinutes) {
      return normalized
    }

    const position = normalized.length + 1
    normalized.push({
      order: toPositiveInteger(source.order, position),
      title: title || `步骤 ${position}`,
      description,
      durationMinutes
    })
    return normalized
  }, [])
}

export function getRecipeTotalMinutes(recipe) {
  const source = isRecord(recipe) ? recipe : {}
  const explicitDuration = [source.totalMinutes, source.durationMinutes, source.estimatedMinutes]
    .map((value) => toPositiveInteger(value))
    .find(Boolean)

  if (explicitDuration) {
    return explicitDuration
  }

  return normalizeCookingSteps(source.steps).reduce(
    (total, step) => total + (step.durationMinutes || 0),
    0
  )
}

export function createCookingSession(options = {}) {
  const { totalSeconds } = normalizeSessionOptions(options)
  return {
    currentStepIndex: 0,
    remainingSeconds: totalSeconds,
    timerRunning: false,
    finished: false
  }
}

export function normalizeCookingSession(session, options = {}) {
  const { stepCount, totalSeconds } = normalizeSessionOptions(options)
  const source = isRecord(session) ? session : {}
  const finished = Boolean(source.finished)
  const maxStepIndex = Math.max(stepCount - 1, 0)
  const remainingSeconds = totalSeconds
    ? clamp(toNonNegativeInteger(source.remainingSeconds, totalSeconds), 0, totalSeconds)
    : 0

  return {
    currentStepIndex: finished && stepCount
      ? maxStepIndex
      : clamp(toNonNegativeInteger(source.currentStepIndex), 0, maxStepIndex),
    remainingSeconds,
    timerRunning: Boolean(source.timerRunning) && remainingSeconds > 0 && !finished,
    finished
  }
}

export function getCookingProgress(currentStepIndex, stepCount, finished = false) {
  const normalizedStepCount = toNonNegativeInteger(stepCount)
  if (!normalizedStepCount) {
    return 0
  }

  if (finished) {
    return 100
  }

  const currentIndex = clamp(toNonNegativeInteger(currentStepIndex), 0, normalizedStepCount - 1)
  return Math.round(((currentIndex + 1) / normalizedStepCount) * 100)
}

export function formatCookingDuration(seconds) {
  const totalSeconds = toNonNegativeInteger(seconds)
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const remainingSeconds = totalSeconds % 60
  const pad = (value) => String(value).padStart(2, '0')

  return hours > 0
    ? `${pad(hours)}:${pad(minutes)}:${pad(remainingSeconds)}`
    : `${pad(minutes)}:${pad(remainingSeconds)}`
}

export function formatCookingMinutes(minutes) {
  const normalizedMinutes = toPositiveInteger(minutes)
  return normalizedMinutes ? `约 ${normalizedMinutes} 分钟` : '未标注时长'
}

export function getCookingSessionStorageKey(storageKey) {
  const normalizedKey = String(storageKey ?? '').trim()
  return normalizedKey ? `${COOKING_SESSION_STORAGE_PREFIX}${normalizedKey}` : ''
}

export function persistCookingSession(storageKey, session, options = {}) {
  const key = getCookingSessionStorageKey(storageKey)
  const storage = getStorage(options.storage)
  if (!key || !storage) {
    return false
  }

  try {
    storage.setItem(key, JSON.stringify({
      version: COOKING_SESSION_VERSION,
      ...normalizeCookingSession(session, options)
    }))
    return true
  } catch {
    return false
  }
}

export function restoreCookingSession(storageKey, options = {}) {
  const fallback = createCookingSession(options)
  const key = getCookingSessionStorageKey(storageKey)
  const storage = getStorage(options.storage)
  if (!key || !storage) {
    return fallback
  }

  try {
    const value = storage.getItem(key)
    if (!value) {
      return fallback
    }

    const parsed = JSON.parse(value)
    return isRecord(parsed) ? normalizeCookingSession(parsed, options) : fallback
  } catch {
    return fallback
  }
}
