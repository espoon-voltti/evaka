// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import type { NamedPerson, PersonNameFormat } from 'lib-common/names'

import { useTranslations, type Translations } from '../i18n'

function formatFirstName(
  firstName: string | undefined,
  i18n: Translations
): string {
  return firstName || i18n.common.noFirstName
}

function formatLastName(
  lastName: string | undefined,
  i18n: Translations
): string {
  return lastName || i18n.common.noLastName
}

function formatFirstFirstName(
  firstName: string | undefined,
  i18n: Translations
): string {
  if (!firstName) return i18n.common.noFirstName

  const firstNames = firstName.split(/\s/)
  return firstNames.length > 0 ? firstNames[0] : i18n.common.noFirstName
}

function formatNickName(
  preferredName: string | undefined,
  firstName: string | undefined,
  i18n: Translations
): string {
  if (preferredName) return preferredName
  if (!firstName) return i18n.common.noFirstName

  const firstNames = firstName.split(/\s/)
  return firstNames.length > 0 ? firstNames[0] : i18n.common.noFirstName
}

export function usePersonName(
  person: NamedPerson | undefined,
  format: PersonNameFormat
): string {
  const i18n = useTranslations()
  const { firstName, lastName, preferredName } = person ?? {}

  switch (format) {
    case 'First Last':
      return `${formatFirstName(firstName, i18n)} ${formatLastName(lastName, i18n)}`
    case 'First':
      return formatFirstName(firstName, i18n)
    case 'FirstFirst Last':
      return `${formatFirstFirstName(firstName, i18n)} ${formatLastName(lastName, i18n)}`
    case 'FirstFirst':
      return formatFirstFirstName(firstName, i18n)
    case 'Last First':
      return `${formatLastName(lastName, i18n)} ${formatFirstName(firstName, i18n)}`
    case 'Last FirstFirst':
      return `${formatLastName(lastName, i18n)} ${formatFirstFirstName(firstName, i18n)}`
    case 'Last Preferred':
      return `${formatLastName(lastName, i18n)} ${formatNickName(preferredName, firstName, i18n)}`
    case 'Last, First':
      return `${formatLastName(lastName, i18n)}, ${formatFirstName(firstName, i18n)}`
    case 'Last, FirstFirst':
      return `${formatLastName(lastName, i18n)}, ${formatFirstFirstName(firstName, i18n)}`
    case 'Last':
      return formatLastName(lastName, i18n)
    case 'Preferred Last':
      return `${formatNickName(preferredName, firstName, i18n)} ${formatLastName(lastName, i18n)}`
    case 'Preferred':
      return formatNickName(preferredName, firstName, i18n)
  }
}

interface PersonNameProps {
  person: NamedPerson
  format: PersonNameFormat
}

export function PersonName(props: PersonNameProps) {
  const { person, format } = props
  const formattedName = usePersonName(person, format)
  return <span translate="no">{formattedName}</span>
}
