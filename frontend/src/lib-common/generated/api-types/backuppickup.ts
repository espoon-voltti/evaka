// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier, @typescript-eslint/no-namespace, @typescript-eslint/no-redundant-type-constituents */

import { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.backuppickup.ChildBackupPickup
*/
export interface ChildBackupPickup {
  childId: UUID
  id: UUID
  name: string
  phone: string
}

/**
* Generated from fi.espoo.evaka.backuppickup.ChildBackupPickupContent
*/
export interface ChildBackupPickupContent {
  name: string
  phone: string
}

/**
* Generated from fi.espoo.evaka.backuppickup.ChildBackupPickupCreateResponse
*/
export interface ChildBackupPickupCreateResponse {
  id: UUID
}
