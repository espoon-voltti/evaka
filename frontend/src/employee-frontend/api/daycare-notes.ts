// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import {
  ChildDailyNoteBody,
  GroupNoteBody,
  NotesByGroupResponse
} from 'lib-common/generated/api-types/note'
import { UUID } from 'lib-common/types'
import { client } from './client'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'

export async function getNotesByGroup(
  groupId: UUID
): Promise<Result<NotesByGroupResponse>> {
  return client
    .get<JsonOf<NotesByGroupResponse>>(`/daycare-groups/${groupId}/notes`)
    .then((res) =>
      Success.of({
        childDailyNotes: res.data.childDailyNotes.map((note) => ({
          ...note,
          modifiedAt: new Date(note.modifiedAt)
        })),
        childStickyNotes: res.data.childStickyNotes.map((note) => ({
          ...note,
          modifiedAt: new Date(note.modifiedAt),
          expires: LocalDate.parseIso(note.expires)
        })),
        groupNotes: res.data.groupNotes.map((note) => ({
          ...note,
          modifiedAt: new Date(note.modifiedAt),
          expires: LocalDate.parseIso(note.expires)
        }))
      })
    )
    .catch((e) => Failure.fromError(e))
}

export async function postGroupNote(
  groupId: UUID,
  body: GroupNoteBody
): Promise<Result<void>> {
  const url = `/daycare-groups/${groupId}/group-notes`
  return client
    .post(url, body)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export async function putGroupNote(
  id: UUID,
  body: GroupNoteBody
): Promise<Result<void>> {
  const url = `/group-notes/${id}`
  return client
    .put(url, body)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export async function deleteGroupNote(id: UUID): Promise<Result<void>> {
  const url = `/group-notes/${id}`
  return client
    .delete(url)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export async function postChildDailyNote(
  childId: UUID,
  body: ChildDailyNoteBody
): Promise<Result<void>> {
  const url = `/children/${childId}/child-daily-notes`
  return client
    .post(url, body)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export async function putChildDailyNote(
  id: UUID,
  body: ChildDailyNoteBody
): Promise<Result<void>> {
  const url = `/child-daily-notes/${id}`
  return client
    .put(url, body)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export async function deleteChildDailyNote(id: UUID): Promise<Result<void>> {
  const url = `/child-daily-notes/${id}`
  return client
    .delete(url)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}
