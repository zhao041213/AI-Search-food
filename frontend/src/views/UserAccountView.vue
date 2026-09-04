<template>
  <section class="account-page" aria-labelledby="account-title">
    <div class="account-heading">
      <div>
        <p class="eyebrow">我的身份档案</p>
        <h1 id="account-title">账号中心</h1>
        <p class="heading-copy">管理你的个人资料与登录安全。手机号仅用于登录和身份验证，不能在此修改。</p>
      </div>
      <RouterLink class="back-link" to="/">返回工作台 <span aria-hidden="true">↗</span></RouterLink>
    </div>

    <div v-if="loading" class="account-loading" role="status">正在读取账号资料…</div>

    <template v-else>
      <div class="account-grid">
        <article class="identity-card account-panel">
          <div class="identity-kicker"><span class="signal-dot" />账号状态</div>
          <div class="avatar-wrap">
            <img v-if="avatarSource" :src="avatarSource" alt="当前头像" class="avatar-image" />
            <div v-else class="avatar-fallback" aria-hidden="true">{{ avatarInitial }}</div>
            <button class="avatar-edit" type="button" aria-label="选择新头像" @click="openFilePicker">
              <Camera :size="17" aria-hidden="true" />
            </button>
          </div>
          <h2>{{ account.nickname || '未设置昵称' }}</h2>
          <p class="identity-phone">{{ account.phone || '—' }}</p>
          <span class="status-chip"><span class="status-pip" />{{ statusLabel }}</span>
          <div class="identity-rule" />
          <p class="identity-note">头像只对当前账号开放访问，上传后会自动替换旧头像。</p>
          <input
            ref="fileInput"
            class="visually-hidden"
            type="file"
            accept="image/jpeg,image/png,image/webp"
            @change="handleAvatarChange"
          />
          <div class="avatar-actions">
            <el-button class="soft-button" :loading="uploading" @click="openFilePicker">更换头像</el-button>
            <el-button v-if="account.avatarUrl" class="text-danger-button" :disabled="uploading" @click="removeAvatar">
              删除头像
            </el-button>
          </div>
          <p class="upload-hint">支持 JPG、PNG、WebP，单张不超过 2MB</p>
        </article>

        <div class="account-main-column">
          <article class="account-panel profile-panel">
            <div class="panel-heading">
              <div>
                <p class="panel-label">PROFILE / 01</p>
                <h2>基本资料</h2>
              </div>
              <span class="panel-mark">A—01</span>
            </div>
            <form class="profile-form" @submit.prevent="saveProfile">
              <label class="field-label" for="nickname">昵称</label>
              <div class="nickname-row">
                <el-input
                  id="nickname"
                  v-model="nickname"
                  class="account-input"
                  maxlength="64"
                  minlength="2"
                  show-word-limit
                  placeholder="给自己一个好记的名字"
                />
                <el-button class="save-button" type="primary" native-type="submit" :loading="saving">保存资料</el-button>
              </div>
              <p class="field-hint">2–64 个字符，保存后会同步更新顶部账号展示。</p>
            </form>

            <dl class="profile-facts">
              <div>
                <dt>绑定手机号</dt>
                <dd>{{ account.phone || '—' }} <span class="readonly-tag">仅展示</span></dd>
              </div>
              <div>
                <dt>注册时间</dt>
                <dd>{{ formatDate(account.registeredAt) }}</dd>
              </div>
              <div>
                <dt>最近登录</dt>
                <dd>{{ formatDate(account.lastLoginAt) }}</dd>
              </div>
            </dl>
          </article>

          <article class="account-panel security-panel">
            <div class="panel-heading">
              <div>
                <p class="panel-label">SECURITY / 02</p>
                <h2>登录安全</h2>
              </div>
              <ShieldCheck :size="22" aria-hidden="true" />
            </div>
            <div class="security-action">
              <div>
                <h3>退出全部设备</h3>
                <p>立即使其他浏览器和设备上的登录状态失效。当前页面也会退出。</p>
              </div>
              <el-button class="outline-button" :loading="loggingOutAll" @click="handleLogoutAll">退出全部设备</el-button>
            </div>
          </article>

          <article class="account-panel danger-panel">
            <div class="panel-heading">
              <div>
                <p class="panel-label">IRREVERSIBLE / 03</p>
                <h2>注销账号</h2>
              </div>
              <AlertTriangle :size="22" aria-hidden="true" />
            </div>
            <p class="danger-copy">注销后将无法恢复当前账号数据。你的资料、密码和头像会被清除，历史记录中的用户关系会保留为匿名状态。</p>
            <div class="cancel-form">
              <label class="field-label" for="cancel-code">短信验证码</label>
              <div class="code-row">
                <el-input id="cancel-code" v-model="cancelCode" class="account-input" inputmode="numeric" maxlength="6" placeholder="输入 6 位验证码" />
                <el-button class="soft-button" :disabled="codeCountdown > 0 || requestingCode" :loading="requestingCode" @click="requestCancelCode">
                  {{ codeCountdown > 0 ? `${codeCountdown}s 后重发` : '获取验证码' }}
                </el-button>
              </div>
              <el-button class="danger-button" :loading="cancelling" :disabled="!cancelCode" @click="handleCancelAccount">
                注销当前账号
              </el-button>
            </div>
          </article>
        </div>
      </div>
    </template>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter, RouterLink } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { AlertTriangle, Camera, ShieldCheck } from 'lucide-vue-next'
