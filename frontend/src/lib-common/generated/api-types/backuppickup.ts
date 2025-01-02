// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { BackupPickupId } from './shared'
import { PersonId } from './shared'

/**
* Generated from fi.espoo.evaka.backuppickup.ChildBackupPickup
*/
export interface ChildBackupPickup {
  childId: PersonId
  id: BackupPickupId
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
  id: BackupPickupId
}
