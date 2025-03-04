// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as ApiTypes from 'lib-common/generated/api-types/incomestatement'
import {
  IncomeSource,
  IncomeStatementAttachmentType,
  OtherIncome
} from 'lib-common/generated/api-types/incomestatement'
import {
  IncomeStatementAttachments,
  toIncomeStatementAttachments
} from 'lib-common/income-statements/attachments'
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
  attachments: IncomeStatementAttachments
  assure: boolean
}

export interface Gross {
  selected: boolean
  incomeSource: IncomeSource | 'NO_INCOME' | null
  noIncomeDescription: string
  estimatedMonthlyIncome: string
  otherIncome: OtherIncome[]
  otherIncomeInfo: string
}

export interface Entrepreneur {
  selected: boolean | null
  fullTime: boolean | null
  startOfEntrepreneurship: LocalDate | null
  companyName: string
  businessId: string
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
    noIncomeDescription: '',
    estimatedMonthlyIncome: '',
    otherIncome: [],
    otherIncomeInfo: ''
  },
  entrepreneur: {
    selected: null,
    fullTime: null,
    startOfEntrepreneurship: null,
    companyName: '',
    businessId: '',
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
  attachments: { typed: true, attachmentsByType: {} },
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
          gross: mapGross(
            incomeStatement.gross,
            incomeStatement.entrepreneur !== null
          ),
          entrepreneur: mapEntrepreneur(incomeStatement.entrepreneur),
          student: incomeStatement.student,
          alimonyPayer: incomeStatement.alimonyPayer,
          otherInfo: incomeStatement.otherInfo,
          attachments: toIncomeStatementAttachments(incomeStatement.attachments)
        }
      : incomeStatement.type === 'CHILD_INCOME'
        ? {
            childIncome: true,
            otherInfo: incomeStatement.otherInfo,
            attachments: toIncomeStatementAttachments(
              incomeStatement.attachments
            )
          }
        : undefined)
  }
}

function mapGross(
  gross: ApiTypes.Gross | null,
  entrepreneurSelected: boolean
): Gross {
  if (!gross) {
    if (!entrepreneurSelected) {
      return emptyIncomeStatementForm.gross
    } else {
      // The user must select gross to be able to select entrepreneur. Previously
      // this was not the case, so for backwards compatibility we must select
      // gross if entrepreneur is selected.
      return { ...emptyIncomeStatementForm.gross, selected: true }
    }
  }
  if (gross.type === 'INCOME') {
    return {
      selected: true,
      incomeSource: gross.incomeSource,
      noIncomeDescription: '',
      estimatedMonthlyIncome: gross.estimatedMonthlyIncome?.toString() ?? '',
      otherIncome: gross.otherIncome,
      otherIncomeInfo: gross.otherIncomeInfo
    }
  } else {
    return {
      selected: true,
      incomeSource: 'NO_INCOME',
      noIncomeDescription: gross.noIncomeDescription,
      estimatedMonthlyIncome: '',
      otherIncome: [],
      otherIncomeInfo: ''
    }
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
    companyName: entrepreneur.companyName,
    businessId: entrepreneur.businessId,
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

export function computeRequiredAttachments(
  formData: IncomeStatementForm
): Set<IncomeStatementAttachmentType> {
  const result = new Set<IncomeStatementAttachmentType>()

  const { gross, entrepreneur, alimonyPayer, student } = formData
  if (gross.selected) {
    if (gross.incomeSource === 'ATTACHMENTS') result.add('PAYSLIP_GROSS')
    if (gross.otherIncome) gross.otherIncome.forEach((item) => result.add(item))
  }
  if (entrepreneur.selected) {
    if (entrepreneur.startupGrant) result.add('STARTUP_GRANT')
    if (
      entrepreneur.selfEmployed.selected &&
      !entrepreneur.selfEmployed.estimation
    ) {
      result.add('PROFIT_AND_LOSS_STATEMENT_SELF_EMPLOYED')
    }
    if (entrepreneur.limitedCompany.selected) {
      if (entrepreneur.limitedCompany.incomeSource === 'ATTACHMENTS') {
        result.add('PAYSLIP_LLC')
      }
      result.add('ACCOUNTANT_REPORT_LLC')
    }
    if (entrepreneur.partnership) {
      result
        .add('PROFIT_AND_LOSS_STATEMENT_PARTNERSHIP')
        .add('ACCOUNTANT_REPORT_PARTNERSHIP')
    }
    if (entrepreneur.lightEntrepreneur) {
      result.add('SALARY')
    }
  }
  if (gross.selected || entrepreneur.selected) {
    if (student) result.add('PROOF_OF_STUDIES')
    if (alimonyPayer) result.add('ALIMONY_PAYOUT')
  }

  return result
}
