// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { useTranslations, type Translations } from '../i18n'

export type PersonNameFormat =
  | 'Last, First'
  | 'Last, Preferred'
  | 'Last First'
  | 'First Last'

function formatFirstName(firstName: string, i18n: Translations): string {
  return firstName || i18n.common.noFirstName
}

function formatLastName(lastName: string, i18n: Translations): string {
  return lastName || i18n.common.noLastName
}

function formatNickName(
  preferredName: string | undefined,
  firstName: string,
  i18n: Translations
): string {
  if (preferredName) return preferredName
  const firstNames = firstName.split(/\s/)
  return firstNames.length > 0 ? firstNames[0] : i18n.common.noFirstName
}

interface PersonNameProps {
  person: {
    firstName: string
    lastName: string
    preferredName?: string
  }
  format: PersonNameFormat
}

function formatName(
  { person: { firstName, lastName, preferredName }, format }: PersonNameProps,
  i18n: Translations
): string {
  switch (format) {
    case 'Last, First':
      return `${formatLastName(lastName, i18n)}, ${formatFirstName(firstName, i18n)}`
    case 'Last, Preferred':
      return `${formatLastName(lastName, i18n)}, ${formatNickName(
        preferredName,
        firstName,
        i18n
      )}`
    case 'First Last':
      return `${formatFirstName(firstName, i18n)} ${formatLastName(lastName, i18n)}`
    case 'Last First':
      return `${formatLastName(lastName, i18n)} ${formatFirstName(firstName, i18n)}`
  }
}

export function PersonName(props: PersonNameProps) {
  const i18n = useTranslations()
  const formattedName = formatName(props, i18n)
  return <span translate="no">{formattedName}</span>
}
