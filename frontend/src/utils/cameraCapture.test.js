import assert from 'node:assert/strict'
import test from 'node:test'
import {
  createCameraImageFile,
  createCameraImageFileName,
  describeCameraSupport,
  getCameraErrorMessage
} from './cameraCapture.js'

test('describeCameraSupport accepts a secure browser with getUserMedia', () => {
  const support = describeCameraSupport({
    isSecureContext: true,
    navigator: { mediaDevices: { getUserMedia: () => Promise.resolve() } },
    location: { hostname: 'recipe.example.com' }
  })

  assert.deepEqual(support, {
    supported: true,
    reason: null,
    message: ''
  })
})

test('describeCameraSupport accepts localhost and explains unsupported contexts', () => {
  const localSupport = describeCameraSupport({
    isSecureContext: false,
    navigator: { mediaDevices: { getUserMedia: () => Promise.resolve() } },
    location: { hostname: 'localhost' }
  })
  const insecureSupport = describeCameraSupport({
    isSecureContext: false,
    navigator: { mediaDevices: { getUserMedia: () => Promise.resolve() } },
    location: { hostname: 'recipe.example.com' }
  })
  const unsupportedSupport = describeCameraSupport({
    isSecureContext: true,
    navigator: {},
    location: { hostname: 'recipe.example.com' }
  })

  assert.equal(localSupport.supported, true)
  assert.deepEqual(insecureSupport, {
    supported: false,
    reason: 'insecure-context',
    message: '拍照识别需要通过 HTTPS 或 localhost 访问当前页面。'
  })
  assert.deepEqual(unsupportedSupport, {
    supported: false,
    reason: 'unsupported',
    message: '当前浏览器不支持摄像头访问，请改用上传图片识别。'
  })
})

test('getCameraErrorMessage provides actionable Chinese messages for common camera failures', () => {
  assert.equal(
    getCameraErrorMessage({ name: 'NotAllowedError' }),
    '未获得摄像头权限，请在浏览器设置中允许使用摄像头后重试。'
  )
  assert.equal(
    getCameraErrorMessage({ name: 'NotFoundError' }),
    '未检测到可用摄像头，请连接或启用设备摄像头后重试。'
  )
  assert.equal(
    getCameraErrorMessage({ name: 'NotReadableError' }),
    '摄像头正在被其他程序占用，请关闭相关程序后重试。'
  )
  assert.equal(
    getCameraErrorMessage({ name: 'SecurityError' }),
    '当前页面无法访问摄像头，请通过 HTTPS 或 localhost 打开后重试。'
  )
})

test('createCameraImageFile keeps JPEG metadata and uses a deterministic file name', () => {
  class FakeFile {
    constructor(parts, name, options) {
      this.parts = parts
      this.name = name
      this.type = options.type
      this.lastModified = options.lastModified
    }
  }

  const capturedAt = new Date(2026, 7, 14, 9, 5, 7)
  const blob = { type: 'image/jpeg' }
  const file = createCameraImageFile(blob, capturedAt, FakeFile)

  assert.equal(createCameraImageFileName(capturedAt), 'ingredient-camera-20260814-090507.jpg')
  assert.equal(file.name, 'ingredient-camera-20260814-090507.jpg')
  assert.equal(file.type, 'image/jpeg')
  assert.equal(file.lastModified, capturedAt.getTime())
  assert.deepEqual(file.parts, [blob])
})
