// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { defineConfig } from 'vitest/config'

const resolve = (path: string) => new URL(path, import.meta.url).pathname

const alias = {
  Icons: resolve('src/lib-icons/free-icons'),
  '@evaka/customizations': resolve('src/lib-customizations/espoo')
}

export default defineConfig({
  test: {
    projects: [
      {
        test: {
          name: 'citizen-frontend',
          environment: 'jsdom',
          include: ['src/citizen-frontend/**/*.spec.{ts,tsx}'],
          setupFiles: ['./vitest.setup.ts'],
          alias
        }
      },
      {
        test: {
          name: 'employee-frontend',
          environment: 'jsdom',
          include: ['src/employee-frontend/**/*.spec.{ts,tsx}'],
          setupFiles: ['./vitest.setup.ts'],
          alias
        }
      },
      {
        test: {
          name: 'lib-components',
          environment: 'jsdom',
          include: ['src/lib-components/**/*.spec.{ts,tsx}'],
          setupFiles: ['./vitest.setup.ts'],
          alias
        }
      },
      {
        test: {
          name: 'lib-common',
          environment: 'node',
          include: ['src/lib-common/**/*.spec.{ts,tsx}']
        }
      },
      {
        test: {
          name: 'eslint-plugin',
          environment: 'node',
          include: ['eslint-plugin/**/*.spec.{ts,tsx}']
        }
      }
    ]
  }
})
