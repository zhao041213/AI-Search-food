export const AI_ENDPOINTS = {
  qwenOpenai: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
  qwenAnthropic: 'https://dashscope.aliyuncs.com/apps/anthropic',
  deepseekOpenai: 'https://api.deepseek.com/v1'
}

export function defaultAiEndpoint(provider = 'qwen', protocol = 'openai') {
  if (provider === 'qwen' && protocol === 'anthropic') {
    return AI_ENDPOINTS.qwenAnthropic
  }
  if (provider === 'deepseek') {
    return AI_ENDPOINTS.deepseekOpenai
  }
  return AI_ENDPOINTS.qwenOpenai
}

export function isAiPresetEndpoint(endpoint) {
  return [
    AI_ENDPOINTS.qwenOpenai,
    `${AI_ENDPOINTS.qwenOpenai}/chat/completions`,
    AI_ENDPOINTS.qwenAnthropic,
    AI_ENDPOINTS.deepseekOpenai,
    `${AI_ENDPOINTS.deepseekOpenai}/chat/completions`
  ].includes(endpoint)
}
