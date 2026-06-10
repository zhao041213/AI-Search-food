import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:7068'
    }
  },
  build: {
    chunkSizeWarningLimit: 1000,
    rollupOptions: {
      onwarn(warning, warn) {
        if (warning.code === 'INVALID_ANNOTATION' && warning.id?.includes('@vueuse/core')) {
          return
        }

        warn(warning)
      },
      output: {
        manualChunks: {
          vue: ['vue', 'vue-router', 'pinia'],
          element: ['element-plus']
        }
      }
    }
  }
})
