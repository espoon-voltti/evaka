// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { Config } from '@jest/types'

const config: Config.InitialOptions = {
  displayName: 'e2e-test',
  preset: 'ts-jest',
  testEnvironment: './jest-environment',
  testRunner: 'jest-circus/runner',
  moduleNameMapper: {
    '@evaka/customizations/(.*)': '<rootDir>/../lib-customizations/espoo/$1',
    Icons: '<rootDir>/../lib-icons/free-icons.ts',
    '\\.(jpg|jpeg|png|svg)$': '<rootDir>/../../assetsTransformer.js'
  }
}
export default config
