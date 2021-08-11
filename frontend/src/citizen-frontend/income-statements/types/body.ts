import LocalDate from 'lib-common/local-date'
import { stringToInt } from 'lib-common/utils/number'
import { UUID } from 'lib-common/types'
import { HighestFee, Income } from './income-statement'
import * as Form from './form'

export type HighestFeeBody = Omit<HighestFee, 'id'>

export interface IncomeBody extends Omit<Income, 'id' | 'attachments'> {
  attachmentIds: UUID[]
}

export type IncomeStatementBody = HighestFeeBody | IncomeBody

export function validateIncomeStatementBody(
  formData: Form.IncomeStatementForm
): IncomeStatementBody | null {
  const startDate = LocalDate.parseFiOrNull(formData.startDate)
  if (!startDate) return null

  if (formData.highestFee) {
    return {
      type: 'HIGHEST_FEE',
      startDate
    }
  }

  const gross = validateGross(formData.gross)
  const entrepreneur = validateEntrepreneur(formData.entrepreneur)

  if (gross === invalid || entrepreneur == invalid) {
    return null
  }

  return {
    type: 'INCOME' as const,
    startDate,
    gross,
    entrepreneur,
    otherInfo: formData.otherInfo,
    attachmentIds: formData.attachments.map((a) => a.id)
  }
}

const invalid: unique symbol = Symbol()

function validateGross(formData: Form.Gross) {
  if (!formData.selected) return null
  if (formData.incomeSource === null) return invalid
  return {
    incomeSource: formData.incomeSource,
    otherIncome: formData.otherIncome ?? []
  }
}

function validateEntrepreneur(formData: Form.Entrepreneur) {
  if (!formData.selected) return null
  const selfEmployed = validateSelfEmployed(formData.selfEmployed)
  const limitedCompany = validateLimitedCompany(formData.limitedCompany)
  const partnership = validatePartnership(formData.partnership)

  if (
    selfEmployed === invalid ||
    limitedCompany === invalid ||
    partnership === invalid
  ) {
    return invalid
  }

  return {
    selfEmployed,
    limitedCompany,
    partnership,
    startupGrant: formData.startupGrant
  }
}

function validateSelfEmployed(formData: Form.SelfEmployed) {
  if (!formData.selected) return null
  if (formData.estimation === null) return invalid
  if (formData.estimation === false) return { type: 'ATTACHMENTS' as const }

  const estimatedMonthlyIncome =
    stringToInt(formData.estimatedMonthlyIncome) ?? invalid
  const incomeStartDate =
    LocalDate.parseFiOrNull(formData.incomeStartDate) ?? invalid
  const incomeEndDate =
    formData.incomeEndDate != ''
      ? LocalDate.parseFiOrNull(formData.incomeEndDate) ?? invalid
      : null

  if (
    estimatedMonthlyIncome === invalid ||
    incomeStartDate === invalid ||
    incomeEndDate === invalid
  ) {
    return invalid
  }

  return {
    type: 'ESTIMATION' as const,
    estimatedMonthlyIncome,
    incomeStartDate,
    incomeEndDate
  }
}

function validateLimitedCompany(formData: Form.LimitedCompany) {
  if (!formData.selected) return null
  if (formData.incomeSource === null) return invalid
  return { incomeSource: formData.incomeSource }
}

function validatePartnership(formData: Form.Partnership) {
  if (!formData.selected) return false
  if (!formData.lookupConsent) return invalid
  return true
}
