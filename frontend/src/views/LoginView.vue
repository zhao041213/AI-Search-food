<template>
  <main class="login-page">
    <section class="login-panel" aria-labelledby="login-title">
      <div class="panel-header">
        <p class="eyebrow">身份认证</p>
        <h1 id="login-title">登录系统</h1>
        <span>手机号登录、注册与管理员入口</span>
      </div>

      <el-tabs v-model="mode" stretch>
        <el-tab-pane label="手机号登录" name="user">
          <div class="login-method-switch" role="tablist" aria-label="用户登录方式">
            <el-button
              link
              :type="userLoginMethod === 'code' ? 'primary' : 'info'"
              @click="switchUserLoginMethod('code')"
            >
              验证码登录
            </el-button>
            <el-button
              link
              :type="userLoginMethod === 'password' ? 'primary' : 'info'"
              @click="switchUserLoginMethod('password')"
            >
              密码登录
            </el-button>
            <el-button
              v-if="userLoginMethod === 'reset'"
              link
              type="primary"
              @click="switchUserLoginMethod('reset')"
            >
              找回密码
            </el-button>
          </div>

          <el-form
            v-if="userLoginMethod !== 'reset'"
            label-position="top"
            @submit.prevent="submitUserLogin"
          >
            <el-form-item label="手机号">
              <el-input
                v-model.trim="phone"
                autocomplete="tel"
                inputmode="tel"
                maxlength="11"
                placeholder="请输入手机号"
              />
            </el-form-item>

            <el-form-item v-if="userLoginMethod === 'code'" label="验证码">
              <el-input
                v-model.trim="code"
                autocomplete="one-time-code"
                inputmode="numeric"
                maxlength="6"
                placeholder="请输入验证码"
              />
            </el-form-item>

            <el-form-item v-else label="密码">
              <el-input
                v-model="passwordLogin"
                type="password"
                autocomplete="current-password"
                placeholder="请输入密码"
                show-password
              />
              <p class="field-hint">密码为 8-64 位，必须同时包含字母和数字</p>
            </el-form-item>

            <div class="form-actions">
              <el-button
                v-if="userLoginMethod === 'code'"
                class="code-button"
                native-type="button"
                :disabled="userCodeCountdown > 0"
                :loading="requestingCode"
                @click="requestCode"
              >
                <KeyRound :size="16" aria-hidden="true" />
                <span>{{ userCodeButtonText }}</span>
              </el-button>
              <el-button type="primary" native-type="submit" :loading="loggingIn">
                <LogIn :size="16" aria-hidden="true" />
                <span>登录</span>
              </el-button>
              <el-button
                v-if="userLoginMethod === 'password'"
                class="forgot-button"
                link
                native-type="button"
                @click="switchUserLoginMethod('reset')"
              >
                忘记密码？
              </el-button>
            </div>
          </el-form>

          <el-form v-else label-position="top" @submit.prevent="submitPasswordReset">
            <el-form-item label="手机号">
              <el-input
                v-model.trim="passwordResetPhone"
                autocomplete="tel"
                inputmode="tel"
                maxlength="11"
                placeholder="请输入注册时的手机号"
              />
            </el-form-item>
            <el-form-item label="验证码">
              <el-input
                v-model.trim="passwordResetCode"
                autocomplete="one-time-code"
                inputmode="numeric"
                maxlength="6"
                placeholder="请输入验证码"
              />
            </el-form-item>
            <el-form-item label="新密码">
              <el-input
                v-model="passwordResetNewPassword"
                type="password"
                autocomplete="new-password"
                placeholder="请输入新密码"
                show-password
              />
            </el-form-item>
            <el-form-item label="确认新密码">
              <el-input
                v-model="passwordResetConfirm"
                type="password"
                autocomplete="new-password"
                placeholder="请再次输入新密码"
                show-password
              />
            </el-form-item>

            <div class="form-actions">
              <el-button
                class="code-button"
                native-type="button"
                :disabled="passwordResetCodeCountdown > 0"
                :loading="requestingPasswordResetCode"
                @click="requestPasswordResetCodeAction"
              >
                <KeyRound :size="16" aria-hidden="true" />
                <span>{{ passwordResetCodeButtonText }}</span>
              </el-button>
              <el-button type="primary" native-type="submit" :loading="resettingPassword">
                <LogIn :size="16" aria-hidden="true" />
                <span>重置密码</span>
              </el-button>
              <el-button link native-type="button" @click="switchUserLoginMethod('password')">
                返回密码登录
              </el-button>
            </div>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="用户注册" name="register">
          <el-form label-position="top" @submit.prevent="submitRegistration">
            <el-form-item label="手机号">
              <el-input
                v-model.trim="registrationPhone"
                autocomplete="tel"
                inputmode="tel"
                maxlength="11"
                placeholder="请输入手机号"
              />
            </el-form-item>
            <el-form-item label="验证码">
              <el-input
                v-model.trim="registrationCode"
                autocomplete="one-time-code"
                inputmode="numeric"
                maxlength="6"
                placeholder="请输入验证码"
              />
            </el-form-item>
            <el-form-item label="昵称">
              <el-input
                v-model.trim="registrationNickname"
                autocomplete="nickname"
                maxlength="64"
                placeholder="请输入昵称"
              />
            </el-form-item>
            <el-form-item label="密码">
              <el-input
                v-model="registrationPassword"
                type="password"
                autocomplete="new-password"
                placeholder="请输入密码"
                show-password
              />
              <p class="field-hint">密码为 8-64 位，必须同时包含字母和数字</p>
            </el-form-item>
            <el-form-item label="确认密码">
              <el-input
                v-model="registrationPasswordConfirm"
                type="password"
                autocomplete="new-password"
                placeholder="请再次输入密码"
                show-password
              />
            </el-form-item>

            <div class="form-actions">
              <el-button
                class="code-button"
                native-type="button"
                :disabled="registrationCodeCountdown > 0"
                :loading="requestingRegistrationCode"
                @click="requestRegisterCode"
              >
                <KeyRound :size="16" aria-hidden="true" />
                <span>{{ registrationCodeButtonText }}</span>
              </el-button>
              <el-button type="primary" native-type="submit" :loading="registering">
                <UserPlus :size="16" aria-hidden="true" />
                <span>注册并登录</span>
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
import { computed, onBeforeUnmount, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { KeyRound, LogIn, ShieldCheck, UserPlus } from 'lucide-vue-next'
import {
  loginAdmin,
  loginUser,
  loginUserWithPassword,
  registerUser,
  requestPasswordResetCode,
  requestRegistrationCode,
  requestUserCode,
  resetUserPassword
} from '../api/auth'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const mode = ref(getRedirectPath('/').startsWith('/admin') ? 'admin' : 'user')
const userLoginMethod = ref('code')
const phone = ref('')
const code = ref('')
const userCodeCountdown = ref(0)
const passwordLogin = ref('')
const passwordResetPhone = ref('')
const passwordResetCode = ref('')
const passwordResetNewPassword = ref('')
const passwordResetConfirm = ref('')
const passwordResetCodeCountdown = ref(0)
const registrationPhone = ref('')
const registrationCode = ref('')
const registrationNickname = ref('')
const registrationPassword = ref('')
const registrationPasswordConfirm = ref('')
const registrationCodeCountdown = ref(0)
const username = ref('')
const password = ref('')
const requestingCode = ref(false)
const requestingPasswordResetCode = ref(false)
const requestingRegistrationCode = ref(false)
const registering = ref(false)
const loggingIn = ref(false)
const resettingPassword = ref(false)
let userCodeCountdownTimer
let passwordResetCountdownTimer
let registrationCountdownTimer

const userCodeButtonText = computed(() => formatCodeButtonText(userCodeCountdown.value))
const passwordResetCodeButtonText = computed(() => formatCodeButtonText(passwordResetCodeCountdown.value))
const registrationCodeButtonText = computed(() => formatCodeButtonText(registrationCodeCountdown.value))

const messageMap = {
  'Invalid admin credentials': '管理员账号或密码错误',
  'Invalid request parameters': '请求参数不合法',
  'Invalid verification code': '验证码错误',
  'Verification code invalid': '验证码错误',
  'Verification code expired': '验证码已过期，请重新获取',
  'Verification code already used': '验证码已使用，请重新获取',
  'Too many verification attempts': '验证码错误次数过多，请重新获取',
  'Code requested too frequently': '验证码发送过于频繁，请稍后再试',
  'Phone already registered': '该手机号已经注册',
  'User not registered': '该手机号尚未注册，请先注册',
  'User account disabled': '账号已被禁用',
  'User phone or password invalid': '手机号或密码错误',
  'Password format invalid': '密码格式不合法：需为 8-64 位且同时包含字母和数字',
  'Phone is required': '手机号不能为空',
  Unauthorized: '登录状态无效，请重新登录',
  Forbidden: '没有权限访问该功能',
  'Bad credentials': '登录状态无效',
  'Network Error': '网络连接失败，请检查后端服务'
}

function getErrorMessage(error, fallback) {
  const message = error?.response?.data?.message || error?.message
  const locked = /^Password login locked; retry after (\d+) seconds$/.exec(message || '')
  if (locked) {
    return `密码登录已锁定，请在 ${locked[1]} 秒后重试`
  }
  if (message?.toLowerCase().includes('timeout')) {
    return '请求超时，请稍后重试'
  }

  return messageMap[message] || message || fallback
}

function getRedirectPath(defaultPath) {
  return typeof route.query.redirect === 'string' ? route.query.redirect : defaultPath
}

function formatCodeButtonText(countdown) {
  return countdown > 0 ? `${countdown} 秒后重试` : '获取验证码'
}

function switchUserLoginMethod(nextMethod) {
  if (nextMethod === 'reset' && !passwordResetPhone.value) {
    passwordResetPhone.value = phone.value
  }
  userLoginMethod.value = nextMethod
}

async function requestCode() {
  if (!/^1[3-9]\d{9}$/.test(phone.value)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }

  requestingCode.value = true

  try {
    const response = await requestUserCode(phone.value)
    const returnedCode = response.data.data?.code
    if (returnedCode) {
      code.value = returnedCode
    }
    ElMessage.success(returnedCode ? '验证码已获取' : '验证码已发送')
    startUserCodeCountdown(response.data.data?.retryAfterSeconds)
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '验证码获取失败'))
  } finally {
    requestingCode.value = false
  }
}

