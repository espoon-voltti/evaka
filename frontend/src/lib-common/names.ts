// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export type PersonNameFormat =
  | 'First Last'
  | 'FirstFirst Last'
  | 'FirstFirst'
  | 'Last First'
  | 'Last FirstFirst'
  | 'Last Preferred'
  | 'Last, First'
  | 'Last, FirstFirst'
  | 'Preferred Last'
  | 'Preferred'

export interface NamedPerson {
  firstName: string
  lastName: string
  preferredName?: string
}

export function formatPersonName(
  person: NamedPerson,
  format: PersonNameFormat
): string {
  const { firstName, lastName, preferredName } = person
  switch (format) {
    case 'First Last':
      return `${firstName} ${lastName}`
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
    case 'Last Preferred':
      return `${lastName} ${preferredName || firstName}`
    case 'Last, First':
      return `${lastName}, ${firstName}`
    case 'Last, FirstFirst': {
      const firstFirstName = firstName.split(/\s/)[0]
      return `${lastName}, ${firstFirstName}`
    }
    case 'Preferred Last':
      return `${preferredName || firstName} ${lastName}`
    case 'Preferred': {
      const firstFirstName = firstName.split(/\s/)[0]
      return preferredName || firstFirstName
    }
  }
}
