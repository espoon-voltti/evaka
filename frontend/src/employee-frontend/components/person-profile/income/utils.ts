// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { formatCents } from 'lib-common/money'

import type { IncomeFields } from '../../../types/income'

import type { IncomeValueString } from './IncomeTable'

export type IncomeTableData = Partial<Record<string, IncomeValueString>>

export function tableDataFromIncomeFields(
  value: IncomeFields
): IncomeTableData {
  return Object.fromEntries(
    Object.entries(value).map(([key, value]) => [
      key,
      value
        ? {
            ...value,
            amount: formatCents(value.amount)
          }
        : {
            amount: '',
            coefficient: 'MONTHLY_NO_HOLIDAY_BONUS',
            monthlyAmount: 0
          }
    ])
  )
}
