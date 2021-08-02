export type IncomeSource = 'INCOMES_REGISTER' | 'ATTACHMENTS'

export const otherIncome = [
  'SHIFT_WORK_ADD_ON',
  'PERKS',
  'SECONDARY_INCOME',
  'PENSION',
  'UNEMPLOYMENT_BENEFITS',
  'SICKNESS_ALLOWANCE',
  'PARENTAL_ALLOWANCE',
  'HOME_CARE_ALLOWANCE',
  'ALIMONY',
  'OTHER_INCOME'
] as const

export type OtherIncome = typeof otherIncome[number]
