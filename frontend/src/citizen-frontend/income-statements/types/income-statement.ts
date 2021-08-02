import { IncomeSource, OtherIncome } from './common'
import { UUID } from 'lib-common/types'
import LocalDate from 'lib-common/local-date'

interface Base {
  id: UUID
  startDate: LocalDate
}

export interface HighestFee extends Base {
  incomeType: 'HIGHEST_FEE'
}

export interface Gross extends Base {
  incomeType: 'GROSS'
  incomeSource: IncomeSource
  otherIncome: OtherIncome[]
}

export interface EntrepreneurSelfEmployedEstimation extends Base {
  incomeType: 'ENTREPRENEUR_SELF_EMPLOYED_ESTIMATION'
  estimatedMonthlyIncome: number
  incomeStartDate: LocalDate
  incomeEndDate: LocalDate | null
}

export interface EntrepreneurSelfEmployedAttachments extends Base {
  incomeType: 'ENTREPRENEUR_SELF_EMPLOYED_ATTACHMENTS'
}

export interface EntrepreneurLimitedCompany extends Base {
  incomeType: 'ENTREPRENEUR_LIMITED_COMPANY'
  incomeSource: IncomeSource
}

export interface EntrepreneurPartnership extends Base {
  incomeType: 'ENTREPRENEUR_PARTNERSHIP'
}

export type IncomeStatement =
  | HighestFee
  | Gross
  | EntrepreneurSelfEmployedEstimation
  | EntrepreneurSelfEmployedAttachments
  | EntrepreneurLimitedCompany
  | EntrepreneurPartnership

export type IncomeStatementBody =
  | Omit<HighestFee, 'id'>
  | Omit<Gross, 'id'>
  | Omit<EntrepreneurSelfEmployedEstimation, 'id'>
  | Omit<EntrepreneurSelfEmployedAttachments, 'id'>
  | Omit<EntrepreneurLimitedCompany, 'id'>
  | Omit<EntrepreneurPartnership, 'id'>
