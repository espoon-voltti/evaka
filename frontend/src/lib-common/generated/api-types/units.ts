// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier */

import { Action } from '../action'
import { Caretakers } from './daycare'
import { DaycareGroup } from './daycare'
import { DaycarePlacementWithDetails } from './placement'
import { MissingGroupPlacement } from './placement'
import { OccupancyResponse } from './occupancy'
import { TerminatedPlacement } from './placement'
import { UnitBackupCare } from './backupcare'
import { UnitChildrenCapacityFactors } from './placement'

/**
* Generated from fi.espoo.evaka.units.GroupCaretakers
*/
export interface GroupCaretakers {
  groupCaretakers: Record<string, Caretakers>
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
  backupCares: UnitBackupCare[]
  caretakers: GroupCaretakers
  groupOccupancies: GroupOccupancies | null
  groups: DaycareGroup[]
  missingGroupPlacements: MissingGroupPlacement[]
  permittedBackupCareActions: Record<string, Action.BackupCare[]>
  permittedGroupPlacementActions: Record<string, Action.GroupPlacement[]>
  permittedPlacementActions: Record<string, Action.Placement[]>
  placements: DaycarePlacementWithDetails[]
  recentlyTerminatedPlacements: TerminatedPlacement[]
  unitChildrenCapacityFactors: UnitChildrenCapacityFactors[]
}
