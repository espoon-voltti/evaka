// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export type PersonNameFormat =
  | 'First Last (Preferred)'
  | 'First Last'
  | 'First'
  | 'FirstFirst Last (Preferred)'
  | 'FirstFirst Last'
  | 'FirstFirst'
  | 'Last First'
  | 'Last FirstFirst'
  | 'Last Preferred'
  | 'Last, First'
  | 'Last, FirstFirst'
  | 'Last'
  | 'Preferred Last'
  | 'Preferred'

export interface NamedPerson {
  firstName: string
  lastName: string
  preferredName?: string
  preferredFirstName?: string | null
}

export function formatPersonName(
  person: NamedPerson,
  format: PersonNameFormat
): string {
  const { firstName, lastName, preferredName, preferredFirstName } = person
  const preferred = preferredName || preferredFirstName
  switch (format) {
    case 'First Last (Preferred)': {
      return `${firstName} ${lastName}${preferred ? ` (${preferred})` : ''}`
    }
    case 'First Last':
      return `${firstName} ${lastName}`
    case 'First':
      return firstName
    case 'FirstFirst Last (Preferred)': {
      const firstFirstName = firstName.split(/\s/)[0]
      return `${firstFirstName} ${lastName}${preferred ? ` (${preferred})` : ''}`
    }
    case 'FirstFirst Last': {
      const firstFirstName = firstName.split(/\s/)[0]
      return `${firstFirstName} ${lastName}`
    }
    case 'FirstFirst':
      return firstName.split(/\s/)[0]
    case 'Last First':
      return `${lastName} ${firstName}`
    case 'Last FirstFirst': {
      const firstFirstName = firstName.split(/\s/)[0]
      return `${lastName} ${firstFirstName}`
    }
    case 'Last Preferred': {
      return `${lastName} ${preferred || firstName.split(/\s/)[0]}`
    }
    case 'Last, First':
      return `${lastName}, ${firstName}`
    case 'Last, FirstFirst': {
      const firstFirstName = firstName.split(/\s/)[0]
      return `${lastName}, ${firstFirstName}`
    }
    case 'Last':
      return lastName
    case 'Preferred Last': {
      return `${preferred || firstName.split(/\s/)[0]} ${lastName}`
    }
    case 'Preferred': {
      return preferred || firstName.split(/\s/)[0]
    }
  }
}
