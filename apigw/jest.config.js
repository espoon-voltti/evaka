// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export default {
  extensionsToTreatAsEsm: ['.ts'],
  moduleNameMapper: {
    '^(\\.{1,2}/.*)\\.js$': '$1'
  },
  transform: {
    '^.+\\.tsx?$': [
      'ts-jest',
      {
        tsconfig: 'tsconfig.test.json',
        useESM: true,
        isolatedModules: true
      }
    ]
  },
  roots: ['<rootDir>/src'],
  testEnvironment: 'node',
  reporters: ['default', 'jest-junit'],
  coverageDirectory: './build/coverage-reports',
  coverageReporters: ['text', 'html']
}
