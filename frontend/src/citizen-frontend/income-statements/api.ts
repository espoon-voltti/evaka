import { Failure, Result, Success } from 'lib-common/api'
import { client } from '../api-client'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { IncomeStatement, IncomeStatementBody } from './types/income-statement'

function deserializeIncomeStatement(
  data: JsonOf<IncomeStatement>
): IncomeStatement {
  if (data.incomeType === 'ENTREPRENEUR_SELF_EMPLOYED_ESTIMATION') {
    return {
      ...data,
      startDate: LocalDate.parseIso(data.startDate),
      incomeStartDate: LocalDate.parseIso(data.incomeStartDate),
      incomeEndDate:
        data.incomeEndDate !== null
          ? LocalDate.parseIso(data.incomeEndDate)
          : null
    }
  }
  return {
    ...data,
    startDate: LocalDate.parseIso(data.startDate)
  }
}

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
