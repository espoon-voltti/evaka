// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier */

import { Action } from '../action'
import { ApplicationUnitSummary } from './application'
import { DaycareGroup } from './daycare'
import { DaycarePlacementWithDetails } from './placement'
import { MissingGroupPlacement } from './placement'
import { OccupancyResponse } from './occupancy'
import { PlacementPlanDetails } from './placement'
import { RealtimeOccupancy } from './occupancy'
import { Stats } from './daycare'
import { TerminatedPlacements } from './placement'
import { UnitBackupCare } from './backupcare'
import { UnitChildrenCapacityFactors } from './placement'

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
