import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react'
import viteTsconfigPaths from 'vite-tsconfig-paths'

const targetURL = process.env.VITE_KG_API_TARGET_URL || 'http://localhost:8080';
// Can be:
//   target: 'http://localhost:8080',
//   target: 'https://search.kg.ebrains.eu',
//   target: 'https://search.kg-int.ebrains.eu',
//   target: 'https://search.kg-ppd.ebrains.eu',
//   target: 'https://search.kg-dev.ebrains.eu',

export default defineConfig({
  base: '/',
  plugins: [react(), viteTsconfigPaths()],
  server: {
    open: true,
    port: 3000,
    proxy: {
      '/api': {
        target: targetURL,
        changeOrigin: true
      }
    }
  },
});