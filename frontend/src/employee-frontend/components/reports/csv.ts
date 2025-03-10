// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export type CsvValue = string | number | null | undefined

const separator = ';'
const quote = '"'

export type ColumnCommon = { label: string; exclude?: boolean }

export interface Column<T> {
  label: string
  value: (row: T) => CsvValue
  exclude?: boolean
}

export function toCsv<T>(data: T[], columns: Column<T>[]) {
  const headerRow = columns
    .filter((col) => !col.exclude)
    .map((col) => escape(col.label))
    .join(separator)
  const dataRows = data.map((row) =>
    columns
      .filter((col) => !col.exclude)
      .map((col) => escape(str(col.value(row))))
      .join(separator)
  )
  return [headerRow, ...dataRows].join('\n') + '\n'
}

function str(value: CsvValue): string {
  return value?.toString() ?? ''
}

function escape(value: string) {
  if (
    value.includes(separator) ||
    value.includes(quote) ||
    value.includes('\n')
  ) {
    return quote + value.replaceAll(quote, quote + quote) + quote
  }
  return value
}
