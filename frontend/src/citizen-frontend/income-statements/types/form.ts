import { IncomeSource, OtherIncome } from './common'
import { Attachment } from 'lib-common/api-types/attachment'

export interface IncomeStatementForm {
  startDate: string
  highestFee: boolean
  gross: Gross
  entrepreneur: Entrepreneur
  otherInfo: string
  attachments: Attachment[]
}

export interface Gross {
  selected: boolean
  incomeSource: IncomeSource | null
  otherIncome: OtherIncome[] | null
}

export interface Entrepreneur {
  selected: boolean
  fullTime: boolean | null
  startOfEntrepreneurship: string
  spouseWorksInCompany: boolean | null
  startupGrant: boolean
  selfEmployed: SelfEmployed
  limitedCompany: LimitedCompany
  partnership: Partnership
}

export interface SelfEmployed {
  selected: boolean
  estimation: boolean | null
  estimatedMonthlyIncome: string
  incomeStartDate: string
  incomeEndDate: string
  kelaConsent: boolean
}

export interface LimitedCompany {
  selected: boolean
  incomeSource: IncomeSource | null
}

export interface Partnership {
  selected: boolean
  lookupConsent: boolean
}
