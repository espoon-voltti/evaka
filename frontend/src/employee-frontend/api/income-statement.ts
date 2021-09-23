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
import { UUID } from '../types'
import { client } from './client'

export async function getIncomeStatements(
  personId: UUID
): Promise<Result<IncomeStatement[]>> {
  return client
    .get<JsonOf<IncomeStatement[]>>(`/income-statements/person/${personId}`)
    .then((res) => res.data.map(deserializeIncomeStatement))
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function getIncomeStatementsAwaitingHandler(
  areas: string[] = [],
  page = 1,
  pageSize = 50
): Promise<
  Result<Paged<IncomeStatementAwaitingHandler> & { currentPage: number }>
> {
  const areasParam = areas.join(',')
  return client
    .get<JsonOf<Paged<IncomeStatementAwaitingHandler>>>(
      `/income-statements/awaiting-handler?areas=${areasParam}&page=${page}&pageSize=${pageSize}`
    )
    .then((res) => res.data)
    .then((body) => ({
      ...body,
      data: body.data.map(deserializeIncomeStatementAwaitingHandler),
      currentPage: page
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

export async function setIncomeStatementHandled(
  incomeStatementId: UUID,
  handled: boolean
): Promise<Result<void>> {
  return client
    .post(`/income-statements/${incomeStatementId}/handled`, { data: handled })
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}