import { cancelMyAccount, deleteMyAvatar, getMyAccount, loadMyAvatar, logoutAllDevices, requestAccountCancellationCode, updateMyProfile, uploadMyAvatar } from '../api/userAccount'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const account = ref({})
const nickname = ref('')
const cancelCode = ref('')
const loading = ref(true)
const saving = ref(false)
const uploading = ref(false)
const requestingCode = ref(false)
const cancelling = ref(false)
const loggingOutAll = ref(false)
const codeCountdown = ref(0)
const fileInput = ref(null)
const avatarPreview = ref('')
const avatarImageUrl = ref('')
let countdownTimer = null

const avatarSource = computed(() => avatarPreview.value || avatarImageUrl.value)
const avatarInitial = computed(() => (account.value.nickname || '用户').trim().slice(0, 1).toUpperCase())
const statusLabel = computed(() => account.value.status === 'ACTIVE' ? '账号正常' : '账号不可用')

onMounted(loadAccount)
onBeforeUnmount(() => {
  revokeAvatarPreview()
  revokeAvatarImageUrl()
  clearCountdown()
})

async function loadAccount() {
  loading.value = true
  try {
    const response = await getMyAccount()
    account.value = response.data.data || {}
    nickname.value = account.value.nickname || ''
    await loadAvatarImage()
  } catch (error) {
    ElMessage.error(messageFrom(error, '账号资料读取失败，请稍后重试'))
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  if (saving.value) return
  if (nickname.value.trim().length < 2) {
    ElMessage.warning('昵称至少需要 2 个字符')
    return
  }
  saving.value = true
  try {
    const response = await updateMyProfile(nickname.value.trim())
    account.value = response.data.data || account.value
    nickname.value = account.value.nickname || nickname.value.trim()
    auth.setDisplayName(nickname.value)
    ElMessage.success('资料已保存')
  } catch (error) {
    ElMessage.error(messageFrom(error, '资料保存失败，请稍后重试'))
  } finally {
    saving.value = false
  }
}

function openFilePicker() {
  fileInput.value?.click()
}

async function handleAvatarChange(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
    ElMessage.error('头像仅支持 JPG、PNG 或 WebP 图片')
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.error('头像图片不能超过 2MB')
    return
  }
  revokeAvatarPreview()
  avatarPreview.value = URL.createObjectURL(file)
  uploading.value = true
  try {
    const response = await uploadMyAvatar(file)
    account.value = response.data.data || account.value
    try {
      await loadAvatarImage()
    } catch {
      revokeAvatarImageUrl()
    }
    auth.refreshAvatar()
    revokeAvatarPreview()
    ElMessage.success('头像已更新')
  } catch (error) {
    revokeAvatarPreview()
    ElMessage.error(messageFrom(error, '头像上传失败，请稍后重试'))
  } finally {
    uploading.value = false
  }
}

