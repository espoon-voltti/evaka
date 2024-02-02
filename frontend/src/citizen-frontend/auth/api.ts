// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { CitizenUserResponse } from 'lib-common/generated/api-types/pis'
import { CitizenAuthLevel } from 'lib-common/generated/api-types/shared'
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
