// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { Action } from '../action'
import { PlacementType } from './placement'
import { UUID } from '../../types'

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
  duplicateOf: UUID | null
  firstName: string
  group: Group | null
  hasCurriculums: boolean
  hasPedagogicalDocuments: boolean
  id: UUID
  imageId: UUID | null
  lastName: string
  permittedActions: Action.Citizen.Child[]
  preferredName: string
  unit: Unit | null
  upcomingPlacementType: PlacementType | null
}

/**
* Generated from fi.espoo.evaka.children.Group
*/
export interface Group {
  id: UUID
  name: string
}

/**
* Generated from fi.espoo.evaka.children.Unit
*/
export interface Unit {
  id: UUID
  name: string
}
