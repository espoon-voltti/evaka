// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { ChildDailyNote } from 'lib-common/generated/api-types/note'
import { ChildDailyNoteBody } from 'lib-common/generated/api-types/note'
import { ChildStickyNote } from 'lib-common/generated/api-types/note'
import { ChildStickyNoteBody } from 'lib-common/generated/api-types/note'
import { GroupNote } from 'lib-common/generated/api-types/note'
import { GroupNoteBody } from 'lib-common/generated/api-types/note'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'
import { client } from '../../client'
import { deserializeJsonChildDailyNote } from 'lib-common/generated/api-types/note'
import { deserializeJsonChildStickyNote } from 'lib-common/generated/api-types/note'
import { deserializeJsonGroupNote } from 'lib-common/generated/api-types/note'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.note.child.daily.ChildDailyNoteController.createChildDailyNote
*/
export async function createChildDailyNote(
  request: {
    childId: UUID,
    body: ChildDailyNoteBody
  }
): Promise<UUID> {
  const { data: json } = await client.request<JsonOf<UUID>>({
    url: uri`/employee-mobile/children/${request.childId}/child-daily-notes`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<ChildDailyNoteBody>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.note.child.daily.ChildDailyNoteController.deleteChildDailyNote
*/
export async function deleteChildDailyNote(
  request: {
    noteId: UUID
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee-mobile/child-daily-notes/${request.noteId}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.note.child.daily.ChildDailyNoteController.updateChildDailyNote
*/
export async function updateChildDailyNote(
  request: {
    noteId: UUID,
    body: ChildDailyNoteBody
  }
): Promise<ChildDailyNote> {
  const { data: json } = await client.request<JsonOf<ChildDailyNote>>({
    url: uri`/employee-mobile/child-daily-notes/${request.noteId}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<ChildDailyNoteBody>
  })
  return deserializeJsonChildDailyNote(json)
}


/**
* Generated from fi.espoo.evaka.note.child.sticky.ChildStickyNoteController.createChildStickyNote
*/
export async function createChildStickyNote(
  request: {
    childId: UUID,
    body: ChildStickyNoteBody
  }
): Promise<UUID> {
  const { data: json } = await client.request<JsonOf<UUID>>({
    url: uri`/employee-mobile/children/${request.childId}/child-sticky-notes`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<ChildStickyNoteBody>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.note.child.sticky.ChildStickyNoteController.deleteChildStickyNote
*/
export async function deleteChildStickyNote(
  request: {
    noteId: UUID
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee-mobile/child-sticky-notes/${request.noteId}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.note.child.sticky.ChildStickyNoteController.updateChildStickyNote
*/
export async function updateChildStickyNote(
  request: {
    noteId: UUID,
    body: ChildStickyNoteBody
  }
): Promise<ChildStickyNote> {
  const { data: json } = await client.request<JsonOf<ChildStickyNote>>({
    url: uri`/employee-mobile/child-sticky-notes/${request.noteId}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<ChildStickyNoteBody>
  })
  return deserializeJsonChildStickyNote(json)
}


/**
* Generated from fi.espoo.evaka.note.group.GroupNoteController.createGroupNote
*/
export async function createGroupNote(
  request: {
    groupId: UUID,
    body: GroupNoteBody
  }
): Promise<UUID> {
  const { data: json } = await client.request<JsonOf<UUID>>({
    url: uri`/employee-mobile/daycare-groups/${request.groupId}/group-notes`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<GroupNoteBody>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.note.group.GroupNoteController.deleteGroupNote
*/
export async function deleteGroupNote(
  request: {
    noteId: UUID
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee-mobile/group-notes/${request.noteId}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.note.group.GroupNoteController.getGroupNotes
*/
export async function getGroupNotes(
  request: {
    groupId: UUID
  }
): Promise<GroupNote[]> {
  const { data: json } = await client.request<JsonOf<GroupNote[]>>({
    url: uri`/employee-mobile/daycare-groups/${request.groupId}/group-notes`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonGroupNote(e))
}


/**
* Generated from fi.espoo.evaka.note.group.GroupNoteController.updateGroupNote
*/
export async function updateGroupNote(
  request: {
    noteId: UUID,
    body: GroupNoteBody
  }
): Promise<GroupNote> {
  const { data: json } = await client.request<JsonOf<GroupNote>>({
    url: uri`/employee-mobile/group-notes/${request.noteId}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<GroupNoteBody>
  })
  return deserializeJsonGroupNote(json)
}
