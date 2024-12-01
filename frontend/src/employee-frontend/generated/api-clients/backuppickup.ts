// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { AxiosHeaders } from 'axios'
import { ChildBackupPickup } from 'lib-common/generated/api-types/backuppickup'
import { ChildBackupPickupContent } from 'lib-common/generated/api-types/backuppickup'
import { ChildBackupPickupCreateResponse } from 'lib-common/generated/api-types/backuppickup'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'
import { client } from '../../api/client'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.backuppickup.BackupPickupController.createBackupPickup
*/
export async function createBackupPickup(
  request: {
    childId: UUID,
    body: ChildBackupPickupContent
  },
  headers?: AxiosHeaders
): Promise<ChildBackupPickupCreateResponse> {
  const { data: json } = await client.request<JsonOf<ChildBackupPickupCreateResponse>>({
    url: uri`/employee/children/${request.childId}/backup-pickups`.toString(),
    method: 'POST',
    headers,
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
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/backup-pickups/${request.id}`.toString(),
    method: 'DELETE',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.backuppickup.BackupPickupController.getBackupPickups
*/
export async function getBackupPickups(
  request: {
    childId: UUID
  },
  headers?: AxiosHeaders
): Promise<ChildBackupPickup[]> {
  const { data: json } = await client.request<JsonOf<ChildBackupPickup[]>>({
    url: uri`/employee/children/${request.childId}/backup-pickups`.toString(),
    method: 'GET',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.backuppickup.BackupPickupController.updateBackupPickup
*/
export async function updateBackupPickup(
  request: {
    id: UUID,
    body: ChildBackupPickupContent
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/backup-pickups/${request.id}`.toString(),
    method: 'PUT',
    headers,
    data: request.body satisfies JsonCompatible<ChildBackupPickupContent>
  })
  return json
}
