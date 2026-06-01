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
      'no-undef': 'error',
      'no-var': 'error',
      'no-unused-private-class-members': 'error',
      'no-unused-vars': 'off',
      'prefer-const': 'off',
      'prefer-template': 'off',
      'no-useless-escape': 'off',
      'no-multiple-empty-lines': 'off',
      'no-trailing-spaces': 'off',
      'eqeqeq': 'off',
      'curly': 'off',
      'dot-notation': 'off',
      'object-shorthand': 'off',
      'prefer-arrow-callback': 'off',
      'no-console': 'off',
      'no-debugger': 'off',
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
      'no-unused-vars': 'off',
    },
  },
];
