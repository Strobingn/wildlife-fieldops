import { defineConfig, loadEnv } from 'vite'
import { resolve } from 'path'

// Validate required environment variables
const REQUIRED_ENV_VARS = [
  'VITE_SUPABASE_URL',
  'VITE_SUPABASE_ANON_KEY',
  'VITE_GOOGLE_MAPS_API_KEY',
  'VITE_OPENWEATHER_API_KEY'
];

function validateEnv(env, mode) {
  if (mode === 'production') {
    const missing = REQUIRED_ENV_VARS.filter(key => !env[key]);
    if (missing.length > 0) {
      console.warn('\n⚠️  Missing required environment variables:');
      missing.forEach(key => console.warn(`   - ${key}`));
      console.warn('Build will continue but functionality may be limited.\n');
    }
  }
}

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), 'VITE_');
  validateEnv(env, mode);

  const isProd = mode === 'production';

  return {
    // Development server
    server: {
      host: true,
      port: 5173,
      strictPort: false,
      open: false
    },

    // Preview server
    preview: {
      port: 4173,
      strictPort: false
    },

    // Build configuration
    build: {
      outDir: 'dist',
      sourcemap: !isProd,
      chunkSizeWarningLimit: 2000,
      target: ['es2022', 'chrome100', 'safari15'],
      cssTarget: ['chrome100', 'safari15'],

      // Rollup options for code splitting
      rollupOptions: {
        input: {
          main: resolve(__dirname, 'index.html')
        },
        output: {
          // Manual chunks for optimal caching
          manualChunks(id) {
            // Vendor chunks - third-party libraries
            if (id.includes('node_modules')) {
              if (id.includes('@supabase')) return 'vendor-supabase';
              if (id.includes('jspdf') || id.includes('html2canvas')) return 'vendor-pdf';
              if (id.includes('@capacitor')) return 'vendor-capacitor';
              return 'vendor';
            }
            // API layer chunk
            if (id.includes('/src/api/')) return 'api';
            // Components chunk
            if (id.includes('/src/components/')) return 'components';
          },
          // Asset naming for cache busting
          entryFileNames: 'assets/[name]-[hash].js',
          chunkFileNames: 'assets/[name]-[hash].js',
          assetFileNames: (assetInfo) => {
            const info = assetInfo.name || '';
            if (info.endsWith('.css')) {
              return 'assets/[name]-[hash][extname]';
            }
            if (info.endsWith('.png') || info.endsWith('.jpg') || info.endsWith('.svg')) {
              return 'assets/[name]-[hash][extname]';
            }
            return 'assets/[name]-[hash][extname]';
          }
        }
      },

      // Minification options (Terser)
      minify: isProd ? 'terser' : false,
      terserOptions: isProd ? {
        compress: {
          drop_console: true,
          drop_debugger: true,
          pure_funcs: ['console.log', 'console.info', 'console.debug'],
          passes: 2,
          dead_code: true,
          unused: true
        },
        mangle: {
          safari10: true
        },
        format: {
          comments: false,
          ecma: 2022
        }
      } : undefined,

      // CSS options
      cssCodeSplit: true,
      cssMinify: isProd
    },

    // CSS configuration
    css: {
      devSourcemap: !isProd
    },

    // Asset handling
    assetsInclude: ['**/*.png', '**/*.jpg', '**/*.svg', '**/*.woff2'],

    // Resolve aliases
    resolve: {
      alias: {
        '@': resolve(__dirname, 'src'),
        '@api': resolve(__dirname, 'src/api'),
        '@components': resolve(__dirname, 'src/components'),
        '@assets': resolve(__dirname, 'assets')
      }
    },

    // Environment variable prefix
    envPrefix: 'VITE_',

    // Define global constants
    define: {
      global: 'globalThis',
      __APP_VERSION__: JSON.stringify(process.env.npm_package_version || '3.0.0'),
      __BUILD_TIME__: JSON.stringify(new Date().toISOString()),
      __BUILD_MODE__: JSON.stringify(mode)
    },

    // Performance optimizations
    esbuild: {
      target: 'es2022',
      legalComments: 'none'
    },

    // Optimize dependencies
    optimizeDeps: {
      include: [
        '@supabase/supabase-js',
        'jspdf',
        'html2canvas'
      ],
      exclude: []
    }
  };
});
