// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { Config } from '@jest/types'

const config: Config.InitialOptions = {
  displayName: 'lib-components',
  preset: 'ts-jest',
  testEnvironment: 'jsdom',
  testRunner: 'jest-circus/runner',
  moduleNameMapper: {
    Icons: '<rootDir>/../lib-icons/free-icons',
    '\\.css$': '<rootDir>/utils/mocks/styleMock.ts',
    '\\.svg$': '<rootDir>/utils/mocks/fileMock.ts',
    '@evaka/customizations/(.*)': '<rootDir>/../lib-customizations/espoo/$1'
  }
}
export default config
