// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { render } from '@testing-library/react'
import React from 'react'
import { describe, expect, it, vi } from 'vitest'

import {
  TestContextProvider,
  testTranslations
} from 'lib-components/utils/TestContextProvider'

// Mock the query hooks to return an empty list + idle mutations
vi.mock('lib-common/query', () => ({
  useQueryResult: () => ({ isSuccess: true, value: [] }),
  useMutationResult: () => ({ mutateAsync: vi.fn(() => Promise.resolve()) })
}))

vi.mock('@tanstack/react-query', () => ({
  useQueryClient: () => ({
    invalidateQueries: vi.fn(() => Promise.resolve())
  })
}))

// Mock queries file (it imports axios client which jsdom can't run)
vi.mock('./queries', () => ({
  passkeysQuery: () => ({}),
  revokePasskeyMutation: {}
}))

vi.mock('./usePasskeyAuth', () => ({
  usePasskeyAuth: () => ({
    state: { status: 'idle' },
    enroll: vi.fn(),
    login: vi.fn(),
    reset: vi.fn()
  })
}))

vi.mock('../auth/state', () => ({
  AuthContext: React.createContext({
    user: {
      getOrElse: () => ({
        authLevel: 'STRONG',
        passkeyCredentialId: null
      })
    },
    refreshAuthStatus: () => Promise.resolve()
  })
}))

vi.mock('../navigation/const', () => ({
  getStrongLoginUri: (target: string) =>
    `/api/citizen/auth/sfi/login?RelayState=${target}`
}))

import { PasskeySection } from './PasskeySection'

describe('PasskeySection', () => {
  it('renders the section with data-qa attribute', () => {
    const { container } = render(
      <TestContextProvider translations={testTranslations}>
        <PasskeySection />
      </TestContextProvider>
    )
    expect(container.querySelector('[data-qa="passkey-section"]')).toBeTruthy()
  })

  it('renders the empty state', () => {
    const { container } = render(
      <TestContextProvider translations={testTranslations}>
        <PasskeySection />
      </TestContextProvider>
    )
    expect(container.querySelector('[data-qa="passkey-empty"]')).toBeTruthy()
  })
})
