import test from 'node:test'
import assert from 'node:assert/strict'
import { AI_ENDPOINTS, defaultAiEndpoint, isAiPresetEndpoint } from './adminAiConfig.js'

test('切换千问接口协议会返回对应的百炼地址', () => {
  assert.equal(defaultAiEndpoint('qwen', 'openai'), AI_ENDPOINTS.qwenOpenai)
  assert.equal(defaultAiEndpoint('qwen', 'anthropic'), AI_ENDPOINTS.qwenAnthropic)
})

test('自定义地址不会被误判为自动生成地址', () => {
  assert.equal(isAiPresetEndpoint(AI_ENDPOINTS.qwenOpenai), true)
  assert.equal(isAiPresetEndpoint(`${AI_ENDPOINTS.qwenOpenai}/chat/completions`), true)
  assert.equal(isAiPresetEndpoint('https://internal.example/recipe'), false)
})
