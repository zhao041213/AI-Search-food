import assert from 'node:assert/strict'
import test from 'node:test'
import {
  getCookingVideoStatusCopy,
  normalizeCookingVideoItems,
  safeCookingVideoUrl,
  selectCookingVideoKeywords
} from './cookingVideos.js'

test('selectCookingVideoKeywords keeps at most three distinct keywords', () => {
  assert.deepEqual(
    selectCookingVideoKeywords([' 家常菜 ', '家常菜', '清炒做法', '摆盘技巧', '多余关键词']),
    ['家常菜', '清炒做法', '摆盘技巧']
  )
  assert.deepEqual(selectCookingVideoKeywords([], ' 番茄炒蛋 '), ['番茄炒蛋'])
})

test('safeCookingVideoUrl only accepts validated Bilibili BV links', () => {
  const validUrl = 'https://www.bilibili.com/video/BV1Abc234567'

  assert.equal(safeCookingVideoUrl(validUrl), validUrl)
  assert.equal(safeCookingVideoUrl('https://www.bilibili.com/video/BV1Abc234567?from=search'), '')
  assert.equal(safeCookingVideoUrl('https://evil.example/video/BV1Abc234567'), '')
  assert.equal(safeCookingVideoUrl(''), '')
})

test('normalizeCookingVideoItems drops cards without safe target URLs', () => {
  const items = normalizeCookingVideoItems([
    { videoId: 'valid', targetUrl: 'https://www.bilibili.com/video/BV1Abc234567' },
    { videoId: 'missing', title: '没有链接' },
    { videoId: 'invalid', targetUrl: 'https://search.bilibili.com/all?keyword=food' }
  ])

  assert.deepEqual(items.map(({ videoId }) => videoId), ['valid'])
  assert.deepEqual(normalizeCookingVideoItems(null), [])
})

test('getCookingVideoStatusCopy distinguishes no result from Bilibili fallback', () => {
  assert.deepEqual(getCookingVideoStatusCopy(), {
    title: '暂未找到相关视频',
    description: '可以尝试切换关键词，或直接前往 B 站搜索。'
  })
  assert.deepEqual(getCookingVideoStatusCopy({ degraded: true, message: '请求被限流' }), {
    title: '未取得具体视频结果',
    description: '请求被限流 可前往 B 站搜索相关内容，不影响当前菜谱。'
  })
  assert.deepEqual(getCookingVideoStatusCopy({ degraded: true, hasItems: true }), {
    title: '本次未取得新的具体视频结果',
    description: '已保留当前已加载的视频卡片，也可以前往 B 站搜索更多相关内容。'
  })
})
