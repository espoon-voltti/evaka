// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { ChildDailyNote } from 'lib-common/generated/api-types/note'
import type { ChildDailyNoteBody } from 'lib-common/generated/api-types/note'
import type { ChildDailyNoteId } from 'lib-common/generated/api-types/shared'
import type { ChildStickyNote } from 'lib-common/generated/api-types/note'
import type { ChildStickyNoteBody } from 'lib-common/generated/api-types/note'
import type { ChildStickyNoteId } from 'lib-common/generated/api-types/shared'
import type { GroupId } from 'lib-common/generated/api-types/shared'
import type { GroupNote } from 'lib-common/generated/api-types/note'
import type { GroupNoteBody } from 'lib-common/generated/api-types/note'
import type { GroupNoteId } from 'lib-common/generated/api-types/shared'
import type { JsonCompatible } from 'lib-common/json'
import type { JsonOf } from 'lib-common/json'
import type { PersonId } from 'lib-common/generated/api-types/shared'
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
    childId: PersonId,
    body: ChildDailyNoteBody
  }
): Promise<ChildDailyNoteId> {
  const { data: json } = await client.request<JsonOf<ChildDailyNoteId>>({
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
    noteId: ChildDailyNoteId
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
    noteId: ChildDailyNoteId,
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
    childId: PersonId,
    body: ChildStickyNoteBody
  }
): Promise<ChildStickyNoteId> {
  const { data: json } = await client.request<JsonOf<ChildStickyNoteId>>({
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
    noteId: ChildStickyNoteId
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
    noteId: ChildStickyNoteId,
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
    groupId: GroupId,
    body: GroupNoteBody
  }
): Promise<GroupNoteId> {
  const { data: json } = await client.request<JsonOf<GroupNoteId>>({
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
    noteId: GroupNoteId
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
    groupId: GroupId
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
    noteId: GroupNoteId,
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