async function requestPasswordResetCodeAction() {
  if (!/^1[3-9]\d{9}$/.test(passwordResetPhone.value)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }

  requestingPasswordResetCode.value = true

  try {
    const response = await requestPasswordResetCode(passwordResetPhone.value)
    const returnedCode = response.data.data?.code
    if (returnedCode) {
      passwordResetCode.value = returnedCode
    }
    ElMessage.success(returnedCode ? '验证码已获取' : '验证码已发送')
    startPasswordResetCountdown(response.data.data?.retryAfterSeconds)
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '验证码获取失败'))
  } finally {
    requestingPasswordResetCode.value = false
  }
}

async function requestRegisterCode() {
  if (!/^1[3-9]\d{9}$/.test(registrationPhone.value)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }

  requestingRegistrationCode.value = true

  try {
    const response = await requestRegistrationCode(registrationPhone.value)
    const returnedCode = response.data.data?.code
    if (returnedCode) {
      registrationCode.value = returnedCode
    }
    ElMessage.success(returnedCode ? '验证码已获取' : '验证码已发送')
    startRegistrationCountdown(response.data.data?.retryAfterSeconds)
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '验证码获取失败'))
  } finally {
    requestingRegistrationCode.value = false
  }
}

function validPassword(passwordValue) {
  return /^(?=.*[A-Za-z])(?=.*\d)[\s\S]{8,64}$/.test(passwordValue)
}

