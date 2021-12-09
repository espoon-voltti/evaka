// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import { ChildrenResponse } from 'lib-common/generated/api-types/children'
import { JsonOf } from 'lib-common/json'
import { client } from '../api-client'

export function getChildren(): Promise<Result<ChildrenResponse>> {
  return client
    .get<JsonOf<ChildrenResponse>>('/citizen/children')
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}
