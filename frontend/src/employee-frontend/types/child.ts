// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from '../types/index'
import LocalDate from 'lib-common/local-date'
import FiniteDateRange from 'lib-common/finite-date-range'
import { ProviderType } from './unit'
import { AssistanceMeasure } from 'lib-customizations/types'
import {
  ServiceNeedOptionSummary,
  PlacementType
} from 'lib-common/api-types/serviceNeed/common'

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

export type { AssistanceMeasure }

export interface AssistanceAction {
  id: UUID
  childId: UUID
  startDate: LocalDate
  endDate: LocalDate
  actions: Set<string>
  otherAction: string
  measures: Set<AssistanceMeasure>
}

export interface AssistanceActionOption {
  value: string
  nameFi: string
}

export interface AdditionalInformation {
  allergies: string
  diet: string
  additionalInfo: string
  preferredName: string | null
  medication: string
}

export interface ServiceNeedOption {
  id: UUID
  name: string
  validPlacementType: PlacementType
  defaultOption: boolean
  feeCoefficient: number
  voucherValueCoefficient: number
  occupancyCoefficient: number
  daycareHoursPerWeek: number
  partDay: boolean
  partWeek: boolean
}

export interface NewServiceNeed {
  id: UUID
  placementId: UUID
  startDate: LocalDate
  endDate: LocalDate
  option: ServiceNeedOptionSummary
  shiftCare: boolean
  confirmed: {
    employeeId: UUID
    firstName: string
    lastName: string
    at: Date
  }
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
  missingNewServiceNeedDays: number
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
  missingNewServiceNeedDays: number
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
