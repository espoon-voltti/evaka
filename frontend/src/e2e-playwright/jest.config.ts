// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { Config } from '@jest/types'

const config: Config.InitialOptions = {
  displayName: 'e2e-playwright',
  preset: 'ts-jest',
  testEnvironment: './jest-environment',
  testRunner: 'jest-circus/runner',
  moduleNameMapper: {
    '^e2e-playwright/(.*)$': '<rootDir>/$1'
  }
}
export default config
