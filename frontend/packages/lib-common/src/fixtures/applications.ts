// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  ChildInfo,
  GuardianInfo,
  CareDetails,
  DaycareFormModel,
  Apply,
  AdditionalDetails,
  DaycareAdditionalDetails,
  PreschoolFormModel,
  PreschoolCareDetails,
  ClubFormModel,
  ClubCareDetails,
  Address
} from '@evaka/lib-common/src/types'
import {
  createGuardian,
  createChild,
  LANGUAGE,
  NATIONALITY,
  createPerson,
  APPLICATION_TYPE
} from '@evaka/lib-common/src/constants'

export const createValidChildBirthDate = (): string => '2019-05-05'
export const createValidChildSSN = (): string => '050519A6559'

export const createValidAddress = (): Address => ({
  city: 'test',
  street: 'test',
  postalCode: '00600',
  editable: true
})

export const createValidChildInfo = (
  dateOfBirth: string = createValidChildBirthDate(),
  socialSecurityNumber: string = createValidChildSSN()
): ChildInfo => ({
  ...createChild(),
  dateOfBirth: dateOfBirth,
  firstName: 'test',
  lastName: 'test',
  language: LANGUAGE._TEST,
  nationality: NATIONALITY._TEST,
  address: createValidAddress(),
  socialSecurityNumber: socialSecurityNumber
})

export const createValidGuardian = (): GuardianInfo => ({
  ...createGuardian(),
  dateOfBirth: '1990-10-20',
  firstName: 'test',
  lastName: 'test',
  phoneNumber: '345345345',
  email: 'email@address.org',
  address: createValidAddress(),
  socialSecurityNumber: '010399-963N'
})

export const createValidCareDetails = (): CareDetails => ({
  assistanceNeeded: true,
  assistanceDescription: 'Lapseni ei osaa kävellä!'
})

export const createValidClubCareDetails = (): ClubCareDetails => ({
  assistanceNeeded: true,
  assistanceDescription: 'Lapseni ei osaa kävellä!',
  assistanceAdditionalDetails: 'Lapseni ei osaa kävellä!'
})

export const createValidPreschoolCareDetails = (): PreschoolCareDetails => ({
  assistanceNeeded: true,
  assistanceDescription: 'Lapseni ei osaa kävellä!',
  preparatory: true
})

export const createValidApply = (): Apply => ({
  preferredUnits: ['1234-5678-9011'],
  siblingBasis: true,
  siblingName: 'Sisko',
  siblingSsn: 'Meikäläinen'
})

export const createValidAdditionalDetails = (): AdditionalDetails => ({
  otherInfo: 'Lapseni ei pidä ulkona olemisesta!'
})

export const createValidDaycareAdditionalDetails = (): DaycareAdditionalDetails => ({
  otherInfo: 'Lapseni ei pidä ulkona olemisesta!',
  dietType: 'Syö vain perunaa',
  allergyType: 'Pähkinä, Maitotuotteet'
})

export const createValidApplicationStatus = (): {
  CREATED: { value: 'CREATED' }
} => ({
  CREATED: { value: 'CREATED' }
})

export const createValidClubForm = (): ClubFormModel => ({
  term: '1234567890',
  wasOnClubCare: true,
  wasOnDaycare: true,
  guardianInformed: true,
  careFactor: 1.5,
  preferredStartDate: '2020-04-20',
  extendedCare: false,
  careDetails: createValidClubCareDetails(),
  apply: createValidApply(),
  child: createValidChildInfo(),
  guardian: createValidGuardian(),
  hasSecondGuardian: false,
  guardiansSeparated: false,
  guardian2: createValidGuardian(),
  additionalDetails: createValidAdditionalDetails(),
  docVersion: 0,
  type: APPLICATION_TYPE.CLUB,
  status: createValidApplicationStatus(),
  hideFromGuardian: false
})

export const createValidDaycareForm = (): DaycareFormModel => ({
  urgent: false,
  preferredStartDate: '2020-04-20',
  serviceStart: '08:00',
  serviceEnd: '14:00',
  extendedCare: false,
  careDetails: createValidCareDetails(),
  apply: createValidApply(),
  child: createValidChildInfo(),
  guardian: createValidGuardian(),
  hasSecondGuardian: false,
  guardiansSeparated: false,
  secondGuardianHasAgreed: true,
  guardian2: createValidGuardian(),
  hasOtherAdults: true,
  otherAdults: [createPerson()],
  hasOtherChildren: true,
  otherChildren: [createPerson()],
  additionalDetails: createValidDaycareAdditionalDetails(),
  docVersion: 0,
  type: APPLICATION_TYPE.DAYCARE,
  status: createValidApplicationStatus(),
  hideFromGuardian: false
})

export const createValidPreschoolForm = (): PreschoolFormModel => ({
  preferredStartDate: '2020-04-20',
  connectedDaycare: true,
  serviceStart: '08:00',
  serviceEnd: '14:00',
  extendedCare: false,
  careDetails: createValidPreschoolCareDetails(),
  apply: createValidApply(),
  child: createValidChildInfo(),
  guardian: createValidGuardian(),
  hasSecondGuardian: false,
  guardiansSeparated: false,
  secondGuardianHasAgreed: true,
  guardian2: createValidGuardian(),
  hasOtherAdults: true,
  otherAdults: [createPerson()],
  hasOtherChildren: true,
  otherChildren: [createPerson()],
  additionalDetails: createValidDaycareAdditionalDetails(),
  docVersion: 0,
  type: APPLICATION_TYPE.DAYCARE,
  status: createValidApplicationStatus(),
  hideFromGuardian: false
})
