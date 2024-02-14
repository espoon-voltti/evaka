// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import DateRange from 'lib-common/date-range'
import {
  ApplicationForm,
  ApplicationOrigin,
  ApplicationStatus,
  ApplicationType
} from 'lib-common/generated/api-types/application'
import { Language } from 'lib-common/generated/api-types/daycare'
import { Coordinate, PilotFeature } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import TimeRange from 'lib-common/time-range'
import { UUID } from 'lib-common/types'

export interface Daycare {
  id: UUID
  areaId: UUID
  name: string
  type: ('CENTRE' | 'PRESCHOOL' | 'PREPARATORY_EDUCATION' | 'CLUB')[]
  dailyPreschoolTime: TimeRange | null
  dailyPreparatoryTime: TimeRange | null
  openingDate?: LocalDate | null
  closingDate?: LocalDate | null
  costCenter: string
  streetAddress: string
  postalCode: string
  postOffice: string
  decisionDaycareName: string
  decisionPreschoolName: string
  decisionHandler: string
  decisionHandlerAddress: string
  daycareApplyPeriod?: DateRange | null
  preschoolApplyPeriod?: DateRange | null
  clubApplyPeriod?: DateRange | null
  providerType:
    | 'MUNICIPAL'
    | 'MUNICIPAL_SCHOOL'
    | 'PURCHASED'
    | 'PRIVATE'
    | 'PRIVATE_SERVICE_VOUCHER'
    | 'EXTERNAL_PURCHASED'
  operationDays: number[]
  operationTimes: (TimeRange | null)[]
  roundTheClock: boolean
  language?: Language
  location?: Coordinate | null
  enabledPilotFeatures: PilotFeature[]
  invoicedByMunicipality?: boolean | null
  businessId: string
  iban: string
  providerId: string
}

export interface Child {
  id: UUID
  allergies?: string
  diet?: string
  medication?: string
}

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

export interface PersonDetailWithDependantsAndGuardians extends PersonDetail {
  dependants?: PersonDetailWithDependantsAndGuardians[]
  guardians?: PersonDetailWithDependantsAndGuardians[]
}

export interface Family {
  guardian: PersonDetailWithDependantsAndGuardians
  otherGuardian?: PersonDetailWithDependantsAndGuardians
  children: PersonDetailWithDependantsAndGuardians[]
}

export interface Application {
  id: UUID
  type: ApplicationType
  createdDate?: LocalDate
  modifiedDate?: LocalDate
  sentDate?: LocalDate
  dueDate?: LocalDate
  status: ApplicationStatus
  guardianId: UUID
  childId: UUID
  origin: ApplicationOrigin
  checkedByAdmin: boolean
  hideFromGuardian: boolean
  transferApplication: boolean
  otherGuardianId?: UUID
  form: ApplicationForm
}

export interface PlacementPlan {
  applicationId: UUID
  unitId: UUID
  periodStart: LocalDate
  periodEnd: LocalDate
  preschoolDaycarePeriodStart?: LocalDate | null
  preschoolDaycarePeriodEnd?: LocalDate | null
}

export interface VtjPerson {
  firstNames: string
  lastName: string
  socialSecurityNumber: string
  address: VtjPersonAddress | null
  dependants: VtjPerson[]
  guardians: VtjPerson[]
  nationalities: VtjNationality[]
  nativeLanguage: VtjNativeLanguage | null
  restrictedDetails: VtjRestrictedDetails | null
  dateOfDeath: LocalDate | null
  residenceCode: string | null
}

export interface VtjPersonAddress {
  streetAddress: string | null
  postalCode: string | null
  postOffice: string | null
  streetAddressSe: string | null
  postOfficeSe: string | null
}

export interface VtjNationality {
  countryName: string
  countryCode: string
}

export interface VtjNativeLanguage {
  languageName: string
  code: string
}

export interface VtjRestrictedDetails {
  enabled: boolean
  endDate: LocalDate | null
}
