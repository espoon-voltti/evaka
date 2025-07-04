// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export interface NamedPerson {
  firstName: string
  lastName: string
  preferredName?: string
  preferredFirstName?: string | null
}

export interface Placeholders {
  noFirstName: string
  noLastName: string
}

/** Format a person's name according to the specified format.
 *
 * If the `placeholders` parameter is given, it will be used to fill in empty first or last names.
 */
export function formatPersonName(
  person: NamedPerson,
  format:
    | 'First Last (Preferred)'
    | 'FirstFirst Last (Preferred)'
    | 'Last Preferred'
    | 'Preferred Last',
  placeholders?: Placeholders
): string
export function formatPersonName(
  person: Pick<NamedPerson, 'firstName' | 'lastName'>,
  format:
    | 'First Last'
    | 'FirstFirst Last'
    | 'Last First'
    | 'Last FirstFirst'
    | 'Last, First'
    | 'Last, FirstFirst',
  placeholders?: Placeholders
): string
export function formatPersonName(
  person: Pick<NamedPerson, 'firstName'>,
  format: 'First' | 'FirstFirst',
  placeholders?: Placeholders
): string
export function formatPersonName(
  person: Pick<NamedPerson, 'lastName'>,
  format: 'Last',
  placeholders?: Placeholders
): string
export function formatPersonName(
  person: Pick<
    NamedPerson,
    'firstName' | 'preferredName' | 'preferredFirstName'
  >,
  format: 'Preferred',
  placeholders?: Placeholders
): string
export function formatPersonName(
  person: Partial<NamedPerson>,
  format:
    | 'First Last (Preferred)'
    | 'FirstFirst Last (Preferred)'
    | 'First Last'
    | 'FirstFirst Last'
    | 'Last First'
    | 'Last FirstFirst'
    | 'Last, First'
    | 'Last, FirstFirst'
    | 'First'
    | 'FirstFirst'
    | 'Last'
    | 'Last Preferred'
    | 'Preferred Last'
    | 'Preferred',
  placeholders: Placeholders = { noFirstName: '', noLastName: '' }
): string {
  const { firstName, lastName, preferredName, preferredFirstName } = person
  const preferred = preferredName || preferredFirstName
  switch (format) {
    case 'First Last (Preferred)':
      return `${formatFirstName(firstName, placeholders)} ${formatLastName(lastName, placeholders)}${preferred ? ` (${preferred})` : ''}`
    case 'First Last':
      return `${formatFirstName(firstName, placeholders)} ${formatLastName(lastName, placeholders)}`
    case 'First':
      return formatFirstName(firstName, placeholders)
    case 'FirstFirst Last (Preferred)':
      return `${formatFirstFirstName(firstName, placeholders)} ${formatLastName(lastName, placeholders)}${preferred ? ` (${preferred})` : ''}`
    case 'FirstFirst Last':
      return `${formatFirstFirstName(firstName, placeholders)} ${formatLastName(lastName, placeholders)}`
    case 'FirstFirst':
      return formatFirstFirstName(firstName, placeholders)
    case 'Last First':
      return `${formatLastName(lastName, placeholders)} ${formatFirstName(firstName, placeholders)}`
    case 'Last FirstFirst':
      return `${formatLastName(lastName, placeholders)} ${formatFirstFirstName(firstName, placeholders)}`
    case 'Last Preferred':
      return `${formatLastName(lastName, placeholders)} ${formatPreferredName(preferred, firstName, placeholders)}`
    case 'Last, First':
      return `${formatLastName(lastName, placeholders)}, ${formatFirstName(firstName, placeholders)}`
    case 'Last, FirstFirst':
      return `${formatLastName(lastName, placeholders)}, ${formatFirstFirstName(firstName, placeholders)}`
    case 'Last':
      return formatLastName(lastName, placeholders)
    case 'Preferred Last':
      return `${formatPreferredName(preferred, firstName, placeholders)} ${formatLastName(lastName, placeholders)}`
    case 'Preferred':
      return formatPreferredName(preferred, firstName, placeholders)
  }
}

function formatFirstName(
  firstName: string | undefined,
  placeholders: Placeholders
): string {
  return firstName || placeholders.noFirstName
}

function formatLastName(
  lastName: string | undefined,
  placeholders: Placeholders
): string {
  return lastName || placeholders.noLastName
}

function formatFirstFirstName(
  firstName: string | undefined,
  placeholders: Placeholders
): string {
  if (!firstName) return placeholders.noFirstName

  const firstNames = firstName.split(/\s/)
  return firstNames.length > 0 ? firstNames[0] : placeholders.noFirstName
}

function formatPreferredName(
  preferredName: string | null | undefined,
  firstName: string | undefined,
  placeholders: Placeholders
): string {
  if (preferredName) return preferredName
  if (!firstName) return placeholders.noFirstName

  const firstNames = firstName.split(/\s/)
  return firstNames.length > 0 ? firstNames[0] : placeholders.noFirstName
}
