// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Paged, Result, Success } from 'lib-common/api'
import {
  deserializeIncomeStatement,
  IncomeStatement
} from 'lib-common/api-types/incomeStatement'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { client } from '../api-client'
import { IncomeStatementBody } from './types/body'

export async function getIncomeStatements(
  page: number,
  pageSize: number
): Promise<Result<Paged<IncomeStatement>>> {
  return client
    .get<JsonOf<Paged<IncomeStatement>>>('/citizen/income-statements', {
      params: { page, pageSize }
    })
    .then(({ data: { data, ...rest } }) => ({
      ...rest,
      data: data.map(deserializeIncomeStatement)
    }))
    .then((data) => Success.of(data))
    .catch((e) => Failure.fromError(e))
}

export async function getIncomeStatement(
  id: UUID
): Promise<Result<IncomeStatement>> {
  return client
    .get<JsonOf<IncomeStatement>>(`/citizen/income-statements/${id}`)
    .then((res) => deserializeIncomeStatement(res.data))
    .then((data) => Success.of(data))
    .catch((e) => Failure.fromError(e))
}

export const getIncomeStatementStartDates = (): Promise<Result<LocalDate[]>> =>
  client
    .get<JsonOf<LocalDate[]>>(`/citizen/income-statements/start-dates`)
    .then(({ data }) => data.map((d) => LocalDate.parseIso(d)))
    .then((data) => Success.of(data))
    .catch((e) => Failure.fromError(e))

export async function createIncomeStatement(
  body: IncomeStatementBody
): Promise<void> {
  return client.post('/citizen/income-statements', body)
}

export async function updateIncomeStatement(
  id: UUID,
  body: IncomeStatementBody
): Promise<void> {
  return client.put(`/citizen/income-statements/${id}`, body)
}

export async function deleteIncomeStatement(id: UUID): Promise<Result<void>> {
  return client
    .delete(`/citizen/income-statements/${id}`)
    .then(() => Success.of())
    .catch((err) => Failure.fromError(err))
}
