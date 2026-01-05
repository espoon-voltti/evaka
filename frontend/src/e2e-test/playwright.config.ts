// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

import { defineConfig } from '@playwright/test'

// frontend directory
const dir = join(dirname(fileURLToPath(import.meta.url)), '..', '..')

const baseURL = process.env.BASE_URL ?? 'http://localhost:9099'
const isCI = process.env.CI === 'true' || process.env.CI === '1'
const isHeaded = process.env.HEADED === 'true' || process.env.HEADED === '1'

export default defineConfig({
  testDir: './specs',
  testMatch: '**/*.pw.spec.ts',
  timeout: isHeaded ? 1_000_000_000 : 60_000,
  workers: 1,
  retries: isCI ? 2 : 0,
  reporter: [
    ['list'],
    ['html', { open: 'never', outputFolder: join(dir, 'test-results') }],
    ['junit', { outputFile: join(dir, 'test-results/junit.xml') }]
  ],
  use: {
    baseURL,
    headless: !isHeaded,
    ignoreHTTPSErrors: true,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    locale: 'fi-FI',
    timezoneId: 'Europe/Helsinki'
  },
  projects: [
    {
      name: 'chromium',
      use: { browserName: 'chromium' }
    }
  ]
})
