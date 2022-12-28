// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  ChildDailyNoteBody,
  ChildStickyNoteBody,
  GroupNoteBody
} from 'lib-common/generated/api-types/note'
import { UUID } from 'lib-common/types'

import { client } from '../client'

export function createGroupNote({
  groupId,
  body
}: {
  groupId: UUID
  body: GroupNoteBody
}): Promise<void> {
  return client
    .post(`/daycare-groups/${groupId}/group-notes`, body)
    .then(() => undefined)
}

export function updateGroupNote({
  id,
  body
}: {
  id: UUID
  body: GroupNoteBody
}): Promise<void> {
  return client.put(`/group-notes/${id}`, body).then(() => undefined)
}

export function deleteGroupNote(id: UUID): Promise<void> {
  return client.delete(`/group-notes/${id}`).then(() => undefined)
}

export async function createChildDailyNote({
  childId,
  body
}: {
  childId: UUID
  body: ChildDailyNoteBody
}): Promise<void> {
  return client
    .post(`/children/${childId}/child-daily-notes`, body)
    .then(() => undefined)
}

export async function updateChildDailyNote({
  id,
  body
}: {
  id: UUID
  body: ChildDailyNoteBody
}): Promise<void> {
  return client.put(`/child-daily-notes/${id}`, body).then(() => undefined)
}

export function deleteChildDailyNote(id: UUID): Promise<void> {
  return client.delete(`/child-daily-notes/${id}`).then(() => undefined)
}

export function createChildStickyNote({
  childId,
  body
}: {
  childId: UUID
  body: ChildStickyNoteBody
}): Promise<void> {
  return client
    .post(`/children/${childId}/child-sticky-notes`, body)
    .then(() => undefined)
}

export function updateChildStickyNote({
  id,
  body
}: {
  id: UUID
  body: ChildStickyNoteBody
}): Promise<void> {
  return client.put(`/child-sticky-notes/${id}`, body).then(() => undefined)
}

export function deleteChildStickyNote(id: UUID): Promise<void> {
  return client.delete(`/child-sticky-notes/${id}`).then(() => undefined)
}
