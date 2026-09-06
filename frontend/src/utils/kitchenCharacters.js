export const KITCHEN_CHARACTER_NAME_MAX_LENGTH = 6

export const KITCHEN_CHARACTERS = [
  { id: 'chef', defaultName: '阿灶', role: 'AI 主厨', room: '主厨料理大厅', spriteNum: 1 },
  { id: 'chef-helper', defaultName: '小灶', role: '食材识别员', room: '主厨料理大厅', spriteNum: 2 },
  { id: 'chef-recipes', defaultName: '小谱', role: '生成记录员', room: '主厨料理大厅', spriteNum: 4 },
  { id: 'chef-nutrition', defaultName: '小衡', role: '饮食偏好顾问', room: '主厨料理大厅', spriteNum: 5 },
  { id: 'pantry', defaultName: '小仓', role: '食材管家', room: '食材储藏室', spriteNum: 3 },
  { id: 'recipes', defaultName: '阿笺', role: '菜谱管理员', room: '菜谱书房', spriteNum: 4 },
  { id: 'nutrition', defaultName: '衡衡', role: '营养师', room: '营养咨询室', spriteNum: 5 },
  { id: 'weekly', defaultName: '周周', role: '菜单规划师', room: '厨房协作区', spriteNum: 6 },
  { id: 'review', defaultName: '味味', role: '成品品鉴员', room: '厨房协作区', spriteNum: 7 },
  { id: 'account', defaultName: '小管', role: '厨房管家', room: '厨房协作区', spriteNum: 8 },
  { id: 'hot', defaultName: '椒椒', role: '市场观察员', room: '美食情报站', spriteNum: 9 }
]

export function buildDefaultKitchenCharacterNames() {
  return Object.fromEntries(KITCHEN_CHARACTERS.map((character) => [character.id, character.defaultName]))
}

export function normalizeKitchenCharacterName(value) {
  return String(value || '').trim().replace(/\s+/g, ' ')
}

export function validateKitchenCharacterNames(values = {}) {
  const input = values && typeof values === 'object' && !Array.isArray(values) ? values : {}
  const names = {}
  const errors = {}
  const ownersByName = new Map()
  const knownIds = new Set(KITCHEN_CHARACTERS.map((character) => character.id))
  const unknownIds = Object.keys(input).filter((characterId) => !knownIds.has(characterId))
  if (unknownIds.length > 0) {
    errors._unknownCharacterId = '包含未知人物编号'
  }

  KITCHEN_CHARACTERS.forEach((character) => {
    const name = normalizeKitchenCharacterName(input[character.id])
    names[character.id] = name
    if (!name) {
      errors[character.id] = '请输入人物名称'
      return
    }
    if (name.length > KITCHEN_CHARACTER_NAME_MAX_LENGTH) {
      errors[character.id] = `最多 ${KITCHEN_CHARACTER_NAME_MAX_LENGTH} 个字符`
      return
    }
    const duplicateKey = name.toLocaleLowerCase('zh-CN')
    const existingId = ownersByName.get(duplicateKey)
    if (existingId) {
      errors[existingId] = '人物名称不能重复'
      errors[character.id] = '人物名称不能重复'
      return
    }
    ownersByName.set(duplicateKey, character.id)
  })

  return {
    names,
    errors,
    valid: Object.keys(errors).length === 0
  }
}

export function getKitchenCharacterOwnerId(token) {
  const value = String(token || '').trim()
  if (!value) return 'guest'
  try {
    const payload = value.split('.')[1]
    if (!payload) return 'guest'
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/').padEnd(Math.ceil(payload.length / 4) * 4, '=')
    const bytes = Uint8Array.from(globalThis.atob(base64), (character) => character.charCodeAt(0))
    const claims = JSON.parse(new TextDecoder().decode(bytes))
    return claims?.id == null ? 'guest' : `user-${claims.id}`
  } catch {
    return 'guest'
  }
}
