// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { defineConfig } from 'oxlint'

export default defineConfig({
  ignorePatterns: [
    'node_modules',
    '.yarn',
    'dist',
    'src/*/generated',
    'test-results'
  ],
  plugins: ['typescript', 'unicorn', 'oxc', 'react'],
  options: {
    typeAware: true,
    typeCheck: true,
    maxWarnings: 0,

    // TODO: Enable later
    reportUnusedDisableDirectives: 'off'
  },
  categories: {
    correctness: 'error'
  },
  jsPlugins: [
    'eslint-plugin-lodash',
    {
      name: '@evaka',
      specifier: './eslint-plugin'
    }
  ],
  env: {
    builtin: true
  },
  rules: {
    '@evaka/no-page-pause': 'error',
    '@evaka/no-testonly': 'error',
    '@evaka/no-relative-lib-imports': 'error',
    '@evaka/no-icons-imports': 'error',
    '@evaka/no-localdate-triple-equals': 'error',
    '@evaka/shortest-relative-imports': 'error',

    'eslint/eqeqeq': ['error', 'smart'],
    'eslint/no-array-constructor': 'error',
    'eslint/no-console': ['error', { allow: ['warn', 'error'] }],
    'eslint/no-case-declarations': 'error',
    'eslint/no-constant-binary-expression': 'error',
    'eslint/no-empty': 'error',
    'eslint/no-empty-function': 'error',
    'eslint/no-fallthrough': 'error',
    'eslint/no-prototype-builtins': 'error',
    'eslint/no-redeclare': 'error',
    'eslint/no-regex-spaces': 'error',
    'eslint/no-unexpected-multiline': 'error',
    'eslint/no-unused-vars': [
      'error',
      {
        argsIgnorePattern: '^_',
        varsIgnorePattern: '^_',
        caughtErrors: 'none'
      }
    ],
    'eslint/no-var': 'error',
    'eslint/prefer-arrow-callback': ['error', { allowNamedFunctions: true }],
    'eslint/prefer-const': 'error',
    'eslint/prefer-rest-params': 'error',
    'eslint/prefer-spread': 'error',

    'lodash/import-scope': ['error', 'method'],

    'react/exhaustive-deps': ['error', { additionalHooks: '(useApiState)' }],
    'react/jsx-curly-brace-presence': [
      'error',
      { props: 'never', children: 'never', propElementValues: 'always' }
    ],
    'react/rules-of-hooks': 'error',
    'react/self-closing-comp': ['error', { component: true, html: true }],

    // TODO: Enable later
    'eslint/arrow-body-style': 'off',
    'unicorn/no-useless-spread': 'off'
  },
  overrides: [
    {
      files: ['*.ts', '*.tsx'],
      rules: {
        'typescript/adjacent-overload-signatures': 'error',
        'typescript/array-type': 'error',
        'typescript/ban-ts-comment': 'error',
        'typescript/ban-tslint-comment': 'error',
        'typescript/class-literal-property-style': 'error',
        'typescript/consistent-generic-constructors': 'error',
        'typescript/consistent-indexed-object-style': 'error',
        'typescript/consistent-type-assertions': 'error',
        'typescript/consistent-type-exports': ['error'],
        'typescript/consistent-type-imports': [
          'error',
          { disallowTypeAnnotations: false }
        ],
        'typescript/dot-notation': 'error',
        'typescript/only-throw-error': 'error',
        'typescript/no-confusing-non-null-assertion': 'error',
        'typescript/no-empty-object-type': [
          'error',
          { allowInterfaces: 'always' }
        ],
        'typescript/no-explicit-any': 'error',
        'typescript/no-inferrable-types': 'error',
        'typescript/no-misused-promises': [
          'error',
          { checksVoidReturn: false }
        ],
        'typescript/no-namespace': 'error',
        'typescript/no-require-imports': 'error',
        'typescript/no-unnecessary-type-assertion': 'error',
        'typescript/no-unnecessary-type-constraint': 'error',
        'typescript/no-unsafe-assignment': 'error',
        'typescript/no-unsafe-call': 'error',
        'typescript/no-unsafe-enum-comparison': 'error',
        'typescript/no-unsafe-function-type': 'error',
        'typescript/no-unsafe-return': 'error',
        'typescript/no-unsafe-argument': 'error',
        'typescript/no-unsafe-member-access': 'error',
        'typescript/non-nullable-type-assertion-style': 'error',
        'typescript/prefer-find': 'error',
        'typescript/prefer-for-of': 'error',
        'typescript/prefer-function-type': 'error',
        'typescript/prefer-includes': 'error',
        'typescript/prefer-string-starts-ends-with': 'error',
        'typescript/require-await': 'error',
        'typescript/restrict-plus-operands': 'error',

        'react/display-name': 'error',
        'react/jsx-no-comment-textnodes': 'error',
        'react/jsx-no-target-blank': 'error',
        'react/no-unescaped-entities': 'error',
        'react/no-unknown-property': 'error',
        'react/require-render-return': 'error',

        // TODO: Enable later
        'typescript/no-meaningless-void-operator': 'off',
        'typescript/no-misused-spread': 'off',
        'typescript/no-redundant-type-constituents': 'off',
        'typescript/no-unnecessary-parameter-property-assignment': 'off',
        'typescript/no-useless-default-assignment': 'off',
        'typescript/prefer-regexp-exec': 'off',
        'typescript/require-array-sort-compare': 'off',
        'typescript/strict-boolean-expressions': 'off' // replaces jsx-expressions/strict-logical-expressions
      }
    }
  ]
})
