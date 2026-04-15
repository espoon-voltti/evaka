// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export interface Ordered<T> {
  isBefore(other: T): boolean
  isEqualOrBefore(other: T): boolean
  isEqual(other: T): boolean
  isEqualOrAfter(other: T): boolean
  isAfter(other: T): boolean
}

export function maxOf<T extends Ordered<T>>(a: T, b: T): T {
  return b.isAfter(a) ? b : a
}

export function minOf<T extends Ordered<T>>(a: T, b: T): T {
  return b.isBefore(a) ? b : a
}
