// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import { CitizenUserResponse } from 'lib-common/generated/api-types/pis'
import { CitizenAuthLevel } from 'lib-common/generated/api-types/shared'

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

export function getAuthStatus(): Promise<Result<AuthStatus>> {
  return client
    .get<AuthStatus>('/auth/status')
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}
