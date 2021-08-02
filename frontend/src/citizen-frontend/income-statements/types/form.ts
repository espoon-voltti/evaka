import { IncomeSource, OtherIncome } from './common'
import { IncomeStatementBody } from './income-statement'
import LocalDate from '../../../lib-common/local-date'
import { stringToInt } from '../../../lib-common/utils/number'

interface Base {
  startDate: string
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

  switch (formData.incomeType) {
    case 'HIGHEST_FEE':
      return {
        incomeType: 'HIGHEST_FEE',
        startDate
      }
    case 'GROSS':
      if (!formData.incomeSource) return null
      return {
        incomeType: 'GROSS',
        startDate,
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
        incomeType: 'ENTREPRENEUR_SELF_EMPLOYED_ESTIMATION',
        startDate,
        estimatedMonthlyIncome,
        incomeStartDate,
        incomeEndDate: incomeEndDate === '' ? null : incomeEndDate
      }
    }
    case 'ENTREPRENEUR_LIMITED_COMPANY': {
      if (!formData.incomeSource) return null
      return {
        incomeType: 'ENTREPRENEUR_LIMITED_COMPANY',
        startDate,
        incomeSource: formData.incomeSource
      }
    }
    default:
      return null
  }
}
