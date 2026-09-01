const OVERVIEW_PANEL = 'overview'
const HOT_INGREDIENT_PANEL = 'hot-ingredients'
const SETTINGS_PANEL = 'settings'

export function shouldShowSavedRecipesNavigation(role) {
  return role === 'USER'
}

export function getHotIngredientNavigation(isAdmin) {
  if (isAdmin) {
    return {
      label: '热门分析',
      to: { name: 'admin', query: { panel: HOT_INGREDIENT_PANEL } }
    }
  }
  return {
    label: '热门食材',
    to: { name: 'hot-ingredients' }
  }
}

export function resolveAdminPanel(panel) {
  const value = Array.isArray(panel) ? panel[0] : panel
  return [OVERVIEW_PANEL, SETTINGS_PANEL, HOT_INGREDIENT_PANEL].includes(value)
    ? value
    : OVERVIEW_PANEL
}

export function buildAdminPanelQuery(currentQuery, panel) {
  const query = { ...currentQuery }
  const resolvedPanel = resolveAdminPanel(panel)
  if (resolvedPanel === OVERVIEW_PANEL) {
    delete query.panel
  } else {
    query.panel = resolvedPanel
  }
  return query
}
