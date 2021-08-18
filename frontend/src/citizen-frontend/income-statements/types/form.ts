import { IncomeSource, OtherIncome } from './common'
import { Attachment } from 'lib-common/api-types/attachment'

export interface IncomeStatementForm {
  startDate: string
  endDate: string
  highestFee: boolean
  gross: Gross
  entrepreneur: Entrepreneur
  student: boolean
  alimonyPayer: boolean
  otherInfo: string
  attachments: Attachment[]
  assure: boolean
}

export interface Gross {
  selected: boolean
  incomeSource: IncomeSource | null
  estimatedMonthlyIncome: string
  incomeStartDate: string
  incomeEndDate: string
  otherIncome: OtherIncome[] | null
}

export interface Entrepreneur {
  selected: boolean
  fullTime: boolean | null
  startOfEntrepreneurship: string
  spouseWorksInCompany: boolean | null
  startupGrant: boolean
  checkupConsent: boolean
  selfEmployed: SelfEmployed
  limitedCompany: LimitedCompany
  partnership: boolean
  lightEntrepreneur: boolean
  accountant: Accountant
}

export interface SelfEmployed {
  selected: boolean
  attachments: boolean
  estimation: boolean
  estimatedMonthlyIncome: string
  incomeStartDate: string
  incomeEndDate: string
}

export interface LimitedCompany {
  selected: boolean
  incomeSource: IncomeSource | null
}

export interface Accountant {
  name: string
  email: string
  address: string
  phone: string
}
