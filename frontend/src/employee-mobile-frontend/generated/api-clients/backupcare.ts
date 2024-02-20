// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import LocalDate from 'lib-common/local-date'
import { BackupCareCreateResponse } from 'lib-common/generated/api-types/backupcare'
import { BackupCareUpdateRequest } from 'lib-common/generated/api-types/backupcare'
import { ChildBackupCaresResponse } from 'lib-common/generated/api-types/backupcare'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { NewBackupCare } from 'lib-common/generated/api-types/backupcare'
import { UUID } from 'lib-common/types'
import { UnitBackupCaresResponse } from 'lib-common/generated/api-types/backupcare'
import { client } from '../../client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonChildBackupCaresResponse } from 'lib-common/generated/api-types/backupcare'
import { deserializeJsonUnitBackupCaresResponse } from 'lib-common/generated/api-types/backupcare'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.backupcare.BackupCareController.createForChild
*/
export async function createForChild(
  request: {
    childId: UUID,
    body: NewBackupCare
  }
): Promise<BackupCareCreateResponse> {
  const { data: json } = await client.request<JsonOf<BackupCareCreateResponse>>({
    url: uri`/children/${request.childId}/backup-cares`.toString(),
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
    id: UUID
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/backup-cares/${request.id}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.backupcare.BackupCareController.getForChild
*/
export async function getForChild(
  request: {
    childId: UUID
  }
): Promise<ChildBackupCaresResponse> {
  const { data: json } = await client.request<JsonOf<ChildBackupCaresResponse>>({
    url: uri`/children/${request.childId}/backup-cares`.toString(),
    method: 'GET'
  })
  return deserializeJsonChildBackupCaresResponse(json)
}


/**
* Generated from fi.espoo.evaka.backupcare.BackupCareController.getForDaycare
*/
export async function getForDaycare(
  request: {
    daycareId: UUID,
    startDate: LocalDate,
    endDate: LocalDate
  }
): Promise<UnitBackupCaresResponse> {
  const params = createUrlSearchParams(
    ['startDate', request.startDate.formatIso()],
    ['endDate', request.endDate.formatIso()]
  )
  const { data: json } = await client.request<JsonOf<UnitBackupCaresResponse>>({
    url: uri`/daycares/${request.daycareId}/backup-cares`.toString(),
    method: 'GET',
    params
  })
  return deserializeJsonUnitBackupCaresResponse(json)
}


/**
* Generated from fi.espoo.evaka.backupcare.BackupCareController.update
*/
export async function update(
  request: {
    id: UUID,
    body: BackupCareUpdateRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/backup-cares/${request.id}`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<BackupCareUpdateRequest>
  })
  return json
}
