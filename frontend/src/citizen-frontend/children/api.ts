// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import { Child } from 'lib-common/generated/api-types/children'
import { JsonOf } from 'lib-common/json'

import { client } from '../api-client'

export function getChildren(): Promise<Result<Child[]>> {
  return client
    .get<JsonOf<Child[]>>('/citizen/children')
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}
