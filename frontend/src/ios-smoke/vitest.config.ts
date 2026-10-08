// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { defineConfig } from 'vitest/config'

export default defineConfig({
  test: {
    include: ['src/ios-smoke/**/*.spec.ts'],
    environment: 'node',
    setupFiles: ['./src/ios-smoke/vitest.setup.ts'],
    fileParallelism: false,
    testTimeout: 5 * 60 * 1000,
    hookTimeout: 5 * 60 * 1000,
    reporters: ['verbose']
  }
})
