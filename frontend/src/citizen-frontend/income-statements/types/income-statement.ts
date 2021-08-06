import { IncomeSource, OtherIncome } from './common'
import { UUID } from 'lib-common/types'
import LocalDate from 'lib-common/local-date'
import { Attachment } from 'lib-common/api-types/attachment'

interface Base {
  id: UUID
  startDate: LocalDate
  attachments: Attachment[]
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

type ToBody<T> = Omit<T, 'id' | 'attachments'> & { attachmentIds: UUID[] }

export type IncomeStatementBody =
  | ToBody<HighestFee>
  | ToBody<Gross>
  | ToBody<EntrepreneurSelfEmployedEstimation>
  | ToBody<EntrepreneurSelfEmployedAttachments>
  | ToBody<EntrepreneurLimitedCompany>
  | ToBody<EntrepreneurPartnership>
