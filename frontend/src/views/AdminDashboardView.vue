<template>
  <main class="admin-page">
    <section class="admin-shell" aria-labelledby="admin-title">
      <header class="admin-header">
        <div>
          <p class="eyebrow">Administration</p>
          <h1 id="admin-title">Admin dashboard</h1>
        </div>
        <el-button type="primary" plain @click="logout">
          <LogOut :size="16" aria-hidden="true" />
          <span>Logout</span>
        </el-button>
      </header>

      <section class="summary-grid" aria-label="System summary">
        <article class="summary-card">
          <span class="card-label">Signed in as</span>
          <strong>{{ auth.displayName || 'Administrator' }}</strong>
        </article>
        <article class="summary-card">
          <span class="card-label">Role</span>
          <strong>{{ auth.role }}</strong>
        </article>
        <article class="summary-card">
          <span class="card-label">Session</span>
          <strong>{{ auth.isLoggedIn ? 'Active' : 'Inactive' }}</strong>
        </article>
      </section>

      <section class="management-panel" aria-labelledby="management-title">
        <div class="panel-title">
          <PanelTopOpen :size="20" aria-hidden="true" />
          <h2 id="management-title">Management queue</h2>
        </div>

        <el-table :data="rows" size="large">
          <el-table-column prop="area" label="Area" min-width="180" />
          <el-table-column prop="status" label="Status" min-width="160" />
          <el-table-column prop="owner" label="Owner" min-width="160" />
        </el-table>
      </section>
    </section>
  </main>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { LogOut, PanelTopOpen } from 'lucide-vue-next'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()

const rows = [
  { area: 'Ingredient recognition', status: 'Foundation ready', owner: 'AI service' },
  { area: 'Recipe recommendation', status: 'Pending phase', owner: 'Recommendation service' },
  { area: 'User access', status: 'JWT enabled', owner: 'Auth service' }
]

function logout() {
  auth.logout()
  router.push({ name: 'login' })
}
</script>

<style scoped>
.admin-page {
  min-height: calc(100vh - 72px);
  padding: clamp(24px, 5vw, 56px);
  color: #0f172a;
}

.admin-shell {
  width: min(1120px, 100%);
  margin: 0 auto;
}

.admin-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
}

.admin-header :deep(.el-button span) {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.eyebrow {
  margin: 0 0 8px;
  color: #047857;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0;
  text-transform: uppercase;
}

h1,
h2 {
  margin: 0;
  color: #164e63;
}

h1 {
  font-size: clamp(30px, 4vw, 42px);
  line-height: 1.15;
}

h2 {
  font-size: 22px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.summary-card,
.management-panel {
  border: 1px solid #bae6fd;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 18px 45px rgba(8, 145, 178, 0.1);
}

.summary-card {
  display: grid;
  gap: 8px;
  min-height: 116px;
  padding: 20px;
}

.card-label {
  color: #475569;
  font-size: 13px;
  font-weight: 800;
  text-transform: uppercase;
}

.summary-card strong {
  align-self: end;
  overflow-wrap: anywhere;
  color: #164e63;
  font-size: 22px;
}

.management-panel {
  padding: 24px;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  color: #0891b2;
}

.management-panel :deep(.el-table) {
  border-radius: 8px;
  overflow: hidden;
}

@media (max-width: 760px) {
  .admin-page {
    min-height: calc(100vh - 123px);
    padding: 24px 16px;
  }

  .admin-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
