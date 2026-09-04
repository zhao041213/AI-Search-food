const BILIBILI_VIDEO_URL_PATTERN = /^https:\/\/www\.bilibili\.com\/video\/BV[0-9A-Za-z]{10}$/

export function selectCookingVideoKeywords(value, recipeTitle = '') {
  const values = Array.isArray(value)
    ? value.filter((keyword) => typeof keyword === 'string' && keyword.trim())
    : []
  const unique = [...new Map(values.map((keyword) => [keyword.trim().toLocaleLowerCase(), keyword.trim()])).values()]
  if (unique.length) return unique.slice(0, 3)

  const fallbackTitle = typeof recipeTitle === 'string' ? recipeTitle.trim() : ''
  return fallbackTitle ? [fallbackTitle] : []
}

export function safeCookingVideoUrl(value) {
  const normalized = String(value || '')
  return BILIBILI_VIDEO_URL_PATTERN.test(normalized) ? normalized : ''
}

export function normalizeCookingVideoItems(value) {
  if (!Array.isArray(value)) return []
  return value.filter((video) => safeCookingVideoUrl(video?.targetUrl))
}

export function getCookingVideoStatusCopy({ degraded = false, hasItems = false, message = '' } = {}) {
  if (!degraded) {
    return {
      title: '暂未找到相关视频',
      description: '可以尝试切换关键词，或直接前往 B 站搜索。'
    }
  }

  if (hasItems) {
    return {
      title: '本次未取得新的具体视频结果',
      description: '已保留当前已加载的视频卡片，也可以前往 B 站搜索更多相关内容。'
    }
  }

  const detail = String(message || '').trim()
  return {
    title: '未取得具体视频结果',
    description: `${detail || '视频服务暂时不可用。'} 可前往 B 站搜索相关内容，不影响当前菜谱。`
  }
}
