// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import { DaycareDailyNoteBody } from 'lib-common/generated/api-types/messaging'
import { UUID } from 'lib-common/types'
import { client } from './client'

export async function insertDaycareDailyNoteForChild(
  childId: UUID,
  body: DaycareDailyNoteBody
): Promise<Result<void>> {
  return client
    .post(`/daycare-daily-note/child/${childId}`, body)
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function updateDaycareDailyNoteForChild(
  childId: UUID,
  noteId: UUID,
  body: DaycareDailyNoteBody
): Promise<Result<void>> {
  return client
    .put(`/daycare-daily-note/child/${childId}/${noteId}`, body)
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function upsertDaycareDailyNoteForGroup(
  groupId: UUID,
  { id, ...body }: DaycareDailyNoteBody & { id: UUID | null }
): Promise<Result<void>> {
  const url = `/daycare-daily-note/group/${groupId}${id ? `/${id}` : ''}`
  return (id ? client.put(url, body) : client.post(url, body))
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function deleteDaycareDailyNote(
  noteId: UUID
): Promise<Result<void>> {
  return client
    .delete(`/daycare-daily-note/${noteId}`)
    .then(() => Success.of(undefined))
    .catch((e) => Failure.fromError(e))
}
