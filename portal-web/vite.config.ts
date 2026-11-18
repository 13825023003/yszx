import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const proxyTarget = env.VITE_PROXY_TARGET || 'http://localhost:8080'
  const minioTarget = env.VITE_MINIO_TARGET || 'http://localhost:9000'
  const bucket = env.VITE_MINIO_BUCKET || 'yszx-files'

  return {
    // 学员端部署在 / 路径（D20）
    base: env.VITE_APP_BASE || '/',
    plugins: [vue()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    server: {
      port: Number(env.VITE_DEV_PORT || 5173),
      host: '0.0.0.0',
      proxy: {
        '/api': {
          target: proxyTarget,
          changeOrigin: true,
        },
        // 开发期直接代理 MinIO 桶，模拟 Nginx 的 /files 直链
        '/files': {
          target: minioTarget,
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/files/, `/${bucket}`),
        },
      },
    },
    build: {
      outDir: 'dist',
      sourcemap: false,
      chunkSizeWarningLimit: 1500,
    },
  }
})
