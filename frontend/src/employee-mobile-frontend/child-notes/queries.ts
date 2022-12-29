// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  ChildDailyNoteBody,
  ChildStickyNoteBody,
  GroupNoteBody
} from 'lib-common/generated/api-types/note'
import { mutation, query } from 'lib-common/query'
import { UUID } from 'lib-common/types'

import { childrenQuery } from '../child-attendance/queries'
import { createQueryKeys } from '../query'

import {
  createChildDailyNote,
  createChildStickyNote,
  createGroupNote,
  deleteChildDailyNote,
  deleteChildStickyNote,
  deleteGroupNote,
  getGroupNotes,
  updateChildDailyNote,
  updateChildStickyNote,
  updateGroupNote
} from './api'

const queryKeys = createQueryKeys('notes', {
  ofGroup: (groupId: UUID) => ['group', groupId]
})

export const groupNotesQuery = query({
  api: getGroupNotes,
  queryKey: queryKeys.ofGroup,
  options: {
    staleTime: 5 * 60 * 1000
  }
})

export const createGroupNoteMutation = mutation({
  api: ({ groupId, body }: { groupId: UUID; body: GroupNoteBody }) =>
    createGroupNote({ groupId, body }),
  invalidateQueryKeys: ({ groupId }) => [groupNotesQuery(groupId).queryKey]
})

export const updateGroupNoteMutation = mutation({
  api: ({ id, body }: { groupId: UUID; id: UUID; body: GroupNoteBody }) =>
    updateGroupNote({ id, body }),
  invalidateQueryKeys: ({ groupId }) => [groupNotesQuery(groupId).queryKey]
})

export const deleteGroupNoteMutation = mutation({
  api: ({ id }: { groupId: UUID; id: UUID }) => deleteGroupNote(id),
  invalidateQueryKeys: ({ groupId }) => [groupNotesQuery(groupId).queryKey]
})

export const createChildDailyNoteMutation = mutation({
  api: ({
    childId,
    body
  }: {
    unitId: UUID
    childId: UUID
    body: ChildDailyNoteBody
  }) =>
    createChildDailyNote({
      childId,
      body
    }),
  invalidateQueryKeys: ({ unitId }) => [childrenQuery(unitId).queryKey]
})

export const updateChildDailyNoteMutation = mutation({
  api: ({ id, body }: { unitId: UUID; id: UUID; body: ChildDailyNoteBody }) =>
    updateChildDailyNote({ id, body }),
  invalidateQueryKeys: ({ unitId }) => [childrenQuery(unitId).queryKey]
})

export const deleteChildDailyNoteMutation = mutation({
  api: ({ id }: { unitId: UUID; id: UUID }) => deleteChildDailyNote(id),
  invalidateQueryKeys: ({ unitId }) => [childrenQuery(unitId).queryKey]
})

export const createChildStickyNoteMutation = mutation({
  api: ({
    childId,
    body
  }: {
    unitId: UUID
    childId: UUID
    body: ChildStickyNoteBody
  }) => createChildStickyNote({ childId, body }),
  invalidateQueryKeys: ({ unitId }) => [childrenQuery(unitId).queryKey]
})

export const updateChildStickyNoteMutation = mutation({
  api: ({ id, body }: { unitId: UUID; id: UUID; body: ChildStickyNoteBody }) =>
    updateChildStickyNote({ id, body }),
  invalidateQueryKeys: ({ unitId }) => [childrenQuery(unitId).queryKey]
})

export const deleteChildStickyNoteMutation = mutation({
  api: ({ id }: { unitId: UUID; id: UUID }) => deleteChildStickyNote(id),
  invalidateQueryKeys: ({ unitId }) => [childrenQuery(unitId).queryKey]
})
