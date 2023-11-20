// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Translations } from 'lib-customizations/employee'

export const formatName = (
  maybeFirstName: string | null,
  maybeLastName: string | null,
  i18n: Translations,
  lastNameFirst = false
): string => {
  const firstName = maybeFirstName || i18n.common.noFirstName
  const lastName = maybeLastName || i18n.common.noLastName
  return firstName && lastName
    ? lastNameFirst
      ? `${lastName} ${firstName}`
      : `${firstName} ${lastName}`
    : lastName
      ? lastName
      : firstName
}

export const formatPersonName = (
  person: { firstName: string | null; lastName: string | null },
  i18n: Translations,
  lastNameFirst = false
): string => formatName(person.firstName, person.lastName, i18n, lastNameFirst)

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
