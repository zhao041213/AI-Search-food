const ANONYMOUS_ID_KEY = 'ai_smart_recipe_anonymous_id'

export function getAnonymousId() {
  const existing = window.localStorage.getItem(ANONYMOUS_ID_KEY)
  if (existing) {
    return existing
  }

  const generated = window.crypto?.randomUUID?.() || `anonymous-${Date.now()}-${Math.random().toString(36).slice(2)}`
  window.localStorage.setItem(ANONYMOUS_ID_KEY, generated)
  return generated
}