async function removeAvatar() {
  if (uploading.value) return
  try {
    await ElMessageBox.confirm('确定删除当前头像吗？', '删除头像', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '保留' })
  } catch {
    return
  }
  uploading.value = true
  try {
    const response = await deleteMyAvatar()
    account.value = response.data.data || { ...account.value, avatarUrl: null }
    revokeAvatarImageUrl()
    revokeAvatarPreview()
    auth.refreshAvatar()
    ElMessage.success('头像已删除')
  } catch (error) {
    ElMessage.error(messageFrom(error, '头像删除失败，请稍后重试'))
  } finally {
    uploading.value = false
  }
}

async function handleLogoutAll() {
  if (loggingOutAll.value) return
  try {
    await ElMessageBox.confirm('这会让全部设备立即退出，包括当前页面。确定继续吗？', '退出全部设备', { type: 'warning', confirmButtonText: '确认退出', cancelButtonText: '暂不退出' })
  } catch {
    return
  }
  loggingOutAll.value = true
  try {
    await logoutAllDevices()
    auth.logout()
    await router.push({ name: 'login' })
  } catch (error) {
    ElMessage.error(messageFrom(error, '操作失败，请稍后重试'))
  } finally {
    loggingOutAll.value = false
  }
}

async function requestCancelCode() {
  if (requestingCode.value || codeCountdown.value > 0) return
  requestingCode.value = true
  try {
    const response = await requestAccountCancellationCode()
    const retryAfter = Number(response.data.data?.retryAfterSeconds || 60)
    codeCountdown.value = retryAfter
    clearCountdown()
    countdownTimer = window.setInterval(() => {
      codeCountdown.value -= 1
      if (codeCountdown.value <= 0) clearCountdown()
    }, 1000)
    ElMessage.success('验证码已发送，请查收短信')
  } catch (error) {
    ElMessage.error(messageFrom(error, '验证码发送失败，请稍后重试'))
  } finally {
    requestingCode.value = false
  }
}

async function handleCancelAccount() {
  if (cancelling.value) return
  try {
    await ElMessageBox.confirm('注销后无法恢复当前账号数据，确定要注销吗？', '最后确认', { type: 'warning', confirmButtonText: '确认注销', cancelButtonText: '我再想想' })
  } catch {
    return
  }
  cancelling.value = true
  try {
    await cancelMyAccount(cancelCode.value.trim(), true)
    auth.logout()
    await router.push({ name: 'login' })
  } catch (error) {
    ElMessage.error(messageFrom(error, '注销失败，请检查验证码后重试'))
  } finally {
    cancelling.value = false
  }
}

function formatDate(value) {
  if (!value) return '暂无记录'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '暂无记录'
  return new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }).format(date)
}

function revokeAvatarPreview() {
  if (avatarPreview.value) URL.revokeObjectURL(avatarPreview.value)
  avatarPreview.value = ''
}

async function loadAvatarImage() {
  if (!account.value.avatarUrl) {
    revokeAvatarImageUrl()
    return
  }
  try {
    const response = await loadMyAvatar()
    revokeAvatarImageUrl()
    avatarImageUrl.value = URL.createObjectURL(response.data)
  } catch (error) {
    revokeAvatarImageUrl()
    if (error?.response?.status !== 404) throw error
  }
}

function revokeAvatarImageUrl() {
  if (avatarImageUrl.value) URL.revokeObjectURL(avatarImageUrl.value)
  avatarImageUrl.value = ''
}

function clearCountdown() {
  if (countdownTimer) window.clearInterval(countdownTimer)
  countdownTimer = null
}

function messageFrom(error, fallback) {
  return error?.response?.data?.message || fallback
}
</script>

