// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { Result } from 'lib-common/api'
import { Failure, Success } from 'lib-common/api'
import type { UserDetailsResponse } from 'lib-common/api-types/vtjclient'

import { client } from '../api-client'

export type AuthStatus =
  | { loggedIn: false; apiVersion: string }
  | { loggedIn: true; user: UserDetailsResponse; apiVersion: string }

export function getAuthStatus(): Promise<Result<AuthStatus>> {
  return client
    .get<AuthStatus>('/auth/status')
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}
