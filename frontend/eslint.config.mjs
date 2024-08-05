// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { fixupPluginRules } from '@eslint/compat'
import eslint from '@eslint/js'
import eslintPluginEvaka from '@evaka/eslint-plugin'
import importPlugin from 'eslint-plugin-import'
import jsxExpressionsPlugin from 'eslint-plugin-jsx-expressions'
import lodashPlugin from 'eslint-plugin-lodash'
import eslintPluginPrettierRecommended from 'eslint-plugin-prettier/recommended'
import reactPlugin from 'eslint-plugin-react'
import reactHooksPlugin from 'eslint-plugin-react-hooks'
import globals from 'globals'
import typescriptEslint from 'typescript-eslint'

export default [
  { ignores: ['.yarn', 'dist', 'src/*/generated'] },
  eslint.configs.recommended,
  ...typescriptEslint.configs.recommendedTypeChecked,
  ...typescriptEslint.configs.stylisticTypeChecked,
  eslintPluginEvaka.configs.recommended,
  {
    languageOptions: {
      parserOptions: {
        projectService: true,
        tsconfigRootDir: import.meta.dirname,
        globals: globals.browser
      }
    }
  },
  {
    files: ['**/*.{js,mjs}'],
    ...typescriptEslint.configs.disableTypeChecked
  },
  {
    plugins: {
      import: fixupPluginRules(importPlugin)
    },
    settings: {
      'import/internal-regex':
        '^(citizen-frontend|e2e-test|employee-frontend|employee-mobile-frontend|lib-common|lib-components|lib-customizations|lib-icons|maintenance-page-frontend)(/|$)'
    }
  },
  {
    files: ['**/*.{ts,tsx,js,mjs}'],
    rules: {
      'import/order': [
        'warn',
        {
          alphabetize: { order: 'asc' },
          groups: [
            'builtin',
            'external',
            'internal',
            'parent',
            'sibling',
            'index'
          ],
          'newlines-between': 'always'
        }
      ],
      'no-console': ['error', { allow: ['warn', 'error'] }],
      'prefer-arrow-callback': ['error', { allowNamedFunctions: true }],
      'arrow-body-style': ['error', 'as-needed'],
      'no-constant-binary-expression': ['error']
    }
  },
  {
    files: ['**/*.{ts,tsx,js,mjs}'],
    rules: {
      '@typescript-eslint/no-unused-vars': [
        'warn',
        {
          args: 'all',
          argsIgnorePattern: '^_',
          caughtErrors: 'all',
          caughtErrorsIgnorePattern: '^.*',
          destructuredArrayIgnorePattern: '^_',
          varsIgnorePattern: '^_',
          ignoreRestSiblings: true
        }
      ]
    }
  },
  {
    files: ['**/*.{ts,tsx}'],
    plugins: {
      react: reactPlugin,
      'react-hooks': fixupPluginRules(reactHooksPlugin),
      'jsx-expressions': fixupPluginRules(jsxExpressionsPlugin)
    },
    settings: {
      react: { version: 'detect' }
    },
    rules: {
      ...reactPlugin.configs.recommended.rules,
      ...reactHooksPlugin.configs.recommended.rules,
      'react/jsx-curly-brace-presence': ['error', 'never'],
      'react/prop-types': 'off',
      'react/self-closing-comp': ['error', { component: true, html: true }],
      'react-hooks/rules-of-hooks': 'error',
      'react-hooks/exhaustive-deps': [
        'warn',
        { additionalHooks: '(useApiState)' }
      ],
      'jsx-expressions/strict-logical-expressions': 'error',
      '@typescript-eslint/consistent-type-definitions': 'off',
      '@typescript-eslint/no-empty-object-type': [
        'error',
        { allowInterfaces: 'always' }
      ],
      '@typescript-eslint/no-misused-promises': [
        'error',
        { checksVoidReturn: false }
      ],
      '@typescript-eslint/prefer-nullish-coalescing': 'off',
      '@typescript-eslint/prefer-optional-chain': 'off',
      '@typescript-eslint/prefer-promise-reject-errors': 'off'
    }
  },
  {
    files: ['**/*.{js,mjs}'],
    languageOptions: {
      globals: globals.node
    },
    rules: {
      '@typescript-eslint/no-var-requires': 'off'
    }
  },
  {
    files: ['**/*.js'],
    rules: {
      '@typescript-eslint/no-require-imports': 'off'
    }
  },
  {
    // Only files that end up in the bundles
    files: ['src/**/*.{ts,tsx,js,mjs}'],
    plugins: {
      lodash: fixupPluginRules(lodashPlugin)
    },
    rules: {
      'lodash/import-scope': ['error', 'method']
    }
  },
  eslintPluginPrettierRecommended
]
