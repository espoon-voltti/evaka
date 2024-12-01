// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { AxiosHeaders } from 'axios'
import { BackupCareCreateResponse } from 'lib-common/generated/api-types/backupcare'
import { BackupCareUpdateRequest } from 'lib-common/generated/api-types/backupcare'
import { ChildBackupCaresResponse } from 'lib-common/generated/api-types/backupcare'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { NewBackupCare } from 'lib-common/generated/api-types/backupcare'
import { UUID } from 'lib-common/types'
import { client } from '../../api/client'
import { deserializeJsonChildBackupCaresResponse } from 'lib-common/generated/api-types/backupcare'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.backupcare.BackupCareController.createBackupCare
*/
export async function createBackupCare(
  request: {
    childId: UUID,
    body: NewBackupCare
  },
  headers?: AxiosHeaders
): Promise<BackupCareCreateResponse> {
  const { data: json } = await client.request<JsonOf<BackupCareCreateResponse>>({
    url: uri`/employee/children/${request.childId}/backup-cares`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<NewBackupCare>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.backupcare.BackupCareController.deleteBackupCare
*/
export async function deleteBackupCare(
  request: {
    id: UUID
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/backup-cares/${request.id}`.toString(),
    method: 'DELETE',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.backupcare.BackupCareController.getChildBackupCares
*/
export async function getChildBackupCares(
  request: {
    childId: UUID
  },
  headers?: AxiosHeaders
): Promise<ChildBackupCaresResponse> {
  const { data: json } = await client.request<JsonOf<ChildBackupCaresResponse>>({
    url: uri`/employee/children/${request.childId}/backup-cares`.toString(),
    method: 'GET',
    headers
  })
  return deserializeJsonChildBackupCaresResponse(json)
}


/**
* Generated from fi.espoo.evaka.backupcare.BackupCareController.updateBackupCare
*/
export async function updateBackupCare(
  request: {
    id: UUID,
    body: BackupCareUpdateRequest
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/backup-cares/${request.id}`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<BackupCareUpdateRequest>
  })
  return json
}
