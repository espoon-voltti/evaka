// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  AttachmentType,
  ApplicationGuardianAgreementStatus,
  ApplicationOrigin,
  ApplicationStatus,
  ApplicationType
} from './enums'
import { JsonOf } from '../../json'
import LocalDate from '@evaka/lib-common/src/local-date'
import { UUID } from '@evaka/lib-common/src/types'

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
  otherGuardianId?: string
  childRestricted: boolean
  guardianRestricted: boolean
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
  createdDate: json.createdDate ? new Date(json.createdDate) : null,
  modifiedDate: json.modifiedDate ? new Date(json.modifiedDate) : null,
  sentDate: LocalDate.parseNullableIso(json.sentDate),
  dueDate: LocalDate.parseNullableIso(json.dueDate),
  attachments: json.attachments.map(({ updated, ...rest }) => ({
    ...rest,
    updated: new Date(updated)
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
}

export interface ApplicationGuardianUpdate {
  futureAddress: ApplicationFutureAddress | null
  phoneNumber: string
  email: string
}

export interface ApplicationSecondGuardian {
  phoneNumber: string
  email: string
  agreementStatus: ApplicationGuardianAgreementStatus
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
}

export interface ApplicationClubDetails {
  wasOnDaycare: boolean
  wasOnClubCare: boolean
}

export interface ApplicationAttachment {
  id: string
  name: string
  contentType: string
  updated: Date
  type: AttachmentType
}

export interface FileObject {
  id: UUID
  key: number
  file: File | undefined
  name: string
  contentType: string
  error: 'FILE_TOO_LARGE' | 'SERVER_ERROR' | undefined
  progress: number
}
