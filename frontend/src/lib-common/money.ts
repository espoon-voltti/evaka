// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export function formatCents(amount: number, fixedCents?: boolean): string
export function formatCents(
  amount: number | undefined,
  fixedCents?: boolean
): string | undefined
export function formatCents(
  amount: number | undefined,
  fixedCents?: boolean
): string | undefined {
  if (amount === undefined || amount === null) {
    return undefined
  }

  const euros = Math.floor(Math.abs(amount) / 100)
  const cents = Math.abs(amount % 100)
  const sign = amount < 0 ? '-' : ''

  if (cents !== 0 || fixedCents) {
    return `${sign}${euros},${cents.toString().padStart(2, '0')}`
  } else {
    return `${sign}${euros}`
  }
}

export const eurosRegex = /^-?[0-9]+(([,.])[0-9]{0,2})?$/

export function isValidCents(value: string): boolean {
  return eurosRegex.test(value)
}

export function parseCents(value: string): number | undefined {
  if (!isValidCents(value)) {
    return undefined
  }

  return Math.round(Number(value.replace(',', '.')) * 100)
}

export function parseCentsOrThrow(value: string): number {
  if (!isValidCents(value)) {
    throw Error(`Invalid cents value: ${value}`)
  }

  return Math.round(Number(value.replace(',', '.')) * 100)
}
