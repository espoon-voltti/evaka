// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { createDefaultEsmPreset } from 'ts-jest'

export default {
  ...createDefaultEsmPreset({
    tsconfig: 'tsconfig.test.json',
    isolatedModules: true
  }),
  moduleNameMapper: {
    '^(\\.{1,2}/.*)\\.js$': '$1'
  },
  testEnvironment: 'node',
  reporters: ['default', 'jest-junit'],
  coverageDirectory: './build/coverage-reports',
  coverageReporters: ['text', 'html']
}
