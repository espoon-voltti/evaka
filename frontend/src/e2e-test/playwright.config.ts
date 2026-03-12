// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

import { defineConfig } from '@playwright/test'

// e2e-test directory
const testDir = dirname(fileURLToPath(import.meta.url))
// frontend directory
const dir = join(testDir, '..', '..')

const baseURL = process.env.BASE_URL ?? 'http://localhost:9099'
const isCI = process.env.CI === 'true' || process.env.CI === '1'
const isHeaded = process.env.HEADED === 'true' || process.env.HEADED === '1'

export default defineConfig({
  testDir: './specs',
  testMatch: '**/*.spec.ts',
  outputDir: join(dir, 'test-results', 'artifacts'),
  fullyParallel: true,
  timeout: isHeaded ? 1_000_000_000 : 60_000,
  workers: 1,
  retries: isCI ? 2 : 0,
  reporter: [
    ['list'],
    [
      'html',
      { open: 'never', outputFolder: join(dir, 'test-results', 'html') }
    ],
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
  ],
  tsconfig: join(testDir, 'tsconfig.json')
})
