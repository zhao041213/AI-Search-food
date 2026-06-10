<template>
  <el-container class="app-shell" :data-theme="activeTheme">
    <el-header class="app-header">
      <RouterLink class="brand" to="/" aria-label="AI 智能菜谱首页">
        <span class="brand-mark" aria-hidden="true">
          <Utensils :size="20" />
        </span>
        <span>AI 智能菜谱</span>
      </RouterLink>

      <nav class="nav-links" aria-label="主导航">
        <RouterLink class="nav-link" to="/">
          <Home :size="17" aria-hidden="true" />
          <span>菜谱搜索</span>
        </RouterLink>
        <RouterLink class="nav-link" to="/admin">
          <ShieldCheck :size="17" aria-hidden="true" />
          <span>管理后台</span>
        </RouterLink>
      </nav>

      <div class="account">
        <el-popover
          placement="bottom-end"
          trigger="click"
          :width="320"
          popper-class="theme-popover"
        >
          <template #reference>
            <button class="theme-trigger" type="button" aria-label="主题设置">
              <Palette :size="16" aria-hidden="true" />
              <span>{{ activeThemeLabel }}</span>
            </button>
          </template>

          <div class="theme-panel">
            <div class="theme-panel-head">
              <strong>主题设置</strong>
              <span>选择后立即保存到本地</span>
            </div>

            <button
              v-for="theme in themeOptions"
              :key="theme.key"
              class="theme-option"
              :class="{ active: theme.key === activeTheme }"
              type="button"
              :aria-pressed="theme.key === activeTheme"
              @click="setTheme(theme.key)"
            >
              <span class="theme-swatch" aria-hidden="true">
                <i
                  v-for="color in theme.preview"
                  :key="color"
                  :style="{ background: color }"
                />
              </span>
              <span class="theme-option-copy">
                <strong>{{ theme.label }}</strong>
                <em>{{ theme.description }}</em>
              </span>
            </button>
          </div>
        </el-popover>

        <template v-if="auth.isLoggedIn">
          <span class="account-name">{{ auth.displayName || roleDisplay }}</span>
          <el-button class="header-button" type="primary" plain @click="logout">
            <LogOut :size="16" aria-hidden="true" />
            <span>退出登录</span>
          </el-button>
        </template>
        <RouterLink v-else class="login-link" to="/login">
          <LogIn :size="16" aria-hidden="true" />
          <span>登录</span>
        </RouterLink>
      </div>
    </el-header>

    <el-main class="app-main">
      <RouterView />
    </el-main>
  </el-container>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { Home, LogIn, LogOut, Palette, ShieldCheck, Utensils } from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import { useAuthStore } from './stores/auth'

