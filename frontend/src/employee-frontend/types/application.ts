// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from '../types/index'
import LocalDate from 'lib-common/local-date'
import { Decision } from '../types/decision'
import { PersonDetails } from '../types/person'
import {
  PlacementPlanConfirmationStatus,
  PlacementPlanRejectReason
} from '../types/unit'
import { PlacementType } from '../types/child'
import { VoucherApplicationFilter } from '../state/application-ui'
import {
  ApplicationAttachment,
  ApplicationDetails
} from 'lib-common/api-types/application/ApplicationDetails'
import {
  ApplicationOrigin,
  ApplicationStatus
} from 'lib-common/api-types/application/enums'

export interface ApplicationSummary {
  applicationId: UUID
  childId: UUID
  guardianId: UUID
  childName: string
  childSsn: string
  guardianName: string
  startDate: LocalDate
  sentDate: LocalDate | null
  preferredUnitId: UUID
  preferredUnitName: string
  type: string
  status: string
  connectedDaycare: boolean
  preparatoryEducation: boolean
}

export interface ApplicationResponse {
  application: ApplicationDetails
  decisions: Decision[]
  guardians: PersonDetails[]
  attachments: ApplicationAttachment[]
}

export type SortByApplications =
  | 'APPLICATION_TYPE'
  | 'CHILD_NAME'
  | 'DUE_DATE'
  | 'START_DATE'
  | 'STATUS'

export interface ApplicationListSummary {
  id: UUID
  firstName: string
  lastName: string
  socialSecurityNumber: string
  dateOfBirth: LocalDate | null
  type: string
  placementType: PlacementType
  dueDate: LocalDate | null
  startDate: LocalDate | null
  preferredUnits: Unit[]
  origin: ApplicationOrigin
  transferApplication: boolean
  checkedByAdmin: boolean
  status: ApplicationStatus
  additionalInfo: boolean
  siblingBasis: boolean
  assistanceNeed: boolean
  wasOnClubCare: boolean | null
  wasOnDaycare: boolean | null
  extendedCare: boolean
  duplicateApplication: boolean
  placementProposalStatus: PlacementProposalStatus | null
  placementProposalUnitName: string | null
  currentPlacementUnit: Unit | null
}

interface PlacementProposalStatus {
  unitConfirmationStatus: PlacementPlanConfirmationStatus
  unitRejectReason: PlacementPlanRejectReason | null
  unitRejectOtherReason: string | null
}

interface Unit {
  id: UUID
  name: string
}

export interface ApplicationSearchParams {
  area?: string
  units?: string
  basis?: string
  type: string
  preschoolType?: string
  status?: string
  dateType?: string
  distinctions?: string
  periodStart?: string
  periodEnd?: string
  searchTerms?: string
  transferApplications?: string
  voucherApplications: VoucherApplicationFilter
}

export type ApplicationSummaryStatus =
  | 'SENT'
  | 'WAITING_PLACEMENT'
  | 'WAITING_DECISION'
  | 'WAITING_UNIT_CONFIRMATION'
  | 'WAITING_MAILING'
  | 'WAITING_CONFIRMATION'
  | 'REJECTED'
  | 'ACTIVE'
  | 'CANCELLED'

export interface ApplicationNote {
  id: UUID
  applicationId: UUID
  text: string
  created: Date
  createdBy: UUID
  createdByName: string
  updated: Date
  updatedBy: UUID | null
  updatedByName: string | null
}
