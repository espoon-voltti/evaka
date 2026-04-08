// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { BackupPickupId } from './shared'
import type { PersonId } from './shared'

/**
* Generated from evaka.core.backuppickup.ChildBackupPickup
*/
export interface ChildBackupPickup {
  childId: PersonId
  id: BackupPickupId
  name: string
  phone: string
}

/**
* Generated from evaka.core.backuppickup.ChildBackupPickupContent
*/
export interface ChildBackupPickupContent {
  name: string
  phone: string
}

/**
* Generated from evaka.core.backuppickup.ChildBackupPickupCreateResponse
*/
export interface ChildBackupPickupCreateResponse {
  id: BackupPickupId
}
