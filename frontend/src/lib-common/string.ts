// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export const capitalizeFirstLetter = (
  str: string | null | undefined
): string => {
  if (!str) return ''
  if (str.length === 1) return str.toUpperCase()
  return str[0].toUpperCase() + str.slice(1)
}

export const toSimpleHash = (str: string) =>
  str
    .split('')
    .reduce(
      (hash, char) => char.charCodeAt(0) + (hash << 6) + (hash << 16) - hash,
      0
    ) >>> 0
