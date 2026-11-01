import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'

export default defineConfig(({ mode }) => {
  // Server-only setting: never expose proxy configuration through VITE_* variables.
  const env = loadEnv(mode, process.cwd(), 'API_PROXY_')
  return {
    plugins: [react()],
    server: {
      proxy: {
        '/api': {
          target: process.env.API_PROXY_TARGET || env.API_PROXY_TARGET || 'http://localhost:8080',
          changeOrigin: true,
        },
      },
    },
  }
})
