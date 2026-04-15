// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { render, screen } from '@testing-library/react'
import React from 'react'
import { describe, expect, it, vi } from 'vitest'

import {
  TestContextProvider,
  testTranslations
} from 'lib-components/utils/TestContextProvider'

// Mock the passkey hook so the button renders even without WebAuthn in jsdom
vi.mock('../passkey/usePasskeyAuth', () => ({
  usePasskeyAuth: () => ({
    state: { status: 'idle' },
    enroll: vi.fn(),
    login: vi.fn(),
    reset: vi.fn()
  }),
  useWebAuthnSupported: () => true
}))

vi.mock('../localization', () => ({
  useTranslation: () => ({
    common: {
      title: 'eVaka',
      openExpandingInfo: 'open'
    },
    loginPage: {
      title: 'Login',
      systemNotification: 'Notice',
      pwaInstall: {
        button: 'Add to home screen',
        iosUseSafariNote: 'Open in Safari',
        notSupported: 'Not supported',
        instructions: { ios: null, android: null }
      },
      login: {
        title: 'Sign in with username',
        link: 'Log in',
        infoBoxText: 'Info',
        username: 'Username',
        password: 'Password',
        rateLimitError: 'Rate limit',
        forgotPassword: 'Forgot?',
        forgotPasswordInfo: 'Forgot info',
        noUsername: 'No username?',
        noUsernameInfo: 'No username info',
        passkey: {
          title: 'Sign in with passkey',
          subtitle: 'Fast, passwordless sign-in on this device',
          noCredentialsHint: 'No passkeys found.',
          failed: 'Sign-in failed.',
          moreOptionsDisclosure: 'More sign-in options'
        }
      },
      applying: {
        title: 'Sign in using Suomi.fi',
        paragraph: 'With strong auth you can',
        infoBoxText: 'Strong auth info',
        infoBullets: ['item 1'],
        link: 'Authenticate',
        mapText: 'See map',
        mapLink: 'Units on the map'
      }
    }
  }),
  useLang: () => ['fi', vi.fn()]
}))

vi.mock('../auth/state', () => ({
  useUser: () => null
}))

vi.mock('../hooks/useIsStandalone', () => ({
  useIsStandalone: () => false
}))

vi.mock('../navigation/const', () => ({
  getWeakLoginUri: (url: string) => `/login/form?next=${url}`,
  getStrongLoginUri: (url: string) =>
    `/api/citizen/auth/sfi/login?RelayState=${url}`
}))

vi.mock('../pwa/PwaInstallButton', () => ({
  PwaInstallButton: () => null
}))

vi.mock('../Footer', () => ({
  default: () => null
}))

vi.mock('../useTitle', () => ({
  default: () => undefined
}))

vi.mock('./queries', () => ({
  systemNotificationsQuery: () => ({ queryKey: ['systemNotifications'] })
}))

vi.mock('lib-common/query', () => ({
  useQueryResult: () => ({ isSuccess: false })
}))

vi.mock('wouter', () => ({
  useSearchParams: () => [new URLSearchParams(), vi.fn()],
  useLocation: () => ['/', vi.fn()],
  Redirect: () => null,
  Link: ({ children, to }: { children: React.ReactNode; to: string }) => (
    <a href={to}>{children}</a>
  )
}))

import LoginPage from './LoginPage'

const wrap = (child: React.JSX.Element) => (
  <TestContextProvider translations={testTranslations}>
    {child}
  </TestContextProvider>
)

describe('LoginPage', () => {
  it('renders the passkey primary button', () => {
    render(wrap(<LoginPage />))
    expect(screen.getByTestId('passkey-login-button')).toBeTruthy()
  })
})
