import { IncomeSource, OtherIncome } from './common'
import { Attachment } from 'lib-common/api-types/attachment'
import * as IncomeStatement from './income-statement'
import LocalDate from 'lib-common/local-date'

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

export interface Estimation {
  estimatedMonthlyIncome: string
  incomeStartDate: string
  incomeEndDate: string
}

export interface Gross extends Estimation {
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
  checkupConsent: boolean
  selfEmployed: SelfEmployed
  limitedCompany: LimitedCompany
  partnership: boolean
  lightEntrepreneur: boolean
  accountant: Accountant
}

export interface SelfEmployed extends Estimation {
  selected: boolean
  attachments: boolean
  estimation: boolean
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

export const empty: IncomeStatementForm = {
  startDate: LocalDate.today().format(),
  endDate: '',
  highestFee: false,
  gross: {
    selected: false,
    incomeSource: null,
    estimatedMonthlyIncome: '',
    incomeStartDate: '',
    incomeEndDate: '',
    otherIncome: null
  },
  entrepreneur: {
    selected: false,
    fullTime: null,
    startOfEntrepreneurship: '',
    spouseWorksInCompany: null,
    startupGrant: false,
    checkupConsent: false,
    selfEmployed: {
      selected: false,
      attachments: false,
      estimation: false,
      estimatedMonthlyIncome: '',
      incomeStartDate: '',
      incomeEndDate: ''
    },
    limitedCompany: {
      selected: false,
      incomeSource: null
    },
    partnership: false,
    lightEntrepreneur: false,
    accountant: {
      name: '',
      email: '',
      address: '',
      phone: ''
    }
  },
  student: false,
  alimonyPayer: false,
  otherInfo: '',
  attachments: [],
  assure: false
}

export function fromIncomeStatement(
  incomeStatement: IncomeStatement.IncomeStatement
): IncomeStatementForm {
  return {
    ...empty,
    startDate: incomeStatement.startDate.format(),
    endDate: incomeStatement.endDate?.format() ?? '',
    highestFee: incomeStatement.type === 'HIGHEST_FEE',
    ...(incomeStatement.type === 'INCOME'
      ? {
          gross: mapGross(incomeStatement.gross),
          entrepreneur: mapEntrepreneur(incomeStatement.entrepreneur)
        }
      : undefined)
  }
}

function mapGross(gross: IncomeStatement.Gross | null): Gross {
  if (!gross) return empty.gross
  return {
    selected: true,
    incomeSource: gross.incomeSource,
    ...mapEstimation(gross.estimatedIncome),
    otherIncome: gross.otherIncome
  }
}

function mapEstimation(
  estimatedIncome: IncomeStatement.EstimatedIncome | null
): Estimation {
  return {
    estimatedMonthlyIncome:
      estimatedIncome?.estimatedMonthlyIncome?.toString() ?? '',
    incomeStartDate: estimatedIncome?.incomeStartDate?.format() ?? '',
    incomeEndDate: estimatedIncome?.incomeEndDate?.format() ?? ''
  }
}

function mapEntrepreneur(
  entrepreneur: IncomeStatement.Entrepreneur | null
): Entrepreneur {
  if (!entrepreneur) return empty.entrepreneur
  return {
    selected: true,
    fullTime: entrepreneur.fullTime,
    startOfEntrepreneurship: entrepreneur.startOfEntrepreneurship.format(),
    spouseWorksInCompany: entrepreneur.spouseWorksInCompany,
    startupGrant: entrepreneur.startupGrant,
    checkupConsent: true,
    selfEmployed: mapSelfEmployed(entrepreneur.selfEmployed),
    limitedCompany: mapLimitedCompany(entrepreneur.limitedCompany),
    partnership: entrepreneur.partnership,
    lightEntrepreneur: entrepreneur.lightEntrepreneur,
    accountant: entrepreneur.accountant ?? empty.entrepreneur.accountant
  }
}

function mapSelfEmployed(
  selfEmployed: IncomeStatement.SelfEmployed | null
): SelfEmployed {
  if (!selfEmployed) return empty.entrepreneur.selfEmployed
  return {
    selected: true,
    attachments: selfEmployed.attachments,
    estimation: selfEmployed.estimatedIncome != null,
    ...mapEstimation(selfEmployed.estimatedIncome)
  }
}

function mapLimitedCompany(
  limitedCompany: IncomeStatement.LimitedCompany | null
): LimitedCompany {
  if (!limitedCompany) return empty.entrepreneur.limitedCompany
  return {
    selected: true,
    incomeSource: limitedCompany.incomeSource
  }
}
