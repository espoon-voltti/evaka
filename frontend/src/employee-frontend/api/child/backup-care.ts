// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import {
  BackupCareUpdateRequest,
  ChildBackupCareResponse,
  ChildBackupCaresResponse
} from 'lib-common/generated/api-types/backupcare'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'

import { client } from '../client'

export async function getChildBackupCares(
  childId: UUID
): Promise<Result<ChildBackupCareResponse[]>> {
  return client
    .get<JsonOf<ChildBackupCaresResponse>>(`/children/${childId}/backup-cares`)
    .then((res) =>
      Success.of(
        res.data.backupCares.map((x) => ({
          ...x,
          backupCare: {
            ...x.backupCare,
            period: FiniteDateRange.parseJson(x.backupCare.period)
          }
        }))
      )
    )
    .catch((e) => Failure.fromError(e))
}

export interface NewBackupCare {
  unitId: UUID
  groupId?: UUID
  period: FiniteDateRange
}

interface BackupCareCreateResponse {
  id: UUID
}

export async function createBackupCare({
  childId,
  payload
}: {
  childId: UUID
  payload: NewBackupCare
}): Promise<UUID> {
  return client
    .post<
      JsonOf<BackupCareCreateResponse>
    >(`/children/${childId}/backup-cares`, payload)
    .then((res) => res.data.id)
}

export type BackupCareUpdate = {
  backupCareId: UUID
} & BackupCareUpdateRequest

// TODO: invalidates getChildBackupCares
export async function updateBackupCare({
  backupCareId,
  ...payload
}: BackupCareUpdate): Promise<void> {
  return client
    .post(`/backup-cares/${backupCareId}`, payload)
    .then(() => undefined)
}

export async function removeBackupCare(
  backupCareId: UUID
): Promise<Result<void>> {
  return client
    .delete(`/backup-cares/${backupCareId}`)
    .then(() => Success.of(undefined))
    .catch((e) => Failure.fromError(e))
}