const RECIPE_THEME_KEY = 'ai-recipe-theme'
const themeOptions = [
  {
    key: 'business',
    label: '专业商务',
    description: '深蓝、蓝绿与琥珀金，适合企业和 SaaS',
    preview: ['#1E3A5F', '#4A90A4', '#E8B339', '#F5F7FA'],
    variables: {
      '--app-bg': '#F5F7FA',
      '--app-surface': '#FFFFFF',
      '--app-surface-strong': '#EEF3F7',
      '--app-surface-soft': '#E3EBF2',
      '--app-field-bg': '#FFFFFF',
      '--app-line': '#D6DEE8',
      '--app-line-strong': '#9FB2C3',
      '--app-text': '#1E3A5F',
      '--app-text-soft': '#304D6D',
      '--app-text-muted': '#4A90A4',
      '--app-text-faint': '#8EA2B5',
      '--app-accent': '#E8B339',
      '--app-accent-hover': '#F2C85C',
      '--app-accent-soft': '#F8E6AF',
      '--app-accent-text': '#1E3A5F',
      '--app-header-bg': 'rgba(245, 247, 250, 0.92)',
      '--app-grid-line': 'rgba(30, 58, 95, 0.055)',
      '--app-grid-line-strong': 'rgba(30, 58, 95, 0.08)',
      '--app-grid-line-soft': 'rgba(74, 144, 164, 0.07)',
      '--app-panel-shadow': '0 18px 60px rgba(30, 58, 95, 0.12)',
      '--app-focus-shadow': '0 0 0 1px #4A90A4 inset',
      '--app-brand-shadow': '0 0 28px rgba(232, 179, 57, 0.22)'
    }
  },
  {
    key: 'minimal',
    label: '现代极简',
    description: '近黑、中灰与翠绿，适合作品集和展示页',
    preview: ['#111827', '#6B7280', '#10B981', '#FFFFFF'],
    variables: {
      '--app-bg': '#FFFFFF',
      '--app-surface': '#FFFFFF',
      '--app-surface-strong': '#F3F4F6',
      '--app-surface-soft': '#E5E7EB',
      '--app-field-bg': '#FFFFFF',
      '--app-line': '#E5E7EB',
      '--app-line-strong': '#D1D5DB',
      '--app-text': '#111827',
      '--app-text-soft': '#374151',
      '--app-text-muted': '#6B7280',
      '--app-text-faint': '#9CA3AF',
      '--app-accent': '#10B981',
      '--app-accent-hover': '#34D399',
      '--app-accent-soft': '#A7F3D0',
      '--app-accent-text': '#FFFFFF',
      '--app-header-bg': 'rgba(255, 255, 255, 0.94)',
      '--app-grid-line': 'rgba(17, 24, 39, 0.045)',
      '--app-grid-line-strong': 'rgba(17, 24, 39, 0.07)',
      '--app-grid-line-soft': 'rgba(107, 114, 128, 0.055)',
      '--app-panel-shadow': '0 18px 60px rgba(17, 24, 39, 0.09)',
      '--app-focus-shadow': '0 0 0 1px #10B981 inset',
      '--app-brand-shadow': '0 0 28px rgba(16, 185, 129, 0.18)'
    }
  },
  {
    key: 'warm',
    label: '温暖活力',
    description: '赭石、亮橙与青绿，适合促销和电商',
    preview: ['#C05621', '#ED8936', '#38B2AC', '#FFFAF0'],
    variables: {
      '--app-bg': '#FFFAF0',
      '--app-surface': '#FFFFFF',
      '--app-surface-strong': '#FFF3DF',
      '--app-surface-soft': '#FEE7C5',
      '--app-field-bg': '#FFFFFF',
      '--app-line': '#F1D7B8',
      '--app-line-strong': '#E9B476',
      '--app-text': '#7C2D12',
      '--app-text-soft': '#9A3412',
      '--app-text-muted': '#C05621',
      '--app-text-faint': '#B7791F',
      '--app-accent': '#38B2AC',
      '--app-accent-hover': '#4FD1C5',
      '--app-accent-soft': '#B2F5EA',
      '--app-accent-text': '#073331',
      '--app-header-bg': 'rgba(255, 250, 240, 0.93)',
      '--app-grid-line': 'rgba(192, 86, 33, 0.06)',
      '--app-grid-line-strong': 'rgba(237, 137, 54, 0.1)',
      '--app-grid-line-soft': 'rgba(56, 178, 172, 0.07)',
      '--app-panel-shadow': '0 18px 60px rgba(192, 86, 33, 0.12)',
      '--app-focus-shadow': '0 0 0 1px #38B2AC inset',
      '--app-brand-shadow': '0 0 28px rgba(237, 137, 54, 0.22)'
    }
  },
  {
    key: 'tech-dark',
    label: '科技深色',
    description: '深蓝黑与亮蓝，适合仪表盘和深色模式',
    preview: ['#0B1120', '#0F172A', '#3B82F6', '#E5E7EB'],
    variables: {
      '--app-bg': '#0B1120',
      '--app-surface': '#0F172A',
      '--app-surface-strong': '#111C33',
      '--app-surface-soft': '#17233D',
      '--app-field-bg': '#0B1120',
      '--app-line': '#1E293B',
      '--app-line-strong': '#334155',
      '--app-text': '#E5E7EB',
      '--app-text-soft': '#CBD5E1',
      '--app-text-muted': '#94A3B8',
      '--app-text-faint': '#64748B',
      '--app-accent': '#3B82F6',
      '--app-accent-hover': '#60A5FA',
      '--app-accent-soft': '#93C5FD',
      '--app-accent-text': '#FFFFFF',
      '--app-header-bg': 'rgba(11, 17, 32, 0.91)',
      '--app-grid-line': 'rgba(59, 130, 246, 0.07)',
      '--app-grid-line-strong': 'rgba(59, 130, 246, 0.12)',
      '--app-grid-line-soft': 'rgba(148, 163, 184, 0.07)',
      '--app-panel-shadow': '0 18px 60px rgba(0, 0, 0, 0.36)',
      '--app-focus-shadow': '0 0 0 1px #3B82F6 inset',
      '--app-brand-shadow': '0 0 28px rgba(59, 130, 246, 0.28)'
    }
  },
  {
    key: 'youth',
    label: '年轻活泼',
    description: '珊瑚红与蒂芙尼蓝，适合社交移动产品',
    preview: ['#FF6B6B', '#4ECDC4', '#F7F9FC', '#263238'],
    variables: {
      '--app-bg': '#F7F9FC',
      '--app-surface': '#FFFFFF',
      '--app-surface-strong': '#F0F5F8',
      '--app-surface-soft': '#E6F7F5',
      '--app-field-bg': '#FFFFFF',
      '--app-line': '#DCE8EF',
      '--app-line-strong': '#A8DADC',
      '--app-text': '#263238',
      '--app-text-soft': '#37474F',
      '--app-text-muted': '#607D8B',
      '--app-text-faint': '#90A4AE',
      '--app-accent': '#FF6B6B',
      '--app-accent-hover': '#FF8585',
      '--app-accent-soft': '#FFD6D6',
      '--app-accent-text': '#FFFFFF',
      '--app-header-bg': 'rgba(247, 249, 252, 0.94)',
      '--app-grid-line': 'rgba(78, 205, 196, 0.08)',
      '--app-grid-line-strong': 'rgba(255, 107, 107, 0.1)',
      '--app-grid-line-soft': 'rgba(78, 205, 196, 0.07)',
      '--app-panel-shadow': '0 18px 60px rgba(38, 50, 56, 0.1)',
      '--app-focus-shadow': '0 0 0 1px #4ECDC4 inset',
      '--app-brand-shadow': '0 0 28px rgba(255, 107, 107, 0.2)'
    }
  }
]

