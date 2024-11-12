// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { isAxiosError } from 'axios'

import { CitizenUserResponse } from 'lib-common/generated/api-types/pis'
import { CitizenAuthLevel } from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { JsonOf } from 'lib-common/json'

import { client } from '../api-client'

export type AuthStatus =
  | { loggedIn: false; apiVersion: string }
  | {
      loggedIn: true
      antiCsrfToken: string
      user: CitizenUserResponse
      apiVersion: string
      authLevel: CitizenAuthLevel
    }

export async function getAuthStatus(): Promise<AuthStatus> {
  return (await client.get<JsonOf<AuthStatus>>('/auth/status')).data
}

interface WeakLoginRequest {
  username: string
  password: string
}

let nextWeakLoginAttempt = HelsinkiDateTime.now()

export async function authWeakLogin(
  username: string,
  password: string,
  opts: { retryOnRateLimit: boolean } = {
    retryOnRateLimit: true
  }
): Promise<void> {
  const reqBody: WeakLoginRequest = { username, password }

  // apply client-side rate limit
  const now = HelsinkiDateTime.now()
  const sleepMs =
    nextWeakLoginAttempt.toSystemTzDate().getTime() -
    now.toSystemTzDate().getTime()
  if (sleepMs > 0) {
    await new Promise((resolve) => setTimeout(resolve, sleepMs))
  }

  try {
    await client.post('/auth/weak-login', reqBody)
    return
  } catch (e) {
    nextWeakLoginAttempt = HelsinkiDateTime.now().addSeconds(1)
    if (
      isAxiosError(e) &&
      e.response?.status === 429 &&
      opts.retryOnRateLimit
    ) {
      return authWeakLogin(username, password, { retryOnRateLimit: false })
    }
    throw e
  }
}
