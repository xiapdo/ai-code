import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    proxy: {
      // API 代理
      '/api': {
        target: 'http://localhost:8123',
        changeOrigin: true,
        secure: false,
      },
      // WebSocket 代理 - 关键：必须匹配 /api/ws 路径
      '/api/ws': {
        target: 'http://localhost:8123',
        changeOrigin: true,
        secure: false,
        ws: true, // 启用 WebSocket 代理
      },
    },
  },
  define: {
    global: 'window',
  },
})
