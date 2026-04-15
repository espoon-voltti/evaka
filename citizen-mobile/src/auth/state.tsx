// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState
} from 'react'
import type { ReactNode } from 'react'

import * as api from '../api/auth'
import { unregisterFromPushNotifications } from '../push/register'

import { tokenStorage } from './storage'

type AuthState =
  | { status: 'loading' }
  | { status: 'signed-out' }
  | { status: 'signed-in'; userId: string; token: string }

interface AuthContextValue {
  state: AuthState
  login: (
    username: string,
    password: string
  ) => Promise<'ok' | 'invalid' | 'rate-limited' | 'network'>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>({ status: 'loading' })

  useEffect(() => {
    void (async () => {
      const token = await tokenStorage.get()
      if (!token) {
        setState({ status: 'signed-out' })
        return
      }
      try {
        const status = await api.status(token)
        if (status.loggedIn) {
          setState({ status: 'signed-in', userId: status.user.id, token })
        } else {
          await tokenStorage.clear()
          setState({ status: 'signed-out' })
        }
      } catch {
        setState({ status: 'signed-out' })
      }
    })()
  }, [])

  const login = useCallback(async (username: string, password: string) => {
    const result = await api.login(username, password)
    if (result.kind === 'ok') {
      await tokenStorage.set(result.token)
      setState({
        status: 'signed-in',
        userId: result.userId,
        token: result.token
      })
      return 'ok' as const
    }
    return result.kind
  }, [])

  const logout = useCallback(async () => {
    if (state.status === 'signed-in') {
      try {
        await unregisterFromPushNotifications(state.token)
      } catch {
        // ignore
      }
      try {
        await api.logout(state.token)
      } catch {
        // ignore network errors during logout
      }
    }
    await tokenStorage.clear()
    setState({ status: 'signed-out' })
  }, [state])

  return (
    <AuthContext.Provider value={{ state, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}
