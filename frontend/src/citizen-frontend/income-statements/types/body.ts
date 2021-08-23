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

export function fromBody(
  formData: Form.IncomeStatementForm
): IncomeStatementBody | null {
  if (!formData.assure) return null

  const startDate = LocalDate.parseFiOrNull(formData.startDate)
  const endDate =
    formData.endDate == ''
      ? null
      : LocalDate.parseFiOrNull(formData.endDate) ?? invalid
  if (startDate === null || endDate === invalid) return null
  if (endDate && startDate > endDate) return null

  if (formData.highestFee) {
    return { type: 'HIGHEST_FEE', startDate, endDate }
  }

  const gross = validateGross(formData.gross)
  const entrepreneur = validateEntrepreneur(formData.entrepreneur)

  if (
    gross === invalid ||
    entrepreneur == invalid ||
    (!gross && !entrepreneur)
  ) {
    return null
  }

  return {
    type: 'INCOME' as const,
    startDate,
    endDate,
    gross,
    entrepreneur,
    student: formData.student,
    alimonyPayer: formData.alimonyPayer,
    otherInfo: formData.otherInfo,
    attachmentIds: formData.attachments.map((a) => a.id)
  }
}

const invalid: unique symbol = Symbol()

function validateGross(formData: Form.Gross) {
  if (!formData.selected) return null
  if (formData.incomeSource === null) return invalid

  const estimatedIncome =
    formData.estimatedMonthlyIncome ||
    formData.incomeStartDate ||
    formData.incomeEndDate
      ? validateEstimatedIncome(formData)
      : null

  if (estimatedIncome === invalid) return invalid

  return {
    incomeSource: formData.incomeSource,
    estimatedIncome,
    otherIncome: formData.otherIncome ?? []
  }
}

function validateEntrepreneur(formData: Form.Entrepreneur) {
  if (!formData.selected) return null

  const {
    fullTime,
    spouseWorksInCompany,
    startupGrant,
    partnership,
    lightEntrepreneur
  } = formData
  const startOfEntrepreneurship =
    LocalDate.parseFiOrNull(formData.startOfEntrepreneurship) ?? invalid

  const selfEmployed = validateSelfEmployed(formData.selfEmployed)
  const limitedCompany = validateLimitedCompany(formData.limitedCompany)

  if (
    fullTime === null ||
    startOfEntrepreneurship === invalid ||
    spouseWorksInCompany === null ||
    selfEmployed === invalid ||
    limitedCompany === invalid ||
    (!selfEmployed && !limitedCompany && !partnership && !lightEntrepreneur)
  ) {
    return invalid
  }

  const accountant =
    limitedCompany || partnership
      ? validateAccountant(formData.accountant)
      : null
  if (accountant === invalid) {
    return invalid
  }

  return {
    fullTime,
    startOfEntrepreneurship,
    spouseWorksInCompany,
    startupGrant,
    selfEmployed,
    limitedCompany,
    partnership,
    lightEntrepreneur,
    accountant
  }
}

function validateSelfEmployed(formData: Form.SelfEmployed) {
  if (!formData.selected) return null
  const estimation = formData.estimation
    ? validateEstimatedIncome(formData)
    : null

  if (estimation === invalid) return invalid

  return {
    attachments: formData.attachments,
    estimatedIncome: estimation
  }
}

function validateEstimatedIncome(formData: {
  estimatedMonthlyIncome: string
  incomeStartDate: string
  incomeEndDate: string
}) {
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

  if (incomeEndDate && incomeStartDate > incomeEndDate) {
    return invalid
  }

  return {
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

function validateAccountant(accountant: Form.Accountant) {
  const result = {
    name: accountant.name.trim(),
    address: accountant.address.trim(),
    phone: accountant.phone.trim(),
    email: accountant.email.trim()
  }
  if (!result.name || !result.phone || !result.email) return invalid
  return result
}
