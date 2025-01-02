// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { BackupCareCreateResponse } from 'lib-common/generated/api-types/backupcare'
import { BackupCareId } from 'lib-common/generated/api-types/shared'
import { BackupCareUpdateRequest } from 'lib-common/generated/api-types/backupcare'
import { ChildBackupCaresResponse } from 'lib-common/generated/api-types/backupcare'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { NewBackupCare } from 'lib-common/generated/api-types/backupcare'
import { PersonId } from 'lib-common/generated/api-types/shared'
import { client } from '../../api/client'
import { deserializeJsonChildBackupCaresResponse } from 'lib-common/generated/api-types/backupcare'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.backupcare.BackupCareController.createBackupCare
*/
export async function createBackupCare(
  request: {
    childId: PersonId,
    body: NewBackupCare
  }
): Promise<BackupCareCreateResponse> {
  const { data: json } = await client.request<JsonOf<BackupCareCreateResponse>>({
    url: uri`/employee/children/${request.childId}/backup-cares`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<NewBackupCare>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.backupcare.BackupCareController.deleteBackupCare
*/
export async function deleteBackupCare(
  request: {
    id: BackupCareId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/backup-cares/${request.id}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.backupcare.BackupCareController.getChildBackupCares
*/
export async function getChildBackupCares(
  request: {
    childId: PersonId
  }
): Promise<ChildBackupCaresResponse> {
  const { data: json } = await client.request<JsonOf<ChildBackupCaresResponse>>({
    url: uri`/employee/children/${request.childId}/backup-cares`.toString(),
    method: 'GET'
  })
  return deserializeJsonChildBackupCaresResponse(json)
}


/**
* Generated from fi.espoo.evaka.backupcare.BackupCareController.updateBackupCare
*/
export async function updateBackupCare(
  request: {
    id: BackupCareId,
    body: BackupCareUpdateRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/backup-cares/${request.id}`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<BackupCareUpdateRequest>
  })
  return json
}
