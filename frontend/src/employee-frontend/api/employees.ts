// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from '@evaka/lib-common/src/api'
import { client } from '../api/client'
import { Employee } from '../types/employee'
import { JsonOf } from '@evaka/lib-common/src/json'
import { FinanceDecisionHandlerOption } from '../state/invoicing-ui'

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
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function getFinanceDecisionHandlers(): Promise<
  Result<FinanceDecisionHandlerOption[]>
> {
  return client
    .get<JsonOf<Employee[]>>(`/employee/finance-decision-handler`)
    .then((res) =>
      res.data.map((data) => ({
        value: data.id,
        label: [data.firstName, data.lastName].join(' ')
      }))
    )
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}
