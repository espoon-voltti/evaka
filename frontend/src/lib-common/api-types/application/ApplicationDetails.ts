// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { JsonOf } from '../../json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { ServiceNeedOption } from 'lib-common/generated/api-types/application'
import { Attachment } from '../attachment'
import {
  ApplicationOrigin,
  ApplicationStatus,
  ApplicationType,
  AttachmentType,
  OtherGuardianAgreementStatus
} from 'lib-common/generated/enums'

export interface GuardianApplications {
  childId: string
  childName: string
  applicationSummaries: ApplicationSummary[]
}

export interface ApplicationSummary {
  applicationId: string
  type: string
  childId: string
  childName: string | null
  preferredUnitName: string | null
  allPreferredUnitNames: string[]
  applicationStatus: ApplicationStatus
  startDate: LocalDate | null
  sentDate: LocalDate | null
  createdDate: Date
  modifiedDate: Date
}

export interface ApplicationDetails {
  id: string
  type: ApplicationType
  form: ApplicationForm
  status: ApplicationStatus
  origin: ApplicationOrigin
  childId: string
  guardianId: string
  otherGuardianId: string | null
  otherGuardianLivesInSameAddress: boolean | null
  childRestricted: boolean
  guardianRestricted: boolean
  guardianDateOfDeath: LocalDate | null
  checkedByAdmin: boolean
  createdDate: Date | null
  modifiedDate: Date | null
  sentDate: LocalDate | null
  dueDate: LocalDate | null
  transferApplication: boolean
  additionalDaycareApplication: boolean
  hideFromGuardian: boolean
  attachments: ApplicationAttachment[]
}

export const deserializeApplicationDetails = (
  json: JsonOf<ApplicationDetails>
): ApplicationDetails => ({
  ...json,
  form: {
    ...json.form,
    child: {
      ...json.form.child,
      dateOfBirth: LocalDate.parseNullableIso(json.form.child.dateOfBirth),
      futureAddress: json.form.child.futureAddress
        ? {
            ...json.form.child.futureAddress,
            movingDate: LocalDate.parseNullableIso(
              json.form.child.futureAddress.movingDate
            )
          }
        : null
    },
    guardian: {
      ...json.form.guardian,
      futureAddress: json.form.guardian.futureAddress
        ? {
            ...json.form.guardian.futureAddress,
            movingDate: LocalDate.parseNullableIso(
              json.form.guardian.futureAddress.movingDate
            )
          }
        : null
    },
    preferences: {
      ...json.form.preferences,
      preferredStartDate: LocalDate.parseNullableIso(
        json.form.preferences.preferredStartDate
      )
    }
  },
  guardianDateOfDeath: LocalDate.parseNullableIso(json.guardianDateOfDeath),
  createdDate: json.createdDate ? new Date(json.createdDate) : null,
  modifiedDate: json.modifiedDate ? new Date(json.modifiedDate) : null,
  sentDate: LocalDate.parseNullableIso(json.sentDate),
  dueDate: LocalDate.parseNullableIso(json.dueDate),
  attachments: json.attachments.map(({ updated, receivedAt, ...rest }) => ({
    ...rest,
    updated: new Date(updated),
    receivedAt: new Date(receivedAt)
  }))
})

export interface ApplicationForm {
  child: ApplicationChildDetails
  guardian: ApplicationGuardian
  secondGuardian: ApplicationSecondGuardian | null
  otherPartner: ApplicationPersonBasics | null
  otherChildren: ApplicationPersonBasics[]
  preferences: ApplicationPreferences
  maxFeeAccepted: boolean
  otherInfo: string
  clubDetails: ApplicationClubDetails | null
}

export interface ApplicationFormUpdate {
  child: ApplicationChildDetailsUpdate
  guardian: ApplicationGuardianUpdate
  secondGuardian: ApplicationSecondGuardian | null
  otherPartner: ApplicationPersonBasics | null
  otherChildren: ApplicationPersonBasics[]
  preferences: ApplicationPreferences
  maxFeeAccepted: boolean
  otherInfo: string
  clubDetails: ApplicationClubDetails | null
}

export interface ApplicationPersonBasics {
  firstName: string
  lastName: string
  socialSecurityNumber: string | null
}

export interface ApplicationAddress {
  street: string
  postalCode: string
  postOffice: string
}

export type ApplicationFutureAddress = ApplicationAddress & {
  movingDate: LocalDate | null
}

export interface ApplicationChildDetails {
  person: ApplicationPersonBasics
  dateOfBirth: LocalDate | null
  address: ApplicationAddress | null
  futureAddress: ApplicationFutureAddress | null
  nationality: string
  language: string
  allergies: string
  diet: string
  assistanceNeeded: boolean
  assistanceDescription: string
}

export interface ApplicationChildDetailsUpdate {
  futureAddress: ApplicationFutureAddress | null
  allergies: string
  diet: string
  assistanceNeeded: boolean
  assistanceDescription: string
}

export interface ApplicationGuardian {
  person: ApplicationPersonBasics
  address: ApplicationAddress | null
  futureAddress: ApplicationFutureAddress | null
  phoneNumber: string
  email: string
  noEmail: boolean | null
}

export interface ApplicationGuardianUpdate {
  futureAddress: ApplicationFutureAddress | null
  phoneNumber: string
  email: string
}

export interface ApplicationSecondGuardian {
  phoneNumber: string
  email: string
  agreementStatus: OtherGuardianAgreementStatus
}

export interface ApplicationPreferences {
  preferredUnits: PreferredUnit[]
  preferredStartDate: LocalDate | null
  serviceNeed: ApplicationServiceNeed | null
  siblingBasis: { siblingName: string; siblingSsn: string } | null
  preparatory: boolean
  urgent: boolean
}

export interface PreferredUnit {
  id: string
  name: string
}

export interface ApplicationServiceNeed {
  startTime: string
  endTime: string
  shiftCare: boolean
  partTime: boolean
  serviceNeedOption: ServiceNeedOption | null
}

export interface ApplicationClubDetails {
  wasOnDaycare: boolean
  wasOnClubCare: boolean
}

export interface ApplicationAttachment extends Attachment {
  updated: Date
  receivedAt: Date
  type: AttachmentType
  uploadedByEmployee?: UUID
  uploadedByPerson?: UUID
}
