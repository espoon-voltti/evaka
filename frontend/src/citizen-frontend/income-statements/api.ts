import { Failure, Result, Success } from 'lib-common/api'
import { client } from '../api-client'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import {
  Entrepreneur,
  IncomeStatement,
  SelfEmployed
} from './types/income-statement'
import { IncomeBody, IncomeStatementBody } from './types/body'

export async function getIncomeStatements(): Promise<
  Result<IncomeStatement[]>
> {
  return client
    .get<JsonOf<IncomeStatement[]>>('/citizen/income-statements')
    .then((res) => res.data.map(deserializeIncomeStatement))
    .then((data) => Success.of(data))
    .catch((e) => Failure.fromError(e))
}

export async function createIncomeStatement(
  body: IncomeStatementBody
): Promise<void> {
  return client.post('/citizen/income-statements', body)
}

type IncomeBodyJson = JsonOf<IncomeBody>
type EntrepreneurJson = Exclude<IncomeBodyJson['entrepreneur'], null>
type SelfEmployedJson = Exclude<EntrepreneurJson['selfEmployed'], null>

function deserializeIncomeStatement(
  data: JsonOf<IncomeStatement>
): IncomeStatement {
  switch (data.type) {
    case 'HIGHEST_FEE':
      return { ...data, startDate: LocalDate.parseIso(data.startDate) }
    case 'INCOME':
      return {
        ...data,
        startDate: LocalDate.parseIso(data.startDate),
        entrepreneur: deserializeEntrepreneur(data.entrepreneur)
      }
  }
}

function deserializeEntrepreneur(
  entrepreneur: EntrepreneurJson | null
): Entrepreneur | null {
  if (!entrepreneur) return null
  return {
    ...entrepreneur,
    startOfEntrepreneurship: LocalDate.parseIso(
      entrepreneur.startOfEntrepreneurship
    ),
    selfEmployed: deserializeSelfEmployed(entrepreneur.selfEmployed)
  }
}

function deserializeSelfEmployed(
  selfEmployed: SelfEmployedJson | null
): SelfEmployed | null {
  if (!selfEmployed) return null
  return selfEmployed.type === 'ESTIMATION'
    ? {
        ...selfEmployed,
        incomeStartDate: LocalDate.parseIso(selfEmployed.incomeStartDate),
        incomeEndDate: selfEmployed.incomeEndDate
          ? LocalDate.parseIso(selfEmployed.incomeEndDate)
          : null
      }
    : selfEmployed
}