<style scoped>
.account-page {
  width: min(1180px, 100%);
  margin: 0 auto;
  padding: 10px 0 64px;
}

.account-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 28px;
}

.eyebrow,
.panel-label {
  margin: 0 0 8px;
  color: var(--app-accent);
  font-size: 11px;
  font-weight: 900;
  letter-spacing: .18em;
}

h1,
h2,
h3,
p {
  margin-top: 0;
}

h1 {
  margin-bottom: 9px;
  color: var(--app-text);
  font-size: clamp(30px, 4vw, 48px);
  letter-spacing: -.06em;
  line-height: 1;
}

.heading-copy {
  max-width: 620px;
  margin-bottom: 0;
  color: var(--app-text-muted);
  font-size: 14px;
  line-height: 1.7;
}

.back-link {
  flex: 0 0 auto;
  color: var(--app-text-muted);
  font-size: 13px;
  font-weight: 800;
  text-decoration: none;
}

.back-link:hover { color: var(--app-accent); }

.account-loading {
  min-height: 240px;
  display: grid;
  place-items: center;
  border: 1px dashed var(--app-line-strong);
  color: var(--app-text-muted);
}

.account-grid {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  align-items: start;
  gap: 18px;
}

.account-main-column { display: grid; gap: 18px; }

.account-panel {
  border: 1px solid var(--app-line);
  border-radius: 8px;
  background: var(--app-surface);
  box-shadow: var(--app-panel-shadow);
}

.identity-card {
  position: sticky;
  top: 22px;
  padding: 24px;
  overflow: hidden;
  background: linear-gradient(160deg, var(--app-surface-strong), var(--app-surface));
}

.identity-kicker {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--app-text-faint);
  font-size: 11px;
  font-weight: 900;
  letter-spacing: .12em;
  text-transform: uppercase;
}

.signal-dot,
.status-pip { width: 7px; height: 7px; border-radius: 50%; background: var(--app-accent); box-shadow: 0 0 0 4px var(--app-accent-soft); }

