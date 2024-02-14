// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Daycare } from 'lib-common/generated/api-types/daycare'
import {
  ChildBasics,
  DaycarePlacementWithDetails,
  PlacementType
} from 'lib-common/generated/api-types/placement'
import { ServiceNeed } from 'lib-common/generated/api-types/serviceneed'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import { DayOfWeek } from './index'

export interface Unit extends Omit<Daycare, 'operationDays'> {
  operationDays: DayOfWeek[]
}

export interface UnitFiltersType {
  startDate: LocalDate
  endDate: LocalDate
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
  daycarePlacements: DaycarePlacementWithDetails[]
): DaycareGroupPlacementDetailed[] =>
  daycarePlacements.reduce((groupPlacements, daycarePlacement) => {
    daycarePlacement.groupPlacements
      .map<DaycareGroupPlacementDetailed>((groupPlacement) => ({
        ...groupPlacement,
        type: daycarePlacement.type,
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

export interface UnitChildrenCapacityFactors {
  childId: UUID
  serviceNeedFactor: number
  assistanceNeedFactor: number
}
