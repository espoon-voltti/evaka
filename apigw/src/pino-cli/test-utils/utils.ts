// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

/**
 * Produces a mutable deep copy of the original object
 */
// eslint-disable-next-line @typescript-eslint/no-explicit-any
export const deepCopyObj = (obj: any): any => {
  return JSON.parse(JSON.stringify(obj))
}
