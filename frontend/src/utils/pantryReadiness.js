export function getPantryReadinessErrorMessage(error) {
  const status = error?.response?.status
  if (status === 404 || status === 405) {
    return '当前后端未提供库存匹配接口，请同时重建 backend 与 frontend'
  }
  if (status === 401) {
    return '登录状态已失效，请重新登录'
  }
  if (status === 403) {
    return '当前账号没有读取库存的权限'
  }
  if (status === 400) {
    return error?.response?.data?.message || '库存匹配参数无效，请重试'
  }
  if (status >= 500) {
    return '后端库存匹配服务异常，请稍后重试'
  }
  if (!error?.response) {
    return '后端库存匹配服务未连接，请确认后端已启动'
  }
  return error?.response?.data?.message || '库存匹配失败，请检查后端服务'
}
