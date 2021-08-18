import { IncomeSource, OtherIncome } from './common'
import { UUID } from 'lib-common/types'
import LocalDate from 'lib-common/local-date'
import { Attachment } from 'lib-common/api-types/attachment'

interface Base {
  id: UUID
  startDate: LocalDate
  endDate: LocalDate | null
}

export interface HighestFee extends Base {
  type: 'HIGHEST_FEE'
}

export interface Income extends Base {
  type: 'INCOME'
  gross: Gross | null
  entrepreneur: Entrepreneur | null
  student: boolean
  alimonyPayer: boolean
  otherInfo: string
  attachments: Attachment[]
}

export interface Gross {
  incomeSource: IncomeSource
  estimatedIncome: EstimatedIncome | null
  otherIncome: OtherIncome[]
}

export interface Entrepreneur {
  fullTime: boolean
  startOfEntrepreneurship: LocalDate
  spouseWorksInCompany: boolean
  startupGrant: boolean
  selfEmployed: SelfEmployed | null
  limitedCompany: LimitedCompany | null
  partnership: boolean
  lightEntrepreneur: boolean
}

export interface SelfEmployed {
  attachments: boolean
  estimatedIncome: EstimatedIncome | null
}

export interface EstimatedIncome {
  estimatedMonthlyIncome: number
  incomeStartDate: LocalDate
  incomeEndDate: LocalDate | null
}

export interface LimitedCompany {
  incomeSource: IncomeSource
}

export type IncomeStatement = HighestFee | Income
