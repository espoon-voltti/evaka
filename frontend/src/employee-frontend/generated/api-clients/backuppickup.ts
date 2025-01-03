// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { BackupPickupId } from 'lib-common/generated/api-types/shared'
import { ChildBackupPickup } from 'lib-common/generated/api-types/backuppickup'
import { ChildBackupPickupContent } from 'lib-common/generated/api-types/backuppickup'
import { ChildBackupPickupCreateResponse } from 'lib-common/generated/api-types/backuppickup'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { PersonId } from 'lib-common/generated/api-types/shared'
import { client } from '../../api/client'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.backuppickup.BackupPickupController.createBackupPickup
*/
export async function createBackupPickup(
  request: {
    childId: PersonId,
    body: ChildBackupPickupContent
  }
): Promise<ChildBackupPickupCreateResponse> {
  const { data: json } = await client.request<JsonOf<ChildBackupPickupCreateResponse>>({
    url: uri`/employee/children/${request.childId}/backup-pickups`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<ChildBackupPickupContent>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.backuppickup.BackupPickupController.deleteBackupPickup
*/
export async function deleteBackupPickup(
  request: {
    id: BackupPickupId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/backup-pickups/${request.id}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.backuppickup.BackupPickupController.getBackupPickups
*/
export async function getBackupPickups(
  request: {
    childId: PersonId
  }
): Promise<ChildBackupPickup[]> {
  const { data: json } = await client.request<JsonOf<ChildBackupPickup[]>>({
    url: uri`/employee/children/${request.childId}/backup-pickups`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.backuppickup.BackupPickupController.updateBackupPickup
*/
export async function updateBackupPickup(
  request: {
    id: BackupPickupId,
    body: ChildBackupPickupContent
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/backup-pickups/${request.id}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<ChildBackupPickupContent>
  })
  return json
}
