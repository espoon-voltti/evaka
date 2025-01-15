// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export function formatDecimal(decimal: number): string
export function formatDecimal(
  decimal: number | undefined | null
): string | undefined
export function formatDecimal(decimal?: number | null): string | undefined {
  if (decimal === null || decimal === undefined) return undefined
  return decimal.toString().replace('.', ',')
}

export function round(num: number, scale = 0) {
  return Math.round(Math.pow(10, scale) * num) / Math.pow(10, scale)
}

export const formatPercentage = (value: number | undefined | null) =>
  value != null ? `${value.toFixed(1).replace('.', ',')} %` : undefined

export const stringToNumber = (str: string): number | undefined => {
  const normalized = str.replace(',', '.')
  if (!/^\d+(\.\d+)?$/.test(normalized)) return undefined
  return parseFloat(normalized)
}

export const stringToInt = (str: string): number | undefined => {
  const value = stringToNumber(str)
  if (value !== undefined && value === round(value)) return value
  return undefined
}