const auth = useAuthStore()
const router = useRouter()
const activeTheme = ref(getInitialTheme())
const roleDisplay = computed(() => ({ ADMIN: '管理员', USER: '普通用户' })[auth.role] || auth.role)
const activeThemeConfig = computed(
  () => themeOptions.find((theme) => theme.key === activeTheme.value) || themeOptions[0]
)
const activeThemeLabel = computed(() => activeThemeConfig.value.label)

watch(
  activeThemeConfig,
  (theme) => {
    applyTheme(theme)
  },
  { immediate: true }
)

function logout() {
  auth.logout()
  router.push({ name: 'login' })
}

function getInitialTheme() {
  if (typeof window === 'undefined') {
    return themeOptions[0].key
  }

  const savedTheme = window.localStorage.getItem(RECIPE_THEME_KEY)
  return themeOptions.some((theme) => theme.key === savedTheme) ? savedTheme : themeOptions[0].key
}

function setTheme(themeKey) {
  if (themeOptions.some((theme) => theme.key === themeKey)) {
    activeTheme.value = themeKey
  }
}

function applyTheme(theme) {
  if (typeof document === 'undefined') {
    return
  }

  Object.entries(theme.variables).forEach(([name, value]) => {
    document.documentElement.style.setProperty(name, value)
  })
  document.body.dataset.theme = theme.key
  window.localStorage.setItem(RECIPE_THEME_KEY, theme.key)
}
</script>

