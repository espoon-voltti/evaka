// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import type { NamedPerson } from 'lib-common/names'
import { formatPersonName } from 'lib-common/names'

import { useTranslations } from '../i18n'

type PersonNameProps =
  | {
      person: NamedPerson
      format:
        | 'First Last (Preferred)'
        | 'FirstFirst Last (Preferred)'
        | 'Last Preferred'
        | 'Preferred Last'
    }
  | {
      person: Pick<NamedPerson, 'firstName' | 'lastName'>
      format:
        | 'First Last'
        | 'FirstFirst Last'
        | 'Last First'
        | 'Last FirstFirst'
        | 'Last, First'
        | 'Last, FirstFirst'
    }
  | { person: Pick<NamedPerson, 'firstName'>; format: 'First' | 'FirstFirst' }
  | { person: Pick<NamedPerson, 'lastName'>; format: 'Last' }
  | {
      person: Pick<
        NamedPerson,
        'firstName' | 'preferredName' | 'preferredFirstName'
      >
      format: 'Preferred'
    }

export function PersonName(props: PersonNameProps) {
  const i18n = useTranslations()
  const { person, format } = props

  // The switch/case is required because TypeScript's type narrowing is not smart enough. This causes a type error:
  // const formattedName = formatPersonName(person, format, i18n.common)

  let formattedName: string
  switch (format) {
    case 'First Last (Preferred)':
    case 'FirstFirst Last (Preferred)':
    case 'Last Preferred':
    case 'Preferred Last':
      formattedName = formatPersonName(person, format, i18n.common)
      break
    case 'First Last':
    case 'FirstFirst Last':
    case 'Last First':
    case 'Last FirstFirst':
    case 'Last, First':
    case 'Last, FirstFirst':
      formattedName = formatPersonName(person, format, i18n.common)
      break
    case 'First':
    case 'FirstFirst':
      formattedName = formatPersonName(person, format, i18n.common)
      break
    case 'Last':
      formattedName = formatPersonName(person, format, i18n.common)
      break
    case 'Preferred':
      formattedName = formatPersonName(person, format, i18n.common)
      break
  }

  return <span translate="no">{formattedName}</span>
}
