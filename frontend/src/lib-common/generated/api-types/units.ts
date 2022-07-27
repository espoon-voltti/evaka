// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier */

import type { Action } from '../action'
import type { ApplicationUnitSummary } from './application'
import type { DaycareGroup } from './daycare'
import type { DaycarePlacementWithDetails } from './placement'
import type { MissingGroupPlacement } from './placement'
import type { OccupancyResponse } from './occupancy'
import type { PlacementPlanDetails } from './placement'
import type { RealtimeOccupancy } from './occupancy'
import type { Stats } from './daycare'
import type { TerminatedPlacements } from './placement'
import type { UnitBackupCare } from './backupcare'
import type { UnitChildrenCapacityFactors } from './placement'

/**
* Generated from fi.espoo.evaka.units.Caretakers
*/
export interface Caretakers {
  groupCaretakers: Record<string, Stats>
  unitCaretakers: Stats
}

/**
* Generated from fi.espoo.evaka.units.GroupOccupancies
*/
export interface GroupOccupancies {
  confirmed: Record<string, OccupancyResponse>
  realized: Record<string, OccupancyResponse>
}

/**
* Generated from fi.espoo.evaka.units.UnitDataResponse
*/
export interface UnitDataResponse {
  applications: ApplicationUnitSummary[] | null
  backupCares: UnitBackupCare[]
  caretakers: Caretakers
  groupOccupancies: GroupOccupancies | null
  groups: DaycareGroup[]
  missingGroupPlacements: MissingGroupPlacement[]
  permittedBackupCareActions: Record<string, Action.BackupCare[]>
  permittedGroupPlacementActions: Record<string, Action.GroupPlacement[]>
  permittedPlacementActions: Record<string, Action.Placement[]>
  placementPlans: PlacementPlanDetails[] | null
  placementProposals: PlacementPlanDetails[] | null
  placements: DaycarePlacementWithDetails[]
  recentlyTerminatedPlacements: TerminatedPlacements[]
  unitChildrenCapacityFactors: UnitChildrenCapacityFactors[]
  unitOccupancies: UnitOccupancies | null
}

/**
* Generated from fi.espoo.evaka.units.UnitOccupancies
*/
export interface UnitOccupancies {
  confirmed: OccupancyResponse
  planned: OccupancyResponse
  realized: OccupancyResponse
  realtime: RealtimeOccupancy | null
}
