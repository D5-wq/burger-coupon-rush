import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// 개발 시 백엔드(8080)로 /api 프록시 → CORS 신경 안 쓰고 개발 가능
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
