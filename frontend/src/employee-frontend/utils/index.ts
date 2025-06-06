// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export function distinct<E>(array: E[]): E[] {
  return [...new Set(array)]
}

export function reducePropertySum<T>(
  rows: T[],
  propertySelector: (row: T) => number
): number {
  return rows.map(propertySelector).reduce((sum, value) => sum + value, 0)
}

export function formatPercent(amount?: number): string | undefined {
  if (amount === undefined || amount === null) {
    return undefined
  }

  return amount.toString().replace('.', ',')
}
