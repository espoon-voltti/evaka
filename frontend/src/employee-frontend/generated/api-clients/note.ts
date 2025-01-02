// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { ChildDailyNote } from 'lib-common/generated/api-types/note'
import { ChildDailyNoteBody } from 'lib-common/generated/api-types/note'
import { ChildDailyNoteId } from 'lib-common/generated/api-types/shared'
import { ChildStickyNote } from 'lib-common/generated/api-types/note'
import { ChildStickyNoteBody } from 'lib-common/generated/api-types/note'
import { ChildStickyNoteId } from 'lib-common/generated/api-types/shared'
import { GroupId } from 'lib-common/generated/api-types/shared'
import { GroupNote } from 'lib-common/generated/api-types/note'
import { GroupNoteBody } from 'lib-common/generated/api-types/note'
import { GroupNoteId } from 'lib-common/generated/api-types/shared'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { NotesByGroupResponse } from 'lib-common/generated/api-types/note'
import { PersonId } from 'lib-common/generated/api-types/shared'
import { client } from '../../api/client'
import { deserializeJsonChildDailyNote } from 'lib-common/generated/api-types/note'
import { deserializeJsonChildStickyNote } from 'lib-common/generated/api-types/note'
import { deserializeJsonGroupNote } from 'lib-common/generated/api-types/note'
import { deserializeJsonNotesByGroupResponse } from 'lib-common/generated/api-types/note'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.note.NotesController.getNotesByGroup
*/
export async function getNotesByGroup(
  request: {
    groupId: GroupId
  }
): Promise<NotesByGroupResponse> {
  const { data: json } = await client.request<JsonOf<NotesByGroupResponse>>({
    url: uri`/employee/daycare-groups/${request.groupId}/notes`.toString(),
    method: 'GET'
  })
  return deserializeJsonNotesByGroupResponse(json)
}


/**
* Generated from fi.espoo.evaka.note.child.daily.ChildDailyNoteController.createChildDailyNote
*/
export async function createChildDailyNote(
  request: {
    childId: PersonId,
    body: ChildDailyNoteBody
  }
): Promise<ChildDailyNoteId> {
  const { data: json } = await client.request<JsonOf<ChildDailyNoteId>>({
    url: uri`/employee/children/${request.childId}/child-daily-notes`.toString(),
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
    noteId: ChildDailyNoteId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/child-daily-notes/${request.noteId}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.note.child.daily.ChildDailyNoteController.updateChildDailyNote
*/
export async function updateChildDailyNote(
  request: {
    noteId: ChildDailyNoteId,
    body: ChildDailyNoteBody
  }
): Promise<ChildDailyNote> {
  const { data: json } = await client.request<JsonOf<ChildDailyNote>>({
    url: uri`/employee/child-daily-notes/${request.noteId}`.toString(),
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
    childId: PersonId,
    body: ChildStickyNoteBody
  }
): Promise<ChildStickyNoteId> {
  const { data: json } = await client.request<JsonOf<ChildStickyNoteId>>({
    url: uri`/employee/children/${request.childId}/child-sticky-notes`.toString(),
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
    noteId: ChildStickyNoteId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/child-sticky-notes/${request.noteId}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.note.child.sticky.ChildStickyNoteController.updateChildStickyNote
*/
export async function updateChildStickyNote(
  request: {
    noteId: ChildStickyNoteId,
    body: ChildStickyNoteBody
  }
): Promise<ChildStickyNote> {
  const { data: json } = await client.request<JsonOf<ChildStickyNote>>({
    url: uri`/employee/child-sticky-notes/${request.noteId}`.toString(),
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
    groupId: GroupId,
    body: GroupNoteBody
  }
): Promise<GroupNoteId> {
  const { data: json } = await client.request<JsonOf<GroupNoteId>>({
    url: uri`/employee/daycare-groups/${request.groupId}/group-notes`.toString(),
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
    noteId: GroupNoteId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/group-notes/${request.noteId}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.note.group.GroupNoteController.updateGroupNote
*/
export async function updateGroupNote(
  request: {
    noteId: GroupNoteId,
    body: GroupNoteBody
  }
): Promise<GroupNote> {
  const { data: json } = await client.request<JsonOf<GroupNote>>({
    url: uri`/employee/group-notes/${request.noteId}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<GroupNoteBody>
  })
  return deserializeJsonGroupNote(json)
}
