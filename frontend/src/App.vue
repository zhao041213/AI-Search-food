<template>
  <el-container class="app-shell">
    <el-header class="app-header">
      <RouterLink class="brand" to="/" aria-label="AI Smart Recipe home">
        <span class="brand-mark" aria-hidden="true">
          <Utensils :size="20" />
        </span>
        <span>AI Smart Recipe</span>
      </RouterLink>

      <nav class="nav-links" aria-label="Primary navigation">
        <RouterLink class="nav-link" to="/">
          <Home :size="17" aria-hidden="true" />
          <span>Search</span>
        </RouterLink>
        <RouterLink class="nav-link" to="/admin">
          <ShieldCheck :size="17" aria-hidden="true" />
          <span>Admin</span>
        </RouterLink>
      </nav>

      <div class="account">
        <template v-if="auth.isLoggedIn">
          <span class="account-name">{{ auth.displayName || auth.role }}</span>
          <el-button class="header-button" type="primary" plain @click="logout">
            <LogOut :size="16" aria-hidden="true" />
            <span>Logout</span>
          </el-button>
        </template>
        <RouterLink v-else class="login-link" to="/login">
          <LogIn :size="16" aria-hidden="true" />
          <span>Login</span>
        </RouterLink>
      </div>
    </el-header>

    <el-main class="app-main">
      <RouterView />
    </el-main>
  </el-container>
</template>

<script setup>
import { Home, LogIn, LogOut, ShieldCheck, Utensils } from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import { useAuthStore } from './stores/auth'

const auth = useAuthStore()
const router = useRouter()

function logout() {
  auth.logout()
  router.push({ name: 'login' })
}
</script>

<style scoped>
:global(:root) {
  --el-color-primary: #059669;
  --el-color-primary-light-3: #34d399;
  --el-color-primary-light-5: #6ee7b7;
  --el-color-primary-light-7: #a7f3d0;
  --el-color-primary-light-9: #ecfdf5;
  --el-color-primary-dark-2: #047857;
  --el-border-radius-base: 8px;
  color: #0f172a;
  background: #ecfeff;
  font-family:
    Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI",
    sans-serif;
}

:global(*) {
  box-sizing: border-box;
}

:global(body) {
  margin: 0;
  min-width: 320px;
  background: #ecfeff;
}

:global(a) {
  color: inherit;
  text-decoration: none;
}

.app-shell {
  min-height: 100vh;
  background:
    linear-gradient(180deg, rgba(236, 254, 255, 0.96), rgba(248, 250, 252, 0.96)),
    #ecfeff;
}

.app-header {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  height: 72px;
  padding: 0 clamp(16px, 4vw, 48px);
  border-bottom: 1px solid #bae6fd;
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(10px);
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
  color: #164e63;
  font-weight: 800;
}

.brand-mark {
  display: inline-grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  color: #ffffff;
  background: #0891b2;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 8px;
}

.nav-link,
.login-link {
  min-height: 44px;
  padding: 0 14px;
  border-radius: 8px;
  color: #164e63;
  font-weight: 700;
  transition:
    background-color 180ms ease,
    color 180ms ease;
}

.nav-link:hover,
.login-link:hover,
.nav-link.router-link-active {
  color: #047857;
  background: #ecfdf5;
}

.account {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-width: 160px;
}

.account-name {
  max-width: 180px;
  margin-right: 12px;
  overflow: hidden;
  color: #334155;
  font-size: 14px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.header-button {
  font-weight: 700;
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

  .account-name {
    display: none;
  }
}
</style>
