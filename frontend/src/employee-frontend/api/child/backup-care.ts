// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from '../../types'
import { Failure, Result, Success } from 'lib-common/api'
import { ChildBackupCare, UnitBackupCare } from '../../types/child'
import { client } from '../../api/client'
import { JsonOf } from 'lib-common/json'
import FiniteDateRange from 'lib-common/finite-date-range'

interface BackupCaresResponse<T extends ChildBackupCare | UnitBackupCare> {
  backupCares: T[]
}

export async function getChildBackupCares(
  childId: UUID
): Promise<Result<ChildBackupCare[]>> {
  return client
    .get<JsonOf<BackupCaresResponse<ChildBackupCare>>>(
      `/children/${childId}/backup-cares`
    )
    .then((res) =>
      Success.of(
        res.data.backupCares.map((x) => ({
          ...x,
          period: FiniteDateRange.parseJson(x.period)
        }))
      )
    )
    .catch((e) => Failure.fromError(e))
}

interface NewBackupCare {
  unitId: UUID
  groupId?: UUID
  period: FiniteDateRange
}

interface BackupCareCreateResponse {
  id: UUID
}

export async function createBackupCare(
  childId: UUID,
  payload: NewBackupCare
): Promise<Result<UUID>> {
  return client
    .post<JsonOf<BackupCareCreateResponse>>(
      `/children/${childId}/backup-cares`,
      payload
    )
    .then((res) => Success.of(res.data.id))
    .catch((e) => Failure.fromError(e))
}

interface BackupCareUpdateRequest {
  period: FiniteDateRange
  groupId?: UUID
}

export async function updateBackupCare(
  backupCareId: UUID,
  payload: BackupCareUpdateRequest
): Promise<Result<void>> {
  return client
    .post(`/backup-cares/${backupCareId}`, payload)
    .then(() => Success.of(undefined))
    .catch((e) => Failure.fromError(e))
}

export async function removeBackupCare(
  backupCareId: UUID
): Promise<Result<void>> {
  return client
    .delete(`/backup-cares/${backupCareId}`)
    .then(() => Success.of(undefined))
    .catch((e) => Failure.fromError(e))
}
