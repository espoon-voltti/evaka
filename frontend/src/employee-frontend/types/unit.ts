// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type FiniteDateRange from 'lib-common/finite-date-range'
import type { Daycare } from 'lib-common/generated/api-types/daycare'
import type {
  PlacementPlanConfirmationStatus,
  PlacementPlanRejectReason,
  PlacementType
} from 'lib-common/generated/api-types/placement'
import type { ServiceNeed } from 'lib-common/generated/api-types/serviceneed'
import type { EvakaUser } from 'lib-common/generated/api-types/user'
import type LocalDate from 'lib-common/local-date'
import type { UUID } from 'lib-common/types'

import type { DayOfWeek } from './index'

export interface Unit extends Omit<Daycare, 'operationDays'> {
  operationDays: DayOfWeek[] | null
}

export interface UnitFiltersType {
  startDate: LocalDate
  endDate: LocalDate
}

export interface DaycarePlacement {
  id: UUID
  child: ChildBasics
  daycare: {
    id: UUID
    name: string
  }
  groupPlacements: DaycareGroupPlacement[]
  type: PlacementType
  missingServiceNeedDays: number
  serviceNeeds: ServiceNeed[]
  startDate: LocalDate
  endDate: LocalDate
  terminationRequestedDate: LocalDate | null
  terminatedBy: EvakaUser | null
}

export interface ChildBasics {
  id: UUID
  socialSecurityNumber: string | null
  firstName: string | null
  lastName: string | null
  dateOfBirth: LocalDate
}

export interface TerminatedPlacement {
  id: UUID
  endDate: LocalDate
  type: PlacementType
  terminationRequestedDate: LocalDate
  child: ChildBasics
  terminatedBy: EvakaUser
  currentDaycareGroupName: string | null
}

export interface DaycarePlacementPlan {
  id: UUID
  unitId: UUID
  applicationId: UUID
  type: PlacementType
  period: FiniteDateRange
  preschoolDaycarePeriod: FiniteDateRange | null
  child: ChildBasics
  unitConfirmationStatus: PlacementPlanConfirmationStatus
  unitRejectReason: PlacementPlanRejectReason | null
  unitRejectOtherReason: string | null
  rejectedByCitizen: boolean
}

export interface DaycareGroupPlacement {
  id: UUID | null
  groupId: UUID | null
  groupName: string | null
  daycarePlacementId: UUID
  type: PlacementType
  startDate: LocalDate
  endDate: LocalDate
}

export interface DaycareGroupPlacementDetailed extends DaycareGroupPlacement {
  daycarePlacementStartDate: LocalDate
  daycarePlacementEndDate: LocalDate
  daycarePlacementMissingServiceNeedDays: number
  child: ChildBasics
  serviceNeeds: ServiceNeed[]
}

export const flatMapGroupPlacements = (
  daycarePlacements: DaycarePlacement[]
): DaycareGroupPlacementDetailed[] =>
  daycarePlacements.reduce((groupPlacements, daycarePlacement) => {
    daycarePlacement.groupPlacements
      .map<DaycareGroupPlacementDetailed>((groupPlacement) => ({
        ...groupPlacement,
        child: daycarePlacement.child,
        serviceNeeds: daycarePlacement.serviceNeeds,
        daycarePlacementStartDate: daycarePlacement.startDate,
        daycarePlacementEndDate: daycarePlacement.endDate,
        daycarePlacementId: daycarePlacement.id,
        daycarePlacementMissingServiceNeedDays:
          daycarePlacement.missingServiceNeedDays
      }))
      .forEach((groupPlacement) => groupPlacements.push(groupPlacement))

    return groupPlacements
  }, [] as DaycareGroupPlacementDetailed[])

export interface DaycareGroup {
  id: UUID
  daycareId: UUID
  name: string
  startDate: LocalDate
  endDate: LocalDate | null
  deletable: boolean
}

export interface DaycareGroupWithPlacements extends DaycareGroup {
  placements: DaycareGroupPlacementDetailed[]
}

export interface Occupancy {
  period: FiniteDateRange
  sum: number
  headcount: number
  caretakers?: number
  percentage?: number
}

export interface UnitChildrenCapacityFactors {
  childId: UUID
  serviceNeedFactor: number
  assistanceNeedFactor: number
}
