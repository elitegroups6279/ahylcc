import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [
    {
      name: 'dev-root-to-index',
      enforce: 'pre',
      configureServer(server) {
        server.middlewares.use((req, _res, next) => {
          const path = req.url?.split('?')[0] ?? ''
          if (path === '/' || path === '') {
            const q = req.url?.includes('?') ? req.url.slice(req.url.indexOf('?')) : ''
            req.url = '/index.html' + q
          }
          next()
        })
      }
    },
    vue()
  ],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          'vue-vendor': ['vue', 'vue-router', 'pinia'],
          'element-plus': ['element-plus', '@element-plus/icons-vue'],
          'echarts': ['echarts'],
        }
      }
    }
  }
})

