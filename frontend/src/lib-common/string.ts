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
