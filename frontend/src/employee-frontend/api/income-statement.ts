import { Failure, Result, Success } from 'lib-common/api'
import {
  deserializeIncomeStatement,
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

export async function getIncomeStatementsAwaitingHandler(): Promise<
  Result<IncomeStatementAwaitingHandler[]>
> {
  return client
    .get<JsonOf<IncomeStatementAwaitingHandler[]>>(
      '/income-statements/awaiting-handler'
    )
    .then((res) => Success.of(res.data))
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
