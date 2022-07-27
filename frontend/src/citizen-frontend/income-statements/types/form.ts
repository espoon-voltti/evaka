// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { Attachment } from 'lib-common/api-types/attachment'
import type * as IncomeStatement from 'lib-common/api-types/incomeStatement'
import type { IncomeSource, OtherIncome } from 'lib-common/generated/enums'
import LocalDate from 'lib-common/local-date'

export interface IncomeStatementForm {
  startDate: LocalDate | null
  endDate: LocalDate | null
  highestFee: boolean
  childIncome: boolean
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
  otherIncome: OtherIncome[]
  otherIncomeInfo: string
}

export interface Entrepreneur {
  selected: boolean
  fullTime: boolean | null
  startOfEntrepreneurship: LocalDate | null
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
  incomeStartDate: LocalDate | null
  incomeEndDate: LocalDate | null
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
  startDate: LocalDate.todayInSystemTz(),
  endDate: null,
  highestFee: false,
  childIncome: false,
  gross: {
    selected: false,
    incomeSource: null,
    estimatedMonthlyIncome: '',
    otherIncome: [],
    otherIncomeInfo: ''
  },
  entrepreneur: {
    selected: false,
    fullTime: null,
    startOfEntrepreneurship: null,
    spouseWorksInCompany: null,
    startupGrant: false,
    checkupConsent: false,
    selfEmployed: {
      selected: false,
      attachments: false,
      estimation: false,
      estimatedMonthlyIncome: '',
      incomeStartDate: null,
      incomeEndDate: null
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

function findValidStartDate(existingStartDates: LocalDate[]): LocalDate {
  const yesterday = LocalDate.todayInSystemTz().subDays(1)
  return existingStartDates
    .reduce((a, b) => (a.isAfter(b) ? a : b), yesterday)
    .addDays(1)
}

export const initialFormData = (
  existingStartDates: LocalDate[]
): IncomeStatementForm => {
  return {
    ...empty,
    startDate: findValidStartDate(existingStartDates)
  }
}

export function fromIncomeStatement(
  incomeStatement: IncomeStatement.IncomeStatement
): IncomeStatementForm {
  return {
    ...empty,
    startDate: incomeStatement.startDate,
    endDate: incomeStatement.endDate,
    highestFee: incomeStatement.type === 'HIGHEST_FEE',
    ...(incomeStatement.type === 'INCOME'
      ? {
          gross: mapGross(incomeStatement.gross),
          entrepreneur: mapEntrepreneur(incomeStatement.entrepreneur),
          student: incomeStatement.student,
          alimonyPayer: incomeStatement.alimonyPayer,
          otherInfo: incomeStatement.otherInfo,
          attachments: incomeStatement.attachments
        }
      : incomeStatement.type === 'CHILD_INCOME'
      ? {
          childIncome: true,
          otherInfo: incomeStatement.otherInfo,
          attachments: incomeStatement.attachments
        }
      : undefined)
  }
}

function mapGross(gross: IncomeStatement.Gross | null): Gross {
  if (!gross) return empty.gross
  return {
    selected: true,
    incomeSource: gross.incomeSource,
    estimatedMonthlyIncome: gross.estimatedMonthlyIncome?.toString() ?? '',
    otherIncome: gross.otherIncome,
    otherIncomeInfo: gross.otherIncomeInfo
  }
}

function mapEstimation(
  estimatedIncome: IncomeStatement.EstimatedIncome | null
): {
  estimatedMonthlyIncome: string
  incomeStartDate: LocalDate | null
  incomeEndDate: LocalDate | null
} {
  return {
    estimatedMonthlyIncome:
      estimatedIncome?.estimatedMonthlyIncome?.toString() ?? '',
    incomeStartDate: estimatedIncome?.incomeStartDate ?? null,
    incomeEndDate: estimatedIncome?.incomeEndDate ?? null
  }
}

function mapEntrepreneur(
  entrepreneur: IncomeStatement.Entrepreneur | null
): Entrepreneur {
  if (!entrepreneur) return empty.entrepreneur
  return {
    selected: true,
    fullTime: entrepreneur.fullTime,
    startOfEntrepreneurship: entrepreneur.startOfEntrepreneurship,
    spouseWorksInCompany: entrepreneur.spouseWorksInCompany,
    startupGrant: entrepreneur.startupGrant,
    checkupConsent: entrepreneur.checkupConsent,
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
