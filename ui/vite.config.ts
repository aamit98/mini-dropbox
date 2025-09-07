import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      // Forward all /api calls to Spring (port 8080)
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        // if you ever serve the backend under a context path, add `rewrite`
      },
      // Public share links also need proxying
      '/d': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
