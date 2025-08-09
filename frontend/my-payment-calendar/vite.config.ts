import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()]
  // 보통 postcss 자동 인식되므로 명시하지 않아도 됩니다.
})