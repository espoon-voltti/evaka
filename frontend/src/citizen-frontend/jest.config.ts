import type { Config } from '@jest/types'

const config: Config.InitialOptions = {
  displayName: 'citizen-frontend',
  preset: 'ts-jest',
  testEnvironment: 'node',
  testRunner: 'jest-circus/runner'
}
export default config
