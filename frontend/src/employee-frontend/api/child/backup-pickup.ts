// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from '../../types'
import { Failure, Result, Success } from 'lib-common/api'
import { ChildBackupPickup } from '../../types/child'
import { client } from '../../api/client'
import { JsonOf } from 'lib-common/json'

export async function getChildBackupPickups(
  childId: UUID
): Promise<Result<ChildBackupPickup[]>> {
  return client
    .get<JsonOf<ChildBackupPickup[]>>(`/children/${childId}/backup-pickups`)
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

interface NewChildBackupPickup {
  childId: UUID
  name: string
  phone: string
}

interface ChildBackupPickupCreateResponse {
  id: UUID
}

export async function createBackupPickup(
  childId: UUID,
  payload: NewChildBackupPickup
): Promise<Result<UUID>> {
  return client
    .post<JsonOf<ChildBackupPickupCreateResponse>>(
      `/children/${childId}/backup-pickups`,
      payload
    )
    .then((res) => Success.of(res.data.id))
    .catch((e) => Failure.fromError(e))
}

export async function removeBackupPickup(
  backupCareId: UUID,
  childId: UUID
): Promise<Result<void>> {
  return client
    .delete(`/children/${childId}/backup-pickups/${backupCareId}`)
    .then(() => Success.of(undefined))
    .catch((e) => Failure.fromError(e))
}

export async function updateBackupPickup(
  childId: UUID,
  payload: ChildBackupPickup
): Promise<Result<void>> {
  return client
    .put(`/children/${childId}/backup-pickups`, payload)
    .then(() => Success.of(undefined))
    .catch((e) => Failure.fromError(e))
}
