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
        google: 'readonly',
        jsPDF: 'readonly',
        gapi: 'readonly',
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
      'prefer-const': 'warn',
      'no-useless-escape': 'warn',
      'no-multiple-empty-lines': ['warn', { max: 2 }],
      'no-trailing-spaces': 'warn',
      'eqeqeq': ['warn', 'smart'],
      'curly': ['warn', 'multi-line'],
      'dot-notation': 'warn',
      'object-shorthand': 'warn',
      'prefer-arrow-callback': 'warn',
      'prefer-template': 'off',
    },
    linterOptions: {
      reportUnusedDisableDirectives: false,
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
