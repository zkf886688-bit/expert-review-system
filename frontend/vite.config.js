import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

const apiTarget = process.env.E2E_API_TARGET || 'http://localhost:8080'

const apiProxy = {
  '/api': { target: apiTarget, changeOrigin: true },
  '/files': { target: apiTarget, changeOrigin: true },
  '/actuator': { target: apiTarget, changeOrigin: true },
  '/swagger-ui.html': { target: apiTarget, changeOrigin: true },
  '/swagger-ui': { target: apiTarget, changeOrigin: true },
  '/v3': { target: apiTarget, changeOrigin: true }
}

export default defineConfig({
  plugins: [vue()],
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          echarts: ['echarts'],
          'element-plus': ['element-plus']
        }
      }
    }
  },
  server: {
    port: 5173,
    strictPort: true,
    proxy: apiProxy
  },
  preview: {
    port: 4173,
    proxy: apiProxy
  }
})
