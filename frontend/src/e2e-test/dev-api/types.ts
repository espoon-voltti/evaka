// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'

export interface PersonDetail {
  id: string
  dateOfBirth: LocalDate
  dateOfDeath?: LocalDate
  firstName: string
  lastName: string
  preferredName?: string
  ssn?: string
  email?: string | null
  phone?: string
  language?: string
  residenceCode?: string
  streetAddress?: string
  postalCode?: string
  postOffice?: string
  nationalities?: string[]
  restrictedDetailsEnabled?: boolean
  restrictedDetailsEndDate?: LocalDate | null
  ophPersonOid?: string | null
  duplicateOf?: string | null
}

export interface PersonDetailWithDependants extends PersonDetail {
  dependants?: string[]
}

export interface Family {
  guardian: PersonDetailWithDependants
  otherGuardian?: PersonDetailWithDependants
  children: PersonDetailWithDependants[]
}
