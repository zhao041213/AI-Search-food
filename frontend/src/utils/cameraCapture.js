export const CAMERA_CAPTURE_MIME_TYPE = 'image/jpeg'

const LOCAL_CAMERA_HOSTS = new Set(['localhost', '127.0.0.1', '::1', '[::1]'])

const CAMERA_SUPPORT_MESSAGES = {
  'insecure-context': '拍照识别需要通过 HTTPS 或 localhost 访问当前页面。',
  unsupported: '当前浏览器不支持摄像头访问，请改用上传图片识别。'
}

function isLocalCameraHost(hostname) {
  return LOCAL_CAMERA_HOSTS.has(String(hostname || '').trim().toLowerCase())
}

function normalizeCapturedAt(value) {
  const capturedAt = value instanceof Date ? new Date(value.getTime()) : new Date(value)
  return Number.isNaN(capturedAt.getTime()) ? new Date() : capturedAt
}

function pad(value) {
  return String(value).padStart(2, '0')
}

export function describeCameraSupport(context = globalThis) {
  const location = context?.location
  const isSecure = context?.isSecureContext === true
    || location?.protocol === 'https:'
    || isLocalCameraHost(location?.hostname)
  const hasGetUserMedia = typeof context?.navigator?.mediaDevices?.getUserMedia === 'function'

  if (!isSecure) {
    return {
      supported: false,
      reason: 'insecure-context',
      message: CAMERA_SUPPORT_MESSAGES['insecure-context']
    }
  }

  if (!hasGetUserMedia) {
    return {
      supported: false,
      reason: 'unsupported',
      message: CAMERA_SUPPORT_MESSAGES.unsupported
    }
  }

  return {
    supported: true,
    reason: null,
    message: ''
  }
}

export function getCameraErrorMessage(error) {
  switch (error?.name) {
    case 'NotAllowedError':
    case 'PermissionDeniedError':
      return '未获得摄像头权限，请在浏览器设置中允许使用摄像头后重试。'
    case 'NotFoundError':
    case 'DevicesNotFoundError':
      return '未检测到可用摄像头，请连接或启用设备摄像头后重试。'
    case 'NotReadableError':
    case 'TrackStartError':
      return '摄像头正在被其他程序占用，请关闭相关程序后重试。'
    case 'SecurityError':
      return '当前页面无法访问摄像头，请通过 HTTPS 或 localhost 打开后重试。'
    case 'OverconstrainedError':
    case 'ConstraintNotSatisfiedError':
      return '当前摄像头不支持所需参数，请检查摄像头后重试。'
    case 'AbortError':
      return '摄像头连接已中断，请重新打开摄像头。'
    default:
      return '无法打开摄像头，请稍后重试或改用上传图片识别。'
  }
}

export function createCameraImageFileName(capturedAt = new Date()) {
  const date = normalizeCapturedAt(capturedAt)
  const datePart = `${date.getFullYear()}${pad(date.getMonth() + 1)}${pad(date.getDate())}`
  const timePart = `${pad(date.getHours())}${pad(date.getMinutes())}${pad(date.getSeconds())}`
  return `ingredient-camera-${datePart}-${timePart}.jpg`
}

export function createCameraImageFile(blob, capturedAt = new Date(), FileConstructor = globalThis.File) {
  if (!blob) {
    throw new TypeError('A captured image blob is required.')
  }
  if (typeof FileConstructor !== 'function') {
    throw new TypeError('The File constructor is not available.')
  }

  const date = normalizeCapturedAt(capturedAt)
  return new FileConstructor([blob], createCameraImageFileName(date), {
    type: CAMERA_CAPTURE_MIME_TYPE,
    lastModified: date.getTime()
  })
}
