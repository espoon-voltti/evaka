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
