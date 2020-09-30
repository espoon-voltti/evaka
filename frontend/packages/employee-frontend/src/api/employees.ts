// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from '~api/index'
import { client } from '~api/client'
import { Employee } from '~types/employee'
import { JsonOf } from '@evaka/lib-common/src/json'

export async function getEmployees(): Promise<Result<Employee[]>> {
  return client
    .get<JsonOf<Employee[]>>(`/employee`)
    .then((res) =>
      res.data.map((data) => ({
        ...data,
        created: new Date(data.created),
        updated: new Date(data.updated)
      }))
    )
    .then(Success)
    .catch(Failure)
}
