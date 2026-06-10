<template>
  <main class="login-page">
    <section class="login-panel" aria-labelledby="login-title">
      <div class="panel-header">
        <p class="eyebrow">身份认证</p>
        <h1 id="login-title">登录系统</h1>
        <span>手机号模拟登录与管理员入口</span>
      </div>

      <el-tabs v-model="mode" stretch>
        <el-tab-pane label="手机号登录" name="user">
          <el-form label-position="top" @submit.prevent="submitUserLogin">
            <el-form-item label="手机号">
              <el-input
                v-model.trim="phone"
                autocomplete="tel"
                inputmode="tel"
                placeholder="请输入手机号"
              />
            </el-form-item>
            <el-form-item label="验证码">
              <el-input
                v-model.trim="code"
                autocomplete="one-time-code"
                inputmode="numeric"
                placeholder="请输入验证码"
              />
            </el-form-item>

            <div class="form-actions">
              <el-button native-type="button" :loading="requestingCode" @click="requestCode">
                <KeyRound :size="16" aria-hidden="true" />
                <span>获取验证码</span>
              </el-button>
              <el-button type="primary" native-type="submit" :loading="loggingIn">
                <LogIn :size="16" aria-hidden="true" />
                <span>登录</span>
              </el-button>
            </div>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="管理员登录" name="admin">
          <el-form label-position="top" @submit.prevent="submitAdminLogin">
            <el-form-item label="管理员账号">
              <el-input v-model.trim="username" autocomplete="username" placeholder="请输入管理员账号" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input
                v-model="password"
                type="password"
                autocomplete="current-password"
                placeholder="请输入密码"
                show-password
              />
            </el-form-item>

            <div class="form-actions single">
              <el-button type="primary" native-type="submit" :loading="loggingIn">
                <ShieldCheck :size="16" aria-hidden="true" />
                <span>管理员登录</span>
              </el-button>
            </div>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </section>
  </main>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { KeyRound, LogIn, ShieldCheck } from 'lucide-vue-next'
import { loginAdmin, loginUser, requestUserCode } from '../api/auth'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const mode = ref(getRedirectPath('/').startsWith('/admin') ? 'admin' : 'user')
const phone = ref('')
const code = ref('')
const username = ref('')
const password = ref('')
const requestingCode = ref(false)
const loggingIn = ref(false)

const messageMap = {
  'Invalid admin credentials': '管理员账号或密码错误',
  'Invalid request parameters': '请求参数不合法',
  'Invalid verification code': '验证码错误',
  'User account disabled': '账号已被禁用',
  'Phone is required': '手机号不能为空',
  Unauthorized: '登录状态无效，请重新登录',
  Forbidden: '没有权限访问该功能',
  'Bad credentials': '登录状态无效',
  'Network Error': '网络连接失败，请检查后端服务'
}

function getErrorMessage(error, fallback) {
  const message = error?.response?.data?.message || error?.message
  if (message?.toLowerCase().includes('timeout')) {
    return '请求超时，请稍后重试'
  }

  return messageMap[message] || message || fallback
}

function getRedirectPath(defaultPath) {
  return typeof route.query.redirect === 'string' ? route.query.redirect : defaultPath
}

async function requestCode() {
  if (!phone.value) {
    ElMessage.warning('请先输入手机号')
    return
  }

  requestingCode.value = true

  try {
    const response = await requestUserCode(phone.value)
    code.value = response.data.data.code
    ElMessage.success('验证码已获取')
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '验证码获取失败'))
  } finally {
    requestingCode.value = false
  }
}

async function submitUserLogin() {
  if (!phone.value || !code.value) {
    ElMessage.warning('请输入手机号和验证码')
    return
  }

  loggingIn.value = true

  try {
    const response = await loginUser(phone.value, code.value)
    auth.setAuth(response.data.data)
    const targetPath = getRedirectPath('/')
    router.push(targetPath.startsWith('/admin') ? '/' : targetPath)
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '手机号登录失败'))
  } finally {
    loggingIn.value = false
  }
}

async function submitAdminLogin() {
  if (!username.value || !password.value) {
    ElMessage.warning('请输入管理员账号和密码')
    return
  }

  loggingIn.value = true

  try {
    const response = await loginAdmin(username.value, password.value)
    auth.setAuth(response.data.data)
    router.push(getRedirectPath('/admin'))
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '管理员登录失败'))
  } finally {
    loggingIn.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: grid;
  min-height: calc(100vh - 58px);
  place-items: center;
  padding: 18px 14px;
  color: var(--app-text);
}

.login-panel {
  width: min(460px, 100%);
  padding: 24px;
  border: 1px solid var(--app-line);
  border-radius: 8px;
  background:
    linear-gradient(90deg, var(--app-grid-line-strong) 1px, transparent 1px),
    linear-gradient(var(--app-grid-line-soft) 1px, transparent 1px),
    var(--app-surface);
  background-size: 32px 32px;
  box-shadow:
    var(--app-panel-shadow),
    inset 0 1px 0 var(--app-grid-line-strong);
}

.panel-header {
  display: grid;
  gap: 4px;
  margin-bottom: 16px;
}

.eyebrow {
  margin: 0;
  color: var(--app-text-muted);
  font-family: "Cascadia Mono", "SFMono-Regular", Consolas, monospace;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0;
}

h1 {
  margin: 0;
  color: var(--app-text);
  font-size: 25px;
  line-height: 1.2;
}

.panel-header span {
  color: var(--app-text-muted);
  font-size: 14px;
}

.login-panel :deep(.el-tabs__item) {
  color: var(--app-text-muted);
  font-weight: 800;
}

.login-panel :deep(.el-tabs__item.is-active) {
  color: var(--app-text);
}

.login-panel :deep(.el-tabs__active-bar) {
  background: var(--app-accent);
}

.login-panel :deep(.el-tabs__nav-wrap::after) {
  background: var(--app-line);
}

.form-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 6px;
}

.form-actions.single {
  justify-content: flex-end;
}

.form-actions :deep(.el-button) {
  min-height: 38px;
  margin-left: 0;
  font-weight: 700;
}

.form-actions :deep(.el-button span) {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

@media (max-width: 720px) {
  .login-page {
    min-height: calc(100vh - 111px);
  }
}

@media (max-width: 420px) {
  .login-panel {
    padding: 22px;
  }

  .form-actions,
  .form-actions.single {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
