// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export function formatCents(amount?: number): string | undefined {
  if (amount === undefined || amount === null) {
    return undefined
  }

  const euros = amount >= 0 ? Math.floor(amount / 100) : Math.ceil(amount / 100)
  const cents = Math.abs(amount % 100)

  return `${euros}${cents !== 0 ? `,${cents.toString().padStart(2, '0')}` : ''}`
}

export const eurosRegex = /^-?[0-9]+((,|\.){1}[0-9]{0,2})?$/

export function isValidCents(value: string): boolean {
  return eurosRegex.test(value)
}

export function parseCents(value: string): number | undefined {
  if (!isValidCents(value)) {
    return undefined
  }

  return Math.round(Number(value.replace(',', '.')) * 100)
}
