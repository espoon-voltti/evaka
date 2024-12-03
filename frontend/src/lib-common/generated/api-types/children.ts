// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { Action } from '../action'
import { ChildImageId } from './shared'
import { DaycareId } from './shared'
import { GroupId } from './shared'
import { PersonId } from './shared'
import { PlacementType } from './placement'

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
  duplicateOf: PersonId | null
  firstName: string
  group: Group | null
  hasPedagogicalDocuments: boolean
  id: PersonId
  imageId: ChildImageId | null
  lastName: string
  permittedActions: Action.Citizen.Child[]
  preferredName: string
  serviceApplicationCreationPossible: boolean
  unit: Unit | null
  upcomingPlacementType: PlacementType | null
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
