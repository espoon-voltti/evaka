// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import eslint from '@eslint/js'
import importPlugin from 'eslint-plugin-import'
import eslintPluginPrettierRecommended from 'eslint-plugin-prettier/recommended'
import typescriptEslint from 'typescript-eslint'

export default [
  { ignores: ['dist/*', '.yarn/*'] },
  eslint.configs.recommended,
  ...typescriptEslint.configs.recommendedTypeChecked,
  ...typescriptEslint.configs.stylistic,
  {
    languageOptions: {
      parserOptions: {
        projectService: true,
        tsconfigRootDir: import.meta.dirname
      }
    }
  },
  {
    files: ['**/*.js'],
    ...typescriptEslint.configs.disableTypeChecked
  },
  importPlugin.flatConfigs.typescript,
  {
    files: ['**/*.ts'],
    rules: {
      '@typescript-eslint/consistent-type-definitions': 'off',
      '@typescript-eslint/consistent-type-exports': ['error'],
      '@typescript-eslint/consistent-type-imports': [
        'error',
        { disallowTypeAnnotations: false }
      ],
      '@typescript-eslint/no-empty-object-type': [
        'error',
        { allowInterfaces: 'always' }
      ],
      '@typescript-eslint/no-unused-vars': [
        'warn',
        {
          argsIgnorePattern: '^_',
          varsIgnorePattern: '^_'
        }
      ],
      'import/order': [
        'warn',
        {
          alphabetize: {
            order: 'asc'
          },
          groups: ['builtin', 'external', 'parent', 'sibling', 'index'],
          'newlines-between': 'always'
        }
      ]
    }
  },
  eslintPluginPrettierRecommended
]
