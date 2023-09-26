// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { IncomeCoefficient } from '../generated/api-types/invoicing'

// todo: use @ConstList once NOT_AVAILABLE has been removed from backend model
export const incomeEffects = [
  'MAX_FEE_ACCEPTED',
  'INCOMPLETE',
  'INCOME'
] as const

export type IncomeEffect = (typeof incomeEffects)[number]

export type IncomeValue = {
  amount: number
  coefficient: IncomeCoefficient
  monthlyAmount: number
}
