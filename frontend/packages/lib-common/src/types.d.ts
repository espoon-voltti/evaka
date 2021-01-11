// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// ABSTRACT TYPES

// Helper for Readonly types with depth until Typescript natively supports them
// See: https://github.com/microsoft/TypeScript/issues/13923
// eslint-disable-next-line @typescript-eslint/ban-types
export type primitive = string | number | boolean | undefined | null | Function
export type DeepReadonly<T> = T extends primitive ? T : DeepReadonlyObject<T>
export type DeepReadonlyObject<T> = {
  readonly [P in keyof T]: DeepReadonly<T[P]>
}

// CONCRETE TYPES

export interface BaseAppConfig {
  sentry: {
    dsn: string
    enabled: boolean
  }
}

export type UUID = string
export type Language = string
export type Nationality = string
export type ApplicationType = string
// FIXME: This makes no sense
export type ApplicationStatus = {
  CREATED: { value: 'CREATED' }
}

export interface ApplicationPerson {
  socialSecurityNumber: string | null
  firstName: string
  lastName: string
  language: Language
  nationality: Nationality
  dateOfBirth: string | null // TODO: Can non-nullability be ensured?
}

export interface ChildInfo extends ApplicationPerson {
  nationality: Nationality
  language: Language
  address: Address
  hasCorrectingAddress: boolean | null
  correctingAddress: Address
  childMovingDate: string | null
  restricted: boolean
}

export interface GuardianInfo extends ApplicationPerson {
  email: string
  phoneNumber: string
  address: Address
  hasCorrectingAddress: boolean | null
  guardianMovingDate: string | null
  correctingAddress: Address
  restricted: boolean
}

export interface Address {
  street: string
  postalCode: string
  city: string
  editable: boolean
}

export interface AdditionalDetails {
  otherInfo: string
}

export interface DaycareAdditionalDetails extends AdditionalDetails {
  dietType: string
  allergyType: string
}

export interface Apply {
  preferredUnits: string[]
  siblingBasis: boolean
  siblingName: string
  siblingSsn: string
}

export interface CareDetails {
  assistanceNeeded: boolean
  assistanceDescription: string
}

export interface ClubCareDetails extends CareDetails {
  assistanceAdditionalDetails: string
}

export interface PreschoolCareDetails extends CareDetails {
  preparatory: boolean
}

export interface FormModel {
  preferredStartDate: string
  extendedCare: boolean
  apply: Apply
  child: ChildInfo
  guardian: GuardianInfo
  hasSecondGuardian: boolean
  guardiansSeparated: boolean
  guardian2: GuardianInfo
  additionalDetails: AdditionalDetails
  docVersion: number
  type: ApplicationType
  status: ApplicationStatus
  hideFromGuardian: boolean
}

export interface DaycareForm extends FormModel {
  // moveToAnotherUnit: boolean
  hasOtherAdults: boolean
  otherAdults: ApplicationPerson[]
  hasOtherChildren: boolean
  otherChildren: ApplicationPerson[]
  serviceStart: string
  serviceEnd: string
  secondGuardianHasAgreed: boolean | null
}

export interface DaycareFormModel extends DaycareForm {
  urgent: boolean
  careDetails: CareDetails
  additionalDetails: DaycareAdditionalDetails
}

export interface PreschoolFormModel extends DaycareForm {
  connectedDaycare: boolean
  careDetails: PreschoolCareDetails
  additionalDetails: DaycareAdditionalDetails
}

export interface DaycareApplicationModel {
  id: string
  status: string
  origin: string
  form: DaycareFormModel
  dueDate: string
  sentDate: string
  createdDate: string
  modifiedDate: string
  placement: string
  childId: string
  guardianId: string
}

// TODO: ClubForm has never had a TS interface; this is made from a club applications JSON response.
export interface ClubFormModel extends FormModel {
  term: string
  wasOnDaycare: boolean
  wasOnClubCare: boolean
  careDetails: ClubCareDetails
  guardianInformed: boolean
  careFactor: number
}
