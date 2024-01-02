// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier, @typescript-eslint/no-namespace, @typescript-eslint/no-redundant-type-constituents */

import HelsinkiDateTime from '../../helsinki-date-time'
import { PlacementType } from './placement'
import { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.children.AttendanceSummary
*/
export interface AttendanceSummary {
  attendanceDays: number
}

/**
* Generated from fi.espoo.evaka.children.Child
*/
export interface Child {
  duplicateOf: UUID | null
  firstName: string
  group: Group | null
  hasCurriculums: boolean
  hasPedagogicalDocuments: boolean
  id: UUID
  imageId: UUID | null
  lastName: string
  preferredName: string
  unit: Unit | null
  upcomingPlacementType: PlacementType | null
}

/**
* Generated from fi.espoo.evaka.children.consent.ChildConsent
*/
export interface ChildConsent {
  given: boolean | null
  givenAt: HelsinkiDateTime | null
  givenByEmployee: string | null
  givenByGuardian: string | null
  type: ChildConsentType
}

/**
* Generated from fi.espoo.evaka.children.consent.ChildConsentType
*/
export const childConsentTypes = [
  'EVAKA_PROFILE_PICTURE'
] as const

export type ChildConsentType = typeof childConsentTypes[number]

/**
* Generated from fi.espoo.evaka.children.consent.CitizenChildConsent
*/
export interface CitizenChildConsent {
  given: boolean | null
  type: ChildConsentType
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

/**
* Generated from fi.espoo.evaka.children.consent.ChildConsentController.UpdateChildConsentRequest
*/
export interface UpdateChildConsentRequest {
  given: boolean | null
  type: ChildConsentType
}
