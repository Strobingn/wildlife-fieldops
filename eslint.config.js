import js from '@eslint/js';
import globals from 'globals';

export default [
  {
    name: 'Wildlife FieldOps Config',
    files: ['src/**/*.js'],
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: 'module',
      globals: {
        ...globals.browser,
        ...globals.es2022,
        console: 'readonly',
        document: 'readonly',
        window: 'readonly',
        navigator: 'readonly',
        localStorage: 'readonly',
        sessionStorage: 'readonly',
        fetch: 'readonly',
        setTimeout: 'readonly',
        clearTimeout: 'readonly',
        setInterval: 'readonly',
        clearInterval: 'readonly',
        requestAnimationFrame: 'readonly',
        IntersectionObserver: 'readonly',
        MutationObserver: 'readonly',
        FileReader: 'readonly',
        URL: 'readonly',
        Blob: 'readonly',
        Image: 'readonly',
        crypto: 'readonly',
        gtag: 'readonly',
      },
    },
    plugins: {
      js,
    },
    rules: {
      ...js.configs.recommended.rules,
      'no-unused-vars': ['warn', { argsIgnorePattern: '^_', varsIgnorePattern: '^_' }],
      'no-console': 'off',
      'no-debugger': 'warn',
      'no-var': 'error',
      'prefer-const': 'error',
      'no-multiple-empty-lines': ['warn', { max: 2 }],
      'no-trailing-spaces': 'warn',
      'eqeqeq': ['warn', 'smart'],
      'curly': ['warn', 'multi-line'],
      'dot-notation': 'warn',
      'object-shorthand': 'warn',
      'prefer-arrow-callback': 'warn',
      'prefer-template': 'warn',
    },
    linterOptions: {
      reportUnusedDisableDirectives: true,
    },
  },
  {
    name: 'Service Worker',
    files: ['sw.js'],
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: 'script',
      globals: {
        ...globals.serviceworker,
        importScripts: 'readonly',
        caches: 'readonly',
        clients: 'readonly',
        self: 'readonly',
      },
    },
    rules: {
      'no-restricted-globals': 'off',
      'no-unused-vars': ['warn', { argsIgnorePattern: '^_' }],
    },
  },
];
