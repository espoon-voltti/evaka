import { Failure, Result, Success } from 'lib-common/api'
import { client } from '../api-client'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import {
  Entrepreneur,
  EstimatedIncome,
  Gross,
  IncomeStatement,
  SelfEmployed
} from './types/income-statement'
import { IncomeBody, IncomeStatementBody } from './types/body'
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

type IncomeBodyJson = JsonOf<IncomeBody>
type GrossJson = Exclude<IncomeBodyJson['gross'], null>
type EntrepreneurJson = Exclude<IncomeBodyJson['entrepreneur'], null>
type SelfEmployedJson = Exclude<EntrepreneurJson['selfEmployed'], null>
type EstimatedIncomeJson = Exclude<SelfEmployedJson['estimatedIncome'], null>

function deserializeIncomeStatement(
  data: JsonOf<IncomeStatement>
): IncomeStatement {
  const startDate = LocalDate.parseIso(data.startDate)
  const endDate = data.endDate ? LocalDate.parseIso(data.endDate) : null
  switch (data.type) {
    case 'HIGHEST_FEE':
      return { ...data, startDate, endDate }
    case 'INCOME':
      return {
        ...data,
        startDate,
        endDate,
        gross: deserializeGross(data.gross),
        entrepreneur: deserializeEntrepreneur(data.entrepreneur)
      }
  }
}

function deserializeGross(gross: GrossJson | null): Gross | null {
  if (!gross) return null
  return {
    ...gross,
    estimatedIncome: deserializeEstimatedIncome(gross.estimatedIncome)
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
  return {
    ...selfEmployed,
    estimatedIncome: deserializeEstimatedIncome(selfEmployed.estimatedIncome)
  }
}

function deserializeEstimatedIncome(
  estimatedIncome: EstimatedIncomeJson | null
): EstimatedIncome | null {
  if (!estimatedIncome) return null
  return {
    ...estimatedIncome,
    incomeStartDate: LocalDate.parseIso(estimatedIncome.incomeStartDate),
    incomeEndDate: estimatedIncome.incomeEndDate
      ? LocalDate.parseIso(estimatedIncome.incomeEndDate)
      : null
  }
}
