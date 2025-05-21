// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { Config } from '@jest/types'

const config: Config.InitialOptions = {
  displayName: 'e2e-test',
  preset: 'ts-jest',
  testEnvironment: './jest-environment.cjs',
  testRunner: 'jest-circus/runner'
}
export default config
