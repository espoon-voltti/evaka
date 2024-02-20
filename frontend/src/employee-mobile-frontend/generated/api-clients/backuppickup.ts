// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { ChildBackupPickup } from 'lib-common/generated/api-types/backuppickup'
import { ChildBackupPickupContent } from 'lib-common/generated/api-types/backuppickup'
import { ChildBackupPickupCreateResponse } from 'lib-common/generated/api-types/backuppickup'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'
import { client } from '../../client'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.backuppickup.BackupPickupController.createForChild
*/
export async function createForChild(
  request: {
    childId: UUID,
    body: ChildBackupPickupContent
  }
): Promise<ChildBackupPickupCreateResponse> {
  const { data: json } = await client.request<JsonOf<ChildBackupPickupCreateResponse>>({
    url: uri`/children/${request.childId}/backup-pickups`.toString(),
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
    id: UUID
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/backup-pickups/${request.id}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.backuppickup.BackupPickupController.getForChild
*/
export async function getForChild(
  request: {
    childId: UUID
  }
): Promise<ChildBackupPickup[]> {
  const { data: json } = await client.request<JsonOf<ChildBackupPickup[]>>({
    url: uri`/children/${request.childId}/backup-pickups`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.backuppickup.BackupPickupController.update
*/
export async function update(
  request: {
    id: UUID,
    body: ChildBackupPickupContent
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/backup-pickups/${request.id}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<ChildBackupPickupContent>
  })
  return json
}
