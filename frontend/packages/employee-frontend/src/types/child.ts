// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from '~types/index'
import LocalDate from '@evaka/lib-common/src/local-date'
import FiniteDateRange from '@evaka/lib-common/src/finite-date-range'

type PlacementType =
  | 'CLUB'
  | 'DAYCARE'
  | 'DAYCARE_PART_TIME'
  | 'PRESCHOOL'
  | 'PRESCHOOL_DAYCARE'
  | 'PREPARATORY'
  | 'PREPARATORY_DAYCARE'

export interface ServiceNeed {
  id: UUID
  childId: UUID
  startDate: LocalDate
  endDate: LocalDate | null
  hoursPerWeek: number
  partDay: boolean
  partWeek: boolean
  shiftCare: boolean
  temporary: boolean
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
  }
  missingServiceNeedDays: number
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
