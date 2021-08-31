import { UUID } from '../types'
import { Failure, Result, Success } from 'lib-common/api'
import { client } from './client'
import { JsonOf } from 'lib-common/json'
import {
  deserializeIncomeStatement,
  IncomeStatement
} from 'lib-common/api-types/incomeStatement'

export async function getIncomeStatements(
  personId: UUID
): Promise<Result<IncomeStatement[]>> {
  return client
    .get<JsonOf<IncomeStatement[]>>(`/income-statements/person/${personId}`)
    .then((res) => res.data.map(deserializeIncomeStatement))
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