async function submitRegistration() {
  if (!/^1[3-9]\d{9}$/.test(registrationPhone.value)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  if (!/^\d{6}$/.test(registrationCode.value)) {
    ElMessage.warning('请输入 6 位验证码')
    return
  }
  if (!registrationNickname.value) {
    ElMessage.warning('请输入昵称')
    return
  }
  if (!validPassword(registrationPassword.value)) {
    ElMessage.warning('密码需为 8-64 位且同时包含字母和数字')
    return
  }
  if (registrationPassword.value !== registrationPasswordConfirm.value) {
    ElMessage.warning('两次密码输入不一致')
    return
  }

  registering.value = true

  try {
    const response = await registerUser(
      registrationPhone.value,
      registrationCode.value,
      registrationNickname.value,
      registrationPassword.value
    )
    auth.setAuth(response.data.data)
    registrationPassword.value = ''
    registrationPasswordConfirm.value = ''
    const targetPath = getRedirectPath('/')
    router.push(targetPath.startsWith('/admin') ? '/' : targetPath)
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '注册失败'))
  } finally {
    registering.value = false
  }
}

async function submitUserLogin() {
  if (userLoginMethod.value === 'password') {
    await submitPasswordLogin()
    return
  }
  if (!/^1[3-9]\d{9}$/.test(phone.value) || !/^\d{6}$/.test(code.value)) {
    ElMessage.warning('请输入正确的手机号和 6 位验证码')
    return
  }

  loggingIn.value = true

  try {
    const response = await loginUser(phone.value, code.value)
    auth.setAuth(response.data.data)
    code.value = ''
    const targetPath = getRedirectPath('/')
    router.push(targetPath.startsWith('/admin') ? '/' : targetPath)
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '手机号登录失败'))
  } finally {
    loggingIn.value = false
  }
}

