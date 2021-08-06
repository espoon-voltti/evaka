import { IncomeSource, OtherIncome } from './common'
import { IncomeStatementBody } from './income-statement'
import LocalDate from 'lib-common/local-date'
import { stringToInt } from 'lib-common/utils/number'
import { Attachment } from 'lib-common/api-types/attachment'

interface Base {
  startDate: string
  attachments: Attachment[]
}

export interface Empty extends Base {
  incomeType: null
}

export interface HighestFee extends Base {
  incomeType: 'HIGHEST_FEE'
}

export interface Gross extends Base {
  incomeType: 'GROSS'
  incomeSource: IncomeSource | null
  otherIncome: OtherIncome[] | null
}

export interface Entrepreneur extends Base {
  incomeType:
    | 'ENTREPRENEUR_EMPTY'
    | 'ENTREPRENEUR_SELF_EMPLOYED_EMPTY'
    | 'ENTREPRENEUR_SELF_EMPLOYED_ESTIMATION'
    | 'ENTREPRENEUR_SELF_EMPLOYED_ATTACHMENTS'
    | 'ENTREPRENEUR_LIMITED_COMPANY'
    | 'ENTREPRENEUR_PARTNERSHIP'
  estimatedMonthlyIncome: string
  incomeStartDate: string
  incomeEndDate: string
  incomeSource: IncomeSource | null
}

export type IncomeStatementForm = Empty | HighestFee | Gross | Entrepreneur

export function isEntrepreneur(
  formData: IncomeStatementForm
): formData is Entrepreneur {
  return (
    formData.incomeType !== null &&
    formData.incomeType.startsWith('ENTREPRENEUR_')
  )
}

export function toIncomeStatementBody(
  formData: IncomeStatementForm
): IncomeStatementBody | null {
  const startDate = LocalDate.parseFiOrNull(formData.startDate)
  if (!startDate) return null

  const base = {
    startDate,
    attachmentIds: formData.attachments.map((a) => a.id)
  }

  switch (formData.incomeType) {
    case 'HIGHEST_FEE':
      return {
        ...base,
        incomeType: 'HIGHEST_FEE'
      }
    case 'GROSS':
      if (!formData.incomeSource) return null
      return {
        ...base,
        incomeType: 'GROSS',
        incomeSource: formData.incomeSource,
        otherIncome: formData.otherIncome === null ? [] : formData.otherIncome
      }
    case 'ENTREPRENEUR_SELF_EMPLOYED_ESTIMATION': {
      const estimatedMonthlyIncome = stringToInt(
        formData.estimatedMonthlyIncome
      )
      const incomeStartDate = LocalDate.parseFiOrNull(formData.incomeStartDate)
      const incomeEndDate =
        formData.incomeEndDate.trim() === ''
          ? ('' as const)
          : LocalDate.parseFiOrNull(formData.incomeEndDate)

      if (
        !estimatedMonthlyIncome ||
        !incomeStartDate ||
        incomeEndDate === null
      ) {
        return null
      }

      return {
        ...base,
        incomeType: 'ENTREPRENEUR_SELF_EMPLOYED_ESTIMATION',
        estimatedMonthlyIncome,
        incomeStartDate,
        incomeEndDate: incomeEndDate === '' ? null : incomeEndDate
      }
    }
    case 'ENTREPRENEUR_LIMITED_COMPANY': {
      if (!formData.incomeSource) return null
      return {
        ...base,
        incomeType: 'ENTREPRENEUR_LIMITED_COMPANY',
        incomeSource: formData.incomeSource
      }
    }
    case 'ENTREPRENEUR_SELF_EMPLOYED_ATTACHMENTS':
    case 'ENTREPRENEUR_PARTNERSHIP':
      return { ...base, incomeType: formData.incomeType }
    default:
      return null
  }
}
