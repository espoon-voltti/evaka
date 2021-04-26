// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from '../types/index'
import LocalDate from 'lib-common/local-date'
import FiniteDateRange from 'lib-common/finite-date-range'
import { ProviderType } from './unit'

export type PlacementType =
  | 'CLUB'
  | 'DAYCARE'
  | 'DAYCARE_PART_TIME'
  | 'DAYCARE_FIVE_YEAR_OLDS'
  | 'DAYCARE_PART_TIME_FIVE_YEAR_OLDS'
  | 'PRESCHOOL'
  | 'PRESCHOOL_DAYCARE'
  | 'PREPARATORY'
  | 'PREPARATORY_DAYCARE'
  | 'TEMPORARY_DAYCARE'
  | 'TEMPORARY_DAYCARE_PART_DAY'

export interface ServiceNeed {
  id: UUID
  childId: UUID
  startDate: LocalDate
  endDate: LocalDate | null
  hoursPerWeek: number
  partDay: boolean
  partWeek: boolean
  shiftCare: boolean
  notes: string
  updated: Date
  updatedByName: string
}

export type AssistanceBasis =
  | 'AUTISM'
  | 'DEVELOPMENTAL_DISABILITY_1'
  | 'DEVELOPMENTAL_DISABILITY_2'
  | 'FOCUS_CHALLENGE'
  | 'LINGUISTIC_CHALLENGE'
  | 'DEVELOPMENT_MONITORING'
  | 'DEVELOPMENT_MONITORING_PENDING'
  | 'MULTI_DISABILITY'
  | 'LONG_TERM_CONDITION'
  | 'REGULATION_SKILL_CHALLENGE'
  | 'DISABILITY'
  | 'OTHER'

export interface AssistanceNeed {
  id: UUID
  childId: UUID
  startDate: LocalDate
  endDate: LocalDate
  capacityFactor: number
  description: string
  bases: Set<AssistanceBasis>
  otherBasis: string
}

export type AssistanceActionType =
  | 'ASSISTANCE_SERVICE_CHILD'
  | 'ASSISTANCE_SERVICE_UNIT'
  | 'SMALLER_GROUP'
  | 'SPECIAL_GROUP'
  | 'PERVASIVE_VEO_SUPPORT'
  | 'RESOURCE_PERSON'
  | 'RATIO_DECREASE'
  | 'PERIODICAL_VEO_SUPPORT'
  | 'OTHER'

export type AssistanceMeasure =
  | 'SPECIAL_ASSISTANCE_DECISION'
  | 'INTENSIFIED_ASSISTANCE'
  | 'EXTENDED_COMPULSORY_EDUCATION'
  | 'CHILD_SERVICE'
  | 'CHILD_ACCULTURATION_SUPPORT'
  | 'TRANSPORT_BENEFIT'

export interface AssistanceAction {
  id: UUID
  childId: UUID
  startDate: LocalDate
  endDate: LocalDate
  actions: Set<AssistanceActionType>
  otherAction: string
  measures: Set<AssistanceMeasure>
}

export interface AdditionalInformation {
  allergies: string
  diet: string
  additionalInfo: string
  preferredName: string | null
  medication: string
}

interface ServiceNeedOptionSummary {
  id: UUID
  name: string
}

export interface NewServiceNeed {
  id: UUID
  placementId: UUID
  startDate: LocalDate
  endDate: LocalDate
  option: ServiceNeedOptionSummary
  shiftCare: boolean
}

export interface Placement {
  id: UUID
  startDate: LocalDate
  endDate: LocalDate
  type: PlacementType
  daycare: {
    id: UUID
    name: string
    area: string
    providerType: ProviderType
  }
  missingServiceNeedDays: number
  serviceNeeds: NewServiceNeed[]
  isRestrictedFromUser: boolean
}

export interface ChildBackupCare {
  id: UUID
  unit: {
    id: UUID
    name: string
  }
  group?: {
    id: UUID
    name: string
  }
  period: FiniteDateRange
}

export interface UnitBackupCare {
  id: UUID
  period: FiniteDateRange
  missingServiceNeedDays: number
  group?: {
    id: UUID
    name: string
  }
  child: {
    id: UUID
    firstName: string
    lastName: string
    birthDate: LocalDate
  }
}

export interface ChildBackupPickup {
  id: UUID
  childId: UUID
  name: string
  phone: string
}