<style scoped>
:global(:root) {
  --app-bg: #F5F7FA;
  --app-surface: #FFFFFF;
  --app-surface-strong: #EEF3F7;
  --app-surface-soft: #E3EBF2;
  --app-field-bg: #FFFFFF;
  --app-line: #D6DEE8;
  --app-line-strong: #9FB2C3;
  --app-text: #1E3A5F;
  --app-text-soft: #304D6D;
  --app-text-muted: #4A90A4;
  --app-text-faint: #8EA2B5;
  --app-accent: #E8B339;
  --app-accent-hover: #F2C85C;
  --app-accent-soft: #F8E6AF;
  --app-accent-text: #1E3A5F;
  --app-header-bg: rgba(245, 247, 250, 0.92);
  --app-grid-line: rgba(30, 58, 95, 0.055);
  --app-grid-line-strong: rgba(30, 58, 95, 0.08);
  --app-grid-line-soft: rgba(74, 144, 164, 0.07);
  --app-panel-shadow: 0 18px 60px rgba(30, 58, 95, 0.12);
  --app-focus-shadow: 0 0 0 1px #4A90A4 inset;
  --app-brand-shadow: 0 0 28px rgba(232, 179, 57, 0.22);
  --el-color-primary: var(--app-accent);
  --el-color-primary-light-3: var(--app-accent-hover);
  --el-color-primary-light-5: var(--app-accent-soft);
  --el-color-primary-light-7: var(--app-line-strong);
  --el-color-primary-light-9: var(--app-surface-soft);
  --el-color-primary-dark-2: var(--app-accent-hover);
  --el-border-radius-base: 6px;
  --el-bg-color: var(--app-surface);
  --el-bg-color-overlay: var(--app-surface-strong);
  --el-border-color: var(--app-line);
  --el-border-color-light: var(--app-line-strong);
  --el-fill-color-blank: var(--app-surface);
  --el-text-color-primary: var(--app-text);
  --el-text-color-regular: var(--app-text-soft);
  --el-text-color-secondary: var(--app-text-muted);
  color: var(--app-text);
  background: var(--app-bg);
  font-family:
    "Inter", "Microsoft YaHei", "PingFang SC", ui-sans-serif, system-ui, -apple-system,
    BlinkMacSystemFont, "Segoe UI", sans-serif;
}

:global(*) {
  box-sizing: border-box;
}

:global(body) {
  margin: 0;
  min-width: 320px;
  background: var(--app-bg);
}

:global(a) {
  color: inherit;
  text-decoration: none;
}

:global(.el-form-item__label) {
  color: var(--app-text-soft);
  font-weight: 700;
}

:global(.el-input__wrapper),
:global(.el-select__wrapper),
:global(.el-textarea__inner) {
  background: var(--app-field-bg) !important;
  box-shadow: 0 0 0 1px var(--app-line) inset !important;
}

:global(.el-input__wrapper:hover),
:global(.el-select__wrapper:hover),
:global(.el-textarea__inner:hover),
:global(.el-input__wrapper.is-focus),
:global(.el-select__wrapper.is-focused),
:global(.el-textarea__inner:focus) {
  box-shadow: var(--app-focus-shadow) !important;
}

:global(.el-input__inner),
:global(.el-textarea__inner),
:global(.el-select__placeholder),
:global(.el-select__selected-item) {
  color: var(--app-text) !important;
}

:global(.el-input__inner::placeholder),
:global(.el-textarea__inner::placeholder) {
  color: var(--app-text-faint) !important;
}

:global(.el-button) {
  border-radius: 6px;
  font-weight: 800;
}

:global(.el-button--primary) {
  --el-button-bg-color: var(--app-accent);
  --el-button-border-color: var(--app-accent);
  --el-button-hover-bg-color: var(--app-accent-hover);
  --el-button-hover-border-color: var(--app-accent-hover);
  --el-button-active-bg-color: var(--app-accent-soft);
  --el-button-active-border-color: var(--app-accent-soft);
  color: var(--app-accent-text);
}

:global(.el-button.is-plain) {
  --el-button-bg-color: transparent;
  --el-button-border-color: var(--app-line-strong);
  --el-button-text-color: var(--app-text);
  --el-button-hover-bg-color: var(--app-accent);
  --el-button-hover-border-color: var(--app-accent);
  --el-button-hover-text-color: var(--app-accent-text);
}

:global(.el-radio-button__inner) {
  border-color: var(--app-line) !important;
  color: var(--app-text-soft);
  background: var(--app-surface);
}

:global(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  border-color: var(--app-accent) !important;
  color: var(--app-accent-text);
  background: var(--app-accent);
  box-shadow: none;
}

:global(.el-tag) {
  border-color: var(--app-line-strong);
  color: var(--app-text);
  background: var(--app-surface-strong);
}

:global(.el-table) {
  --el-table-bg-color: var(--app-surface);
  --el-table-tr-bg-color: var(--app-surface);
  --el-table-header-bg-color: var(--app-surface-strong);
  --el-table-header-text-color: var(--app-text);
  --el-table-text-color: var(--app-text-soft);
  --el-table-border-color: var(--app-line);
  --el-table-row-hover-bg-color: var(--app-surface-soft);
  color: var(--app-text-soft);
}

