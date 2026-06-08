<template>
  <main class="login-page">
    <section class="login-panel" aria-labelledby="login-title">
      <div class="panel-header">
        <p class="eyebrow">Account access</p>
        <h1 id="login-title">Sign in</h1>
      </div>

      <el-tabs v-model="mode" stretch>
        <el-tab-pane label="Phone" name="user">
          <el-form label-position="top" @submit.prevent>
            <el-form-item label="Phone number">
              <el-input
                v-model.trim="phone"
                autocomplete="tel"
                inputmode="tel"
                placeholder="Phone number"
              />
            </el-form-item>
            <el-form-item label="Verification code">
              <el-input
                v-model.trim="code"
                autocomplete="one-time-code"
                inputmode="numeric"
                placeholder="Verification code"
              />
            </el-form-item>

            <div class="form-actions">
              <el-button :loading="requestingCode" @click="requestCode">
                <KeyRound :size="16" aria-hidden="true" />
                <span>Get code</span>
              </el-button>
              <el-button type="primary" :loading="loggingIn" @click="submitUserLogin">
                <LogIn :size="16" aria-hidden="true" />
                <span>Login</span>
              </el-button>
            </div>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="Admin" name="admin">
          <el-form label-position="top" @submit.prevent>
            <el-form-item label="Username">
              <el-input v-model.trim="username" autocomplete="username" placeholder="Username" />
            </el-form-item>
            <el-form-item label="Password">
              <el-input
                v-model="password"
                type="password"
                autocomplete="current-password"
                placeholder="Password"
                show-password
              />
            </el-form-item>

            <div class="form-actions single">
              <el-button type="primary" :loading="loggingIn" @click="submitAdminLogin">
                <ShieldCheck :size="16" aria-hidden="true" />
                <span>Admin login</span>
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

function getErrorMessage(error, fallback) {
  return error?.response?.data?.message || error?.message || fallback
}

function getRedirectPath(defaultPath) {
  return typeof route.query.redirect === 'string' ? route.query.redirect : defaultPath
}

async function requestCode() {
  if (!phone.value) {
    ElMessage.warning('Enter a phone number first')
    return
  }

  requestingCode.value = true

  try {
    const response = await requestUserCode(phone.value)
    code.value = response.data.data.code
    ElMessage.success('Verification code received')
  } catch (error) {
    ElMessage.error(getErrorMessage(error, 'Could not request verification code'))
  } finally {
    requestingCode.value = false
  }
}

async function submitUserLogin() {
  if (!phone.value || !code.value) {
    ElMessage.warning('Enter phone number and verification code')
    return
  }

  loggingIn.value = true

  try {
    const response = await loginUser(phone.value, code.value)
    auth.setAuth(response.data.data)
    const targetPath = getRedirectPath('/')
    router.push(targetPath.startsWith('/admin') ? '/' : targetPath)
  } catch (error) {
    ElMessage.error(getErrorMessage(error, 'User login failed'))
  } finally {
    loggingIn.value = false
  }
}

async function submitAdminLogin() {
  if (!username.value || !password.value) {
    ElMessage.warning('Enter username and password')
    return
  }

  loggingIn.value = true

  try {
    const response = await loginAdmin(username.value, password.value)
    auth.setAuth(response.data.data)
    router.push(getRedirectPath('/admin'))
  } catch (error) {
    ElMessage.error(getErrorMessage(error, 'Admin login failed'))
  } finally {
    loggingIn.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: grid;
  min-height: calc(100vh - 72px);
  place-items: center;
  padding: 40px 16px;
  color: #0f172a;
}

.login-panel {
  width: min(460px, 100%);
  padding: 28px;
  border: 1px solid #bae6fd;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 18px 45px rgba(8, 145, 178, 0.12);
}

.panel-header {
  margin-bottom: 18px;
}

.eyebrow {
  margin: 0 0 8px;
  color: #047857;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0;
  text-transform: uppercase;
}

h1 {
  margin: 0;
  color: #164e63;
  font-size: 28px;
  line-height: 1.2;
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
  min-height: 44px;
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
    min-height: calc(100vh - 123px);
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
