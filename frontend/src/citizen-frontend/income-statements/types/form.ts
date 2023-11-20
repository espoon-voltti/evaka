// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Attachment } from 'lib-common/api-types/attachment'
import * as ApiTypes from 'lib-common/generated/api-types/incomestatement'
import {
  IncomeSource,
  OtherIncome
} from 'lib-common/generated/api-types/incomestatement'
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

export const emptyIncomeStatementForm: IncomeStatementForm = {
  startDate: null,
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

export function fromIncomeStatement(
  incomeStatement: ApiTypes.IncomeStatement
): IncomeStatementForm {
  return {
    ...emptyIncomeStatementForm,
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

function mapGross(gross: ApiTypes.Gross | null): Gross {
  if (!gross) return emptyIncomeStatementForm.gross
  return {
    selected: true,
    incomeSource: gross.incomeSource,
    estimatedMonthlyIncome: gross.estimatedMonthlyIncome?.toString() ?? '',
    otherIncome: gross.otherIncome,
    otherIncomeInfo: gross.otherIncomeInfo
  }
}

function mapEstimation(estimatedIncome: ApiTypes.EstimatedIncome | null): {
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
  entrepreneur: ApiTypes.Entrepreneur | null
): Entrepreneur {
  if (!entrepreneur) return emptyIncomeStatementForm.entrepreneur
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
    accountant:
      entrepreneur.accountant ??
      emptyIncomeStatementForm.entrepreneur.accountant
  }
}

function mapSelfEmployed(
  selfEmployed: ApiTypes.SelfEmployed | null
): SelfEmployed {
  if (!selfEmployed) return emptyIncomeStatementForm.entrepreneur.selfEmployed
  return {
    selected: true,
    attachments: selfEmployed.attachments,
    estimation: selfEmployed.estimatedIncome != null,
    ...mapEstimation(selfEmployed.estimatedIncome)
  }
}

function mapLimitedCompany(
  limitedCompany: ApiTypes.LimitedCompany | null
): LimitedCompany {
  if (!limitedCompany)
    return emptyIncomeStatementForm.entrepreneur.limitedCompany
  return {
    selected: true,
    incomeSource: limitedCompany.incomeSource
  }
}
