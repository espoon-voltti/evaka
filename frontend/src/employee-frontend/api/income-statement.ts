// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Paged, Result, Success } from 'lib-common/api'
import {
  deserializeIncomeStatement,
  deserializeIncomeStatementAwaitingHandler,
  IncomeStatement,
  IncomeStatementAwaitingHandler
} from 'lib-common/api-types/incomeStatement'
import { JsonOf } from 'lib-common/json'
import { SetIncomeStatementHandledBody } from 'lib-common/generated/api-types/incomestatement'
import { client } from './client'
import { UUID } from 'lib-common/types'

export async function getIncomeStatements(
  personId: UUID,
  page: number,
  pageSize = 10
): Promise<Result<Paged<IncomeStatement>>> {
  return client
    .get<JsonOf<Paged<IncomeStatement>>>(
      `/income-statements/person/${personId}`,
      {
        params: { page, pageSize }
      }
    )
    .then(({ data: { data, ...rest } }) => ({
      ...rest,
      data: data.map(deserializeIncomeStatement)
    }))
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export interface IncomeStatementsAwaitingHandlerParams {
  areas: string[]
  page: number
  pageSize: number
}

export async function getIncomeStatementsAwaitingHandler({
  areas,
  page,
  pageSize
}: IncomeStatementsAwaitingHandlerParams): Promise<
  Result<Paged<IncomeStatementAwaitingHandler>>
> {
  return client
    .get<JsonOf<Paged<IncomeStatementAwaitingHandler>>>(
      '/income-statements/awaiting-handler',
      { params: { areas: areas.join(','), page, pageSize } }
    )
    .then((res) => res.data)
    .then((body) => ({
      ...body,
      data: body.data.map(deserializeIncomeStatementAwaitingHandler)
    }))
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function getIncomeStatement(
  personId: UUID,
  incomeStatementId: UUID
): Promise<Result<IncomeStatement>> {
  return client
    .get<JsonOf<IncomeStatement>>(
      `/income-statements/person/${personId}/${incomeStatementId}`
    )
    .then((res) => deserializeIncomeStatement(res.data))
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function updateIncomeStatementHandled(
  incomeStatementId: UUID,
  body: SetIncomeStatementHandledBody
): Promise<Result<void>> {
  return client
    .post(`/income-statements/${incomeStatementId}/handled`, body)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}
