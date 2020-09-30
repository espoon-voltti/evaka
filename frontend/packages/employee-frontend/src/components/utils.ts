// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export function formatDecimal(decimal: number): string
export function formatDecimal(decimal: number | undefined): string | undefined
export function formatDecimal(decimal?: number) {
  return decimal?.toString().replace('.', ',')
}

export function unique<T>(xs: T[]): T[] {
  return xs.filter((x, i) => xs.indexOf(x) === i)
}

export function round(num: number, scale = 0) {
  return Math.round(Math.pow(10, scale) * num) / Math.pow(10, scale)
}

export const formatPercentage = (value?: number) =>
  value !== undefined ? `${value.toFixed(1).replace('.', ',')} %` : undefined

export function textAreaRows(
  text: string,
  minRows = 1,
  maxRows = 15,
  lineLength = 62
): number {
  const lines = text.length / lineLength + (text.split(/\n/).length - 1)
  return Math.ceil(Math.max(minRows, Math.min(maxRows, lines)))
}
