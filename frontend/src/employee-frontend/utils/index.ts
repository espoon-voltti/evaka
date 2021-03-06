// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { MutableRefObject } from 'react'
import { Translations } from 'lib-customizations/employee'

export const formatName = (
  maybeFirstName: string | null,
  maybeLastName: string | null,
  i18n: Translations,
  lastNameFirst = false
): string => {
  const firstName = maybeFirstName || i18n.common.noFirstName
  const lastName = maybeLastName || i18n.common.noLastName
  const formattedName =
    firstName && lastName
      ? lastNameFirst
        ? `${lastName} ${firstName}`
        : `${firstName} ${lastName}`
      : lastName
      ? lastName
      : firstName
  return formattedName
}

export const capitalizeFirstLetter = (str: string | null): string => {
  if (!str || str.length === 0) return ''
  if (str.length === 1) return str.toUpperCase()
  return str[0].toUpperCase() + str.slice(1)
}

export function distinct<E>(array: E[]): E[] {
  return [...new Set(array)]
}

export function reducePropertySum<T>(
  rows: T[],
  propertySelector: (row: T) => number
): number {
  return rows.map(propertySelector).reduce((sum, value) => sum + value, 0)
}

export function formatPercent(amount?: number): string | undefined {
  if (amount === undefined || amount === null) {
    return undefined
  }

  return amount.toString().replace('.', ',')
}

export function scrollToRef(ref: MutableRefObject<HTMLElement | null>) {
  window.setTimeout(() => {
    let offset = 0
    if (ref.current) {
      offset = getDocumentOffsetPosition(ref.current)
    }
    if (offset > 0) {
      window.scrollTo({
        top: offset,
        behavior: 'smooth'
      })
    }
  }, 100)
}

function getDocumentOffsetPosition(elem: HTMLElement): number {
  let elemTop = elem.offsetTop
  if (elem.offsetParent) {
    const parentOffsetTop = getDocumentOffsetPosition(
      elem.offsetParent as HTMLElement
    )
    elemTop = elemTop + parentOffsetTop
  }
  return elemTop
}