.avatar-wrap { position: relative; width: 112px; height: 112px; margin: 28px 0 20px; }
.avatar-image,
.avatar-fallback { width: 100%; height: 100%; border-radius: 50%; }
.avatar-image { display: block; object-fit: cover; }
.avatar-fallback { display: grid; place-items: center; color: var(--app-accent-text); background: var(--app-accent); font-size: 42px; font-weight: 900; }
.avatar-edit { position: absolute; right: 0; bottom: 0; display: grid; width: 34px; height: 34px; place-items: center; border: 3px solid var(--app-surface); border-radius: 50%; color: var(--app-accent-text); background: var(--app-text); cursor: pointer; }
.avatar-edit:hover { background: var(--app-accent); }
.identity-card h2 { margin-bottom: 5px; color: var(--app-text); font-size: 25px; letter-spacing: -.04em; }
.identity-phone { margin-bottom: 14px; color: var(--app-text-muted); font-size: 14px; }
.status-chip { display: inline-flex; align-items: center; gap: 8px; padding: 6px 10px; border: 1px solid var(--app-line); border-radius: 999px; color: var(--app-text-soft); font-size: 12px; font-weight: 800; }
.status-pip { width: 6px; height: 6px; box-shadow: none; }
.identity-rule { height: 1px; margin: 24px 0 16px; background: var(--app-line); }
.identity-note, .upload-hint { color: var(--app-text-faint); font-size: 12px; line-height: 1.6; }
.identity-note { margin-bottom: 20px; }
.avatar-actions { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.upload-hint { margin: 10px 0 0; }

.profile-panel, .security-panel, .danger-panel { padding: 25px 28px; }
.panel-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; margin-bottom: 22px; }
.panel-heading h2 { margin-bottom: 0; color: var(--app-text); font-size: 23px; letter-spacing: -.04em; }
.panel-heading > svg { color: var(--app-accent); }
.panel-mark { color: var(--app-text-faint); font-family: monospace; font-size: 12px; }
.profile-form { padding-bottom: 22px; border-bottom: 1px solid var(--app-line); }
.field-label { display: block; margin-bottom: 8px; color: var(--app-text-soft); font-size: 13px; font-weight: 900; }
.nickname-row, .code-row { display: flex; align-items: center; gap: 10px; }
.account-input { flex: 1; }
.field-hint { margin: 8px 0 0; color: var(--app-text-faint); font-size: 12px; }
.profile-facts { display: grid; grid-template-columns: repeat(3, 1fr); gap: 18px; margin: 22px 0 0; }
.profile-facts div { min-width: 0; }
.profile-facts dt { margin-bottom: 7px; color: var(--app-text-faint); font-size: 12px; }
.profile-facts dd { margin: 0; color: var(--app-text-soft); font-size: 13px; font-weight: 800; line-height: 1.5; }
.readonly-tag { display: inline-block; margin-left: 5px; padding: 2px 5px; color: var(--app-text-faint); border: 1px solid var(--app-line); font-size: 10px; font-weight: 700; }
.security-action { display: flex; align-items: center; justify-content: space-between; gap: 24px; padding: 15px 0 2px; }
.security-action h3 { margin-bottom: 6px; color: var(--app-text-soft); font-size: 15px; }
.security-action p, .danger-copy { max-width: 650px; margin-bottom: 0; color: var(--app-text-muted); font-size: 13px; line-height: 1.7; }
.danger-panel { border-color: color-mix(in srgb, #c56b55 32%, var(--app-line)); }
.danger-panel .panel-heading > svg { color: #c56b55; }
.danger-copy { margin-bottom: 20px; }
.cancel-form { max-width: 600px; }
.danger-button { margin-top: 15px; color: #fff !important; border-color: #b45c48 !important; background: #b45c48 !important; }
.danger-button:hover { border-color: #914332 !important; background: #914332 !important; }
.soft-button, .outline-button, .save-button, .text-danger-button { min-height: 36px; font-weight: 800; }
.soft-button { color: var(--app-text-soft); border-color: var(--app-line-strong); background: var(--app-surface-strong); }
.soft-button:hover, .outline-button:hover { color: var(--app-accent); border-color: var(--app-accent); }
.outline-button { color: var(--app-text-soft); border-color: var(--app-line-strong); background: transparent; }
.save-button { flex: 0 0 auto; }
.text-danger-button { padding: 0 4px; color: #b45c48; border: 0; background: transparent; }
.visually-hidden { position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px; overflow: hidden; clip: rect(0, 0, 0, 0); white-space: nowrap; border: 0; }

button:focus-visible, a:focus-visible, input:focus-visible { outline: 3px solid color-mix(in srgb, var(--app-accent) 55%, transparent); outline-offset: 3px; }

@media (prefers-reduced-motion: reduce) { *, *::before, *::after { scroll-behavior: auto !important; transition-duration: .01ms !important; animation-duration: .01ms !important; } }

@media (max-width: 860px) {
  .account-page { padding-top: 0; }
  .account-grid { grid-template-columns: 1fr; }
  .identity-card { position: static; }
  .identity-card { display: grid; grid-template-columns: 112px minmax(0, 1fr); column-gap: 18px; align-items: center; }
  .identity-kicker, .avatar-wrap, .identity-card h2, .identity-phone, .status-chip, .identity-rule, .identity-note, .avatar-actions, .upload-hint { grid-column: 1 / -1; }
  .avatar-wrap { margin: 24px 0 0; }
  .identity-card h2 { margin-top: 0; }
}

@media (max-width: 620px) {
  .account-heading { align-items: flex-start; flex-direction: column; gap: 14px; }
  .profile-panel, .security-panel, .danger-panel, .identity-card { padding: 20px; }
  .nickname-row, .code-row, .security-action { align-items: stretch; flex-direction: column; }
  .profile-facts { grid-template-columns: 1fr; gap: 13px; }
  .save-button, .code-row .soft-button, .outline-button { width: 100%; }
}
</style>
