// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from '../../types'
import { Failure, Result, Success } from 'lib-common/api'
import { ChildBackupPickup } from '../../types/child'
import { client } from '../client'
import { JsonOf } from 'lib-common/json'

interface ChildBackupPickupContent {
  name: string
  phone: string
}

interface ChildBackupPickupCreateResponse {
  id: UUID
}

export async function createBackupPickup(
  childId: UUID,
  payload: ChildBackupPickupContent
): Promise<Result<UUID>> {
  return client
    .post<JsonOf<ChildBackupPickupCreateResponse>>(
      `/children/${childId}/backup-pickups`,
      payload
    )
    .then((res) => Success.of(res.data.id))
    .catch((e) => Failure.fromError(e))
}

export async function getChildBackupPickups(
  childId: UUID
): Promise<Result<ChildBackupPickup[]>> {
  return client
    .get<JsonOf<ChildBackupPickup[]>>(`/children/${childId}/backup-pickups`)
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function updateBackupPickup(
  backupPickupId: UUID,
  payload: ChildBackupPickupContent
): Promise<Result<void>> {
  return client
    .put(`/backup-pickups/${backupPickupId}`, payload)
    .then(() => Success.of(undefined))
    .catch((e) => Failure.fromError(e))
}

export async function removeBackupPickup(
  backupPickupId: UUID
): Promise<Result<void>> {
  return client
    .delete(`/backup-pickups/${backupPickupId}`)
    .then(() => Success.of(undefined))
    .catch((e) => Failure.fromError(e))
}
