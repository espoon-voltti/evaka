// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

module.exports = {
  preset: '@vue/cli-plugin-unit-jest/presets/typescript-and-babel',
  globals: {
    'ts-jest': {
      babelConfig: true,
      tsConfig: 'tsconfig.test.json',
    },
    'vue-jest': {
      tsConfig: 'tsconfig.test.json',
    }
  },
  testMatch: ['<rootDir>/src/**/*.(test|spec).(ts|tsx|js)'],
  modulePathIgnorePatterns: ['.cache', '.yarn_cache'],
  setupFilesAfterEnv: ['jest-expect-message'],
  reporters: ['default', 'jest-junit'],
}
