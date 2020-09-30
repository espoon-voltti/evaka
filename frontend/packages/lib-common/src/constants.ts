// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  ApplicationPerson,
  ChildInfo,
  GuardianInfo,
  Address,
  Language,
  Nationality,
  ApplicationType,
  AdditionalDetails,
  Apply,
  CareDetails,
  PreschoolCareDetails,
  DaycareFormModel,
  PreschoolFormModel,
  DaycareAdditionalDetails
} from '@evaka/lib-common/src/types'

export const LANGUAGE: { [name: string]: Language } = {
  _TEST: 'test',
  FI: 'fi'
}

export const NATIONALITY: { [name: string]: Nationality } = {
  _TEST: 'test',
  FI: 'FI'
}

export const APPLICATION_TYPE: { [name: string]: ApplicationType } = {
  CLUB: 'club',
  DAYCARE: 'daycare',
  PRESCHOOL: 'preschool',
  FAMILY: 'family',
  CENTRE: 'centre',
  GROUP_FAMILY: 'group_family'
}

// Helper functions

export const createAddress = (): Address => ({
  street: '',
  postalCode: '',
  city: '',
  editable: true
})

export const createPerson = (): ApplicationPerson => ({
  dateOfBirth: '',
  firstName: '',
  language: LANGUAGE.FI,
  lastName: '',
  nationality: NATIONALITY.FI,
  socialSecurityNumber: ''
})

export const createChild = (): ChildInfo => ({
  ...createPerson(),
  address: {
    ...createAddress()
  },
  hasCorrectingAddress: false,
  childMovingDate: null,
  correctingAddress: {
    ...createAddress()
  },
  restricted: false
})

export const createGuardian = (): GuardianInfo => ({
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

export const TIMEZONE_HELSINKI = 'Europe/Helsinki'
export const DATE_FORMAT_TZ = 'YYYY-MM-DD'

export const createAdditionalDetails = (): AdditionalDetails => ({
  otherInfo: ''
})

export const createDaycareAdditionalDetails = (): DaycareAdditionalDetails => ({
  otherInfo: '',
  dietType: '',
  allergyType: ''
})

export const createApply = (): Apply => ({
  preferredUnits: [],
  siblingBasis: false,
  siblingName: '',
  siblingSsn: ''
})

export const createCareDetails = (): CareDetails => ({
  assistanceNeeded: false,
  assistanceDescription: ''
})

export const createPreschoolCareDetails = (): PreschoolCareDetails => ({
  assistanceNeeded: false,
  assistanceDescription: '',
  preparatory: false
})

export const createApplicationStatus = (): {
  CREATED: { value: 'CREATED' }
} => ({
  CREATED: { value: 'CREATED' }
})

export const createDaycareForm = (): DaycareFormModel => ({
  urgent: false,
  preferredStartDate: '',
  serviceStart: '',
  serviceEnd: '',
  extendedCare: false,
  careDetails: createCareDetails(),
  apply: createApply(),
  child: createChild(),
  guardian: createGuardian(),
  hasSecondGuardian: false,
  guardiansSeparated: false,
  secondGuardianHasAgreed: null,
  guardian2: createGuardian(),
  hasOtherAdults: false,
  otherAdults: [createPerson()],
  hasOtherChildren: false,
  otherChildren: [createPerson()],
  additionalDetails: createDaycareAdditionalDetails(),
  docVersion: 0,
  type: APPLICATION_TYPE.DAYCARE,
  status: createApplicationStatus(),
  hideFromGuardian: false
})

export const createPreschoolForm = (): PreschoolFormModel => ({
  preferredStartDate: '',
  serviceStart: '',
  serviceEnd: '',
  extendedCare: false,
  careDetails: createPreschoolCareDetails(),
  apply: createApply(),
  child: createChild(),
  guardian: createGuardian(),
  hasSecondGuardian: false,
  guardiansSeparated: false,
  secondGuardianHasAgreed: null,
  guardian2: createGuardian(),
  hasOtherAdults: false,
  otherAdults: [createPerson()],
  hasOtherChildren: false,
  otherChildren: [createPerson()],
  additionalDetails: createDaycareAdditionalDetails(),
  docVersion: 0,
  type: APPLICATION_TYPE.PRESCHOOL,
  connectedDaycare: false,
  status: createApplicationStatus(),
  hideFromGuardian: false
})