async function submitPasswordLogin() {
  if (!/^1[3-9]\d{9}$/.test(phone.value) || !passwordLogin.value) {
    ElMessage.warning('请输入正确的手机号和密码')
    return
  }

  loggingIn.value = true

  try {
    const response = await loginUserWithPassword(phone.value, passwordLogin.value)
    auth.setAuth(response.data.data)
    passwordLogin.value = ''
    const targetPath = getRedirectPath('/')
    router.push(targetPath.startsWith('/admin') ? '/' : targetPath)
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '密码登录失败'))
  } finally {
    loggingIn.value = false
  }
}

async function submitPasswordReset() {
  if (!/^1[3-9]\d{9}$/.test(passwordResetPhone.value)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  if (!/^\d{6}$/.test(passwordResetCode.value)) {
    ElMessage.warning('请输入 6 位验证码')
    return
  }
  if (!validPassword(passwordResetNewPassword.value)) {
    ElMessage.warning('密码需为 8-64 位且同时包含字母和数字')
    return
  }
  if (passwordResetNewPassword.value !== passwordResetConfirm.value) {
    ElMessage.warning('两次密码输入不一致')
    return
  }

  resettingPassword.value = true

  try {
    await resetUserPassword(
      passwordResetPhone.value,
      passwordResetCode.value,
      passwordResetNewPassword.value
    )
    auth.logout()
    phone.value = passwordResetPhone.value
    passwordLogin.value = ''
    passwordResetCode.value = ''
    passwordResetNewPassword.value = ''
    passwordResetConfirm.value = ''
    switchUserLoginMethod('password')
    ElMessage.success('密码已重置，请重新登录')
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '密码重置失败'))
  } finally {
    resettingPassword.value = false
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
    password.value = ''
    router.push(getRedirectPath('/admin'))
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '管理员登录失败'))
  } finally {
    loggingIn.value = false
  }
}

function startUserCodeCountdown(retryAfterSeconds) {
  userCodeCountdownTimer = startCountdown(
    userCodeCountdown,
    userCodeCountdownTimer,
    retryAfterSeconds,
    (timer) => { userCodeCountdownTimer = timer }
  )
}

function startPasswordResetCountdown(retryAfterSeconds) {
  passwordResetCountdownTimer = startCountdown(
    passwordResetCodeCountdown,
    passwordResetCountdownTimer,
    retryAfterSeconds,
    (timer) => { passwordResetCountdownTimer = timer }
  )
}

function startRegistrationCountdown(retryAfterSeconds) {
  registrationCountdownTimer = startCountdown(
    registrationCodeCountdown,
    registrationCountdownTimer,
    retryAfterSeconds,
    (timer) => { registrationCountdownTimer = timer }
  )
}

function startCountdown(countdown, currentTimer, retryAfterSeconds, saveTimer) {
  clearInterval(currentTimer)
  const parsedSeconds = Number(retryAfterSeconds)
  countdown.value = Number.isFinite(parsedSeconds) ? Math.max(0, Math.ceil(parsedSeconds)) : 60
  if (countdown.value === 0) {
    saveTimer(undefined)
    return undefined
  }
  const timer = window.setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      clearInterval(timer)
      saveTimer(undefined)
    }
  }, 1000)
  saveTimer(timer)
  return timer
}

onBeforeUnmount(() => {
  clearInterval(userCodeCountdownTimer)
  clearInterval(passwordResetCountdownTimer)
  clearInterval(registrationCountdownTimer)
})
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

.login-method-switch {
  display: flex;
  gap: 12px;
  margin: 4px 0 14px;
}

.login-method-switch :deep(.el-button) {
  margin-left: 0;
  font-weight: 700;
}

.field-hint {
  margin: 5px 0 0;
  color: var(--app-text-muted);
  font-size: 12px;
  line-height: 1.4;
}

.forgot-button {
  margin-left: auto !important;
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

.form-actions :deep(.code-button) {
  min-width: 132px;
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

  .forgot-button {
    margin-left: 0 !important;
  }
}
</style>
