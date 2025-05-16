// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { CitizenUserResponse } from 'lib-common/generated/api-types/pis'
import type { CitizenAuthLevel } from 'lib-common/generated/api-types/shared'
import type { JsonOf } from 'lib-common/json'

import { client } from '../api-client'

export type AuthStatus =
  | { loggedIn: false; apiVersion: string }
  | {
      loggedIn: true
      user: CitizenUserResponse
      apiVersion: string
      authLevel: CitizenAuthLevel
    }

export async function getAuthStatus(): Promise<AuthStatus> {
  return (await client.get<JsonOf<AuthStatus>>('/citizen/auth/status')).data
}

interface WeakLoginRequest {
  username: string
  password: string
}

export async function authWeakLogin(
  username: string,
  password: string
): Promise<void> {
  const reqBody: WeakLoginRequest = { username, password }
  await client.post('/citizen/auth/weak-login', reqBody)
}
