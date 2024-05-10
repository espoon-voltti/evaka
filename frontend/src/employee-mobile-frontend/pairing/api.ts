// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import { JsonOf } from 'lib-common/json'

import { client } from '../client'

export function authMobile(
  id: string,
  challengeKey: string,
  responseKey: string
): Promise<Result<void>> {
  return client
    .post<JsonOf<void>>(`/auth/mobile`, {
      id,
      challengeKey,
      responseKey
    })
    .then((res) => res.data)
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}
