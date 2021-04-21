// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export function formatDecimal(decimal: number): string
export function formatDecimal(
  decimal: number | undefined | null
): string | undefined
export function formatDecimal(decimal?: number | null): string | undefined {
  return decimal?.toString().replace('.', ',')
}

export function round(num: number, scale = 0) {
  return Math.round(Math.pow(10, scale) * num) / Math.pow(10, scale)
}

export const formatPercentage = (value?: number) =>
  value !== undefined ? `${value.toFixed(1).replace('.', ',')} %` : undefined
