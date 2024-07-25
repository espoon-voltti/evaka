// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { DaycareGroup } from 'lib-common/generated/api-types/daycare'
import {
  ChildBasics,
  DaycarePlacementWithDetails,
  PlacementType
} from 'lib-common/generated/api-types/placement'
import { ServiceNeed } from 'lib-common/generated/api-types/serviceneed'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

export interface DaycareGroupPlacementDetailed {
  id: UUID | null
  groupId: UUID | null
  groupName: string | null
  daycarePlacementId: UUID
  type: PlacementType
  startDate: LocalDate
  endDate: LocalDate
  daycarePlacementStartDate: LocalDate
  daycarePlacementEndDate: LocalDate
  daycarePlacementMissingServiceNeedDays: number
  child: ChildBasics
  serviceNeeds: ServiceNeed[]
  defaultServiceNeedOptionNameFi: string | null
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
        defaultServiceNeedOptionNameFi:
          daycarePlacement.defaultServiceNeedOptionNameFi,
        daycarePlacementStartDate: daycarePlacement.startDate,
        daycarePlacementEndDate: daycarePlacement.endDate,
        daycarePlacementId: daycarePlacement.id,
        daycarePlacementMissingServiceNeedDays:
          daycarePlacement.missingServiceNeedDays
      }))
      .forEach((groupPlacement) => groupPlacements.push(groupPlacement))

    return groupPlacements
  }, [] as DaycareGroupPlacementDetailed[])

export interface DaycareGroupWithPlacements extends DaycareGroup {
  placements: DaycareGroupPlacementDetailed[]
}

export interface UnitChildrenCapacityFactors {
  childId: UUID
  serviceNeedFactor: number
  assistanceNeedFactor: number
}
