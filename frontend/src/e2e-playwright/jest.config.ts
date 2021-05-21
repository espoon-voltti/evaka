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
