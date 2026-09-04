<template>
  <main class="admin-page">
    <section class="admin-shell" aria-labelledby="admin-title">
      <header class="admin-header">
        <div>
          <p class="eyebrow">系统控制台</p>
          <h1 id="admin-title">管理后台</h1>
        </div>
        <div class="admin-header-actions">
          <el-radio-group v-model="activePanel" size="small" aria-label="后台功能">
            <el-radio-button value="overview">运营概览</el-radio-button>
            <el-radio-button value="settings">系统设置</el-radio-button>
            <el-radio-button value="hot-ingredients">热门分析</el-radio-button>
            <el-radio-button value="operation-logs">操作日志</el-radio-button>
            <el-radio-button value="error-logs">异常日志</el-radio-button>
            <el-radio-button value="user-management">用户管理</el-radio-button>
          </el-radio-group>
          <el-button type="primary" plain @click="logout">
            <LogOut :size="16" aria-hidden="true" />
            <span>退出登录</span>
          </el-button>
        </div>
      </header>

      <AdminOperationsOverview v-if="activePanel === 'overview'" />

      <div v-else-if="activePanel === 'settings'" class="admin-grid">
        <section class="summary-grid" aria-label="系统概览">
          <article class="summary-card">
            <span class="card-label">当前账号</span>
            <strong>{{ auth.displayName || '管理员' }}</strong>
          </article>
          <article class="summary-card">
            <span class="card-label">角色</span>
            <strong>{{ roleLabel }}</strong>
          </article>
          <article class="summary-card">
            <span class="card-label">会话状态</span>
            <strong>{{ auth.isLoggedIn ? '已登录' : '未登录' }}</strong>
          </article>
        </section>

        <section class="config-panel" v-loading="configLoading" aria-labelledby="ai-config-title">
          <div class="panel-title">
            <PlugZap :size="20" aria-hidden="true" />
            <div>
              <span>模型接入</span>
              <h2 id="ai-config-title">AI 接入配置</h2>
            </div>
          </div>

          <el-form class="config-form" label-position="top" @submit.prevent="saveConfig">
            <div class="config-grid">
              <el-form-item label="服务商">
                <el-select v-model="aiConfig.provider" placeholder="请选择服务商">
                  <el-option label="千问" value="qwen" />
                  <el-option label="DeepSeek" value="deepseek" />
                </el-select>
              </el-form-item>

              <el-form-item label="模型名称">
                <el-input v-model.trim="aiConfig.modelName" placeholder="例如：qwen-plus" />
              </el-form-item>

              <el-form-item label="接口地址">
                <el-input
                  v-model.trim="aiConfig.endpoint"
                  placeholder="https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
                />
              </el-form-item>

              <el-form-item label="API Key">
                <el-input
                  v-model.trim="aiConfig.apiKey"
                  type="password"
                  show-password
                  autocomplete="off"
                  placeholder="留空则保留现有 Key"
                />
              </el-form-item>
            </div>

            <div class="config-footer">
              <div class="config-status">
                <span>Key 状态</span>
                <el-tag :type="apiKeyConfigured ? 'success' : 'warning'">
                  {{ apiKeyConfigured ? `已配置 ${apiKeyPreview}` : '未配置' }}
                </el-tag>
              </div>
              <div class="config-actions">
                <el-switch
                  v-model="aiConfig.enabled"
                  active-text="启用"
                  inactive-text="停用"
                  inline-prompt
                />
                <el-button type="primary" :loading="savingConfig" @click="saveConfig">
                  <Save :size="16" aria-hidden="true" />
                  <span>保存配置</span>
                </el-button>
              </div>
            </div>
          </el-form>
        </section>

      </div>
      <AdminHotIngredientsPanel v-else-if="activePanel === 'hot-ingredients'" />
      <AdminErrorLogs v-else-if="activePanel === 'error-logs'" />
      <AdminUserManagement v-else-if="activePanel === 'user-management'" />
      <AdminOperationLogs v-else />
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { LogOut, PlugZap, Save } from 'lucide-vue-next'
import { getTextRecipeAiConfig, saveTextRecipeAiConfig } from '../api/adminAiConfig'
import AdminHotIngredientsPanel from '../components/AdminHotIngredientsPanel.vue'
import AdminErrorLogs from '../components/AdminErrorLogs.vue'
import AdminOperationLogs from '../components/AdminOperationLogs.vue'
import AdminOperationsOverview from '../components/AdminOperationsOverview.vue'
import AdminUserManagement from '../components/AdminUserManagement.vue'
import { useAuthStore } from '../stores/auth'
import { buildAdminPanelQuery, resolveAdminPanel } from '../utils/hotIngredientNavigation'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const roleLabel = computed(() => ({ ADMIN: '管理员', USER: '普通用户' })[auth.role] || auth.role)
const configLoading = ref(false)
const savingConfig = ref(false)
const apiKeyConfigured = ref(false)
const apiKeyPreview = ref('')
const activePanel = computed({
  get: () => resolveAdminPanel(route.query.panel),
  set: (panel) => {
    router.replace({ name: 'admin', query: buildAdminPanelQuery(route.query, panel) })
  }
})
const aiConfig = ref({
  provider: 'qwen',
  modelName: 'qwen-plus',
  endpoint: 'https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions',
  apiKey: '',
  enabled: true
})

