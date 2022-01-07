// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import { client } from '../api-client'
import { User } from './state'

export type AuthStatus =
  | { loggedIn: false; apiVersion: string }
  | { loggedIn: true; user: User; apiVersion: string }

export function getAuthStatus(): Promise<Result<AuthStatus>> {
  return client
    .get<AuthStatus>('/auth/status')
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}
