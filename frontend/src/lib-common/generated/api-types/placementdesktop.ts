// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import LocalDate from '../../local-date'
import { ApplicationId } from './shared'
import { DaycareId } from './shared'
import { JsonOf } from '../../json'
import { PlacementType } from './placement'

/**
* Generated from fi.espoo.evaka.placementdesktop.PlacementApplication
*/
export interface PlacementApplication {
  assistanceNeed: string | null
  dateOfBirth: LocalDate
  firstName: string
  id: ApplicationId
  lastName: string
  otherPreferences: DaycareId[]
  placementType: PlacementType
  plannedPlacementUnit: DaycareId | null
  preferredUnits: DaycareId[]
  primaryPreference: DaycareId
  serviceEnd: string
  serviceStart: string
  trialPlacementUnit: DaycareId | null
}

/**
* Generated from fi.espoo.evaka.placementdesktop.PlacementDaycare
*/
export interface PlacementDaycare {
  capacity: number
  careAreaName: string
  childOccupancyConfirmed: number
  childOccupancyPlanned: number
  id: DaycareId
  name: string
  serviceWorkerNote: string | null
}

/**
* Generated from fi.espoo.evaka.placementdesktop.PlacementDesktopController.TrialPlacementUnitRequest
*/
export interface TrialPlacementUnitRequest {
  trialPlacementUnit: DaycareId | null
}


export function deserializeJsonPlacementApplication(json: JsonOf<PlacementApplication>): PlacementApplication {
  return {
    ...json,
    dateOfBirth: LocalDate.parseIso(json.dateOfBirth)
  }
}