onMounted(() => {
  loadConfig()
})

async function loadConfig() {
  configLoading.value = true
  try {
    const response = await getTextRecipeAiConfig()
    applyConfigResponse(response.data.data)
  } catch (error) {
    ElMessage.error(errorMessage(error, 'AI 接入配置加载失败'))
  } finally {
    configLoading.value = false
  }
}

async function saveConfig() {
  if (!aiConfig.value.provider || !aiConfig.value.modelName || !aiConfig.value.endpoint) {
    ElMessage.warning('请完整填写服务商、模型名称和接口地址')
    return
  }

  savingConfig.value = true
  try {
    const response = await saveTextRecipeAiConfig({
      provider: aiConfig.value.provider,
      modelName: aiConfig.value.modelName,
      endpoint: aiConfig.value.endpoint,
      apiKey: aiConfig.value.apiKey,
      enabled: aiConfig.value.enabled
    })
    applyConfigResponse(response.data.data)
    ElMessage.success('AI 接入配置已保存')
  } catch (error) {
    ElMessage.error(errorMessage(error, 'AI 接入配置保存失败'))
  } finally {
    savingConfig.value = false
  }
}

function applyConfigResponse(config) {
  aiConfig.value = {
    provider: config?.provider || 'qwen',
    modelName: config?.modelName || 'qwen-plus',
    endpoint: config?.endpoint || 'https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions',
    apiKey: '',
    enabled: config?.enabled ?? true
  }
  apiKeyConfigured.value = Boolean(config?.apiKeyConfigured)
  apiKeyPreview.value = config?.apiKeyPreview || ''
}

function errorMessage(error, fallback) {
  const message = error?.response?.data?.message || error?.message
  const messages = {
    Unauthorized: '登录状态无效，请重新登录',
    Forbidden: '当前账号没有管理员权限',
    'Invalid request parameters': '请求参数不合法',
    'Network Error': '网络连接失败，请检查后端服务'
  }
  return messages[message] || message || fallback
}

function logout() {
  auth.logout()
  router.push({ name: 'login' })
}
</script>

<style scoped>
.admin-page {
  height: calc(100vh - 58px);
  overflow: hidden;
  padding: clamp(10px, 1.4vw, 18px);
  color: var(--app-text);
}

.admin-shell {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 10px;
  width: min(1360px, 100%);
  height: 100%;
  margin: 0 auto;
}

.admin-header {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 12px;
}

.admin-header :deep(.el-button span) {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.admin-header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.eyebrow {
  margin: 0 0 4px;
  color: var(--app-text-muted);
  font-family: "Cascadia Mono", "SFMono-Regular", Consolas, monospace;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0;
}

h1,
h2 {
  margin: 0;
  color: var(--app-text);
}

h1 {
  font-size: clamp(24px, 2.5vw, 36px);
  line-height: 1.1;
}

h2 {
  font-size: 18px;
}

.admin-grid {
  display: grid;
  grid-template-columns: minmax(220px, 0.7fr) minmax(420px, 1.35fr);
  grid-template-rows: auto minmax(0, 1fr);
  gap: 12px;
  min-height: 0;
}

.summary-grid {
  display: grid;
  grid-row: 1 / span 2;
  gap: 9px;
  min-height: 0;
}

.summary-card,
.config-panel {
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

.summary-card {
  display: grid;
  align-content: space-between;
  min-height: 124px;
  padding: 14px;
}

.card-label,
.panel-title span,
.config-status span {
  color: var(--app-text-muted);
  font-family: "Cascadia Mono", "SFMono-Regular", Consolas, monospace;
  font-size: 11px;
  font-weight: 800;
}

.summary-card strong {
  overflow-wrap: anywhere;
  color: var(--app-text);
  font-size: 21px;
}

.config-panel {
  min-height: 0;
  padding: 16px;
  overflow: auto;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  color: var(--app-text);
}

.panel-title div {
  display: grid;
  gap: 4px;
}

.config-form {
  display: grid;
  gap: 10px;
}

.config-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.config-form :deep(.el-select) {
  width: 100%;
}

.config-grid {
  display: grid;
  grid-template-columns: minmax(160px, 0.72fr) minmax(220px, 1fr);
  gap: 10px;
}

.config-grid :deep(.el-form-item:nth-child(3)),
.config-grid :deep(.el-form-item:nth-child(4)) {
  grid-column: span 2;
}

.config-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 2px;
}

.config-status,
.config-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.config-actions :deep(.el-button span) {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

@media (max-width: 980px) {
  .admin-page {
    height: auto;
    min-height: calc(100vh - 58px);
    overflow: visible;
  }

  .admin-grid {
    grid-template-columns: 1fr;
  }

  .summary-grid {
    grid-row: auto;
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .admin-page {
    min-height: calc(100vh - 121px);
    padding: 16px;
  }

  .admin-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .admin-header-actions {
    width: 100%;
    align-items: flex-start;
    flex-direction: column;
  }

  .summary-grid,
  .config-grid {
    grid-template-columns: 1fr;
  }

  .config-grid :deep(.el-form-item:nth-child(3)),
  .config-grid :deep(.el-form-item:nth-child(4)) {
    grid-column: auto;
  }

  .config-footer {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
