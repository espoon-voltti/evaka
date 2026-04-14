// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  startAuthentication,
  startRegistration
} from '@simplewebauthn/browser'
import type {
  PublicKeyCredentialCreationOptionsJSON,
  PublicKeyCredentialRequestOptionsJSON
} from '@simplewebauthn/browser'
import { useCallback, useState } from 'react'

export type PasskeyAuthState =
  | { status: 'idle' }
  | { status: 'running' }
  | { status: 'error'; code: 'cancelled' | 'no-credentials' | 'unsupported' | 'failed' }
  | { status: 'success' }

interface RegisterOptionsResponse {
  token: string
  options: PublicKeyCredentialCreationOptionsJSON
}

interface LoginOptionsResponse {
  token: string
  options: PublicKeyCredentialRequestOptionsJSON
}

export function useWebAuthnSupported(): boolean {
  if (typeof window === 'undefined') return false
  return typeof window.PublicKeyCredential === 'function'
}

function classifyError(err: unknown): 'cancelled' | 'no-credentials' | 'unsupported' | 'failed' {
  if (err instanceof Error) {
    if (err.name === 'NotAllowedError') return 'no-credentials'
    if (err.name === 'NotSupportedError') return 'unsupported'
  }
  return 'failed'
}

const CSRF_HEADERS: Record<string, string> = {
  'x-evaka-csrf': '1'
}

const JSON_POST_HEADERS: Record<string, string> = {
  ...CSRF_HEADERS,
  'Content-Type': 'application/json'
}

export function usePasskeyAuth() {
  const [state, setState] = useState<PasskeyAuthState>({ status: 'idle' })

  const enroll = useCallback(async (): Promise<boolean> => {
    setState({ status: 'running' })
    try {
      const optRes = await fetch('/api/citizen/auth/passkey/register/options', {
        method: 'POST',
        credentials: 'same-origin',
        headers: CSRF_HEADERS
      })
      if (!optRes.ok) throw new Error('register-options')
      const { token, options } = (await optRes.json()) as RegisterOptionsResponse
      const attestation = await startRegistration({ optionsJSON: options })
      const verRes = await fetch('/api/citizen/auth/passkey/register/verify', {
        method: 'POST',
        credentials: 'same-origin',
        headers: JSON_POST_HEADERS,
        body: JSON.stringify({ token, attestation })
      })
      if (!verRes.ok) throw new Error('register-verify')
      setState({ status: 'success' })
      return true
    } catch (err) {
      setState({ status: 'error', code: classifyError(err) })
      return false
    }
  }, [])

  const login = useCallback(async (): Promise<boolean> => {
    setState({ status: 'running' })
    try {
      const optRes = await fetch('/api/citizen/auth/passkey/login/options', {
        method: 'POST',
        credentials: 'same-origin',
        headers: CSRF_HEADERS
      })
      if (!optRes.ok) throw new Error('login-options')
      const { token, options } = (await optRes.json()) as LoginOptionsResponse
      const assertion = await startAuthentication({ optionsJSON: options })
      const verRes = await fetch('/api/citizen/auth/passkey/login/verify', {
        method: 'POST',
        credentials: 'same-origin',
        headers: JSON_POST_HEADERS,
        body: JSON.stringify({ token, assertion })
      })
      if (!verRes.ok) throw new Error('login-verify')
      setState({ status: 'success' })
      return true
    } catch (err) {
      setState({ status: 'error', code: classifyError(err) })
      return false
    }
  }, [])

  return {
    state,
    enroll,
    login,
    reset: () => setState({ status: 'idle' })
  }
}
