// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { Action } from '../action'
import type { ChildImageId } from './shared'
import type { DaycareId } from './shared'
import type { GroupId } from './shared'
import type { JsonOf } from '../../json'
import LocalDate from '../../local-date'
import type { PersonId } from './shared'
import type { PlacementType } from './placement'

/**
* Generated from fi.espoo.evaka.children.AttendanceSummary
*/
export interface AttendanceSummary {
  attendanceDays: number
}

/**
* Generated from fi.espoo.evaka.children.ChildAndPermittedActions
*/
export interface ChildAndPermittedActions {
  absenceApplicationCreationPossible: boolean
  duplicateOf: PersonId | null
  firstName: string
  group: Group | null
  id: PersonId
  imageId: ChildImageId | null
  lastName: string
  permittedActions: Action.Citizen.Child[]
  preferredName: string
  serviceApplicationCreationPossible: boolean
  unit: Unit | null
  upcomingPlacementIsCalendarOpen: boolean | null
  upcomingPlacementStartDate: LocalDate | null
  upcomingPlacementType: PlacementType | null
  upcomingPlacementUnit: Unit | null
}

/**
* Generated from fi.espoo.evaka.children.Group
*/
export interface Group {
  id: GroupId
  name: string
}

/**
* Generated from fi.espoo.evaka.children.Unit
*/
export interface Unit {
  id: DaycareId
  name: string
}


export function deserializeJsonChildAndPermittedActions(json: JsonOf<ChildAndPermittedActions>): ChildAndPermittedActions {
  return {
    ...json,
    upcomingPlacementStartDate: (json.upcomingPlacementStartDate != null) ? LocalDate.parseIso(json.upcomingPlacementStartDate) : null
  }
}
