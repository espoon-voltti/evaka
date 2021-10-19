// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import { client } from './client'
import { JsonOf } from 'lib-common/json'
import { AuthStatus } from 'lib-common/api-types/employee-auth'

export async function getAuthStatus(): Promise<Result<AuthStatus>> {
  return client
    .get<JsonOf<AuthStatus>>('/auth/status')
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}
