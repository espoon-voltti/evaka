// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ApplicationType } from '@/types'
import { APPLICATION_TYPE } from '@/constants'

export interface PersonType {
  firstName: string
  lastName: string
  socialSecurityNumber: string
}

export const createPerson = (): PersonType => ({
  firstName: '',
  lastName: '',
  socialSecurityNumber: ''
})

export interface ChildInfoType extends PersonType {
  nationality: string
  language: string
  address: AddressType
  hasCorrectingAddress: boolean
  correctingAddress: AddressType
  restricted: boolean
}

export const createChild = (): ChildInfoType => ({
  ...createPerson(),
  nationality: '',
  language: '',
  address: {
    ...createAddress()
  },
  hasCorrectingAddress: false,
  correctingAddress: {
    ...createAddress()
  },
  restricted: false
})

export interface GuardianInfoType extends PersonType {
  email: string
  phoneNumber: string
  address: AddressType
  hasCorrectingAddress: boolean | null
  guardianMovingDate: string | null
  correctingAddress: AddressType
  restricted: boolean
}

export const createGuardian = (): GuardianInfoType => ({
  ...createPerson(),
  email: '',
  phoneNumber: '',
  address: {
    ...createAddress()
  },
  hasCorrectingAddress: false,
  guardianMovingDate: null,
  correctingAddress: {
    ...createAddress()
  },
  restricted: false
})

export interface AddressType {
  street: string
  postalCode: string
  city: string
}

export const createAddress = (): AddressType => ({
  street: '',
  postalCode: '',
  city: ''
})

export interface AdditionalDetails {
  otherInfo: string
  dietType: string
  allergyType: string
}

export const createAdditionalDetails = (): AdditionalDetails => ({
  otherInfo: '',
  dietType: '',
  allergyType: ''
})

export interface Apply {
  preferredUnits: string[]
  siblingBasis: boolean
  siblingName: string
  siblingSsn: string
}

export const createApply = (): Apply => ({
  preferredUnits: [],
  siblingBasis: false,
  siblingName: '',
  siblingSsn: ''
})

export interface CareDetails {
  assistanceNeeded: boolean
  assistanceDescription: string
}

export interface PreschoolCareDetails extends CareDetails {
  preparatory: boolean
}

export const createCareDetails = (): CareDetails => ({
  assistanceNeeded: false,
  assistanceDescription: ''
})

export const createPreschoolCareDetails = (): PreschoolCareDetails => ({
  assistanceNeeded: false,
  assistanceDescription: '',
  preparatory: false
})

export const createApplicationStatus = (): {} => ({
  CREATED: { value: 'CREATED' }
})

export interface DaycareFormModel {
  // moveToAnotherUnit: boolean
  partTime: boolean
  urgent: boolean
  preferredStartDate: string
  serviceStart: string
  serviceEnd: string
  extendedCare: boolean
  careDetails: CareDetails
  apply: Apply
  child: ChildInfoType
  guardian: GuardianInfoType
  hasOtherVtjGuardian: boolean
  otherVtjGuardianHasSameAddress: boolean
  hasSecondGuardian: boolean
  guardiansSeparated: boolean
  secondGuardianHasAgreed: boolean | null
  guardian2: GuardianInfoType
  hasOtherAdults: boolean
  otherAdults: PersonType[]
  hasOtherChildren: boolean
  otherChildren: PersonType[]
  additionalDetails: AdditionalDetails
  docVersion: number
  type: ApplicationType
  status: {}
  maxFeeAccepted: boolean
}

export interface PreschoolFormModel {
  // moveToAnotherUnit: boolean
  // urgent: boolean
  preferredStartDate: string
  serviceStart: string
  serviceEnd: string
  extendedCare: boolean
  careDetails: PreschoolCareDetails
  apply: Apply
  child: ChildInfoType
  guardian: GuardianInfoType
  hasOtherVtjGuardian: boolean
  otherVtjGuardianHasSameAddress: boolean
  hasSecondGuardian: boolean
  guardiansSeparated: boolean
  secondGuardianHasAgreed: boolean | null
  guardian2: GuardianInfoType
  hasOtherAdults: boolean
  otherAdults: PersonType[]
  hasOtherChildren: boolean
  otherChildren: PersonType[]
  additionalDetails: AdditionalDetails
  docVersion: number
  type: ApplicationType
  connectedDaycare: boolean
  status: {}
  maxFeeAccepted: boolean
}

export const createDaycareForm = (): DaycareFormModel => ({
  // moveToAnotherUnit: false,
  partTime: false,
  urgent: false,
  preferredStartDate: '',
  serviceStart: '',
  serviceEnd: '',
  extendedCare: false,
  careDetails: createCareDetails(),
  apply: createApply(),
  child: createChild(),
  guardian: createGuardian(),
  hasOtherVtjGuardian: false,
  otherVtjGuardianHasSameAddress: false,
  hasSecondGuardian: false,
  guardiansSeparated: false,
  secondGuardianHasAgreed: null,
  guardian2: createGuardian(),
  hasOtherAdults: false,
  otherAdults: [createPerson()],
  hasOtherChildren: false,
  otherChildren: [createPerson()],
  additionalDetails: createAdditionalDetails(),
  docVersion: 0,
  type: APPLICATION_TYPE.DAYCARE,
  status: createApplicationStatus(),
  maxFeeAccepted: false
})

export const createPreschoolForm = (): PreschoolFormModel => ({
  // moveToAnotherUnit: false,
  // urgent: false,
  preferredStartDate: '',
  serviceStart: '',
  serviceEnd: '',
  extendedCare: false,
  careDetails: createPreschoolCareDetails(),
  apply: createApply(),
  child: createChild(),
  guardian: createGuardian(),
  hasOtherVtjGuardian: false,
  otherVtjGuardianHasSameAddress: false,
  hasSecondGuardian: false,
  guardiansSeparated: false,
  secondGuardianHasAgreed: null,
  guardian2: createGuardian(),
  hasOtherAdults: false,
  otherAdults: [createPerson()],
  hasOtherChildren: false,
  otherChildren: [createPerson()],
  additionalDetails: createAdditionalDetails(),
  docVersion: 0,
  type: APPLICATION_TYPE.PRESCHOOL,
  connectedDaycare: false,
  status: createApplicationStatus(),
  maxFeeAccepted: false
})

export interface SectionName {
  service: string
  preferredUnits: string
  personalInfo: string
  payment: string
  additional: string
}

export const DAYCARE_SECTION: SectionName = {
  service: 'service',
  preferredUnits: 'preferredUnits',
  personalInfo: 'personalInfo',
  payment: 'payment',
  additional: 'additional'
}

export interface DayCareSectionErrors {
  service: string[]
  preferredUnits: string[]
  personalInfo: string[]
  payment: string[]
  additional: string[]
}

export const createDaycareSectionError = (): DayCareSectionErrors => ({
  service: [],
  preferredUnits: [],
  personalInfo: [],
  payment: [],
  additional: []
})
