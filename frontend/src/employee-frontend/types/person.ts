// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from '../types'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'

export interface PersonIdentity {
  id: UUID
  socialSecurityNumber: string | null
  customerId: number | null
}

export interface PersonDetails {
  id: UUID
  socialSecurityNumber: string | null
  firstName: string | null
  lastName: string | null
  email: string | null
  phone: string | null
  backupPhone: string | null
  language: string | null
  dateOfBirth: LocalDate
  dateOfDeath: LocalDate | null
  streetAddress: string | null
  postOffice: string | null
  postalCode: string | null
  residenceCode: string | null
  restrictedDetailsEnabled: boolean
  invoiceRecipientName: string
  invoicingStreetAddress: string
  invoicingPostalCode: string
  invoicingPostOffice: string
  forceManualFeeDecisions: boolean
}

export const deserializePersonDetails = (
  data: JsonOf<PersonDetails>
): PersonDetails => ({
  ...data,
  dateOfBirth: LocalDate.parseIso(data.dateOfBirth),
  dateOfDeath: LocalDate.parseNullableIso(data.dateOfDeath)
})

export interface DependantAddress {
  origin: string
  streetAddress: string
  postalCode: string
  city: string
}

export interface Nationality {
  countyName: string
  countryCode: string
}

export interface Language {
  languageName: string
  code: string
}

export interface RestrictedDetails {
  enabled: boolean
  endDate: LocalDate | null
}

interface PersonDependantChild {
  id: UUID
  socialSecurityNumber: string
  dateOfBirth: LocalDate
  firstName: string
  lastName: string
  addresses: DependantAddress[]
  nationalities: Nationality[]
  nativeLanguage: Language | null
  restrictedDetails: RestrictedDetails
  source: string
}

export interface PersonWithChildren extends PersonDependantChild {
  children: PersonDependantChild[]
}

export interface PersonContactInfo {
  email: string | null
  phone: string | null
}

export type SearchColumn =
  | 'last_name,first_name'
  | 'date_of_birth'
  | 'street_address'
  | 'social_security_number'
