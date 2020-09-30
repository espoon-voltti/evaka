// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from '~types'
import { Failure, Result, Success } from '~api/index'
import { ChildBackupCare, UnitBackupCare } from '~types/child'
import { client } from '~api/client'
import { JsonOf } from '@evaka/lib-common/src/json'
import LocalDate from '@evaka/lib-common/src/local-date'

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
      Success(
        res.data.backupCares.map((x) => ({
          ...x,
          period: {
            start: LocalDate.parseIso(x.period.start),
            end: LocalDate.parseIso(x.period.end)
          }
        }))
      )
    )
    .catch(Failure)
}

interface NewBackupCare {
  unitId: UUID
  groupId?: UUID
  period: {
    start: LocalDate
    end: LocalDate
  }
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
    .then((res) => Success(res.data.id))
    .catch(Failure)
}

interface BackupCareUpdateRequest {
  period: {
    start: LocalDate
    end: LocalDate
  }
  groupId?: UUID
}

export async function updateBackupCare(
  backupCareId: UUID,
  payload: BackupCareUpdateRequest
): Promise<Result<void>> {
  return client
    .post(`/backup-cares/${backupCareId}`, payload)
    .then(() => Success(undefined))
    .catch(Failure)
}

export async function removeBackupCare(
  backupCareId: UUID
): Promise<Result<void>> {
  return client
    .delete(`/backup-cares/${backupCareId}`)
    .then(() => Success(undefined))
    .catch(Failure)
}
