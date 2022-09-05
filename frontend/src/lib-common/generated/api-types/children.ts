// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier */

import HelsinkiDateTime from '../../helsinki-date-time'
import { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.children.Child
*/
export interface Child {
  firstName: string
  group: Group | null
  id: UUID
  imageId: UUID | null
  lastName: string
  preferredName: string
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
* Generated from fi.espoo.evaka.children.ChildrenResponse
*/
export interface ChildrenResponse {
  children: Child[]
}

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
* Generated from fi.espoo.evaka.children.consent.ChildConsentController.UpdateChildConsentRequest
*/
export interface UpdateChildConsentRequest {
  given: boolean | null
  type: ChildConsentType
}
