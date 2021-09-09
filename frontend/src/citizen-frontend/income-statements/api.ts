// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import { client } from '../api-client'
import { JsonOf } from 'lib-common/json'
import {
  deserializeIncomeStatement,
  IncomeStatement
} from 'lib-common/api-types/incomeStatement'
import { IncomeStatementBody } from './types/body'
import { UUID } from 'lib-common/types'

export async function getIncomeStatements(): Promise<
  Result<IncomeStatement[]>
> {
  return client
    .get<JsonOf<IncomeStatement[]>>('/citizen/income-statements')
    .then((res) => res.data.map(deserializeIncomeStatement))
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
