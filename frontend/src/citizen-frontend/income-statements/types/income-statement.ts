import { IncomeSource, OtherIncome } from './common'
import { UUID } from 'lib-common/types'
import LocalDate from 'lib-common/local-date'
import { Attachment } from 'lib-common/api-types/attachment'

interface Base {
  id: UUID
  startDate: LocalDate
}

export interface HighestFee extends Base {
  type: 'HIGHEST_FEE'
}

export interface Income extends Base {
  type: 'INCOME'
  gross: Gross | null
  entrepreneur: Entrepreneur | null
  otherInfo: string
  attachments: Attachment[]
}

export interface Gross {
  incomeSource: IncomeSource
  otherIncome: OtherIncome[]
}

export interface Entrepreneur {
  fullTime: boolean
  startOfEntrepreneurship: LocalDate
  spouseWorksInCompany: boolean
  startupGrant: boolean
  selfEmployed: SelfEmployedAttachments | SelfEmployedEstimation | null
  limitedCompany: LimitedCompany | null
  partnership: boolean
}

export interface SelfEmployedAttachments {
  type: 'ATTACHMENTS'
}

export interface SelfEmployedEstimation {
  type: 'ESTIMATION'
  estimatedMonthlyIncome: number
  incomeStartDate: LocalDate
  incomeEndDate: LocalDate | null
}

export type SelfEmployed = SelfEmployedAttachments | SelfEmployedEstimation

export interface LimitedCompany {
  incomeSource: IncomeSource
}

export type IncomeStatement = HighestFee | Income
