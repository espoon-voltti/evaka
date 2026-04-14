// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// Smoke-import test: verifies that all passkey module exports resolve without errors.
// No behaviour is exercised here — browser APIs are not available in the test environment.

import { it, expect } from 'vitest'

import { passkeysQuery, renamePasskeyMutation, revokePasskeyMutation } from './queries'
import { usePasskeyAuth, useWebAuthnSupported } from './usePasskeyAuth'
import { useIsStandalone } from '../hooks/useIsStandalone'

it('exports resolve', () => {
  expect(passkeysQuery).toBeDefined()
  expect(renamePasskeyMutation).toBeDefined()
  expect(revokePasskeyMutation).toBeDefined()
  expect(usePasskeyAuth).toBeTypeOf('function')
  expect(useWebAuthnSupported).toBeTypeOf('function')
  expect(useIsStandalone).toBeTypeOf('function')
})