:global(.el-empty__description p) {
  color: var(--app-text-muted);
}

:global(.theme-popover.el-popper) {
  border-color: var(--app-line);
  background: var(--app-surface-strong);
  color: var(--app-text);
  box-shadow: var(--app-panel-shadow);
}

:global(.theme-popover .el-popper__arrow::before) {
  border-color: var(--app-line);
  background: var(--app-surface-strong);
}

.app-shell {
  min-height: 100vh;
  background:
    linear-gradient(var(--app-grid-line) 1px, transparent 1px),
    linear-gradient(90deg, var(--app-grid-line) 1px, transparent 1px),
    var(--app-bg);
  background-size: 28px 28px;
}

.app-header {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  height: 58px;
  padding: 0 clamp(12px, 3vw, 36px);
  border-bottom: 1px solid var(--app-line);
  background: var(--app-header-bg);
  backdrop-filter: blur(14px);
}

.brand,
.nav-link,
.login-link,
.header-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.brand {
  min-width: max-content;
  color: var(--app-text);
  font-weight: 900;
  letter-spacing: 0;
}

.brand-mark {
  display: inline-grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border: 1px solid var(--app-accent);
  border-radius: 6px;
  color: var(--app-accent-text);
  background: var(--app-accent);
  box-shadow: var(--app-brand-shadow);
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 8px;
}

.nav-link,
.login-link {
  min-height: 38px;
  padding: 0 11px;
  border: 1px solid transparent;
  border-radius: 6px;
  color: var(--app-text-muted);
  font-weight: 700;
  transition:
    border-color 180ms ease,
    background-color 180ms ease,
    color 180ms ease;
}

.nav-link:hover,
.login-link:hover,
.nav-link.router-link-active {
  border-color: var(--app-line-strong);
  color: var(--app-text);
  background: var(--app-surface-strong);
}

.account {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  min-width: 150px;
}

.account-name {
  max-width: 180px;
  overflow: hidden;
  color: var(--app-text-soft);
  font-size: 14px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.header-button {
  font-weight: 700;
}

.theme-trigger {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 36px;
  padding: 0 10px;
  border: 1px solid var(--app-line-strong);
  border-radius: 6px;
  color: var(--app-text);
  background: var(--app-surface);
  font: inherit;
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
  transition:
    border-color 180ms ease,
    background-color 180ms ease,
    color 180ms ease;
}

.theme-trigger:hover,
.theme-trigger:focus-visible {
  border-color: var(--app-accent);
  background: var(--app-surface-strong);
  outline: none;
}

.theme-panel {
  display: grid;
  gap: 10px;
}

.theme-panel-head {
  display: grid;
  gap: 4px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--app-line);
}

.theme-panel-head strong {
  color: var(--app-text);
  font-size: 16px;
}

.theme-panel-head span {
  color: var(--app-text-muted);
  font-size: 13px;
}

.theme-option {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 12px;
  width: 100%;
  min-height: 64px;
  padding: 10px;
  border: 1px solid var(--app-line);
  border-radius: 8px;
  color: var(--app-text);
  background: var(--app-surface);
  text-align: left;
  cursor: pointer;
  transition:
    border-color 180ms ease,
    background-color 180ms ease;
}

.theme-option:hover,
.theme-option:focus-visible,
.theme-option.active {
  border-color: var(--app-accent);
  background: var(--app-surface-soft);
  outline: none;
}

.theme-swatch {
  display: grid;
  grid-template-columns: repeat(4, 14px);
  overflow: hidden;
  border: 1px solid var(--app-line-strong);
  border-radius: 999px;
}

.theme-swatch i {
  display: block;
  width: 14px;
  height: 30px;
}

.theme-option-copy {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.theme-option-copy strong {
  color: var(--app-text);
  font-size: 14px;
}

.theme-option-copy em {
  overflow: hidden;
  color: var(--app-text-muted);
  font-size: 12px;
  font-style: normal;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-main {
  padding: 0;
}

@media (max-width: 720px) {
  .app-header {
    flex-wrap: wrap;
    height: auto;
    padding: 12px 16px;
  }

  .brand {
    width: 100%;
  }

  .nav-links {
    flex: 1;
  }

  .account {
    min-width: auto;
  }

  .theme-trigger span {
    display: none;
  }

  .account-name {
    display: none;
  }
}
</style>
