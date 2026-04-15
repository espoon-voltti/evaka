// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { api, ApiError } from './client'

type LoginResult =
  | { kind: 'ok'; token: string; userId: string; expiresAt: number }
  | { kind: 'invalid' }
  | { kind: 'rate-limited' }
  | { kind: 'network' }

type StatusResponse =
  | {
      loggedIn: true
      user: { id: string; authType: 'citizen-mobile-weak' }
      expiresAt: number
    }
  | { loggedIn: false }

export async function login(
  username: string,
  password: string
): Promise<LoginResult> {
  try {
    const res = await api<{
      token: string
      expiresAt: number
      user: { id: string }
    }>('/citizen-mobile/auth/login/v1', {
      method: 'POST',
      body: { username, password }
    })
    return {
      kind: 'ok',
      token: res.token,
      userId: res.user.id,
      expiresAt: res.expiresAt
    }
  } catch (err) {
    if (err instanceof ApiError) {
      if (err.status === 403 || err.status === 401) return { kind: 'invalid' }
      if (err.status === 429) return { kind: 'rate-limited' }
    }
    return { kind: 'network' }
  }
}

export async function logout(token: string): Promise<void> {
  await api<void>('/citizen-mobile/auth/logout/v1', {
    method: 'POST',
    token
  })
}

export async function status(token: string): Promise<StatusResponse> {
  try {
    return await api<StatusResponse>('/citizen-mobile/auth/status/v1', {
      token
    })
  } catch (err) {
    if (
      err instanceof ApiError &&
      (err.status === 401 || err.status === 403)
    ) {
      return { loggedIn: false }
    }
    throw err
  }
}
