// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { fixupPluginRules } from '@eslint/compat'
import eslint from '@eslint/js'
import importPlugin from 'eslint-plugin-import'
import eslintPluginPrettierRecommended from 'eslint-plugin-prettier/recommended'
import typescriptEslint from 'typescript-eslint'

export default [
  { ignores: ['dist/*', '.yarn/*'] },
  ...typescriptEslint.config(
    // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access,@typescript-eslint/no-unsafe-argument
    eslint.configs.recommended,
    ...typescriptEslint.configs.recommendedTypeChecked,
    ...typescriptEslint.configs.stylistic
  ),
  {
    languageOptions: {
      parserOptions: {
        project: 'tsconfig.json'
      }
    }
  },
  {
    // Compatibility tricks for plugins that don't support ESLint v9 flat configs yet
    plugins: {
      // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
      import: fixupPluginRules(importPlugin)
    }
  },
  {
    rules: {
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
